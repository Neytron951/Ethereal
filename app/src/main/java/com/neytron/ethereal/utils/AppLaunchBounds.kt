package com.neytron.ethereal.utils

import android.graphics.Rect
import android.view.View
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import kotlin.math.roundToInt

fun LayoutCoordinates.screenBounds(rootView: View): Rect {
    val position = localToRoot(Offset.Zero)
    val rootLocation = IntArray(2)
    rootView.getLocationOnScreen(rootLocation)
    return Rect(
        (position.x + rootLocation[0]).roundToInt(),
        (position.y + rootLocation[1]).roundToInt(),
        (position.x + rootLocation[0] + size.width).roundToInt(),
        (position.y + rootLocation[1] + size.height).roundToInt()
    )
}
