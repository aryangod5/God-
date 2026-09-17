package com.example.god.gesture

import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import kotlin.math.abs
import kotlin.math.hypot

/** Stable, debounced interpretation of MediaPipe hand results. */
class GestureInterpreter {
    private var lastWristX = Float.NaN
    private var lastGestureAt = 0L
    private var pinchLatched = false
    private var stableCandidate: String? = null
    private var stableFrames = 0

    fun reset() {
        lastWristX = Float.NaN
        lastGestureAt = 0L
        pinchLatched = false
        stableCandidate = null
        stableFrames = 0
    }

    fun interpret(result: GestureRecognizerResult, now: Long = System.currentTimeMillis()): String? {
        val hand = result.landmarks().firstOrNull()
        val label = result.gestures().firstOrNull()?.firstOrNull()?.categoryName().orEmpty()

        var candidate: String? = when (label) {
            "Open_Palm" -> "WAKE"
            "Closed_Fist" -> "CANCEL"
            else -> null
        }

        if (hand != null && hand.size >= 21) {
            val thumb = hand[4]
            val index = hand[8]
            val pinchDistance = hypot(thumb.x() - index.x(), thumb.y() - index.y())
            if (pinchDistance < 0.065f) {
                if (!pinchLatched) candidate = "SELECT"
                pinchLatched = true
            } else if (pinchDistance > 0.105f) {
                pinchLatched = false
            }

            // Swipe is based on a meaningful wrist displacement, not a single noisy frame.
            val wristX = hand[0].x()
            if (!lastWristX.isNaN()) {
                val dx = wristX - lastWristX
                if (abs(dx) > 0.13f && !pinchLatched) {
                    candidate = if (dx > 0f) "SWIPE_RIGHT" else "SWIPE_LEFT"
                }
            }
            lastWristX = wristX
        } else {
            lastWristX = Float.NaN
        }

        if (candidate == null) {
            stableCandidate = null
            stableFrames = 0
            return null
        }

        if (candidate == stableCandidate) stableFrames++ else {
            stableCandidate = candidate
            stableFrames = 1
        }

        val requiredFrames = if (candidate == "SELECT" || candidate.startsWith("SWIPE")) 1 else 2
        if (stableFrames < requiredFrames) return null
        if (now - lastGestureAt < 550L) return null

        lastGestureAt = now
        stableFrames = 0
        stableCandidate = null
        return candidate
    }
}
