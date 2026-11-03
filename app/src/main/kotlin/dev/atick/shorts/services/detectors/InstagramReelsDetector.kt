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
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import timber.log.Timber

/**
 * Detector for Instagram Reels short-form content.
 *
 * This detector identifies when a user is actively watching Instagram Reels using
 * a two-pronged detection strategy: explicit Reels tab detection and fullscreen
 * media detection.
 *
 * **Detection Strategy:**
 * 1. **Reels Tab Detection**: Checks if "clips_tab" element exists and is selected
 * 2. **Fullscreen Detection**: Detects fullscreen media viewing when feed_tab is absent
 *
 * The dual strategy accounts for Instagram's various entry points to Reels content
 * (dedicated tab, explore page, profile reels, etc.)
 *
 * **Implementation Details:**
 * - Scans up to 10 nodes (shallow scan for performance)
 * - Uses feed_tab absence as indicator of fullscreen viewing
 * - Instagram uses "clips_viewer" for all video content, requiring careful detection
 *
 * **Known Limitations:**
 * - May trigger on fullscreen regular posts (rare false positives)
 * - Detection patterns may break after Instagram app updates
 * - Does not distinguish between Reels in feed vs dedicated viewing
 *
 * **Compatibility:**
 * - Tested with Instagram app versions 300.x - 320.x
 * - May require updates for major Instagram redesigns
 */
class InstagramReelsDetector : ShortFormContentDetector {

    override fun getPackageName(): String = "com.instagram.android"

    override fun isShortFormContent(
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo,
        resources: Resources,
    ): Boolean {
        // Instagram uses clips_viewer for everything, so we need to be very specific
        // Check if we're in the Reels tab/activity specifically

        val className = event.className?.toString()
        Timber.v("[Instagram] Event className: $className")

        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.add(rootNode)
        var nodesScanned = 0
        var feedTabCount = 0
        var isDirectMessage = false

        while (stack.isNotEmpty() && nodesScanned < 10) {
            val n = stack.removeFirst()
            nodesScanned++

            val id = n.viewIdResourceName

            if (id != null && ("feed_tab" in id)) {
                feedTabCount++
            }

            if (id != null && ("clips_tab" in id && n.isSelected)) {
                Timber.i("[Instagram] ✓ User is actively watching Reels in Reels tab")
                return true
            }

            if (id != null && ("direct_" in id || "thread_" in id)) {
                Timber.d("[Instagram] Direct message detected, skipping")
                isDirectMessage = true
            }

            for (i in 0 until n.childCount) {
                n.getChild(i)?.let { stack.add(it) }
            }
        }

        if (feedTabCount == 0 && !isDirectMessage) {
            Timber.i("[Instagram] ✓ User is actively watching Media in Fullscreen")
            return true
        }
        Timber.v("[Instagram] No Reels detected ($nodesScanned nodes scanned, feedTabCount=$feedTabCount)")
        return false
    }
}
