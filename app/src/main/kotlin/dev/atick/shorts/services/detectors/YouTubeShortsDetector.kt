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

package dev.atick.shorts.services.detectors

import android.content.res.Resources
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import timber.log.Timber

class YouTubeShortsDetector : ShortFormContentDetector {

    override fun getPackageName(): String = "com.google.android.youtube"

    override fun isShortFormContent(
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo,
        resources: Resources,
    ): Boolean {
        var hasPlayerIndicator = false
        var isFullScreen = false
        var hasPlayerControls = false

        val rect = Rect()
        rootNode.getBoundsInScreen(rect)

        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val screenWidth = displayMetrics.widthPixels

        Timber.v("[YouTube] Container bounds: ${rect.width()}x${rect.height()}, Screen: ${screenWidth}x${screenHeight}")

        // 1) Exclude small containers (thumbnails, shelf items)
        if (rect.height() < 400 || rect.width() < 300) {
            Timber.v("[YouTube] Container too small, likely thumbnail - skipping")
            return false
        }

        // 2) Check view-id patterns, but EXCLUDE shelf/grid/chip patterns
        val viewId = rootNode.viewIdResourceName
        if (viewId != null) {
            // Exclude shelf, grid, thumbnails, chips
            if (viewId.contains("shelf", ignoreCase = true) ||
                viewId.contains("grid", ignoreCase = true) ||
                viewId.contains("chip", ignoreCase = true) ||
                viewId.contains("thumbnail", ignoreCase = true) ||
                viewId.contains("preview", ignoreCase = true)
            ) {
                Timber.d("[YouTube] Excluded pattern found in view ID: $viewId - not full-screen player")
                return false
            }

            // Look for actual player IDs
            if (viewId.contains("shorts_player", ignoreCase = true) ||
                viewId.contains("reel_player", ignoreCase = true) ||
                viewId.contains("shorts_video", ignoreCase = true) ||
                viewId.contains("reel_video_player", ignoreCase = true)
            ) {
                Timber.d("[YouTube] Player indicator found in view ID: $viewId")
                hasPlayerIndicator = true
            }
        }

        // 3) Check for full-screen: container must cover at least 85% of screen height
        val heightCoverage = rect.height().toFloat() / screenHeight
        val aspect = if (rect.width() == 0) 0f else rect.height().toFloat() / rect.width()

        if (heightCoverage >= 0.85f && aspect > 1.5f) {
            Timber.d("[YouTube] Full-screen container detected: height coverage=${heightCoverage * 100}%, aspect=$aspect")
            isFullScreen = true
        } else {
            Timber.v("[YouTube] Not full-screen: height coverage=${heightCoverage * 100}%, aspect=$aspect")
        }

        // 4) Look for video player controls (stronger indicator)
        if (isFullScreen && hasVideoPlayerControls(rootNode)) {
            Timber.d("[YouTube] Video player controls detected in full-screen container")
            hasPlayerControls = true
            hasPlayerIndicator = true
        }

        // 5) Require FULL-SCREEN + (PLAYER_INDICATOR or PLAYER_CONTROLS)
        val isActuallyWatchingShorts = isFullScreen && (hasPlayerIndicator || hasPlayerControls)

        if (isActuallyWatchingShorts) {
            Timber.i("[YouTube] ✓ User is actively watching Shorts in full-screen")
        } else {
            Timber.v("[YouTube] ✗ Not watching Shorts: fullScreen=$isFullScreen, playerIndicator=$hasPlayerIndicator, controls=$hasPlayerControls")
        }

        return isActuallyWatchingShorts
    }

    private fun hasVideoPlayerControls(node: AccessibilityNodeInfo): Boolean {
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.add(node)
        var nodesScanned = 0
        var foundPlayerControls = 0

        val playerControlIds = listOf(
            "player_control",
            "play_pause",
            "progress", "seek", "seekbar",
            "video_surface",
            "exo_player",
            "media_controller",
        )

        val excludedIds = listOf(
            "shelf", "grid", "chip", "tab",
            "thumbnail", "preview", "feed", "recycler",
        )

        while (stack.isNotEmpty()) {
            val n = stack.removeFirst()
            nodesScanned++

            if (nodesScanned > 80) break

            val id = n.viewIdResourceName
            if (id != null) {
                var isExcluded = false
                for (excluded in excludedIds) {
                    if (id.contains(excluded, ignoreCase = true)) {
                        isExcluded = true
                        break
                    }
                }

                if (!isExcluded) {
                    for (controlId in playerControlIds) {
                        if (id.contains(controlId, ignoreCase = true)) {
                            Timber.d("[YouTube] Player control found: $id")
                            foundPlayerControls++
                        }
                    }

                    if (id.contains("shorts_player", ignoreCase = true) ||
                        id.contains("reel_player", ignoreCase = true) ||
                        id.contains("shorts_video", ignoreCase = true)
                    ) {
                        Timber.d("[YouTube] Shorts player component found: $id")
                        foundPlayerControls++
                    }
                }
            }

            val desc = n.contentDescription?.toString()
            if (!desc.isNullOrBlank() && desc.length > 15) {
                if ((desc.contains("playing", true) || desc.contains("paused", true)) &&
                    (desc.contains("video", true) || desc.contains("short", true) || desc.contains("reel", true))
                ) {
                    Timber.d("[YouTube] Video playback state description found: $desc")
                    foundPlayerControls++
                }
            }

            for (i in 0 until n.childCount) {
                n.getChild(i)?.let { stack.add(it) }
            }
        }

        val hasControls = foundPlayerControls > 0
        Timber.v("[YouTube] Scanned $nodesScanned nodes, found $foundPlayerControls player control indicators")
        return hasControls
    }
}
