package com.example.god.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityEvent

/** User-authorized Android-wide action layer for GOD gestures. */
class GodAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile private var instance: GodAccessibilityService? = null
        fun current(): GodAccessibilityService? = instance
    }

    override fun onServiceConnected() { instance = this }
    override fun onDestroy() { if (instance === this) instance = null; super.onDestroy() }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { }
    override fun onInterrupt() {}

    fun performGestureAction(action: String): Boolean = when (action) {
        "CANCEL" -> performGlobalAction(GLOBAL_ACTION_BACK)
        "SELECT" -> clickFocusedOrFirstClickable()
        "SWIPE_LEFT" -> dispatchHorizontalSwipe(leftToRight = false)
        "SWIPE_RIGHT" -> dispatchHorizontalSwipe(leftToRight = true)
        else -> false
    }

    private fun clickFocusedOrFirstClickable(): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
        if (focused?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) return true
        return findClickable(root)?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
    }

    private fun findClickable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isVisibleToUser && node.isClickable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findClickable(child)
            if (found != null) return found
        }
        return null
    }

    private fun dispatchHorizontalSwipe(leftToRight: Boolean): Boolean {
        val path = Path()
        val y = resources.displayMetrics.heightPixels * 0.5f
        val start = if (leftToRight) 0.18f else 0.82f
        val end = if (leftToRight) 0.82f else 0.18f
        path.moveTo(resources.displayMetrics.widthPixels * start, y)
        path.lineTo(resources.displayMetrics.widthPixels * end, y)
        return dispatchGesture(
            GestureDescription.Builder().addStroke(
                GestureDescription.StrokeDescription(path, 0, 300)
            ).build(), null, null
        )
    }
}
