/*
 * Copyright 2025 Atick Faisal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.atick.shorts.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import dev.atick.shorts.services.detectors.InstagramReelsDetector
import dev.atick.shorts.services.detectors.ShortFormContentDetector
import dev.atick.shorts.services.detectors.YouTubeShortsDetector
import dev.atick.shorts.utils.UserPreferencesProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Accessibility service that detects and blocks short-form content across supported apps.
 *
 * This service monitors accessibility events from tracked applications (YouTube, Instagram)
 * and automatically performs a back action when short-form content (Shorts, Reels) is detected.
 * Uses platform-specific detectors to identify when users are actively watching short-form content.
 */
@SuppressLint("AccessibilityPolicy")
class ShortFormContentBlockerService : AccessibilityService() {

    private val lastActionTimestamps = ConcurrentHashMap<String, Long>()
    private val actionCooldownMillis = 1500L
    private val userPreferencesProvider by lazy { UserPreferencesProvider(applicationContext) }

    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + job)

    private val detectors: Map<String, ShortFormContentDetector> by lazy {
        mapOf(
            "com.google.android.youtube" to YouTubeShortsDetector(),
            "com.instagram.android" to InstagramReelsDetector(),
        )
    }

    override fun onServiceConnected() {
        Timber.d("ShortFormContentBlockerService connected")

        userPreferencesProvider.getTrackedPackages()
            .onEach { packages ->
                Timber.d("Tracked packages updated: ${packages.joinToString()}")
                val info = AccessibilityServiceInfo().apply {
                    eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                    feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                    flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                            AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                    packageNames = packages.toTypedArray()
                    notificationTimeout = 100
                }
                serviceInfo = info
            }
            .catch { error ->
                Timber.e(error, "Error fetching tracked packages")
            }
            .launchIn(coroutineScope)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            (
                    event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                            event.contentChangeTypes and
                            AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE != 0
                    )
        ) {
            val packageName = event.packageName?.toString()
            Timber.v(
                "Accessibility event: type=${event.eventType}, " +
                        "className=${event.className}, package=$packageName",
            )

            // Get the appropriate detector for this package
            val detector = packageName?.let { detectors[it] }
            if (detector == null) {
                Timber.v("No detector found for package: $packageName")
                return
            }

            val windows = windows
            Timber.v("Inspecting ${windows.size} windows for package: $packageName")
            for (win in windows) {
                // Only process focused application windows
                if (!win.isFocused || !win.isActive) {
                    Timber.v("Skipping non-focused/inactive window")
                    continue
                }

                val root = win.root ?: continue
                if (detector.isShortFormContent(event, root, resources)) {
                    Timber.i("[$packageName] Short-form content detected!")
                    val key = "${packageName}_content_detected"
                    if (shouldPerformAction(key)) {
                        handleShortFormContentDetected(packageName)
                    } else {
                        Timber.d("[$packageName] Action skipped due to cooldown")
                    }
                    break
                }
            }
        }
    }

    override fun onInterrupt() {
        Timber.w("ShortFormContentBlockerService interrupted")
    }

    /**
     * Handles detection of short-form content by performing a back navigation action.
     *
     * @param packageName The package name of the app where content was detected
     */
    private fun handleShortFormContentDetected(packageName: String) {
        Timber.i("[$packageName] Handling short-form content detection - performing BACK action")
        val success = performGlobalAction(GLOBAL_ACTION_BACK)
        if (success) {
            Timber.d("[$packageName] BACK action performed successfully")
        } else {
            Timber.w("[$packageName] BACK action failed")
        }
    }

    /**
     * Checks if an action should be performed based on cooldown period.
     *
     * Prevents repeated actions from being performed too frequently by enforcing
     * a cooldown period of 1.5 seconds between actions.
     *
     * @param key Unique identifier for the action
     * @return true if the action should be performed, false if still in cooldown
     */
    private fun shouldPerformAction(key: String): Boolean {
        val now = SystemClock.uptimeMillis()
        val last = lastActionTimestamps[key] ?: 0L
        val timeSinceLastAction = now - last
        if (timeSinceLastAction < actionCooldownMillis) {
            Timber.v("Action '$key' cooldown active: ${timeSinceLastAction}ms since last action")
            return false
        }
        lastActionTimestamps[key] = now
        Timber.d("Action '$key' allowed (cooldown: ${actionCooldownMillis}ms)")
        return true
    }
}
