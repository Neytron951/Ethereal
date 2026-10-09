package com.neytron.ethereal.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Палитра темы.
 * isLight — true, если фон светлый (для статус-бара и light scheme).
 */
data class EtherealPalette(
    val id: String,
    val name: String,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val secondary: Color,
    val outline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val isLight: Boolean = false
)

// ============================================================
// ТЁМНЫЕ ПАЛИТРЫ
// ============================================================

val EtherealGreenPalette = EtherealPalette(
    id = "ethereal_green",
    name = "Ethereal",
    background = Color(0xFF0A0F0A),
    surface = Color(0xFF131A13),
    surfaceVariant = Color(0xFF1C261C),
    primary = Color(0xFFB8FF3D),
    secondary = Color(0xFF7DDF64),
    outline = Color(0xFF2A352A),
    textPrimary = Color(0xFFE8F0E8),
    textSecondary = Color(0xFF8FA38F)
)

val ObsidianPalette = EtherealPalette(
    id = "obsidian", name = "Obsidian",
    background = Color(0xFF0B0B0F),
    surface = Color(0xFF15151A),
    surfaceVariant = Color(0xFF1F1F26),
    primary = Color(0xFFB4B4C8),
    secondary = Color(0xFF8888A0),
    outline = Color(0xFF2A2A33),
    textPrimary = Color(0xFFE8E8F0),
    textSecondary = Color(0xFF9090A0)
)

val AmoledPalette = EtherealPalette(
    id = "amoled", name = "AMOLED",
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFF141414),
    primary = Color(0xFFFFFFFF),
    secondary = Color(0xFFAAAAAA),
    outline = Color(0xFF1F1F1F),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF888888)
)

val CyberpunkPalette = EtherealPalette(
    id = "cyberpunk", name = "Cyberpunk",
    background = Color(0xFF0D0221),
    surface = Color(0xFF1A0B2E),
    surfaceVariant = Color(0xFF261440),
    primary = Color(0xFFFFEE00),
    secondary = Color(0xFFFF2A6D),
    outline = Color(0xFF3D1F5C),
    textPrimary = Color(0xFFF0E8FF),
    textSecondary = Color(0xFF9A7FB8)
)

val GithubDarkPalette = EtherealPalette(
    id = "github_dark", name = "GitHub Dark",
    background = Color(0xFF0D1117),
    surface = Color(0xFF161B22),
    surfaceVariant = Color(0xFF21262D),
    primary = Color(0xFF58A6FF),
    secondary = Color(0xFF79C0FF),
    outline = Color(0xFF30363D),
    textPrimary = Color(0xFFC9D1D9),
    textSecondary = Color(0xFF8B949E)
)

val AyuDarkPalette = EtherealPalette(
    id = "ayu_dark", name = "Ayu Dark",
    background = Color(0xFF0A0E14),
    surface = Color(0xFF0F1419),
    surfaceVariant = Color(0xFF1A1F26),
    primary = Color(0xFFFFB454),
    secondary = Color(0xFF36A3D9),
    outline = Color(0xFF1F262E),
    textPrimary = Color(0xFFE6E1CF),
    textSecondary = Color(0xFF8A9199)
)

val NightOwlPalette = EtherealPalette(
    id = "night_owl", name = "Night Owl",
    background = Color(0xFF011627),
    surface = Color(0xFF0B253F),
    surfaceVariant = Color(0xFF122F4F),
    primary = Color(0xFF82AAFF),
    secondary = Color(0xFF7FDBCA),
    outline = Color(0xFF1D3E5E),
    textPrimary = Color(0xFFD6DEEB),
    textSecondary = Color(0xFF637777)
)

val MaterialOceanPalette = EtherealPalette(
    id = "material_ocean", name = "Material Ocean",
    background = Color(0xFF0F111A),
    surface = Color(0xFF1A1C25),
    surfaceVariant = Color(0xFF232632),
    primary = Color(0xFF82AAFF),
    secondary = Color(0xFF89DDFF),
    outline = Color(0xFF2F3340),
    textPrimary = Color(0xFFA6ACCD),
    textSecondary = Color(0xFF717CB4)
)

val CarbonfoxPalette = EtherealPalette(
    id = "carbonfox", name = "Carbonfox",
    background = Color(0xFF161616),
    surface = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFF2A2A2A),
    primary = Color(0xFFEE5396),
    secondary = Color(0xFF08BDBA),
    outline = Color(0xFF393939),
    textPrimary = Color(0xFFF2F4F8),
    textSecondary = Color(0xFF8D8D8D)
)

val DraculaPalette = EtherealPalette(
    id = "dracula", name = "Dracula",
    background = Color(0xFF282A36),
    surface = Color(0xFF343746),
    surfaceVariant = Color(0xFF44475A),
    primary = Color(0xFFBD93F9),
    secondary = Color(0xFFFF79C6),
    outline = Color(0xFF4A4D5E),
    textPrimary = Color(0xFFF8F8F2),
    textSecondary = Color(0xFF6272A4)
)

val NordPalette = EtherealPalette(
    id = "nord", name = "Nord",
    background = Color(0xFF2E3440),
    surface = Color(0xFF3B4252),
    surfaceVariant = Color(0xFF434C5E),
    primary = Color(0xFF88C0D0),
    secondary = Color(0xFF81A1C1),
    outline = Color(0xFF4C566A),
    textPrimary = Color(0xFFECEFF4),
    textSecondary = Color(0xFF7A8393)
)

val CatppuccinPalette = EtherealPalette(
    id = "catppuccin", name = "Catppuccin Mocha",
    background = Color(0xFF1E1E2E),
    surface = Color(0xFF313244),
    surfaceVariant = Color(0xFF45475A),
    primary = Color(0xFFCBA6F7),
    secondary = Color(0xFF89B4FA),
    outline = Color(0xFF585B70),
    textPrimary = Color(0xFFCDD6F4),
    textSecondary = Color(0xFF7F849C)
)

val TokyoNightPalette = EtherealPalette(
    id = "tokyo_night", name = "Tokyo Night",
    background = Color(0xFF1A1B26),
    surface = Color(0xFF24283B),
    surfaceVariant = Color(0xFF2F334D),
    primary = Color(0xFF7AA2F7),
    secondary = Color(0xFFBB9AF7),
    outline = Color(0xFF3B4261),
    textPrimary = Color(0xFFC0CAF5),
    textSecondary = Color(0xFF565F89)
)

val RosePinePalette = EtherealPalette(
    id = "rose_pine", name = "Rosé Pine",
    background = Color(0xFF191724),
    surface = Color(0xFF1F1D2E),
    surfaceVariant = Color(0xFF26233A),
    primary = Color(0xFFEBBCBA),
    secondary = Color(0xFFC4A7E7),
    outline = Color(0xFF403D52),
    textPrimary = Color(0xFFE0DEF4),
    textSecondary = Color(0xFF6E6A86)
)

val GruvboxPalette = EtherealPalette(
    id = "gruvbox", name = "Gruvbox Dark",
    background = Color(0xFF282828),
    surface = Color(0xFF3C3836),
    surfaceVariant = Color(0xFF504945),
    primary = Color(0xFFFABD2F),
    secondary = Color(0xFFFB4934),
    outline = Color(0xFF665C54),
    textPrimary = Color(0xFFEBDBB2),
    textSecondary = Color(0xFFA89984)
)

val OneDarkPalette = EtherealPalette(
    id = "one_dark", name = "One Dark",
    background = Color(0xFF282C34),
    surface = Color(0xFF2F343F),
    surfaceVariant = Color(0xFF3A404D),
    primary = Color(0xFF61AFEF),
    secondary = Color(0xFFC678DD),
    outline = Color(0xFF4B5263),
    textPrimary = Color(0xFFABB2BF),
    textSecondary = Color(0xFF5C6370)
)

val EverforestPalette = EtherealPalette(
    id = "everforest", name = "Everforest",
    background = Color(0xFF2D353B),
    surface = Color(0xFF343F44),
    surfaceVariant = Color(0xFF3D484D),
    primary = Color(0xFFA7C080),
    secondary = Color(0xFFDBBC7F),
    outline = Color(0xFF475258),
    textPrimary = Color(0xFFD3C6AA),
    textSecondary = Color(0xFF859289)
)

val KanagawaPalette = EtherealPalette(
    id = "kanagawa", name = "Kanagawa",
    background = Color(0xFF1F1F28),
    surface = Color(0xFF2A2A37),
    surfaceVariant = Color(0xFF363646),
    primary = Color(0xFF7E9CD8),
    secondary = Color(0xFFD27E99),
    outline = Color(0xFF54546D),
    textPrimary = Color(0xFFDCD7BA),
    textSecondary = Color(0xFF727169)
)

val SynthwavePalette = EtherealPalette(
    id = "synthwave", name = "Synthwave",
    background = Color(0xFF1A0B2E),
    surface = Color(0xFF241041),
    surfaceVariant = Color(0xFF311654),
    primary = Color(0xFFFF6AC1),
    secondary = Color(0xFF36F9F6),
    outline = Color(0xFF4A2278),
    textPrimary = Color(0xFFF0E8FF),
    textSecondary = Color(0xFF9A7FB8)
)

val SolarizedDarkPalette = EtherealPalette(
    id = "solarized_dark", name = "Solarized Dark",
    background = Color(0xFF002B36),
    surface = Color(0xFF073642),
    surfaceVariant = Color(0xFF0E4B58),
    primary = Color(0xFF268BD2),
    secondary = Color(0xFF2AA198),
    outline = Color(0xFF155E6E),
    textPrimary = Color(0xFF93A1A1),
    textSecondary = Color(0xFF657B83)
)

val MonochromePalette = EtherealPalette(
    id = "monochrome", name = "Monochrome",
    background = Color(0xFF1A1A1A),
    surface = Color(0xFF242424),
    surfaceVariant = Color(0xFF2E2E2E),
    primary = Color(0xFFFFFFFF),
    secondary = Color(0xFFB0B0B0),
    outline = Color(0xFF404040),
    textPrimary = Color(0xFFF0F0F0),
    textSecondary = Color(0xFF999999)
)

// ============================================================
// СВЕТЛЫЕ ПАЛИТРЫ
// ============================================================

val SolarizedLightPalette = EtherealPalette(
    id = "solarized_light", name = "Solarized Light",
    background = Color(0xFFFDF6E3),
    surface = Color(0xFFEEE8D5),
    surfaceVariant = Color(0xFFE4DDC8),
    primary = Color(0xFF268BD2),
    secondary = Color(0xFF2AA198),
    outline = Color(0xFFD3CBB7),
    textPrimary = Color(0xFF073642),
    textSecondary = Color(0xFF586E75),
    isLight = true
)

val GithubLightPalette = EtherealPalette(
    id = "github_light", name = "GitHub Light",
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF6F8FA),
    surfaceVariant = Color(0xFFEAEEF2),
    primary = Color(0xFF0969DA),
    secondary = Color(0xFF218BFF),
    outline = Color(0xFFD0D7DE),
    textPrimary = Color(0xFF1F2328),
    textSecondary = Color(0xFF656D76),
    isLight = true
)

val OneLightPalette = EtherealPalette(
    id = "one_light", name = "One Light",
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFF0F0F1),
    surfaceVariant = Color(0xFFE5E5E6),
    primary = Color(0xFF4078F2),
    secondary = Color(0xFFA626A4),
    outline = Color(0xFFD4D4D5),
    textPrimary = Color(0xFF383A42),
    textSecondary = Color(0xFF696C77),
    isLight = true
)

val CatppuccinLattePalette = EtherealPalette(
    id = "catppuccin_latte", name = "Catppuccin Latte",
    background = Color(0xFFEFF1F5),
    surface = Color(0xFFE6E9EF),
    surfaceVariant = Color(0xFFDCE0E8),
    primary = Color(0xFF8839EF),
    secondary = Color(0xFF1E66F5),
    outline = Color(0xFFCCD0DA),
    textPrimary = Color(0xFF4C4F69),
    textSecondary = Color(0xFF6C6F85),
    isLight = true
)

val RosePineDawnPalette = EtherealPalette(
    id = "rose_pine_dawn", name = "Rosé Pine Dawn",
    background = Color(0xFFFAF4ED),
    surface = Color(0xFFF2E9E1),
    surfaceVariant = Color(0xFFEBE0D8),
    primary = Color(0xFFB4637A),
    secondary = Color(0xFF907AA9),
    outline = Color(0xFFDFDAD9),
    textPrimary = Color(0xFF575279),
    textSecondary = Color(0xFF797593),
    isLight = true
)

val GruvboxLightPalette = EtherealPalette(
    id = "gruvbox_light", name = "Gruvbox Light",
    background = Color(0xFFFBF1C7),
    surface = Color(0xFFEBDBB2),
    surfaceVariant = Color(0xFFD5C4A1),
    primary = Color(0xFFB57614),
    secondary = Color(0xFFAF3A03),
    outline = Color(0xFFBDAE93),
    textPrimary = Color(0xFF3C3836),
    textSecondary = Color(0xFF7C6F64),
    isLight = true
)

val NordLightPalette = EtherealPalette(
    id = "nord_light", name = "Nord Light",
    background = Color(0xFFECEFF4),
    surface = Color(0xFFE5E9F0),
    surfaceVariant = Color(0xFFD8DEE9),
    primary = Color(0xFF5E81AC),
    secondary = Color(0xFF81A1C1),
    outline = Color(0xFFC5CCD9),
    textPrimary = Color(0xFF2E3440),
    textSecondary = Color(0xFF4C566A),
    isLight = true
)

val PaperPalette = EtherealPalette(
    id = "paper", name = "Paper",
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFFEBEBEB),
    primary = Color(0xFF212121),
    secondary = Color(0xFF616161),
    outline = Color(0xFFE0E0E0),
    textPrimary = Color(0xFF1A1A1A),
    textSecondary = Color(0xFF757575),
    isLight = true
)

// ============================================================
// СПИСКИ И ХЕЛПЕРЫ
// ============================================================

val AllPalettes: List<EtherealPalette> = listOf(
    // Тёмные
    EtherealGreenPalette,
    ObsidianPalette,
    AmoledPalette,
    MonochromePalette,
    CyberpunkPalette,
    GithubDarkPalette,
    AyuDarkPalette,
    NightOwlPalette,
    MaterialOceanPalette,
    CarbonfoxPalette,
    DraculaPalette,
    NordPalette,
    CatppuccinPalette,
    TokyoNightPalette,
    RosePinePalette,
    GruvboxPalette,
    OneDarkPalette,
    EverforestPalette,
    KanagawaPalette,
    SynthwavePalette,
    SolarizedDarkPalette,
    // Светлые
    PaperPalette,
    GithubLightPalette,
    SolarizedLightPalette,
    OneLightPalette,
    CatppuccinLattePalette,
    RosePineDawnPalette,
    GruvboxLightPalette,
    NordLightPalette
)

fun paletteById(id: String): EtherealPalette =
    AllPalettes.find { it.id == id } ?: EtherealGreenPalette