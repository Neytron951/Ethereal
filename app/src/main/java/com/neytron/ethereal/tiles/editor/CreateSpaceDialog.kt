package com.neytron.ethereal.tiles.editor

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neytron.ethereal.R
import com.neytron.ethereal.ui.theme.EtherealPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Диалог создания пространства.
 * Используется из TileEditorPanel (пресет «Пространство»).
 *
 * Возвращает onCreate(name, iconUri, iconText).
 */
@Composable
fun CreateSpaceDialog(
    palette: EtherealPalette,
    onDismiss: () -> Unit,
    onCreate: (name: String, iconUri: String?, iconText: String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var iconText by remember { mutableStateOf("") }
    var iconUri by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    copySpaceIconToInternal(context, uri)
                }
                if (saved != null) {
                    iconUri = saved
                    iconText = ""
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(20.dp))
                .background(palette.surface)
                .clickable(enabled = false, onClick = {})
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.create_space_title),
                color = palette.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ИМЯ
            Text(
                text = stringResource(R.string.create_space_name_label),
                color = palette.textSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                if (name.isEmpty()) {
                    Text(
                        text = stringResource(R.string.create_space_name_hint),
                        color = palette.textSecondary,
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    textStyle = TextStyle(
                        color = palette.textPrimary,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(palette.primary),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ИКОНКА-ЭМОДЗИ
            Text(
                text = stringResource(R.string.create_space_icon_text_label),
                color = palette.textSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                if (iconText.isEmpty()) {
                    Text(
                        text = stringResource(R.string.create_space_icon_text_hint),
                        color = palette.textSecondary,
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = iconText,
                    onValueChange = { newValue ->
                        iconText = newValue.take(2)
                        if (iconText.isNotEmpty()) iconUri = null
                    },
                    textStyle = TextStyle(
                        color = palette.textPrimary,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(palette.primary),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ИКОНКА-КАРТИНКА
            Text(
                text = stringResource(R.string.create_space_icon_image_label),
                color = palette.textSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.primary.copy(alpha = 0.15f))
                        .clickable {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (iconUri != null) stringResource(R.string.replace) else stringResource(R.string.select),
                        color = palette.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (iconUri != null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.surfaceVariant.copy(alpha = 0.6f))
                            .clickable { iconUri = null }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.remove),
                            color = palette.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // КНОПКИ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.surfaceVariant.copy(alpha = 0.5f))
                        .clickable(onClick = onDismiss)
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = palette.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (name.isNotBlank()) palette.primary
                            else palette.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .clickable(enabled = name.isNotBlank()) {
                            onCreate(
                                name.trim(),
                                iconUri,
                                iconText.trim().takeIf { it.isNotEmpty() }
                            )
                        }
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.create),
                        color = if (name.isNotBlank()) palette.background else palette.textSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Копирует выбранную картинку в filesDir/space_icons и возвращает путь.
 */
fun copySpaceIconToInternal(context: Context, uri: Uri): String? {
    return try {
        val dir = File(context.filesDir, "space_icons")
        if (!dir.exists()) dir.mkdirs()

        val mimeType = context.contentResolver.getType(uri)
        val extension = when {
            mimeType == "image/gif" -> "gif"
            mimeType == "image/png" -> "png"
            mimeType == "image/webp" -> "webp"
            mimeType != null -> MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mimeType) ?: "jpg"
            else -> "jpg"
        }

        val file = File(dir, "space_${System.currentTimeMillis()}.$extension")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}