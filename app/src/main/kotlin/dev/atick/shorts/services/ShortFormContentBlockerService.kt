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
import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import dev.atick.shorts.utils.UserPreferencesProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("AccessibilityPolicy")
class ShortFormContentBlockerService : AccessibilityService() {

    private val lastActionTimestamps = ConcurrentHashMap<String, Long>()
    private val actionCooldownMillis = 1500L
    private val userPreferencesProvider by lazy { UserPreferencesProvider(applicationContext) }

    override fun onServiceConnected() {
        Timber.d("ShortFormContentBlockerService connected")

        CoroutineScope(Dispatchers.IO).launch {
            val packages = userPreferencesProvider.getTrackedPackages().first()
            val info = AccessibilityServiceInfo().apply {
                eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                        AccessibilityEvent.TYPE_VIEW_SCROLLED
                feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                packageNames = packages.toTypedArray()
                notificationTimeout = 100
            }
            serviceInfo = info
            Timber.d("AccessibilityServiceInfo configured for tracked packages: ${packages.joinToString()}")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        Timber.v("Accessibility event: type=${event.eventType}, className=${event.className}, package=${event.packageName}")

        val windows = windows
        Timber.v("Inspecting ${windows.size} windows")
        for (win in windows) {
            // Only process focused application windows
            if (!win.isFocused || !win.isActive) {
                Timber.v("Skipping non-focused/inactive window")
                continue
            }

            val root = win.root ?: continue
            if (isShortFormContent(root)) {
                Timber.i("Short-form content detected in full-screen mode")
                val key = "content_detected"
                if (shouldPerformAction(key)) {
                    handleShortFormContentDetected(root)
                } else {
                    Timber.d("Action skipped due to cooldown")
                }
                break
            }
        }
    }

    override fun onInterrupt() {
        Timber.w("ShortFormContentBlockerService interrupted")
    }

    private fun isShortFormContent(node: AccessibilityNodeInfo): Boolean {
        var hasPlayerIndicator = false
        var isFullScreen = false
        var hasPlayerControls = false

        // Get screen bounds for full-screen verification
        val rect = Rect()
        node.getBoundsInScreen(rect)
        
        // Get display metrics for comparison
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val screenWidth = displayMetrics.widthPixels
        
        Timber.v("Container bounds: ${rect.width()}x${rect.height()}, Screen: ${screenWidth}x${screenHeight}")

        // 1) Exclude small containers (thumbnails, shelf items)
        if (rect.height() < 400 || rect.width() < 300) {
            Timber.v("Container too small, likely thumbnail - skipping")
            return false
        }

        // 2) Check view-id patterns, but EXCLUDE shelf/grid/chip patterns
        val viewId = node.viewIdResourceName
        if (viewId != null) {
            // Exclude shelf, grid, thumbnails, chips
            if (viewId.contains("shelf", ignoreCase = true) ||
                viewId.contains("grid", ignoreCase = true) ||
                viewId.contains("chip", ignoreCase = true) ||
                viewId.contains("thumbnail", ignoreCase = true) ||
                viewId.contains("preview", ignoreCase = true)
            ) {
                Timber.d("Excluded pattern found in view ID: $viewId - not full-screen player")
                return false
            }

            // Look for actual player IDs
            if (viewId.contains("shorts_player", ignoreCase = true) ||
                viewId.contains("reel_player", ignoreCase = true) ||
                viewId.contains("shorts_video", ignoreCase = true) ||
                viewId.contains("reel_video_player", ignoreCase = true)
            ) {
                Timber.d("Player indicator found in view ID: $viewId")
                hasPlayerIndicator = true
            }
        }

        // 3) Check for full-screen: container must cover at least 85% of screen height
        val heightCoverage = rect.height().toFloat() / screenHeight
        val aspect = if (rect.width() == 0) 0f else rect.height().toFloat() / rect.width()
        
        if (heightCoverage >= 0.85f && aspect > 1.5f) {
            Timber.d("Full-screen container detected: height coverage=${heightCoverage * 100}%, aspect=$aspect")
            isFullScreen = true
        } else {
            Timber.v("Not full-screen: height coverage=${heightCoverage * 100}%, aspect=$aspect")
        }

        // 4) Look for video player controls (stronger indicator)
        if (isFullScreen && hasVideoPlayerControls(node)) {
            Timber.d("Video player controls detected in full-screen container")
            hasPlayerControls = true
            hasPlayerIndicator = true // Strong indicator
        }

        // 5) Require FULL-SCREEN + (PLAYER_INDICATOR or PLAYER_CONTROLS)
        val isActuallyWatchingShorts = isFullScreen && (hasPlayerIndicator || hasPlayerControls)

        if (isActuallyWatchingShorts) {
            Timber.i("✓ User is actively watching short-form content in full-screen")
        } else {
            Timber.v("✗ Not watching shorts: fullScreen=$isFullScreen, playerIndicator=$hasPlayerIndicator, controls=$hasPlayerControls")
        }

        return isActuallyWatchingShorts
    }

    private fun hasVideoPlayerControls(node: AccessibilityNodeInfo): Boolean {
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.add(node)
        var nodesScanned = 0
        var foundPlayerControls = 0

        // Look for actual video player controls (more specific than just "shorts" in ID)
        val playerControlIds = listOf(
            "player_control", // Generic player controls
            "play_pause", // Play/pause button
            "progress", "seek", "seekbar", // Video progress/seek bar
            "video_surface", // Video rendering surface
            "exo_player", // ExoPlayer components
            "media_controller", // Media controller
        )

        // Exclude these - they indicate shelf/grid, not active player
        val excludedIds = listOf(
            "shelf",
            "grid",
            "chip",
            "tab",
            "thumbnail",
            "preview",
            "feed",
            "recycler",
        )

        while (stack.isNotEmpty()) {
            val n = stack.removeFirst()
            nodesScanned++

            // Limit scan depth
            if (nodesScanned > 80) break

            val id = n.viewIdResourceName
            if (id != null) {
                // Skip if it's part of excluded patterns
                var isExcluded = false
                for (excluded in excludedIds) {
                    if (id.contains(excluded, ignoreCase = true)) {
                        isExcluded = true
                        break
                    }
                }

                if (!isExcluded) {
                    // Look for player controls
                    for (controlId in playerControlIds) {
                        if (id.contains(controlId, ignoreCase = true)) {
                            Timber.d("Player control found: $id")
                            foundPlayerControls++
                        }
                    }

                    // Look for shorts-specific player components (not shelf)
                    if (id.contains("shorts_player", ignoreCase = true) ||
                        id.contains("reel_player", ignoreCase = true) ||
                        id.contains("shorts_video", ignoreCase = true)
                    ) {
                        Timber.d("Shorts player component found: $id")
                        foundPlayerControls++
                    }
                }
            }

            // Check for video playback state indicators in content description
            val desc = n.contentDescription?.toString()
            if (!desc.isNullOrBlank() && desc.length > 15) {
                if ((desc.contains("playing", true) || desc.contains("paused", true)) &&
                    (desc.contains("video", true) || desc.contains("short", true) || desc.contains("reel", true))
                ) {
                    Timber.d("Video playback state description found: $desc")
                    foundPlayerControls++
                }
            }

            for (i in 0 until n.childCount) {
                n.getChild(i)?.let { stack.add(it) }
            }
        }

        val hasControls = foundPlayerControls > 0
        Timber.v("Scanned $nodesScanned nodes, found $foundPlayerControls player control indicators")
        return hasControls
    }

    private fun handleShortFormContentDetected(root: AccessibilityNodeInfo) {
        Timber.i("Handling short-form content detection - performing BACK action")
        val success = performGlobalAction(GLOBAL_ACTION_BACK)
        if (success) {
            Timber.d("BACK action performed successfully")
        } else {
            Timber.w("BACK action failed")
        }
    }

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
