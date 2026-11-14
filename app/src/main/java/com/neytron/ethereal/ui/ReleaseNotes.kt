package com.neytron.ethereal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neytron.ethereal.ui.theme.EtherealPalette

/**
 * Мини-рендерер заметок к релизу.
 *
 * GitHub-релизы у Ethereal написаны смесью Markdown и HTML (`<p align="center">`,
 * `<img>`, `<h3>`), из-за чего сторонний markdown-рендерер показывал пустоту или мусор.
 * Здесь HTML вычищается, а из разметки поддерживается ровно то, что реально используется:
 * заголовки, списки, таблицы, код, цитаты и разделители.
 */
sealed interface NoteBlock {
    data class Heading(val level: Int, val text: String) : NoteBlock
    data class Paragraph(val text: String) : NoteBlock
    data class Bullets(val items: List<String>) : NoteBlock
    data class Table(val rows: List<List<String>>) : NoteBlock
    data class Code(val text: String) : NoteBlock
    data class Quote(val text: String) : NoteBlock
    data object Divider : NoteBlock
}

object ReleaseNotes {

    private val htmlEntity = Regex("&(amp|lt|gt|quot|#39|nbsp);")
    private val imageLine = Regex("^!\\[[^\\]]*]\\([^)]*\\)\\s*$")
    private val leadingBullet = Regex("^(\\s*)[-*+]\\s+(.*)$")
    private val heading = Regex("^(#{1,6})\\s+(.*)$")
    private val fence = Regex("^```.*$")

    fun parse(raw: String): List<NoteBlock> {
        val text = sanitize(raw)
        if (text.isBlank()) return emptyList()

        val blocks = mutableListOf<NoteBlock>()
        val paragraph = StringBuilder()
        val bullets = mutableListOf<String>()
        val tableRows = mutableListOf<List<String>>()
        var codeBuffer: StringBuilder? = null

        fun flushParagraph() {
            val p = paragraph.toString().trim()
            if (p.isNotEmpty()) blocks += NoteBlock.Paragraph(p)
            paragraph.clear()
        }

        fun flushBullets() {
            if (bullets.isNotEmpty()) {
                blocks += NoteBlock.Bullets(bullets.toList())
                bullets.clear()
            }
        }

        fun flushTable() {
            if (tableRows.isNotEmpty()) {
                blocks += NoteBlock.Table(tableRows.toList())
                tableRows.clear()
            }
        }

        fun flushAll() {
            flushParagraph()
            flushBullets()
            flushTable()
        }

        for (line in text.lines()) {
            // Код-блок
            if (codeBuffer != null) {
                if (fence.matches(line.trim())) {
                    blocks += NoteBlock.Code(codeBuffer.toString().trimEnd())
                    codeBuffer = null
                } else {
                    codeBuffer.appendLine(line)
                }
                continue
            }
            if (fence.matches(line.trim())) {
                flushAll()
                codeBuffer = StringBuilder()
                continue
            }

            val trimmed = line.trim()

            when {
                trimmed.isEmpty() -> flushAll()

                trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                    flushAll()
                    blocks += NoteBlock.Divider
                }

                heading.matches(trimmed) -> {
                    flushAll()
                    val match = heading.find(trimmed)!!
                    val level = match.groupValues[1].length
                    blocks += NoteBlock.Heading(level, match.groupValues[2].trim())
                }

                trimmed.startsWith(">") -> {
                    flushAll()
                    blocks += NoteBlock.Quote(trimmed.removePrefix(">").trim())
                }

                trimmed.startsWith("|") -> {
                    flushParagraph()
                    flushBullets()
                    val cells = trimmed.trim('|').split("|").map { it.trim() }
                    // строка-разделитель |---|---|
                    if (cells.all { it.isEmpty() || it.all { ch -> ch == '-' || ch == ':' } }) {
                        continue
                    }
                    tableRows += cells
                }

                leadingBullet.matches(line) -> {
                    flushParagraph()
                    flushTable()
                    val match = leadingBullet.find(line)!!
                    val indent = match.groupValues[1].length
                    val item = match.groupValues[2].trim()
                    bullets += if (indent >= 2) "◦ $item" else item
                }

                else -> {
                    flushBullets()
                    flushTable()
                    if (paragraph.isNotEmpty()) paragraph.append(' ')
                    paragraph.append(trimmed)
                }
            }
        }

        codeBuffer?.let { blocks += NoteBlock.Code(it.toString().trimEnd()) }
        flushAll()
        return blocks
    }

    /** HTML → Markdown-подобный текст. Всё, что не умеем рисовать, просто убираем. */
    fun sanitize(raw: String): String {
        var text = raw.replace("\r\n", "\n").replace('\r', '\n')

        text = text.replace(Regex("(?i)<br\\s*/?>"), "\n")
        text = text.replace(Regex("(?i)</(p|div|center|details|summary|h[1-6])>"), "\n")
        text = text.replace(Regex("(?i)<h([1-6])[^>]*>")) { m ->
            "#".repeat(m.groupValues[1].toIntOrNull() ?: 3) + " "
        }
        text = text.replace(Regex("(?i)<a\\s+[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>")) { m ->
            "[${m.groupValues[2]}](${m.groupValues[1]})"
        }
        text = text.replace(Regex("(?i)<img[^>]*>"), "")
        text = text.replace(Regex("(?i)<[^>]+>"), "")
        text = text.replace(htmlEntity) { m ->
            when (m.groupValues[1]) {
                "amp" -> "&"
                "lt" -> "<"
                "gt" -> ">"
                "quot" -> "\""
                "#39" -> "'"
                else -> " "
            }
        }

        // строки, состоящие только из картинки, не нужны
        text = text.lines().filterNot { imageLine.matches(it.trim()) }.joinToString("\n")
        // больше двух пустых строк подряд — лишнее
        text = text.replace(Regex("\n{3,}"), "\n\n")
        return text.trim()
    }

    /** Инлайн-разметка: **жирный**, *курсив*, `код`, [ссылка](url). */
    fun inline(text: String, palette: EtherealPalette): AnnotatedString = buildAnnotatedString {
        val pattern = Regex("\\*\\*(.+?)\\*\\*|\\*([^*]+?)\\*|`([^`]+)`|\\[([^\\]]+)]\\(([^)]+)\\)|__(.+?)__")
        var last = 0
        for (match in pattern.findAll(text)) {
            if (match.range.first > last) append(text.substring(last, match.range.first))
            val v = match.groupValues
            when {
                v[1].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(v[1]) }
                v[2].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(v[2]) }
                v[3].isNotEmpty() -> withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        color = palette.primary,
                        fontSize = 12.sp
                    )
                ) { append(v[3]) }
                v[4].isNotEmpty() -> withStyle(
                    SpanStyle(color = palette.primary, fontStyle = FontStyle.Italic)
                ) { append(v[4]) }
                v[6].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(v[6]) }
            }
            last = match.range.last + 1
        }
        if (last < text.length) append(text.substring(last))
    }
}

@Composable
fun ReleaseNotesView(
    blocks: List<NoteBlock>,
    palette: EtherealPalette,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (block in blocks) {
            when (block) {
                is NoteBlock.Heading -> {
                    val (size, weight) = when (block.level) {
                        1 -> 18.sp to FontWeight.Bold
                        2 -> 16.sp to FontWeight.Bold
                        else -> 14.sp to FontWeight.SemiBold
                    }
                    Text(
                        text = ReleaseNotes.inline(block.text, palette),
                        color = if (block.level <= 2) palette.primary else palette.textPrimary,
                        fontSize = size,
                        fontWeight = weight,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                is NoteBlock.Paragraph -> Text(
                    text = ReleaseNotes.inline(block.text, palette),
                    color = palette.textPrimary.copy(alpha = 0.92f),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )

                is NoteBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    block.items.forEach { item ->
                        Row(verticalAlignment = Alignment.Top) {
                            val nested = item.startsWith("◦ ")
                            Box(
                                modifier = Modifier
                                    .padding(top = if (nested) 7.dp else 6.dp)
                                    .padding(start = if (nested) 14.dp else 0.dp)
                                    .size(if (nested) 3.dp else 4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(palette.primary.copy(alpha = if (nested) 0.5f else 0.9f))
                            )
                            Spacer(modifier = Modifier.width(9.dp))
                            Text(
                                text = ReleaseNotes.inline(item.removePrefix("◦ "), palette),
                                color = palette.textPrimary.copy(alpha = 0.92f),
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }

                is NoteBlock.Table -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.surfaceVariant.copy(alpha = 0.25f))
                ) {
                    block.rows.forEachIndexed { index, row ->
                        if (index > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(palette.outline.copy(alpha = 0.2f))
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
                            row.forEachIndexed { cellIndex, cell ->
                                Text(
                                    text = ReleaseNotes.inline(cell, palette),
                                    color = if (index == 0) palette.textSecondary else palette.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal,
                                    lineHeight = 17.sp,
                                    modifier = Modifier
                                        .weight(if (cellIndex == 0) 0.8f else 1.6f)
                                        .padding(end = 8.dp)
                                )
                            }
                        }
                    }
                }

                is NoteBlock.Code -> Text(
                    text = block.text,
                    color = palette.primary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.surfaceVariant.copy(alpha = 0.35f))
                        .padding(10.dp)
                )

                is NoteBlock.Quote -> Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(palette.primary.copy(alpha = 0.6f))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = ReleaseNotes.inline(block.text, palette),
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 19.sp
                    )
                }

                NoteBlock.Divider -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .height(1.dp)
                        .background(palette.outline.copy(alpha = 0.25f))
                )
            }
        }
    }
}
