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
import java.util.ArrayDeque

/**
 * Detector for YouTube Shorts short-form content.
 *
 * This detector identifies when a user is actively watching YouTube Shorts by searching
 * for the distinctive "reel_progress_bar" UI element in the accessibility node hierarchy.
 * This progress bar is unique to the Shorts viewing experience.
 *
 * **Detection Strategy:**
 * - Performs BFS traversal of the accessibility node tree (max 120 nodes)
 * - Searches for view IDs containing "reel_progress_bar"
 * - Only triggers when this element is present and visible
 *
 * **Known Limitations:**
 * - Detection patterns may break after YouTube app updates
 * - Does not detect Shorts in the home feed shelf (intentional)
 * - Requires accessibility service to have window content access
 *
 * **Compatibility:**
 * - Tested with YouTube app versions 18.x - 19.x
 * - May require updates for major YouTube redesigns
 */
class YouTubeShortsDetector : ShortFormContentDetector {

    override fun getPackageName(): String = "com.google.android.youtube"

    override fun isShortFormContent(
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo,
        resources: Resources,
    ): Boolean {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)

        Timber.v("Accessibility event type: ${event.className?.toString()}")

        var nodeCount = 0
        while (queue.isNotEmpty() && nodeCount < 120) {
            val node = queue.removeFirst()
            nodeCount++

            val id = node.viewIdResourceName?.lowercase()
            if (id != null && "reel_progress_bar" in id) {
                Timber.d("[YouTube] Reels detected from id: $id")
                return true
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let(queue::add)
            }
        }

        Timber.v("[YouTube] No Shorts detected ($nodeCount nodes scanned)")
        return false
    }
}
