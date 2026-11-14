package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.model.AppLanguage

/**
 * Self-hosted local fonts bundled via font-util into res/font/
 * per android-typography skill and Specification §5.2.
 */
val PlayfairDisplayFontFamily = FontFamily(
  Font(R.font.playfair_display, FontWeight.Normal),
  Font(R.font.playfair_display, FontWeight.Medium),
  Font(R.font.playfair_display, FontWeight.SemiBold),
  Font(R.font.playfair_display, FontWeight.Bold),
)

val InterFontFamily = FontFamily(
  Font(R.font.inter, FontWeight.Normal),
  Font(R.font.inter, FontWeight.Medium),
  Font(R.font.inter, FontWeight.SemiBold),
  Font(R.font.inter, FontWeight.Bold),
)

val NotoSerifBengaliFontFamily = FontFamily(
  Font(R.font.noto_serif_bengali, FontWeight.Normal),
  Font(R.font.noto_serif_bengali, FontWeight.Medium),
  Font(R.font.noto_serif_bengali, FontWeight.SemiBold),
  Font(R.font.noto_serif_bengali, FontWeight.Bold),
)

val NotoSansBengaliFontFamily = FontFamily(
  Font(R.font.noto_sans_bengali, FontWeight.Normal),
  Font(R.font.noto_sans_bengali, FontWeight.Medium),
  Font(R.font.noto_sans_bengali, FontWeight.SemiBold),
  Font(R.font.noto_sans_bengali, FontWeight.Bold),
)

val CaveatFontFamily = FontFamily(Font(R.font.caveat, FontWeight.Normal))
val GaladaFontFamily = FontFamily(Font(R.font.galada, FontWeight.Normal))
val AtmaFontFamily = FontFamily(Font(R.font.atma, FontWeight.Normal))

fun resolveEditorFontFamily(fontId: String?): FontFamily =
  when (fontId) {
    "playfair" -> PlayfairDisplayFontFamily
    "inter" -> InterFontFamily
    "noto_serif_bn" -> NotoSerifBengaliFontFamily
    "noto_sans_bn" -> NotoSansBengaliFontFamily
    "caveat" -> CaveatFontFamily
    "galada" -> GaladaFontFamily
    "atma" -> AtmaFontFamily
    else -> NotoSansBengaliFontFamily
  }

/**
 * Generates typography tuned for the active UI language and user textScale.
 * Bangla paragraph line-height starts in 1.65-1.85x range; English in 1.45-1.65x.
 */
fun sankalanTypography(language: AppLanguage, textScale: Float): Typography {
  val isBangla = language == AppLanguage.BN_BD
  val headingFont = if (isBangla) NotoSerifBengaliFontFamily else PlayfairDisplayFontFamily
  val bodyFont = if (isBangla) NotoSansBengaliFontFamily else InterFontFamily
  val bodyLineMultiplier = if (isBangla) 1.72f else 1.52f
  val headingLineMultiplier = if (isBangla) 1.45f else 1.28f
  val s = textScale.coerceIn(0.85f, 1.4f)

  return Typography(
    displayLarge = TextStyle(
      fontFamily = headingFont,
      fontWeight = FontWeight.SemiBold,
      fontSize = (36f * s).sp,
      lineHeight = (36f * s * headingLineMultiplier).sp,
      letterSpacing = if (isBangla) 0.sp else (-0.25).sp,
    ),
    headlineMedium = TextStyle(
      fontFamily = headingFont,
      fontWeight = FontWeight.SemiBold,
      fontSize = (24f * s).sp,
      lineHeight = (24f * s * headingLineMultiplier).sp,
    ),
    titleLarge = TextStyle(
      fontFamily = headingFont,
      fontWeight = FontWeight.Medium,
      fontSize = (20f * s).sp,
      lineHeight = (20f * s * headingLineMultiplier).sp,
    ),
    titleMedium = TextStyle(
      fontFamily = bodyFont,
      fontWeight = FontWeight.SemiBold,
      fontSize = (16f * s).sp,
      lineHeight = (16f * s * 1.45f).sp,
    ),
    bodyLarge = TextStyle(
      fontFamily = bodyFont,
      fontWeight = FontWeight.Normal,
      fontSize = (15.5f * s).sp,
      lineHeight = (15.5f * s * bodyLineMultiplier).sp,
    ),
    bodyMedium = TextStyle(
      fontFamily = bodyFont,
      fontWeight = FontWeight.Normal,
      fontSize = (14f * s).sp,
      lineHeight = (14f * s * bodyLineMultiplier).sp,
    ),
    labelLarge = TextStyle(
      fontFamily = bodyFont,
      fontWeight = FontWeight.SemiBold,
      fontSize = (13.5f * s).sp,
      lineHeight = (13.5f * s * 1.35f).sp,
    ),
    labelMedium = TextStyle(
      fontFamily = bodyFont,
      fontWeight = FontWeight.Medium,
      fontSize = (12f * s).sp,
      lineHeight = (12f * s * 1.35f).sp,
    ),
    labelSmall = TextStyle(
      fontFamily = bodyFont,
      fontWeight = FontWeight.Medium,
      fontSize = (11f * s).sp,
      lineHeight = (11f * s * 1.35f).sp,
    ),
  )
}
