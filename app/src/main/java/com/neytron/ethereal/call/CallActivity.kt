package com.neytron.ethereal.call

import android.os.Bundle
import android.telecom.Call
import android.telecom.CallAudioState
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neytron.ethereal.data.SettingsRepository
import com.neytron.ethereal.ui.theme.EtherealPalette
import com.neytron.ethereal.ui.theme.paletteById
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ЗАКОНСЕРВИРОВАНО.
 *
 * Экран активного звонка.
 * Сейчас НЕ вызывается, потому что EtherealInCallService отключён.
 *
 * Чтобы включить обратно — вернуть блок звонков в AndroidManifest.xml.
 */
class CallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        setContent {
            val context = LocalContext.current
            val paletteId = remember { SettingsRepository.getPaletteId(context) }
            val palette = paletteById(paletteId)

            CallScreen(
                palette = palette,
                onFinish = { finish() }
            )
        }
    }
}

@Composable
fun CallScreen(
    palette: EtherealPalette,
    onFinish: () -> Unit
) {
    val state by EtherealInCallService.callState
    val audioState by EtherealInCallService.audioState
    val call = EtherealInCallService.currentCall
    val scope = rememberCoroutineScope()

    var callerName by remember { mutableStateOf("Загрузка...") }
    var callDuration by remember { mutableStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isHeld by remember { mutableStateOf(false) }
    var showKeypad by remember { mutableStateOf(false) }
    var showAudioMenu by remember { mutableStateOf(false) }
    var dialedNumber by remember { mutableStateOf("") }
    var currentRoute by remember { mutableStateOf(CallAudioState.ROUTE_EARPIECE) }
    var supportedRoutes by remember { mutableStateOf(0) }

    // Имя/номер
    LaunchedEffect(call) {
        val name = call?.details?.callerDisplayName?.toString()
        val number = call?.details?.handle?.schemeSpecificPart
        callerName = when {
            !name.isNullOrBlank() -> name
            !number.isNullOrBlank() -> number
            else -> "Неизвестный"
        }
    }

    // Таймер — НЕ сбрасывается при удержании
    LaunchedEffect(state) {
        if (state == Call.STATE_ACTIVE) {
            while (true) {
                delay(1000)
                callDuration++
            }
        }
    }

    // Сброс таймера ТОЛЬКО при новом звонке
    LaunchedEffect(call) {
        callDuration = 0
    }

    // Чтение аудио-состояния — через сервис
    LaunchedEffect(audioState) {
        audioState?.let {
            currentRoute = it.route
            supportedRoutes = it.supportedRouteMask
        }
    }

    // Автозакрытие
    LaunchedEffect(state) {
        if (state == Call.STATE_DISCONNECTED ||
            state == Call.STATE_DISCONNECTING ||
            state == null) {
            delay(800)
            onFinish()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
        contentAlignment = Alignment.Center
    ) {
        SoundWavePair(
            palette = palette,
            active = state == Call.STATE_ACTIVE,
            ringing = state == Call.STATE_RINGING,
            holding = state == Call.STATE_HOLDING
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Аватарка
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(palette.primary.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                AvatarIcon(palette = palette, size = 60.dp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = callerName,
                color = palette.textPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            when (state) {
                Call.STATE_RINGING -> Text(
                    "Входящий звонок",
                    color = palette.primary, fontSize = 16.sp
                )
                Call.STATE_DIALING -> Text(
                    "Исходящий звонок",
                    color = palette.primary, fontSize = 16.sp
                )
                Call.STATE_CONNECTING -> Text(
                    "Соединение...",
                    color = palette.primary, fontSize = 16.sp
                )
                Call.STATE_ACTIVE, Call.STATE_HOLDING -> Text(
                    formatDuration(callDuration),
                    color = palette.primary, fontSize = 16.sp
                )
                Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> Text(
                    "Звонок завершён",
                    color = Color(0xFFFF5555), fontSize = 16.sp
                )
                else -> Text("...", color = palette.textSecondary, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            if (dialedNumber.isNotEmpty()) {
                Text(
                    text = dialedNumber,
                    color = palette.textPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (showKeypad && (state == Call.STATE_ACTIVE || state == Call.STATE_HOLDING)) {
                DtmfKeypad(
                    palette = palette,
                    onDigit = { digit ->
                        dialedNumber += digit
                        call?.playDtmfTone(digit)
                        scope.launch {
                            delay(120)
                            call?.stopDtmfTone()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (state == Call.STATE_ACTIVE || state == Call.STATE_HOLDING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ControlButton(
                        icon = { MicIcon(muted = isMuted, palette = palette, size = 28.dp) },
                        label = "Микрофон",
                        active = isMuted,
                        palette = palette,
                        onClick = {
                            val newMute = !isMuted
                            EtherealInCallService.setMute(newMute)
                            isMuted = newMute
                        }
                    )
                    ControlButton(
                        icon = { SpeakerIcon(active = showAudioMenu, palette = palette, size = 28.dp) },
                        label = "Аудио",
                        active = showAudioMenu,
                        palette = palette,
                        onClick = { showAudioMenu = !showAudioMenu }
                    )
                    ControlButton(
                        icon = { HoldIcon(active = isHeld, palette = palette, size = 28.dp) },
                        label = "Удержать",
                        active = isHeld,
                        palette = palette,
                        onClick = {
                            val newHold = !isHeld
                            if (newHold) call?.hold() else call?.unhold()
                            isHeld = newHold
                        }
                    )
                    ControlButton(
                        icon = { KeypadIcon(palette = palette, size = 28.dp) },
                        label = "Клавиши",
                        active = showKeypad,
                        palette = palette,
                        onClick = { showKeypad = !showKeypad }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Меню аудио-выходов
            if (showAudioMenu && (state == Call.STATE_ACTIVE || state == Call.STATE_HOLDING)) {
                AudioRouteMenu(
                    palette = palette,
                    currentRoute = currentRoute,
                    supportedRoutes = supportedRoutes,
                    onRouteSelect = { route ->
                        EtherealInCallService.setAudioRoute(route)
                        currentRoute = route
                        showAudioMenu = false
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                if (state != Call.STATE_DISCONNECTED &&
                    state != Call.STATE_DISCONNECTING) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF5555))
                            .clickable {
                                EtherealInCallService.currentCall?.disconnect()
                                onFinish()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        CallEndIcon(palette = palette, size = 36.dp)
                    }
                }

                if (state == Call.STATE_RINGING) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6FE0A0))
                            .clickable {
                                EtherealInCallService.currentCall?.answer(0)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        CallAnswerIcon(palette = palette, size = 36.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ControlButton(
    icon: @Composable () -> Unit,
    label: String,
    active: Boolean,
    palette: EtherealPalette,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (active) palette.primary else palette.surface),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = palette.textSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun AudioRouteMenu(
    palette: EtherealPalette,
    currentRoute: Int,
    supportedRoutes: Int,
    onRouteSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surface)
            .padding(8.dp)
    ) {
        if (supportedRoutes and CallAudioState.ROUTE_EARPIECE != 0) {
            AudioRouteItem(
                label = "Телефон",
                selected = currentRoute == CallAudioState.ROUTE_EARPIECE,
                palette = palette,
                onClick = { onRouteSelect(CallAudioState.ROUTE_EARPIECE) }
            )
        }
        if (supportedRoutes and CallAudioState.ROUTE_SPEAKER != 0) {
            AudioRouteItem(
                label = "Громкая связь",
                selected = currentRoute == CallAudioState.ROUTE_SPEAKER,
                palette = palette,
                onClick = { onRouteSelect(CallAudioState.ROUTE_SPEAKER) }
            )
        }
        if (supportedRoutes and CallAudioState.ROUTE_WIRED_HEADSET != 0) {
            AudioRouteItem(
                label = "Наушники",
                selected = currentRoute == CallAudioState.ROUTE_WIRED_HEADSET,
                palette = palette,
                onClick = { onRouteSelect(CallAudioState.ROUTE_WIRED_HEADSET) }
            )
        }
        if (supportedRoutes and CallAudioState.ROUTE_BLUETOOTH != 0) {
            AudioRouteItem(
                label = "Bluetooth",
                selected = currentRoute == CallAudioState.ROUTE_BLUETOOTH,
                palette = palette,
                onClick = { onRouteSelect(CallAudioState.ROUTE_BLUETOOTH) }
            )
        }
    }
}

@Composable
private fun AudioRouteItem(
    label: String,
    selected: Boolean,
    palette: EtherealPalette,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) palette.primary.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (selected) palette.primary else palette.textPrimary,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Text("✓", color = palette.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DtmfKeypad(
    palette: EtherealPalette,
    onDigit: (Char) -> Unit
) {
    val digits = listOf(
        "1", "2", "3",
        "4", "5", "6",
        "7", "8", "9",
        "*", "0", "#"
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth(0.7f)
    ) {
        items(digits.size) { index ->
            val digit = digits[index]
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(palette.surface)
                    .clickable { onDigit(digit.first()) },
                contentAlignment = Alignment.Center
            ) {
                Text(digit, color = palette.textPrimary, fontSize = 22.sp)
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}