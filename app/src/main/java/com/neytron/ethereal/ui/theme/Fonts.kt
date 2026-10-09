package com.neytron.ethereal.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.neytron.ethereal.R

enum class EtherealFont(val displayName: String) {
    DEFAULT("Системный"),
    ALDRICH("Aldrich"),
    CANTARELL("Cantarell"),
    CHERRY("Cherry"),
    ENCODE("Encode Sans"),
    INTER("Inter"),
    JARO("Jaro"),
    JETBRAINS_MONO("JetBrains Mono"),
    LIMELIGHT("Limelight"),
    QUANTICO("Quantico"),
    ROBOTO("Roboto"),
    ROBOTO_MONO("Roboto Mono"),
    SANSATION("Sansation"),
    SHORTSTACK("Short Stack"),
    SNIGLET("Sniglet"),
    SPACE_GROTESK("Space Grotesk")
}

fun fontFamilyFor(font: EtherealFont): FontFamily? = when (font) {
    EtherealFont.DEFAULT -> null
    EtherealFont.ALDRICH -> FontFamily(Font(R.font.aldrich_regular, FontWeight.Normal))
    EtherealFont.CANTARELL -> FontFamily(Font(R.font.cantarell_regular, FontWeight.Normal))
    EtherealFont.CHERRY -> FontFamily(Font(R.font.cherry_regular, FontWeight.Normal))
    EtherealFont.ENCODE -> FontFamily(Font(R.font.encode_regular, FontWeight.Normal))
    EtherealFont.INTER -> FontFamily(Font(R.font.inter_regular, FontWeight.Normal))
    EtherealFont.JARO -> FontFamily(Font(R.font.jaro_regular, FontWeight.Normal))
    EtherealFont.JETBRAINS_MONO -> FontFamily(Font(R.font.jetbrains_mono_regular, FontWeight.Normal))
    EtherealFont.LIMELIGHT -> FontFamily(Font(R.font.limelight_regular, FontWeight.Normal))
    EtherealFont.QUANTICO -> FontFamily(Font(R.font.quantico_regular, FontWeight.Normal))
    EtherealFont.ROBOTO -> FontFamily(Font(R.font.roboto_regular, FontWeight.Normal))
    EtherealFont.ROBOTO_MONO -> FontFamily(Font(R.font.roboto_mono_regular, FontWeight.Normal))
    EtherealFont.SANSATION -> FontFamily(Font(R.font.sansation_regular, FontWeight.Normal))
    EtherealFont.SHORTSTACK -> FontFamily(Font(R.font.shortstack_regular, FontWeight.Normal))
    EtherealFont.SNIGLET -> FontFamily(Font(R.font.sniglet_regular, FontWeight.Normal))
    EtherealFont.SPACE_GROTESK -> FontFamily(Font(R.font.space_grotesk_regular, FontWeight.Normal))
}