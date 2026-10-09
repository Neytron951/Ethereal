package com.neytron.ethereal.call.dialer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neytron.ethereal.ui.theme.EtherealPalette

enum class DialerTab(val label: String) {
    CALLS("Звонки"),
    KEYPAD("Клавиатура")
}

@Composable
fun DialerTabs(
    selected: DialerTab,
    palette: EtherealPalette,
    onSelect: (DialerTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surface)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DialerTab.values().forEach { tab ->
            DialerTabItem(
                tab = tab,
                selected = tab == selected,
                palette = palette,
                onClick = { onSelect(tab) }
            )
        }
    }
}

@Composable
private fun DialerTabItem(
    tab: DialerTab,
    selected: Boolean,
    palette: EtherealPalette,
    onClick: () -> Unit
) {
    val color = if (selected) palette.primary else palette.textSecondary

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (tab) {
            DialerTab.CALLS -> TabCallsIcon(color = color, size = 22.dp)
            DialerTab.KEYPAD -> TabKeypadIcon(color = color, size = 22.dp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tab.label,
            color = color,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}