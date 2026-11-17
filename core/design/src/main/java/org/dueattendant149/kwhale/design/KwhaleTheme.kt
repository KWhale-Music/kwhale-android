package org.dueattendant149.kwhale.design

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ThemeMode { SYSTEM, DARK, LIGHT, AMOLED }

data class KwhaleAccent(val id: Int, val name: String, val argb: Long) {
    val color: Color get() = Color((0xFF000000L or argb).toInt())
}

/** Accents lifted from the original app catalog `theme_catalog.json`. */
object KwhaleAccents {
    val all: List<KwhaleAccent> = AllKwhaleAccents
    val default: KwhaleAccent = all.firstOrNull { it.id == 2 } ?: all.first()
    fun byId(id: Int): KwhaleAccent = all.firstOrNull { it.id == id } ?: default
}

private val VkSansText = FontFamily(
    Font(R.font.vk_sans_text_light, FontWeight.Light),
    Font(R.font.vk_sans_text_regular, FontWeight.Normal),
    Font(R.font.vk_sans_text_medium, FontWeight.Medium),
    Font(R.font.vk_sans_text_demibold, FontWeight.SemiBold),
    Font(R.font.vk_sans_text_bold, FontWeight.Bold),
)

private val VkSansDisplay = FontFamily(
    Font(R.font.vk_sans_display_regular, FontWeight.Normal),
    Font(R.font.vk_sans_display_medium, FontWeight.Medium),
    Font(R.font.vk_sans_display_demibold, FontWeight.SemiBold),
    Font(R.font.vk_sans_display_bold, FontWeight.Bold),
)

val KwhaleTypography = Typography(
    displayLarge = TextStyle(fontFamily = VkSansDisplay, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp),
    displayMedium = TextStyle(fontFamily = VkSansDisplay, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    displaySmall = TextStyle(fontFamily = VkSansDisplay, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineLarge = TextStyle(fontFamily = VkSansDisplay, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = VkSansDisplay, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontFamily = VkSansDisplay, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = VkSansDisplay, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = VkSansText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = VkSansText, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = VkSansText, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = VkSansText, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = VkSansText, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = VkSansText, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = VkSansText, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = VkSansText, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)

val KwhaleShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private fun Color.onColor(): Color = if (luminance() > 0.52f) Color(0xFF0B0B0B) else Color.White

private fun blend(base: Color, accent: Color, fraction: Float): Color =
    lerp(base, accent, fraction.coerceIn(0f, 1f))

private fun darkScheme(accent: Color, amoled: Boolean): ColorScheme {
    val surface = if (amoled) Color(0xFF000000) else Color(0xFF121212)
    val containerLowest = if (amoled) Color(0xFF000000) else Color(0xFF0D0D0D)
    val containerLow = if (amoled) Color(0xFF080808) else Color(0xFF1A1A1A)
    val container = if (amoled) Color(0xFF0D0D0D) else Color(0xFF1E1E1E)
    val containerHigh = if (amoled) Color(0xFF151515) else Color(0xFF2A2A2A)
    val containerHighest = if (amoled) Color(0xFF1D1D1D) else Color(0xFF353535)

    val primary = if (accent.luminance() < 0.55f) blend(accent, Color.White, 0.45f) else accent
    val onPrimary = primary.onColor()
    val primaryContainer = blend(container, accent, 0.30f)
    val onPrimaryContainer = if (primaryContainer.luminance() > 0.48f) Color(0xFF071018) else Color(0xFFE8F1FF)
    val secondaryContainer = blend(container, accent, 0.16f)
    val onSecondaryContainer = if (secondaryContainer.luminance() > 0.48f) Color(0xFF071018) else Color(0xFFDCE7F5)

    return darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = blend(primary, Color.Black, 0.25f),
        secondary = blend(primary, Color.White, 0.35f),
        onSecondary = Color(0xFF071018),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = Color(0xFF9CCFB8),
        onTertiary = Color(0xFF06281C),
        tertiaryContainer = Color(0xFF1D4D3A),
        onTertiaryContainer = Color(0xFFB9F0D5),
        background = surface,
        onBackground = Color(0xFFE6E1E5),
        surface = surface,
        onSurface = Color(0xFFE6E1E5),
        surfaceVariant = containerHigh,
        onSurfaceVariant = Color(0xFFCAC4D0),
        surfaceTint = primary,
        inverseSurface = Color(0xFFE6E1E5),
        inverseOnSurface = Color(0xFF303030),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF938F99),
        outlineVariant = Color(0xFF49454F),
        scrim = Color.Black,
        surfaceBright = containerHighest,
        surfaceDim = containerLowest,
        surfaceContainer = container,
        surfaceContainerHigh = containerHigh,
        surfaceContainerHighest = containerHighest,
        surfaceContainerLow = containerLow,
        surfaceContainerLowest = containerLowest,
    )
}

private fun lightScheme(accent: Color): ColorScheme {
    val surface = Color(0xFFF7F7FA)
    val primary = if (accent.luminance() > 0.28f) blend(accent, Color.Black, 0.42f) else accent
    val onPrimary = primary.onColor()
    val primaryContainer = blend(Color.White, accent, 0.18f)
    val onPrimaryContainer = if (primaryContainer.luminance() > 0.48f) Color(0xFF071018) else Color(0xFF001D36)
    val secondaryContainer = blend(surface, accent, 0.12f)
    val onSecondaryContainer = if (secondaryContainer.luminance() > 0.48f) Color(0xFF071018) else Color(0xFF101C26)

    return lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = blend(primary, Color.White, 0.30f),
        secondary = blend(primary, Color.Black, 0.15f),
        onSecondary = Color.White,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = Color(0xFF386A54),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFBBF0D7),
        onTertiaryContainer = Color(0xFF002117),
        background = surface,
        onBackground = Color(0xFF1B1B1F),
        surface = surface,
        onSurface = Color(0xFF1B1B1F),
        surfaceVariant = Color(0xFFE0E0E5),
        onSurfaceVariant = Color(0xFF45464F),
        surfaceTint = primary,
        inverseSurface = Color(0xFF303034),
        inverseOnSurface = Color(0xFFF3F0F4),
        error = Color(0xFFBA1A1A),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        outline = Color(0xFF767680),
        outlineVariant = Color(0xFFC6C6D0),
        scrim = Color.Black,
        surfaceBright = Color.White,
        surfaceDim = Color(0xFFD8D8DE),
        surfaceContainer = Color(0xFFECECF1),
        surfaceContainerHigh = Color(0xFFE6E6EB),
        surfaceContainerHighest = Color(0xFFE0E0E5),
        surfaceContainerLow = Color(0xFFF2F2F7),
        surfaceContainerLowest = Color.White,
    )
}

@Composable
fun KwhaleTheme(
    mode: ThemeMode,
    accent: KwhaleAccent,
    monet: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val useDark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.SYSTEM -> systemDark
    }
    val scheme = when {
        monet && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamic = if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (mode == ThemeMode.AMOLED) {
                dynamic.copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceDim = Color.Black,
                    surfaceContainerLowest = Color.Black,
                    surfaceContainerLow = Color(0xFF080808),
                    surfaceContainer = Color(0xFF0E0E0E),
                    surfaceContainerHigh = Color(0xFF161616),
                    surfaceContainerHighest = Color(0xFF1E1E1E),
                )
            } else {
                dynamic
            }
        }
        useDark -> darkScheme(accent.color, amoled = mode == ThemeMode.AMOLED)
        else -> lightScheme(accent.color)
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = KwhaleTypography,
        shapes = KwhaleShapes,
        content = content,
    )
}
