package com.neytron.ethereal.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import com.neytron.ethereal.R
import com.neytron.ethereal.ui.theme.EtherealPalette

data class AppWidgetGroup(
    val appName: String,
    val appIcon: Drawable?,
    val widgets: List<AppWidgetProviderInfo>
)

@Composable
fun WidgetPickerBottomSheet(
    palette: EtherealPalette,
    onWidgetSelected: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    var searchQuery by remember { mutableStateOf("") }

    val widgetGroups by remember {
        mutableStateOf(
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val providers = appWidgetManager.installedProviders ?: emptyList()

                providers.groupBy { info ->
                    try {
                        val appInfo = packageManager.getApplicationInfo(info.provider.packageName, 0)
                        packageManager.getApplicationLabel(appInfo).toString()
                    } catch (e: Exception) {
                        info.provider.packageName
                    }
                }.map { (appName, infos) ->
                    val appIcon = try {
                        packageManager.getApplicationIcon(infos.first().provider.packageName)
                    } catch (e: Exception) {
                        null
                    }
                    AppWidgetGroup(
                        appName = appName,
                        appIcon = appIcon,
                        widgets = infos.sortedBy { it.loadLabel(packageManager) }
                    )
                }.sortedBy { it.appName }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        )
    }

    val filteredGroups = remember(widgetGroups, searchQuery) {
        if (searchQuery.isBlank()) {
            widgetGroups
        } else {
            val q = searchQuery.lowercase()
            widgetGroups.mapNotNull { group ->
                val matchesApp = group.appName.lowercase().contains(q)
                val matchingWidgets = group.widgets.filter { info ->
                    matchesApp || info.loadLabel(packageManager).lowercase().contains(q)
                }
                if (matchingWidgets.isNotEmpty()) {
                    group.copy(widgets = matchingWidgets)
                } else null
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(palette.surface)
                .clickable(enabled = false, onClick = {})
                .imePadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.widget_picker_title),
                    color = palette.textPrimary,
                    fontSize = 20.sp
                )
                Text(
                    text = stringResource(R.string.close),
                    color = palette.primary,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.widget_picker_search_hint),
                        color = palette.textSecondary
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary,
                    focusedBorderColor = palette.primary,
                    unfocusedBorderColor = palette.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            if (filteredGroups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) {
                            stringResource(R.string.widget_picker_empty)
                        } else {
                            stringResource(R.string.widget_picker_nothing_found)
                        },
                        color = palette.textSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredGroups, key = { it.appName }) { group ->
                        AppWidgetGroupCard(
                            group = group,
                            isSearching = searchQuery.isNotBlank(),
                            palette = palette,
                            onWidgetSelected = onWidgetSelected
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppWidgetGroupCard(
    group: AppWidgetGroup,
    isSearching: Boolean,
    palette: EtherealPalette,
    onWidgetSelected: (AppWidgetProviderInfo) -> Unit
) {
    var expanded by remember(isSearching) { mutableStateOf(isSearching) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val appIconBmp = remember(group.appIcon) {
                try { group.appIcon?.toBitmap(48, 48)?.asImageBitmap() } catch (e: Exception) { null }
            }
            if (appIconBmp != null) {
                Image(
                    bitmap = appIconBmp,
                    contentDescription = group.appName,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = group.appName,
                color = palette.textPrimary,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${group.widgets.size}",
                color = palette.textSecondary,
                fontSize = 12.sp
            )
            Text(
                text = if (expanded) "▲" else "▼",
                color = palette.textSecondary,
                fontSize = 12.sp
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                group.widgets.forEach { widgetInfo ->
                    WidgetInfoRow(
                        info = widgetInfo,
                        palette = palette,
                        onClick = { onWidgetSelected(widgetInfo) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetInfoRow(
    info: AppWidgetProviderInfo,
    palette: EtherealPalette,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager

    val label = remember(info) {
        info.loadLabel(packageManager).ifBlank { "Widget" }
    }

    val reqW = ((info.minWidth + 69) / 70).coerceAtLeast(1)
    val reqH = ((info.minHeight + 69) / 70).coerceAtLeast(1)

    val previewDrawable = remember(info) {
        try {
            info.loadPreviewImage(context, 0)
                ?: info.loadIcon(context, 0)
                ?: packageManager.getApplicationIcon(info.provider.packageName)
        } catch (e: Exception) {
            null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.background.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (previewDrawable != null) {
            AsyncImage(
                model = previewDrawable,
                contentDescription = label,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = palette.textPrimary,
                fontSize = 14.sp
            )
            Text(
                text = stringResource(R.string.widget_picker_size_format, reqW, reqH),
                color = palette.textSecondary,
                fontSize = 12.sp
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(palette.primary)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = stringResource(R.string.widget_picker_add),
                color = palette.background,
                fontSize = 12.sp
            )
        }
    }
}