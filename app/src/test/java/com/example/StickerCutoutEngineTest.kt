package com.example

import com.example.core.i18n.StringsBnBd
import com.example.core.i18n.StringsEn
import com.example.features.studio.StickerBorderOptions
import com.example.features.studio.StickerBrushAction
import com.example.features.studio.StickerBrushStroke
import com.example.features.studio.StickerCropRect
import com.example.features.studio.StickerCutoutEngine
import com.example.features.studio.StickerCutoutMode
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StickerCutoutEngineTest {

  private fun makeSyntheticObjectImage(w: Int = 80, h: Int = 80): IntArray {
    // Cream background (#EEE4D4) with a distinct Terracotta-Red circular flower/object in the center (radius 18)
    val bg = 0xFFEEE4D4.toInt()
    val obj = 0xFFC83E2B.toInt()
    val cx = w * 0.5f
    val cy = h * 0.5f
    val r = 18f
    return IntArray(w * h) { idx ->
      val x = idx % w
      val y = idx / w
      if (hypot((x - cx).toDouble(), (y - cy).toDouble()) <= r) obj else bg
    }
  }

  @Test
  fun `auto subject isolates central object from surrounding backdrop`() {
    val w = 80
    val h = 80
    val pixels = makeSyntheticObjectImage(w, h)
    val alpha =
      StickerCutoutEngine.computeSmartCutoutAlpha(
        pixels = pixels,
        width = w,
        height = h,
        mode = StickerCutoutMode.AUTO_SUBJECT,
      )

    // Center pixel of object must be opaque
    assertTrue(alpha[40 * w + 40] >= 200)
    // Corner pixel of background must be transparent
    assertTrue(alpha[4 * w + 4] <= 20)
  }

  @Test
  fun `rough oversized finger lasso snaps tightly to inner object and trims background collar`() {
    val w = 80
    val h = 80
    val pixels = makeSyntheticObjectImage(w, h)
    // User draws a rough oversized octagon/box from 0.12..0.88 (well outside radius-18 object!)
    val roughFingerLoop =
      listOf(
        0.12f to 0.15f,
        0.88f to 0.12f,
        0.90f to 0.85f,
        0.14f to 0.88f,
      )

    val alpha =
      StickerCutoutEngine.computeSmartCutoutAlpha(
        pixels = pixels,
        width = w,
        height = h,
        mode = StickerCutoutMode.LASSO_SMART_SNAP,
        lassoPolygon = roughFingerLoop,
      )

    // Center object preserved cleanly
    assertTrue(alpha[40 * w + 40] >= 200)
    // Outer margin INSIDE the rough finger loop (e.g. (14, 14) which is inside 0.12..0.88 but outside radius 18) should be trimmed away!
    assertEquals(0, alpha[14 * w + 14])
    // Corner outside lasso must definitely be transparent
    assertEquals(0, alpha[2 * w + 2])
  }

  @Test
  fun `keep and erase brushes override cutout decision`() {
    val w = 64
    val h = 64
    val pixels = makeSyntheticObjectImage(w, h)
    val strokes =
      listOf(
        StickerBrushStroke(
          action = StickerBrushAction.KEEP_OBJECT,
          points = listOf(0.10f to 0.10f),
          radiusNorm = 0.06f,
        ),
        StickerBrushStroke(
          action = StickerBrushAction.ERASE_BACKGROUND,
          points = listOf(0.50f to 0.50f),
          radiusNorm = 0.06f,
        ),
      )

    val alpha =
      StickerCutoutEngine.computeSmartCutoutAlpha(
        pixels = pixels,
        width = w,
        height = h,
        mode = StickerCutoutMode.AUTO_SUBJECT,
        brushStrokes = strokes,
      )

    // Top-left kept by Keep brush
    assertTrue(alpha[6 * w + 6] >= 180)
    // Exact center erased by Erase brush
    assertTrue(alpha[32 * w + 32] <= 64)
  }

  @Test
  fun `die cut sticker border is enabled by default and builds rounded halo around object`() {
    val w = 60
    val h = 60
    val pixels = makeSyntheticObjectImage(w, h)
    val alpha = IntArray(w * h) { idx ->
      val x = idx % w
      val y = idx / w
      if (hypot((x - 30).toDouble(), (y - 30).toDouble()) <= 10.0) 255 else 0
    }

    val defaultBorder = StickerBorderOptions()
    assertTrue(defaultBorder.enabled)

    val (outPixels, outW, outH) =
      StickerCutoutEngine.applyDieCutStickerBorder(
        pixels = pixels,
        alpha = alpha,
        width = w,
        height = h,
        border = defaultBorder.copy(thicknessPx = 8, shadowEnabled = false),
        trimToBounds = true,
      )

    // Object radius 10 + border 8 -> trimmed bounding box width roughly 36..46px
    assertTrue(outW in 32..50)
    assertTrue(outH in 32..50)

    // Border pixel ~4px outside radius-10 edge should be opaque white (#FFFDF8)
    val cx = outW / 2
    val cy = outH / 2
    val haloPixel = outPixels[cy * outW + (cx + 13)]
    val haloAlpha = (haloPixel ushr 24) and 0xFF
    val haloRed = (haloPixel ushr 16) and 0xFF
    assertEquals(255, haloAlpha)
    assertEquals(255, haloRed)
  }

  @Test
  fun `crop rect normalizes bounds safely`() {
    val safe = StickerCropRect(left = -0.2f, top = 0.85f, right = 0.86f, bottom = 0.5f).normalized()
    assertTrue(safe.left in 0f..1f)
    assertTrue(safe.right > safe.left)
    assertTrue(safe.bottom > safe.top)
  }

  @Test
  fun `bilingual sticker studio strings are complete`() {
    listOf(StringsEn, StringsBnBd).forEach { s ->
      assertTrue(s.stickerEditorTitle.isNotBlank())
      assertTrue(s.stickerTabCrop.isNotBlank())
      assertTrue(s.stickerTabCutout.isNotBlank())
      assertTrue(s.stickerTabBorder.isNotBlank())
      assertTrue(s.stickerLassoGuideHint.isNotBlank())
      assertTrue(s.stickerBorderToggleLabel.isNotBlank())
      assertTrue(s.stickerSaveBtn.isNotBlank())
    }
  }
}
