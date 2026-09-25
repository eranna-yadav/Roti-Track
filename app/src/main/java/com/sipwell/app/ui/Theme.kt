package com.sipwell.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Palette {
    val brand = Color(0xFF1B4FF0)
    val brandDeep = Color(0xFF123BC6)
    val aqua = Color(0xFF3FC1FF)
    val waterTop = Color(0xFF7FE3FF)
    val waterDeep = Color(0xFF1E7BEA)
    val background = Color(0xFFF1F4FB)
    val card = Color.White
    val ink = Color(0xFF0B1220)
    val inkSoft = Color(0xFF3A4762)
    val muted = Color(0xFF8494AE)
    val divider = Color(0xFFEDF1F8)
    val track = Color(0xFFE4EAF6)
    val chip = Color(0xFFEEF2FA)

    val leaf = Color(0xFF1F9D55)
    val leafSoft = Color(0xFFE6F6EC)
    val saffron = Color(0xFFFF8A1F)
    val saffronSoft = Color(0xFFFFF4E5)
    val protein = Color(0xFF7C5CFF)
    val carbs = Color(0xFFFFB020)
    val fat = Color(0xFFFF5C8A)
    val nonveg = Color(0xFFB3261E)
    val danger = Color(0xFFEF4444)
}

@Composable
fun SipwellTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Palette.brand,
            onPrimary = Color.White,
            secondary = Palette.leaf,
            background = Palette.background,
            surface = Palette.card,
            onSurface = Palette.ink,
            onBackground = Palette.ink,
            surfaceContainer = Color.White,
            surfaceContainerLow = Color.White,
            error = Palette.danger,
        ),
        content = content,
    )
}
