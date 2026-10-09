package com.neytron.ethereal.call

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neytron.ethereal.call.dialer.DialerHomeScreen
import com.neytron.ethereal.call.dialer.DialerKeypadScreen
import com.neytron.ethereal.call.dialer.DialerTab
import com.neytron.ethereal.call.dialer.DialerTabs
import com.neytron.ethereal.call.dialer.SearchIcon
import com.neytron.ethereal.data.SettingsRepository
import com.neytron.ethereal.ui.theme.paletteById

/**
 * ЗАКОНСЕРВИРОВАНО.
 *
 * Экран набора номера и истории звонков.
 * Сейчас НЕ активен в манифесте.
 *
 * Чтобы включить обратно — раскомментировать <activity> в AndroidManifest.xml.
 */
class DialerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Если пришли через tel: — открываем клавиатуру с номером
        val initialNumber = intent?.data?.schemeSpecificPart?.let { raw ->
            java.net.URLDecoder.decode(raw, "UTF-8")
        } ?: ""

        setContent {
            val context = LocalContext.current
            val paletteId = remember { SettingsRepository.getPaletteId(context) }
            val palette = paletteById(paletteId)

            var currentTab by remember {
                mutableStateOf(
                    if (initialNumber.isNotEmpty()) DialerTab.KEYPAD
                    else DialerTab.CALLS
                )
            }
            var searchOpen by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.background)
            ) {
                // === Хедер ===
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (currentTab) {
                            DialerTab.CALLS -> "Звонки"
                            DialerTab.KEYPAD -> "Клавиатура"
                        },
                        color = palette.textPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { searchOpen = !searchOpen },
                        contentAlignment = Alignment.Center
                    ) {
                        SearchIcon(color = palette.textPrimary, size = 22.dp)
                    }
                }

                // === Контент ===
                Box(modifier = Modifier.weight(1f)) {
                    when (currentTab) {
                        DialerTab.CALLS -> DialerHomeScreen(
                            palette = palette,
                            modifier = Modifier.fillMaxSize()
                        )
                        DialerTab.KEYPAD -> DialerKeypadScreen(
                            palette = palette,
                            initialNumber = initialNumber,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // === Таб-бар ===
                DialerTabs(
                    selected = currentTab,
                    palette = palette,
                    onSelect = { currentTab = it }
                )
            }
        }
    }
}