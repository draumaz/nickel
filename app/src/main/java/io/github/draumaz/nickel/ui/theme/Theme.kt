package io.github.draumaz.nickel.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext

// Fallback scheme for devices without dynamic color: a soft sage/mint palette.
private val LightColors = lightColorScheme(
    primary = Color(0xFF2F6A4F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB2F1CF),
    onPrimaryContainer = Color(0xFF002113),
    secondary = Color(0xFF4D6357),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE9D9),
    onSecondaryContainer = Color(0xFF092016),
    tertiary = Color(0xFF3D6473),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC1E9FB),
    onTertiaryContainer = Color(0xFF001F29),
    background = Color(0xFFF6FBF4),
    onBackground = Color(0xFF171D19),
    surface = Color(0xFFF6FBF4),
    onSurface = Color(0xFF171D19),
    surfaceVariant = Color(0xFFDCE5DC),
    onSurfaceVariant = Color(0xFF404943),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F5EE),
    surfaceContainer = Color(0xFFEAEFE9),
    surfaceContainerHigh = Color(0xFFE4EAE3),
    surfaceContainerHighest = Color(0xFFDFE4DD),
    outline = Color(0xFF707972),
    outlineVariant = Color(0xFFC0C9C1),
    surfaceBright = Color(0xFFF6FBF4),
    surfaceDim = Color(0xFFD6DBD5),
    inverseSurface = Color(0xFF2C322D),
    inverseOnSurface = Color(0xFFEDF2EB),
    inversePrimary = Color(0xFF96D5B4),
    surfaceTint = Color(0xFF2F6A4F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF96D5B4),
    onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF145138),
    onPrimaryContainer = Color(0xFFB2F1CF),
    secondary = Color(0xFFB3CCBD),
    onSecondary = Color(0xFF1F352A),
    secondaryContainer = Color(0xFF354B40),
    onSecondaryContainer = Color(0xFFCFE9D9),
    tertiary = Color(0xFFA5CDDE),
    onTertiary = Color(0xFF063543),
    tertiaryContainer = Color(0xFF244C5B),
    onTertiaryContainer = Color(0xFFC1E9FB),
    background = Color(0xFF0F1511),
    onBackground = Color(0xFFDFE4DD),
    surface = Color(0xFF0F1511),
    onSurface = Color(0xFFDFE4DD),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFC0C9C1),
    surfaceContainerLowest = Color(0xFF0A0F0C),
    surfaceContainerLow = Color(0xFF171D19),
    surfaceContainer = Color(0xFF1B211D),
    surfaceContainerHigh = Color(0xFF252B27),
    surfaceContainerHighest = Color(0xFF303632),
    outline = Color(0xFF8A938B),
    outlineVariant = Color(0xFF404943),
    surfaceBright = Color(0xFF353B36),
    surfaceDim = Color(0xFF0F1511),
    inverseSurface = Color(0xFFDFE4DD),
    inverseOnSurface = Color(0xFF2C322D),
    inversePrimary = Color(0xFF2F6A4F),
    surfaceTint = Color(0xFF96D5B4),
)

/**
 * Income and expense are communicated with a custom green/red pair, harmonized toward
 * the scheme's primary so they sit comfortably beside dynamic color.
 */
@Immutable
data class MoneyColors(
    val income: Color,
    val incomeContainer: Color,
    val onIncomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val onExpenseContainer: Color,
)

val LocalMoneyColors = staticCompositionLocalOf {
    MoneyColors(Color.Green, Color.Green, Color.Black, Color.Red, Color.Red, Color.Black)
}

private fun moneyColors(scheme: ColorScheme, dark: Boolean): MoneyColors {
    // Pull the hues toward the scheme so they feel native next to dynamic color.
    fun harmonize(c: Color, amount: Float = 0.2f) = lerp(c, scheme.primary, amount)
    // The pills sit on primaryContainer, so their containers are pushed away from it
    // (lighter in dark mode, darker in light mode) to stay legible when the scheme is green.
    fun pill(c: Color) = lerp(c, scheme.onPrimaryContainer, 0.12f)
    return if (dark) MoneyColors(
        income = harmonize(Color(0xFF8FDCA8)),
        incomeContainer = pill(Color(0xFF2B6B47)),
        onIncomeContainer = Color(0xFFD6F8E0),
        expense = harmonize(Color(0xFFFFB4A9)),
        expenseContainer = pill(Color(0xFF7A3A33)),
        onExpenseContainer = Color(0xFFFFE2DD),
    ) else MoneyColors(
        income = harmonize(Color(0xFF1B7A45)),
        incomeContainer = pill(Color(0xFFD3F5DD)),
        onIncomeContainer = Color(0xFF00210E),
        expense = harmonize(Color(0xFFB3261E)),
        expenseContainer = pill(Color(0xFFFFE1DC)),
        onExpenseContainer = Color(0xFF410002),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NickelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val target = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    val colorScheme = animateColorScheme(target)
    CompositionLocalProvider(
        LocalMoneyColors provides moneyColors(colorScheme, darkTheme)
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}

/** Cross-fades every role, so switching palettes washes over the whole app at once. */
@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    val spec = spring<Color>(stiffness = Spring.StiffnessLow)
    @Composable
    fun a(c: Color) = animateColorAsState(c, spec, label = "scheme").value
    return target.copy(
        primary = a(target.primary),
        onPrimary = a(target.onPrimary),
        primaryContainer = a(target.primaryContainer),
        onPrimaryContainer = a(target.onPrimaryContainer),
        inversePrimary = a(target.inversePrimary),
        secondary = a(target.secondary),
        onSecondary = a(target.onSecondary),
        secondaryContainer = a(target.secondaryContainer),
        onSecondaryContainer = a(target.onSecondaryContainer),
        tertiary = a(target.tertiary),
        onTertiary = a(target.onTertiary),
        tertiaryContainer = a(target.tertiaryContainer),
        onTertiaryContainer = a(target.onTertiaryContainer),
        background = a(target.background),
        onBackground = a(target.onBackground),
        surface = a(target.surface),
        onSurface = a(target.onSurface),
        surfaceVariant = a(target.surfaceVariant),
        onSurfaceVariant = a(target.onSurfaceVariant),
        surfaceTint = a(target.surfaceTint),
        inverseSurface = a(target.inverseSurface),
        inverseOnSurface = a(target.inverseOnSurface),
        outline = a(target.outline),
        outlineVariant = a(target.outlineVariant),
        surfaceBright = a(target.surfaceBright),
        surfaceDim = a(target.surfaceDim),
        surfaceContainer = a(target.surfaceContainer),
        surfaceContainerHigh = a(target.surfaceContainerHigh),
        surfaceContainerHighest = a(target.surfaceContainerHighest),
        surfaceContainerLow = a(target.surfaceContainerLow),
        surfaceContainerLowest = a(target.surfaceContainerLowest),
    )
}

object NickelTheme {
    val money: MoneyColors
        @Composable get() = LocalMoneyColors.current
}
