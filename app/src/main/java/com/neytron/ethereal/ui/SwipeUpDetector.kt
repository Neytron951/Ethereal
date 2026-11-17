package com.neytron.ethereal.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

/**
 * Свайп вверх для открытия ESearch.
 *
 * Важно: жест срабатывает только для **одного** пальца. Иначе pinch-«отдаление»
 * обзора пространств распознавался как свайп и открывал поиск.
 */
fun Modifier.swipeUpToOpen(
    threshold: Float = 150f,
    onSwipeUp: () -> Unit
): Modifier = this.pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)

        var totalY = 0f
        var totalX = 0f
        var multiTouch = false

        do {
            val event = awaitPointerEvent()
            if (event.changes.count { it.pressed } > 1) multiTouch = true

            val change = event.changes.firstOrNull { it.id == down.id }
            if (change != null && change.pressed) {
                totalY += change.position.y - change.previousPosition.y
                totalX += change.position.x - change.previousPosition.x
            }
        } while (event.changes.any { it.pressed })

        val vertical = abs(totalY) > abs(totalX) * 1.5f
        if (!multiTouch && vertical && totalY < -threshold) {
            onSwipeUp()
        }
    }
}
