package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class EliteExtendedColors(
    val background: Color,
    val surfacePure: Color,
    val surfaceSoft: Color,
    val surfaceTinted: Color,
    val cobaltPrimary: Color,
    val cobaltTile: Color,
    val cobaltTileBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val divider: Color,
    val mintBg: Color,
    val mintText: Color,
    val isDark: Boolean
)

val LocalEliteColors = staticCompositionLocalOf {
    EliteExtendedColors(
        background = EliteIceBackground,
        surfacePure = EliteSurfacePure,
        surfaceSoft = EliteSurfaceSoft,
        surfaceTinted = EliteSurfaceTinted,
        cobaltPrimary = EliteCobaltPrimary,
        cobaltTile = EliteCobaltTile,
        cobaltTileBorder = EliteCobaltTileBorder,
        textPrimary = EliteTextPrimary,
        textSecondary = EliteTextSecondary,
        textMuted = EliteTextMuted,
        divider = EliteDividerLight,
        mintBg = EliteMintBg,
        mintText = EliteMintText,
        isDark = false
    )
}

private val LightColorScheme = lightColorScheme(
    primary = EliteCobaltPrimary,
    onPrimary = Color.White,
    primaryContainer = EliteCobaltLight,
    onPrimaryContainer = EliteCobaltDark,
    secondary = EliteCobaltSecondary,
    onSecondary = Color.White,
    secondaryContainer = EliteSurfaceTinted,
    onSecondaryContainer = EliteTextPrimary,
    background = EliteIceBackground,
    onBackground = EliteTextPrimary,
    surface = EliteSurfaceSoft,
    onSurface = EliteTextPrimary,
    surfaceVariant = EliteSurfaceTinted,
    onSurfaceVariant = EliteTextSecondary,
    outline = EliteDividerLight
)

private val DarkColorScheme = darkColorScheme(
    primary = EliteCobaltPrimary,
    onPrimary = Color.White,
    primaryContainer = EliteCobaltDark,
    onPrimaryContainer = Color.White,
    secondary = EliteCobaltSecondary,
    onSecondary = Color.White,
    secondaryContainer = EliteDarkSurfaceTinted,
    onSecondaryContainer = EliteDarkTextPrimary,
    background = EliteDarkBackground,
    onBackground = EliteDarkTextPrimary,
    surface = EliteDarkSurfaceSoft,
    onSurface = EliteDarkTextPrimary,
    surfaceVariant = EliteDarkSurfaceTinted,
    onSurfaceVariant = EliteDarkTextSecondary,
    outline = EliteDarkDivider
)

val EliteShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) {
        EliteExtendedColors(
            background = EliteDarkBackground,
            surfacePure = EliteDarkSurfacePure,
            surfaceSoft = EliteDarkSurfaceSoft,
            surfaceTinted = EliteDarkSurfaceTinted,
            cobaltPrimary = EliteCobaltPrimary,
            cobaltTile = EliteCobaltTile,
            cobaltTileBorder = EliteCobaltTileBorder,
            textPrimary = EliteDarkTextPrimary,
            textSecondary = EliteDarkTextSecondary,
            textMuted = EliteDarkTextSecondary.copy(alpha = 0.7f),
            divider = EliteDarkDivider,
            mintBg = EliteDarkMintBg,
            mintText = EliteDarkMintText,
            isDark = true
        )
    } else {
        EliteExtendedColors(
            background = EliteIceBackground,
            surfacePure = EliteSurfacePure,
            surfaceSoft = EliteSurfaceSoft,
            surfaceTinted = EliteSurfaceTinted,
            cobaltPrimary = EliteCobaltPrimary,
            cobaltTile = EliteCobaltTile,
            cobaltTileBorder = EliteCobaltTileBorder,
            textPrimary = EliteTextPrimary,
            textSecondary = EliteTextSecondary,
            textMuted = EliteTextMuted,
            divider = EliteDividerLight,
            mintBg = EliteMintBg,
            mintText = EliteMintText,
            isDark = false
        )
    }

    CompositionLocalProvider(LocalEliteColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = EliteShapes,
            content = content
        )
    }
}
