package com.neytron.ethereal.ui

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.swipeUpToOpen(
    threshold: Float = 150f,
    onSwipeUp: () -> Unit
): Modifier = this.pointerInput(Unit) {
    var totalDrag = 0f
    detectVerticalDragGestures(
        onDragStart = { totalDrag = 0f },
        onDragEnd = {
            if (totalDrag < -threshold) onSwipeUp()
        },
        onVerticalDrag = { _, dragAmount ->
            totalDrag += dragAmount
        }
    )
}