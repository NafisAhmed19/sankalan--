package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Exact approved semantic colour tokens from Specification §5.1 & Handoff §22.1.
 * No neon colours in core UI.
 */
object SankalanTokens {
  val Walnut900 = Color(0xFF241A16) // Deepest backgrounds, welcome backdrop
  val Walnut800 = Color(0xFF38271F) // Primary shelf and navigation
  val Walnut700 = Color(0xFF4A3428) // Secondary wood surfaces
  val Leather = Color(0xFF593B2B) // Luxury covers, subtle grain
  val AntiqueGold = Color(0xFFB08A4F) // Accent, thin borders, foil details
  val GoldHighlight = Color(0xFFD4B477) // Small glints, used sparingly
  val Cream = Color(0xFFF4EBDD) // Main light surface / warm ivory text on dark
  val Paper = Color(0xFFE9DDC9) // Diary page base
  val PaperShadow = Color(0xFFD5C4AA) // Page edges and depth
  val Ink = Color(0xFF302C29) // Text on paper
  val Muted = Color(0xFF8C857C) // Secondary text (light mode / paper)
  val MutedOnDark = Color(0xFFBBAFA2) // Accessible secondary text on walnut surfaces
  val Sage = Color(0xFF7D8971) // Botanical accent
  val Terracotta = Color(0xFFA9634B) // Warm secondary accent
  val ErrorDark = Color(0xFFE07A63)
  val ErrorLight = Color(0xFFA83E2B)
}

@Immutable
data class SankalanPalette(
  val isDark: Boolean,
  val background: Color,
  val shelfSurface: Color,
  val cardSurface: Color,
  val cardElevated: Color,
  val leatherAccent: Color,
  val goldAccent: Color,
  val goldHighlight: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val borderSubtle: Color,
  val borderGold: Color,
  val paperBase: Color,
  val paperShadow: Color,
  val paperInk: Color,
  val sage: Color,
  val terracotta: Color,
)

fun darkSankalanPalette(highContrast: Boolean): SankalanPalette =
  SankalanPalette(
    isDark = true,
    background = if (highContrast) Color(0xFF17100D) else SankalanTokens.Walnut900,
    shelfSurface = if (highContrast) Color(0xFF261A14) else SankalanTokens.Walnut800,
    cardSurface = if (highContrast) Color(0xFF2D1F18) else Color(0xFF30211A),
    cardElevated = if (highContrast) Color(0xFF3E2B21) else SankalanTokens.Walnut700,
    leatherAccent = SankalanTokens.Leather,
    goldAccent = if (highContrast) SankalanTokens.GoldHighlight else SankalanTokens.AntiqueGold,
    goldHighlight = SankalanTokens.GoldHighlight,
    textPrimary = if (highContrast) Color(0xFFFFF8EE) else SankalanTokens.Cream,
    textSecondary = if (highContrast) Color(0xFFDCCFC2) else SankalanTokens.MutedOnDark,
    borderSubtle =
      if (highContrast) SankalanTokens.GoldHighlight.copy(alpha = 0.42f)
      else SankalanTokens.AntiqueGold.copy(alpha = 0.24f),
    borderGold =
      if (highContrast) SankalanTokens.GoldHighlight.copy(alpha = 0.75f)
      else SankalanTokens.AntiqueGold.copy(alpha = 0.55f),
    paperBase = SankalanTokens.Paper,
    paperShadow = SankalanTokens.PaperShadow,
    paperInk = SankalanTokens.Ink,
    sage = SankalanTokens.Sage,
    terracotta = SankalanTokens.Terracotta,
  )

fun lightSankalanPalette(highContrast: Boolean): SankalanPalette =
  SankalanPalette(
    isDark = false,
    background = if (highContrast) Color(0xFFFAF4EA) else SankalanTokens.Cream,
    shelfSurface = if (highContrast) Color(0xFFF0E4D1) else Color(0xFFEDE1CE),
    cardSurface = if (highContrast) Color(0xFFFFFFFF) else Color(0xFFF8F2E7),
    cardElevated = if (highContrast) Color(0xFFE7D7BF) else SankalanTokens.Paper,
    leatherAccent = SankalanTokens.Leather,
    goldAccent = if (highContrast) Color(0xFF7B5B29) else Color(0xFF8C6934),
    goldHighlight = SankalanTokens.AntiqueGold,
    textPrimary = if (highContrast) Color(0xFF191512) else SankalanTokens.Ink,
    textSecondary = if (highContrast) Color(0xFF4A423B) else Color(0xFF6B6259),
    borderSubtle =
      if (highContrast) SankalanTokens.Walnut700.copy(alpha = 0.35f)
      else SankalanTokens.Walnut700.copy(alpha = 0.16f),
    borderGold =
      if (highContrast) Color(0xFF7B5B29)
      else SankalanTokens.AntiqueGold.copy(alpha = 0.65f),
    paperBase = SankalanTokens.Paper,
    paperShadow = SankalanTokens.PaperShadow,
    paperInk = SankalanTokens.Ink,
    sage = SankalanTokens.Sage,
    terracotta = SankalanTokens.Terracotta,
  )

val LocalSankalanPalette = staticCompositionLocalOf { darkSankalanPalette(false) }
