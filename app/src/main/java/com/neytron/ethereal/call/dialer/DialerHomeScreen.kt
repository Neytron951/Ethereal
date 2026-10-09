package com.neytron.ethereal.call.dialer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neytron.ethereal.ui.theme.EtherealPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ЗАКОНСЕРВИРОВАНО.
 *
 * Главный экран дилера — история звонков.
 * Сейчас НЕ вызывается.
 */
@Composable
fun DialerHomeScreen(
    palette: EtherealPalette,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf<List<CallLogEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        entries = CallLogRepository.getRecentCalls(context)
    }

    val grouped = remember(entries) {
        CallLogRepository.groupByDay(entries)
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        grouped.forEach { (day, dayEntries) ->
            item(key = "header_$day") {
                Text(
                    text = day,
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(
                        start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp
                    )
                )
            }
            items(dayEntries, key = { it.id }) { entry ->
                CallLogRow(
                    entry = entry,
                    palette = palette,
                    timeFormat = timeFormat,
                    onClick = { dialNumber(context, entry.number) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CallLogRow(
    entry: CallLogEntry,
    palette: EtherealPalette,
    timeFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Аватарка
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(palette.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            DefaultAvatarIcon(color = palette.textSecondary, size = 26.dp)
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Имя + стрелка + время
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.displayName,
                color = palette.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val missedColor = Color(0xFFFF5555)
                when (entry.type) {
                    CallType.OUTGOING -> ArrowUpRightIcon(
                        color = palette.textSecondary, size = 14.dp
                    )
                    CallType.INCOMING -> ArrowDownLeftIcon(
                        color = palette.textSecondary, size = 14.dp
                    )
                    CallType.MISSED -> ArrowDownLeftIcon(
                        color = missedColor, size = 14.dp
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = timeFormat.format(Date(entry.timestamp)),
                    color = if (entry.type == CallType.MISSED) missedColor
                    else palette.textSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Кнопка "Позвонить"
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            DialerCallIcon(color = palette.primary, size = 22.dp)
        }
    }
}

private fun dialNumber(context: android.content.Context, number: String) {
    if (number.isBlank()) return
    try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${Uri.encode(number)}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}