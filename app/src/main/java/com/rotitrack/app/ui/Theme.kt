package com.rotitrack.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.rotitrack.app.data.Appearance

/**
 * App colours. Surface and text colours follow [dark]; brand and accent colours
 * stay the same in both themes. Reads are Compose state, so the whole UI
 * recolours when the appearance changes.
 */
object Palette {
    var dark by mutableStateOf(false)

    /** Always-dark surface (Pro screen, dark buttons), the same in both themes. */
    val night = Color(0xFF0B1220)
    val brand = Color(0xFF1B4FF0)
    val brandDeep = Color(0xFF123BC6)
    val aqua = Color(0xFF3FC1FF)
    val waterTop = Color(0xFF7FE3FF)
    val waterDeep = Color(0xFF1E7BEA)

    val background get() = if (dark) Color(0xFF0D121C) else Color(0xFFF1F4FB)
    val card get() = if (dark) Color(0xFF182030) else Color.White
    val ink get() = if (dark) Color(0xFFE9EDF5) else Color(0xFF0B1220)
    val inkSoft get() = if (dark) Color(0xFFB8C2D6) else Color(0xFF3A4762)
    val muted get() = if (dark) Color(0xFF8391AA) else Color(0xFF8494AE)
    val divider get() = if (dark) Color(0xFF263045) else Color(0xFFEDF1F8)
    val track get() = if (dark) Color(0xFF2A3448) else Color(0xFFE4EAF6)
    val chip get() = if (dark) Color(0xFF232C3E) else Color(0xFFEEF2FA)
    val glassBg get() = if (dark) Color(0xFF1C2A44) else Color(0xFFE9F0FF)

    val leaf = Color(0xFF1F9D55)
    val leafSoft get() = if (dark) Color(0xFF15301F) else Color(0xFFE6F6EC)
    val saffron = Color(0xFFFF8A1F)
    val saffronSoft get() = if (dark) Color(0xFF3A2812) else Color(0xFFFFF4E5)
    val protein = Color(0xFF7C5CFF)
    val carbs = Color(0xFFFFB020)
    val fat = Color(0xFFFF5C8A)
    val nonveg = Color(0xFFB3261E)
    val danger = Color(0xFFEF4444)
}

@Composable
fun isDark(appearance: Appearance): Boolean = when (appearance) {
    Appearance.LIGHT -> false
    Appearance.DARK -> true
    Appearance.SYSTEM -> isSystemInDarkTheme()
}

@Composable
fun RotiTrackTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    if (Palette.dark != dark) Palette.dark = dark
    val scheme = if (dark) {
        darkColorScheme(
            primary = Palette.brand,
            onPrimary = Color.White,
            secondary = Palette.leaf,
            background = Palette.background,
            surface = Palette.card,
            onSurface = Palette.ink,
            onBackground = Palette.ink,
            error = Palette.danger,
        )
    } else {
        lightColorScheme(
            primary = Palette.brand,
            onPrimary = Color.White,
            secondary = Palette.leaf,
            background = Palette.background,
            surface = Palette.card,
            onSurface = Palette.ink,
            onBackground = Palette.ink,
            error = Palette.danger,
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
