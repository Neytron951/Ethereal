package com.neytron.ethereal.tiles.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.neytron.ethereal.R
import com.neytron.ethereal.data.AppInfo
import com.neytron.ethereal.tiles.TileBorderStyle
import com.neytron.ethereal.tiles.TileStyle
import com.neytron.ethereal.ui.theme.EtherealPalette
import com.neytron.ethereal.utils.toImageBitmap

data class MultiAppsBatchConfig(
    val style: TileStyle = TileStyle(),
    val showLabel: Boolean = true,
    val w: Int = 1,
    val h: Int = 1
)

@Composable
fun MultiAppsPickerDialog(
    palette: EtherealPalette,
    installedApps: List<AppInfo>,
    onBatchConfigReady: (List<AppInfo>, MultiAppsBatchConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var query by remember { mutableStateOf("") }
    val selectedPackages = remember { mutableStateSetOf<String>() }

    var opacity by remember { mutableStateOf(0.55f) }
    var blur by remember { mutableStateOf(0f) }
    var borderStyle by remember { mutableStateOf(TileBorderStyle.NONE) }
    var borderWidth by remember { mutableStateOf(0f) }
    var borderOpacity by remember { mutableStateOf(0.3f) }
    var showLabel by remember { mutableStateOf(true) }
    var tileWidth by remember { mutableIntStateOf(1) }
    var tileHeight by remember { mutableIntStateOf(1) }

    val filteredApps = remember(query, installedApps) {
        if (query.isBlank()) installedApps
        else installedApps.filter { it.label.contains(query, ignoreCase = true) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.85f)
                    .clickable(onClick = {}),
                shape = RoundedCornerShape(24.dp),
                color = palette.background.copy(alpha = 0.98f),
                border = BorderStroke(1.dp, palette.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    if (step == 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.tile_multi_apps),
                                color = palette.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${selectedPackages.size}",
                                color = palette.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            if (query.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.widget_picker_search_hint),
                                    color = palette.textSecondary,
                                    fontSize = 14.sp
                                )
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                textStyle = TextStyle(color = palette.textPrimary, fontSize = 14.sp),
                                cursorBrush = SolidColor(palette.primary),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                val selected = app.packageName in selectedPackages
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) palette.primary.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable {
                                            if (selected) selectedPackages.remove(app.packageName)
                                            else selectedPackages.add(app.packageName)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val bmp = remember(app.packageName) { app.icon.toImageBitmap() }
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp,
                                            contentDescription = app.label,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = app.label,
                                        color = if (selected) palette.primary else palette.textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (selected) {
                                        Text("✓", color = palette.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text(text = stringResource(R.string.cancel), color = palette.textSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selectedPackages.isNotEmpty()) palette.primary else palette.surfaceVariant)
                                    .clickable(enabled = selectedPackages.isNotEmpty()) {
                                        step = 2
                                    }
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.continue_button),
                                    color = if (selectedPackages.isNotEmpty()) palette.background else palette.textSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.tile_multi_apps) + " (${selectedPackages.size} apps)",
                            color = palette.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Tile Size W x H
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Width ($tileWidth)", color = palette.textPrimary, fontSize = 13.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        for (w in 1..3) {
                                            val sel = tileWidth == w
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (sel) palette.primary else palette.surfaceVariant.copy(alpha = 0.5f))
                                                    .clickable { tileWidth = w },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = "$w", color = if (sel) palette.background else palette.textPrimary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Height ($tileHeight)", color = palette.textPrimary, fontSize = 13.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        for (h in 1..3) {
                                            val sel = tileHeight == h
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (sel) palette.primary else palette.surfaceVariant.copy(alpha = 0.5f))
                                                    .clickable { tileHeight = h },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = "$h", color = if (sel) palette.background else palette.textPrimary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // Opacity slider (0% to 100%)
                            Column {
                                Text(
                                    text = "${stringResource(R.string.editor_opacity)}: ${(opacity * 100).toInt()}%",
                                    color = palette.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Slider(
                                    value = opacity,
                                    onValueChange = { opacity = it },
                                    valueRange = 0f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = palette.primary,
                                        activeTrackColor = palette.primary
                                    )
                                )
                            }

                            // Blur slider
                            Column {
                                Text(
                                    text = "${stringResource(R.string.editor_blur)}: ${blur.toInt()} dp",
                                    color = palette.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Slider(
                                    value = blur,
                                    onValueChange = { blur = it },
                                    valueRange = 0f..25f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = palette.primary,
                                        activeTrackColor = palette.primary
                                    )
                                )
                            }

                            // Border / Frame style
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = stringResource(R.string.editor_border_title),
                                    color = palette.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                val borderPresets = listOf(
                                    TileBorderStyle.NONE to stringResource(R.string.editor_border_none),
                                    TileBorderStyle.OUTLINE to stringResource(R.string.editor_border_outline),
                                    TileBorderStyle.LIQUID_GLASS to stringResource(R.string.editor_border_liquid_glass),
                                    TileBorderStyle.ACCENT to stringResource(R.string.editor_border_accent),
                                    TileBorderStyle.CUSTOM to stringResource(R.string.editor_border_custom)
                                )
                                borderPresets.forEach { (styleOption, label) ->
                                    val sel = borderStyle == styleOption
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (sel) palette.primary.copy(alpha = 0.18f) else palette.surfaceVariant.copy(alpha = 0.3f))
                                            .clickable {
                                                borderStyle = styleOption
                                                borderWidth = when (styleOption) {
                                                    TileBorderStyle.NONE -> 0f
                                                    TileBorderStyle.OUTLINE -> 1f
                                                    TileBorderStyle.LIQUID_GLASS -> 1.5f
                                                    TileBorderStyle.ACCENT -> 1.5f
                                                    TileBorderStyle.CUSTOM -> 1.5f
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (sel) palette.primary else palette.textPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Show Label",
                                    color = palette.textPrimary,
                                    fontSize = 14.sp
                                )
                                Switch(
                                    checked = showLabel,
                                    onCheckedChange = { showLabel = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = palette.primary)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { step = 1 }) {
                                Text(text = "Back", color = palette.textSecondary)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(palette.primary)
                                    .clickable {
                                        val chosen = installedApps.filter { it.packageName in selectedPackages }
                                        val batchConfig = MultiAppsBatchConfig(
                                            style = TileStyle(
                                                backgroundOpacity = opacity,
                                                backgroundBlur = blur,
                                                borderStyle = borderStyle,
                                                borderWidth = borderWidth,
                                                borderOpacity = borderOpacity
                                            ),
                                            showLabel = showLabel,
                                            w = tileWidth,
                                            h = tileHeight
                                        )
                                        onBatchConfigReady(chosen, batchConfig)
                                    }
                                    .padding(horizontal = 24.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Create Tiles",
                                    color = palette.background,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
