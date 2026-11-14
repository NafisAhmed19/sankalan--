package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode

/**
 * Spacing & Shape tokens from Specification §5.3 (4dp scale: 4, 8, 12, 16, 24, 32, 48).
 */
object SankalanSpacing {
  val xs = 4.dp
  val sm = 8.dp
  val md = 12.dp
  val lg = 16.dp
  val xl = 24.dp
  val xxl = 32.dp
  val xxxl = 48.dp
}

val SankalanShapes = Shapes(
  extraSmall = RoundedCornerShape(8.dp), // Small chips/controls
  small = RoundedCornerShape(10.dp), // Buttons 8-12dp
  medium = RoundedCornerShape(14.dp), // Cards 12-16dp
  large = RoundedCornerShape(18.dp), // Modals 16-20dp
  extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun SankalanTheme(
  themeMode: AppThemeMode = AppThemeMode.DARK,
  highContrast: Boolean = false,
  language: AppLanguage = AppLanguage.BN_BD,
  textScale: Float = 1.0f,
  content: @Composable () -> Unit,
) {
  val systemDark = isSystemInDarkTheme()
  val isDark =
    when (themeMode) {
      AppThemeMode.DARK -> true
      AppThemeMode.LIGHT -> false
      AppThemeMode.SYSTEM -> systemDark
    }

  val palette =
    if (isDark) darkSankalanPalette(highContrast) else lightSankalanPalette(highContrast)

  val colorScheme =
    if (isDark) {
      darkColorScheme(
        primary = palette.goldAccent,
        onPrimary = SankalanTokens.Walnut900,
        primaryContainer = SankalanTokens.Walnut700,
        onPrimaryContainer = palette.textPrimary,
        secondary = palette.sage,
        onSecondary = SankalanTokens.Walnut900,
        tertiary = palette.terracotta,
        onTertiary = SankalanTokens.Cream,
        background = palette.background,
        onBackground = palette.textPrimary,
        surface = palette.shelfSurface,
        onSurface = palette.textPrimary,
        surfaceVariant = palette.cardSurface,
        onSurfaceVariant = palette.textSecondary,
        outline = palette.borderSubtle,
        outlineVariant = palette.borderGold,
        error = SankalanTokens.ErrorDark,
        onError = SankalanTokens.Walnut900,
      )
    } else {
      lightColorScheme(
        primary = palette.goldAccent,
        onPrimary = SankalanTokens.Cream,
        primaryContainer = SankalanTokens.Paper,
        onPrimaryContainer = palette.textPrimary,
        secondary = palette.sage,
        onSecondary = SankalanTokens.Cream,
        tertiary = palette.terracotta,
        onTertiary = SankalanTokens.Cream,
        background = palette.background,
        onBackground = palette.textPrimary,
        surface = palette.shelfSurface,
        onSurface = palette.textPrimary,
        surfaceVariant = palette.cardSurface,
        onSurfaceVariant = palette.textSecondary,
        outline = palette.borderSubtle,
        outlineVariant = palette.borderGold,
        error = SankalanTokens.ErrorLight,
        onError = SankalanTokens.Cream,
      )
    }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window ?: return@SideEffect
      val controller = WindowCompat.getInsetsController(window, view)
      controller.isAppearanceLightStatusBars = !isDark
      controller.isAppearanceLightNavigationBars = !isDark
    }
  }

  CompositionLocalProvider(LocalSankalanPalette provides palette) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = sankalanTypography(language, textScale),
      shapes = SankalanShapes,
      content = content,
    )
  }
}
