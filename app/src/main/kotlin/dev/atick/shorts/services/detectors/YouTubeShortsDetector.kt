package dev.atick.shorts.services.detectors

import android.content.res.Resources
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import timber.log.Timber
import java.util.ArrayDeque

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

        return false
    }
}