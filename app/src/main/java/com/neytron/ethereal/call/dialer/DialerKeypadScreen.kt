package com.neytron.ethereal.call.dialer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neytron.ethereal.ui.theme.EtherealPalette

/**
 * ЗАКОНСЕРВИРОВАНО.
 *
 * Экран клавиатуры дилера.
 * Сейчас НЕ вызывается.
 */
private data class KeypadKey(
    val digit: String,
    val letters: String = ""
)

private val KEYPAD_LAYOUT = listOf(
    KeypadKey("1"),
    KeypadKey("2", "ABC"),
    KeypadKey("3", "DEF"),
    KeypadKey("4", "GHI"),
    KeypadKey("5", "JKL"),
    KeypadKey("6", "MNO"),
    KeypadKey("7", "PQRS"),
    KeypadKey("8", "TUV"),
    KeypadKey("9", "WXYZ"),
    KeypadKey("*"),
    KeypadKey("0", "+"),
    KeypadKey("#")
)

@Composable
fun DialerKeypadScreen(
    palette: EtherealPalette,
    initialNumber: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var number by remember { mutableStateOf(initialNumber) }
    var matches by remember { mutableStateOf<List<ContactRepository.ContactMatch>>(emptyList()) }

    // Поиск совпадений при наборе
    LaunchedEffect(number) {
        if (number.length >= 2) {
            matches = ContactRepository.search(context, number, limit = 6)
        } else {
            matches = emptyList()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // === Список совпадений ===
        if (matches.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 220.dp)
            ) {
                items(matches, key = { "${it.name}|${it.number}" }) { match ->
                    ContactMatchRow(
                        match = match,
                        palette = palette,
                        onClick = { dialNumber(context, match.number) }
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // === Поле с набранным номером ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.ifEmpty { "" },
                color = palette.textPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        // === Клавиатура 3x4 ===
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (row in 0 until 4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (col in 0 until 3) {
                        val idx = row * 3 + col
                        val key = KEYPAD_LAYOUT[idx]
                        KeypadButton(
                            key = key,
                            palette = palette,
                            modifier = Modifier.weight(1f),
                            onDigit = { digit ->
                                number += digit
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // === Нижняя панель: пусто | call | backspace ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Кнопка "Позвонить"
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        if (number.isNotEmpty()) palette.primary
                        else palette.surfaceVariant
                    )
                    .clickable(
                        enabled = number.isNotEmpty(),
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (number.isNotEmpty()) dialNumber(context, number)
                    },
                contentAlignment = Alignment.Center
            ) {
                DialerCallIcon(
                    color = if (number.isNotEmpty()) palette.background
                    else palette.textSecondary,
                    size = 28.dp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Backspace (короткое — 1 символ, длинное — очистить всё)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                if (number.isNotEmpty()) {
                                    number = number.dropLast(1)
                                }
                            },
                            onLongPress = {
                                number = ""
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (number.isNotEmpty()) {
                    BackspaceIcon(color = palette.textPrimary, size = 26.dp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun KeypadButton(
    key: KeypadKey,
    palette: EtherealPalette,
    modifier: Modifier = Modifier,
    onDigit: (String) -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surfaceVariant.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onDigit(key.digit) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = key.digit,
                color = palette.textPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium
            )
            if (key.letters.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = key.letters,
                    color = palette.textSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ContactMatchRow(
    match: ContactRepository.ContactMatch,
    palette: EtherealPalette,
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
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(palette.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            DefaultAvatarIcon(color = palette.textSecondary, size = 24.dp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = match.name,
                color = palette.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = match.number,
                color = palette.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DialerCallIcon(color = palette.primary, size = 20.dp)
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