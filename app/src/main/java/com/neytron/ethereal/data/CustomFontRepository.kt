package com.neytron.ethereal.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.neytron.ethereal.ui.theme.EtherealFont
import com.neytron.ethereal.ui.theme.fontFamilyFor
import java.io.File

data class CustomFontInfo(
    val id: String,
    val displayName: String,
    val file: File
)

object CustomFontRepository {

    private const val CUSTOM_PREFIX = "custom_font_"

    fun getFontsDir(context: Context): File {
        val dir = File(context.filesDir, "fonts")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun saveCustomFont(context: Context, uri: Uri): CustomFontInfo? {
        return try {
            val contentResolver = context.contentResolver
            var originalName = "custom_font_${System.currentTimeMillis()}"

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) {
                        originalName = name
                    }
                }
            }

            val ext = when {
                originalName.endsWith(".otf", ignoreCase = true) -> ".otf"
                originalName.endsWith(".ttf", ignoreCase = true) -> ".ttf"
                else -> ".ttf"
            }

            val baseName = originalName.substringBeforeLast(".")
                .replace(Regex("[^a-zA-Z0-9_\\-\\s\\u0400-\\u04FF]"), "")
                .trim()
                .ifEmpty { "Font_${System.currentTimeMillis()}" }

            val fileId = "${CUSTOM_PREFIX}${System.currentTimeMillis()}_${baseName}$ext"
            val targetFile = File(getFontsDir(context), fileId)

            contentResolver.openInputStream(uri)?.use { ins ->
                targetFile.outputStream().use { outs ->
                    ins.copyTo(outs)
                }
            } ?: return null

            CustomFontInfo(
                id = fileId,
                displayName = baseName,
                file = targetFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getCustomFonts(context: Context): List<CustomFontInfo> {
        val dir = getFontsDir(context)
        return dir.listFiles()
            ?.filter { it.isFile && (it.name.endsWith(".ttf", ignoreCase = true) || it.name.endsWith(".otf", ignoreCase = true)) }
            ?.map { file ->
                val displayName = file.name
                    .removePrefix(CUSTOM_PREFIX)
                    .substringAfter("_")
                    .substringBeforeLast(".")
                    .replace("_", " ")
                    .ifEmpty { file.nameWithoutExtension }
                CustomFontInfo(
                    id = file.name,
                    displayName = displayName,
                    file = file
                )
            }
            ?.sortedBy { it.displayName.lowercase() }
            ?: emptyList()
    }

    fun deleteCustomFont(context: Context, fontId: String): Boolean {
        val file = File(getFontsDir(context), fontId)
        return if (file.exists()) {
            file.delete()
        } else false
    }

    fun getFontFamily(context: Context, fontName: String): FontFamily? {
        try {
            val builtin = EtherealFont.valueOf(fontName)
            return fontFamilyFor(builtin)
        } catch (_: Exception) {
        }

        if (fontName.startsWith(CUSTOM_PREFIX) || fontName.contains(".")) {
            val file = File(getFontsDir(context), fontName)
            if (file.exists() && file.isFile) {
                return try {
                    FontFamily(Font(file, FontWeight.Normal))
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }

        return null
    }

    fun getDisplayName(context: Context, fontName: String): String {
        try {
            val builtin = EtherealFont.valueOf(fontName)
            return builtin.displayName
        } catch (_: Exception) {
            val customFont = getCustomFonts(context).firstOrNull { it.id == fontName }
            if (customFont != null) {
                return "✦ ${customFont.displayName}"
            }
        }
        return fontName
    }
}
