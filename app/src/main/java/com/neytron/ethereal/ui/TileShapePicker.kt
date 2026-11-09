package com.neytron.ethereal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neytron.ethereal.ui.theme.EtherealPalette
import com.neytron.ethereal.ui.theme.TileShape
import com.neytron.ethereal.ui.theme.tileShapeFor

/**
 * Сетка выбора формы плитки. Превью — это сама форма, а не картинка,
 * поэтому выглядит ровно так, как будет на плитке.
 */
@Composable
fun TileShapePicker(
    shapes: List<TileShape>,
    selected: TileShape,
    palette: EtherealPalette,
    modifier: Modifier = Modifier,
    defaultShape: TileShape = TileShape.ROUNDED,
    radiusDp: Float = 8f,
    columns: Int = 5,
    onSelect: (TileShape) -> Unit
) {
    val safeColumns = columns.coerceAtLeast(1)
    val rows = shapes.chunked(safeColumns)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { rowShapes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowShapes.forEach { shape ->
                    TileShapeSwatch(
                        shape = shape,
                        selected = shape == selected,
                        palette = palette,
                        defaultShape = defaultShape,
                        radiusDp = radiusDp,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(shape) }
                    )
                }
                repeat(safeColumns - rowShapes.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TileShapeSwatch(
    shape: TileShape,
    selected: Boolean,
    palette: EtherealPalette,
    defaultShape: TileShape,
    radiusDp: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val clipShape: Shape = tileShapeFor(shape, radiusDp, defaultShape)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(clipShape)
            .background(
                if (selected) palette.primary.copy(alpha = 0.28f)
                else palette.surfaceVariant.copy(alpha = 0.55f)
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) palette.primary else palette.outline.copy(alpha = 0.5f),
                shape = clipShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (shape == TileShape.AUTO) {
            Text(
                text = "A",
                color = if (selected) palette.primary else palette.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
