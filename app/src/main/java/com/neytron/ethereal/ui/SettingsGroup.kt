package com.neytron.ethereal.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neytron.ethereal.ui.theme.EtherealPalette

/**
 * Состояние «свёрнуто/развёрнуто» для групп настроек.
 * Одно на приложение, поэтому группы не схлопываются при переключении вкладок
 * и не сбрасываются при пересоздании списка.
 */
object SettingsGroupState {
    val expanded = mutableStateMapOf<String, Boolean>()
}

val LocalSettingsGroupState = compositionLocalOf { SettingsGroupState.expanded }

/**
 * Сворачиваемая группа настроек: заголовок + содержимое.
 * Нужна, чтобы длинные списки настроек не превращались в стену текста.
 */
@Composable
fun SettingsGroup(
    groupKey: String,
    title: String,
    palette: EtherealPalette,
    subtitle: String? = null,
    defaultExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val state = LocalSettingsGroupState.current
    val expanded = state[groupKey] ?: defaultExpanded
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        label = "settingsGroupChevron"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { state[groupKey] = !expanded }
                .padding(vertical = 6.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.primary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = palette.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = palette.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            Text(
                text = "▾",
                color = palette.textSecondary,
                fontSize = 14.sp,
                modifier = Modifier.rotate(chevronRotation)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

/** Некликабельный подзаголовок — для списков, которые нельзя свернуть (например, приложения в доке). */
@Composable
fun SettingsGroupLabel(
    title: String,
    palette: EtherealPalette,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = palette.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
