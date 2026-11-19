package com.example.features.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class StickerCutoutMode {
  AUTO_SUBJECT,
  LASSO_SMART_SNAP,
  ORIGINAL_RECT,
}

enum class StickerBrushAction {
  KEEP_OBJECT,
  ERASE_BACKGROUND,
}

data class StickerBrushStroke(
  val action: StickerBrushAction,
  val points: List<Pair<Float, Float>>,
  val radiusNorm: Float = 0.045f,
)

data class StickerCropRect(
  val left: Float = 0f,
  val top: Float = 0f,
  val right: Float = 1f,
  val bottom: Float = 1f,
) {
  fun normalized(): StickerCropRect {
    val l = left.coerceIn(0f, 0.9f)
    val t = top.coerceIn(0f, 0.9f)
    val r = right.coerceIn(l + 0.08f, 1f)
    val b = bottom.coerceIn(t + 0.08f, 1f)
    return StickerCropRect(l, t, r, b)
  }
}

data class StickerBorderOptions(
  val enabled: Boolean = true,
  val thicknessPx: Int = 14,
  val colorHex: String = "#FFFDF8",
  val doubleGoldTrim: Boolean = false,
  val shadowEnabled: Boolean = true,
)

/**
 * Pure on-device smart object cutout and Euclidean distance-field die-cut sticker border engine.
 * Works completely offline with no external Play Services download failures.
 */
object StickerCutoutEngine {

  private const val HIST_BINS = 8
  private const val HIST_SIZE = HIST_BINS * HIST_BINS * HIST_BINS
  private const val INF_DIST = 1e12f

  /**
   * Decodes an image [Uri] safely using ContentResolver with ExifInterface orientation fix.
   */
  fun decodeUriWithExifCorrection(context: Context, uri: Uri, maxDim: Int = 960): Bitmap? =
    runCatching {
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, bounds)
      }
      if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

      var sample = 1
      while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) {
        sample *= 2
      }
      val decodeOpts =
        BitmapFactory.Options().apply {
          inSampleSize = sample
          inPreferredConfig = Bitmap.Config.ARGB_8888
        }
      val decoded =
        context.contentResolver.openInputStream(uri)?.use {
          BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: return null

      val orientation =
        runCatching {
          context.contentResolver.openInputStream(uri)?.use { input ->
            ExifInterface(input).getAttributeInt(
              ExifInterface.TAG_ORIENTATION,
              ExifInterface.ORIENTATION_NORMAL,
            )
          }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

      val matrix = Matrix()
      when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
      }

      val rotated =
        if (!matrix.isIdentity) {
          val out = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
          if (out !== decoded) decoded.recycle()
          out
        } else {
          decoded
        }

      ensureMaxDim(rotated, maxDim)
    }.getOrNull()

  fun ensureMaxDim(source: Bitmap, maxDim: Int): Bitmap {
    if (source.isRecycled) return source
    val w = source.width
    val h = source.height
    val longest = max(w, h)
    if (longest <= maxDim || longest <= 0) return source
    val scale = maxDim.toFloat() / longest.toFloat()
    val nw = (w * scale).roundToInt().coerceAtLeast(1)
    val nh = (h * scale).roundToInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(source, nw, nh, true)
    if (scaled !== source && !source.isRecycled) source.recycle()
    return scaled
  }

  fun cropAndRotateBitmap(
    source: Bitmap,
    cropRect: StickerCropRect,
    rotationDegrees: Int,
    maxDim: Int = 560,
  ): Bitmap {
    if (source.isRecycled || source.width <= 0 || source.height <= 0) {
      return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }
    val normRot = ((rotationDegrees % 360) + 360) % 360
    val rotated =
      if (normRot != 0) {
        val m = Matrix().apply { postRotate(normRot.toFloat()) }
        Bitmap.createBitmap(source, 0, 0, source.width, source.height, m, true)
      } else {
        source
      }

    val safe = cropRect.normalized()
    val x = (safe.left * rotated.width).roundToInt().coerceIn(0, (rotated.width - 1).coerceAtLeast(0))
    val y = (safe.top * rotated.height).roundToInt().coerceIn(0, (rotated.height - 1).coerceAtLeast(0))
    val w = ((safe.right - safe.left) * rotated.width).roundToInt().coerceIn(1, rotated.width - x)
    val h = ((safe.bottom - safe.top) * rotated.height).roundToInt().coerceIn(1, rotated.height - y)

    val cropped = Bitmap.createBitmap(rotated, x, y, w, h)
    if (rotated !== source && rotated !== cropped && !rotated.isRecycled) rotated.recycle()

    val longest = max(cropped.width, cropped.height)
    return if (longest > maxDim && longest > 0) {
      val scale = maxDim.toFloat() / longest.toFloat()
      val nw = (cropped.width * scale).roundToInt().coerceAtLeast(1)
      val nh = (cropped.height * scale).roundToInt().coerceAtLeast(1)
      val resized = Bitmap.createScaledBitmap(cropped, nw, nh, true)
      if (resized !== cropped && cropped !== source && !cropped.isRecycled) cropped.recycle()
      resized
    } else {
      cropped
    }
  }

  fun renderStickerBitmap(
    cropped: Bitmap,
    mode: StickerCutoutMode,
    lassoPolygon: List<Pair<Float, Float>>,
    brushStrokes: List<StickerBrushStroke>,
    sensitivity: Float,
    border: StickerBorderOptions,
    trimOutput: Boolean,
  ): Bitmap {
    if (cropped.isRecycled || cropped.width <= 0 || cropped.height <= 0) {
      return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }
    val w = cropped.width.coerceAtLeast(1)
    val h = cropped.height.coerceAtLeast(1)
    val pixels = IntArray(w * h)
    cropped.getPixels(pixels, 0, w, 0, 0, w, h)

    val alpha =
      computeSmartCutoutAlpha(
        pixels = pixels,
        width = w,
        height = h,
        mode = mode,
        lassoPolygon = lassoPolygon,
        brushStrokes = brushStrokes,
        sensitivity = sensitivity,
      )

    val (outPixels, outW, outH) =
      applyDieCutStickerBorder(
        pixels = pixels,
        alpha = alpha,
        width = w,
        height = h,
        border = border,
        trimToBounds = trimOutput,
      )

    return Bitmap.createBitmap(outPixels, outW, outH, Bitmap.Config.ARGB_8888)
  }

  /**
   * Core GrabCut-inspired + edge-snapping foreground alpha computation on raw ARGB pixel array.
   * Separated from Android Bitmap APIs for deterministic unit testing and fast execution.
   */
  fun computeSmartCutoutAlpha(
    pixels: IntArray,
    width: Int,
    height: Int,
    mode: StickerCutoutMode,
    lassoPolygon: List<Pair<Float, Float>> = emptyList(),
    brushStrokes: List<StickerBrushStroke> = emptyList(),
    sensitivity: Float = 0.55f,
  ): IntArray {
    val n = width * height
    if (n == 0) return IntArray(0)

    // Preserve source alpha if transparent asset is being re-edited cleanly
    val srcAlpha = IntArray(n) { i -> (pixels[i] ushr 24) and 0xFF }

    if (mode == StickerCutoutMode.ORIGINAL_RECT && brushStrokes.isEmpty()) {
      return srcAlpha
    }

    val lArr = FloatArray(n)
    val aArr = FloatArray(n)
    val bArr = FloatArray(n)
    val colorBin = IntArray(n)

    for (i in 0 until n) {
      val c = pixels[i]
      val r = ((c ushr 16) and 0xFF).toFloat()
      val g = ((c ushr 8) and 0xFF).toFloat()
      val b = (c and 0xFF).toFloat()
      val lum = 0.299f * r + 0.587f * g + 0.114f * b
      val chA = ((r - g) * 0.5f + 128f).coerceIn(0f, 255f)
      val chB = ((0.5f * (r + g) - b) * 0.5f + 128f).coerceIn(0f, 255f)
      lArr[i] = lum
      aArr[i] = chA
      bArr[i] = chB

      val qL = (lum / 32f).toInt().coerceIn(0, HIST_BINS - 1)
      val qA = (chA / 32f).toInt().coerceIn(0, HIST_BINS - 1)
      val qB = (chB / 32f).toInt().coerceIn(0, HIST_BINS - 1)
      colorBin[i] = (qL shl 6) or (qA shl 3) or qB
    }

    // Sobel edge magnitude normalized to 0..1 for edge-aware boundary snapping
    val edgeMag = FloatArray(n)
    var maxEdge = 1f
    for (y in 1 until height - 1) {
      val row = y * width
      for (x in 1 until width - 1) {
        val idx = row + x
        val gxL =
          -lArr[idx - width - 1] + lArr[idx - width + 1] -
            2f * lArr[idx - 1] + 2f * lArr[idx + 1] -
            lArr[idx + width - 1] + lArr[idx + width + 1]
        val gyL =
          -lArr[idx - width - 1] - 2f * lArr[idx - width] - lArr[idx - width + 1] +
            lArr[idx + width - 1] + 2f * lArr[idx + width] + lArr[idx + width + 1]
        val gxA =
          -aArr[idx - 1] + aArr[idx + 1] - bArr[idx - 1] + bArr[idx + 1]
        val gyA =
          -aArr[idx - width] + aArr[idx + width] - bArr[idx - width] + bArr[idx + width]
        val m = hypot(gxL, gyL) + hypot(gxA, gyA) * 1.4f
        edgeMag[idx] = m
        if (m > maxEdge) maxEdge = m
      }
    }
    val invMaxEdge = 1f / maxEdge
    for (i in 0 until n) edgeMag[i] = (edgeMag[i] * invMaxEdge).coerceIn(0f, 1f)

    // Rasterize user brush constraints (+ Keep / - Erase)
    val forcedFg = BooleanArray(n)
    val forcedBg = BooleanArray(n)
    for (stroke in brushStrokes) {
      if (stroke.points.isEmpty()) continue
      val rPx = max(2f, stroke.radiusNorm * max(width, height))
      val rSq = rPx * rPx
      for (pIdx in stroke.points.indices) {
        val (nx, ny) = stroke.points[pIdx]
        val cx = nx * (width - 1)
        val cy = ny * (height - 1)
        val x0 = max(0, (cx - rPx).toInt())
        val x1 = min(width - 1, (cx + rPx).toInt())
        val y0 = max(0, (cy - rPx).toInt())
        val y1 = min(height - 1, (cy + rPx).toInt())
        for (yy in y0..y1) {
          val dy = yy - cy
          val rowOff = yy * width
          for (xx in x0..x1) {
            val dx = xx - cx
            if (dx * dx + dy * dy <= rSq) {
              val id = rowOff + xx
              if (stroke.action == StickerBrushAction.KEEP_OBJECT) {
                forcedFg[id] = true
                forcedBg[id] = false
              } else {
                forcedBg[id] = true
                forcedFg[id] = false
              }
            }
          }
        }
      }
    }

    val useLasso = mode == StickerCutoutMode.LASSO_SMART_SNAP && lassoPolygon.size >= 3
    val insideLasso = BooleanArray(n) { !useLasso }
    var lassoCx = width * 0.5f
    var lassoCy = height * 0.5f

    if (useLasso) {
      rasterizePolygon(lassoPolygon, width, height, insideLasso)
      var sumX = 0.0
      var sumY = 0.0
      var cnt = 0
      for (y in 0 until height) {
        val row = y * width
        for (x in 0 until width) {
          if (insideLasso[row + x]) {
            sumX += x
            sumY += y
            cnt++
          }
        }
      }
      if (cnt > 0) {
        lassoCx = (sumX / cnt).toFloat()
        lassoCy = (sumY / cnt).toFloat()
      }
    }

    // Mode ORIGINAL_RECT with manual brush erasures
    if (mode == StickerCutoutMode.ORIGINAL_RECT) {
      val rawMask = BooleanArray(n) { i -> srcAlpha[i] >= 96 && !forcedBg[i] }
      for (i in 0 until n) if (forcedFg[i]) rawMask[i] = true
      return featherMaskToAlpha(rawMask, srcAlpha, width, height)
    }

    // Distance from boundary of region (polygon rim or crop border) for spatial prior
    val fgLabel = BooleanArray(n)
    val bgSeedMask = BooleanArray(n)
    val fgSeedMask = BooleanArray(n)
    var innerCenterCount = 0

    if (useLasso) {
      val distOutsideOrRim = computeEuclideanDistanceField(BooleanArray(n) { !insideLasso[it] }, width, height)
      var maxInDist = 1f
      for (i in 0 until n) {
        if (insideLasso[i] && distOutsideOrRim[i] > maxInDist) maxInDist = distOutsideOrRim[i]
      }
      val rimWidth = max(3f, maxInDist * 0.18f)

      // First pass: learn confirmed outer background palette from everything outside loop + rim
      val bgInitHist = FloatArray(HIST_SIZE) { 0.05f }
      var bgInitTotal = HIST_SIZE * 0.05f
      for (i in 0 until n) {
        if (srcAlpha[i] < 64) continue
        if (!insideLasso[i] || distOutsideOrRim[i] <= rimWidth) {
          bgSeedMask[i] = true
          bgInitHist[colorBin[i]] += 1f
          bgInitTotal += 1f
        }
      }
      val smoothBgInit = FloatArray(HIST_SIZE)
      smoothHistogram3D(bgInitHist, smoothBgInit, bgInitTotal)

      // Seed foreground strictly on interior pixels whose color stands out from the outer background
      for (i in 0 until n) {
        if (srcAlpha[i] < 64 || !insideLasso[i] || bgSeedMask[i]) {
          fgLabel[i] = false
        } else {
          val bgProb = smoothBgInit[colorBin[i]]
          val d = distOutsideOrRim[i]
          val standsOut = bgProb < 0.0045f
          if (standsOut) {
            fgSeedMask[i] = true
            fgLabel[i] = true
            innerCenterCount++
          } else if (d > maxInDist * 0.65f) {
            fgLabel[i] = false
          }
        }
      }
      // Fallback if object shares identical hue with outer backdrop: seed deepest 30% core
      if (innerCenterCount < 8) {
        for (i in 0 until n) {
          if (insideLasso[i] && srcAlpha[i] >= 64 && distOutsideOrRim[i] > maxInDist * 0.65f) {
            fgSeedMask[i] = true
            fgLabel[i] = true
          }
        }
      }
    } else {
      // AUTO_SUBJECT: learn outer frame ring background palette first, then seed contrasting central pixels
      val padX = max(3, (width * 0.10f).toInt())
      val padY = max(3, (height * 0.10f).toInt())
      val coreX0 = (width * 0.22f).toInt()
      val coreX1 = (width * 0.78f).toInt()
      val coreY0 = (height * 0.18f).toInt()
      val coreY1 = (height * 0.82f).toInt()

      val bgInitHist = FloatArray(HIST_SIZE) { 0.05f }
      var bgInitTotal = HIST_SIZE * 0.05f
      for (y in 0 until height) {
        val row = y * width
        for (x in 0 until width) {
          val i = row + x
          val isBorder = x < padX || x >= width - padX || y < padY || y >= height - padY
          if (srcAlpha[i] >= 64 && isBorder) {
            bgSeedMask[i] = true
            bgInitHist[colorBin[i]] += 1f
            bgInitTotal += 1f
          }
        }
      }
      val smoothBgInit = FloatArray(HIST_SIZE)
      smoothHistogram3D(bgInitHist, smoothBgInit, bgInitTotal)

      for (y in 0 until height) {
        val row = y * width
        for (x in 0 until width) {
          val i = row + x
          val transparentBg = srcAlpha[i] < 64
          if (transparentBg || bgSeedMask[i]) {
            fgLabel[i] = false
          } else if (x in coreX0..coreX1 && y in coreY0..coreY1) {
            val standsOut = smoothBgInit[colorBin[i]] < 0.0055f
            if (standsOut) {
              fgSeedMask[i] = true
              fgLabel[i] = true
              innerCenterCount++
            }
          }
        }
      }
      if (innerCenterCount < 8) {
        for (y in coreY0..coreY1) {
          val row = y * width
          for (x in coreX0..coreX1) {
            val i = row + x
            if (srcAlpha[i] >= 64 && !bgSeedMask[i]) {
              fgSeedMask[i] = true
              fgLabel[i] = true
            }
          }
        }
      }
    }

    // Apply explicit user brush constraints to seeds
    for (i in 0 until n) {
      if (forcedFg[i]) {
        fgSeedMask[i] = true
        bgSeedMask[i] = false
        fgLabel[i] = true
      } else if (forcedBg[i]) {
        bgSeedMask[i] = true
        fgSeedMask[i] = false
        fgLabel[i] = false
      }
    }

    // GrabCut-style iterative color histogram + edge-aware MRF energy minimization
    val fgHist = FloatArray(HIST_SIZE)
    val bgHist = FloatArray(HIST_SIZE)
    val smoothFg = FloatArray(HIST_SIZE)
    val smoothBg = FloatArray(HIST_SIZE)
    val nextLabel = BooleanArray(n)

    // Sensitivity 0..1 shifts foreground acceptance threshold cleanly
    val sensitivityBias = (sensitivity.coerceIn(0f, 1f) - 0.5f) * 2.0f

    repeat(5) { iter ->
      fgHist.fill(0.05f)
      bgHist.fill(0.05f)
      var fgTotal = HIST_SIZE * 0.05f
      var bgTotal = HIST_SIZE * 0.05f

      for (i in 0 until n) {
        if (srcAlpha[i] < 48) continue
        val bin = colorBin[i]
        val wBoost = if (forcedFg[i] || forcedBg[i]) 6f else 1f
        if (fgLabel[i]) {
          fgHist[bin] += wBoost
          fgTotal += wBoost
        } else {
          bgHist[bin] += wBoost
          bgTotal += wBoost
        }
      }

      smoothHistogram3D(fgHist, smoothFg, fgTotal)
      smoothHistogram3D(bgHist, smoothBg, bgTotal)

      for (y in 0 until height) {
        val row = y * width
        val yNorm = abs(y - lassoCy) / max(1f, height * 0.5f)
        for (x in 0 until width) {
          val i = row + x
          if (forcedFg[i]) {
            nextLabel[i] = true
            continue
          }
          if (forcedBg[i] || srcAlpha[i] < 48) {
            nextLabel[i] = false
            continue
          }
          if (useLasso && !insideLasso[i]) {
            nextLabel[i] = false
            continue
          }

          val bin = colorBin[i]
          val pFg = smoothFg[bin]
          val pBg = smoothBg[bin]
          val colorLogRatio = ln((pFg + 1e-6f) / (pBg + 1e-6f)).coerceIn(-6f, 6f)

          val xNorm = abs(x - lassoCx) / max(1f, width * 0.5f)
          val spatialPrior =
            if (useLasso) {
              (0.28f - 0.35f * hypot(xNorm, yNorm).coerceIn(0f, 1.3f))
            } else {
              (0.35f - 0.55f * hypot(xNorm, yNorm).coerceIn(0f, 1.4f))
            }

          // Normalized edge-stopping Ising smoothness term across 4-connected neighbors
          var neighborVote = 0f
          var neighborWeightSum = 0f
          val eSelf = edgeMag[i]
          if (x > 0) {
            val wN = exp(-7.5f * max(eSelf, edgeMag[i - 1]))
            neighborVote += if (fgLabel[i - 1]) wN else -wN
            neighborWeightSum += wN
          }
          if (x < width - 1) {
            val wN = exp(-7.5f * max(eSelf, edgeMag[i + 1]))
            neighborVote += if (fgLabel[i + 1]) wN else -wN
            neighborWeightSum += wN
          }
          if (y > 0) {
            val wN = exp(-7.5f * max(eSelf, edgeMag[i - width]))
            neighborVote += if (fgLabel[i - width]) wN else -wN
            neighborWeightSum += wN
          }
          if (y < height - 1) {
            val wN = exp(-7.5f * max(eSelf, edgeMag[i + width]))
            neighborVote += if (fgLabel[i + width]) wN else -wN
            neighborWeightSum += wN
          }

          val smoothnessTerm =
            if (neighborWeightSum > 1e-4f) (neighborVote / neighborWeightSum) * 1.15f else 0f
          val score =
            if (iter == 0 && fgSeedMask[i]) {
              colorLogRatio + spatialPrior + sensitivityBias + 0.45f + smoothnessTerm * 0.5f
            } else {
              colorLogRatio + spatialPrior + sensitivityBias + smoothnessTerm
            }

          nextLabel[i] = score > 0f
        }
      }
      nextLabel.copyInto(fgLabel)
    }

    // Connected component cleanup: preserve main subject(s) & any Keep-brushed parts, drop tiny specks
    cleanupSmallIslandsAndHoles(fgLabel, forcedFg, forcedBg, width, height)

    // Smooth anti-aliased alpha along foreground boundary
    return featherMaskToAlpha(fgLabel, srcAlpha, width, height)
  }

  private fun smoothHistogram3D(src: FloatArray, dst: FloatArray, total: Float) {
    val invTotal = 1f / max(total, 1f)
    for (l in 0 until HIST_BINS) {
      for (a in 0 until HIST_BINS) {
        for (b in 0 until HIST_BINS) {
          var acc = 0f
          var wAcc = 0f
          for (dl in -1..1) {
            val nl = l + dl
            if (nl !in 0 until HIST_BINS) continue
            for (da in -1..1) {
              val na = a + da
              if (na !in 0 until HIST_BINS) continue
              for (db in -1..1) {
                val nb = b + db
                if (nb !in 0 until HIST_BINS) continue
                val w = if (dl == 0 && da == 0 && db == 0) 4f else 1f
                acc += src[(nl shl 6) or (na shl 3) or nb] * w
                wAcc += w
              }
            }
          }
          dst[(l shl 6) or (a shl 3) or b] = (acc / wAcc) * invTotal
        }
      }
    }
  }

  private fun rasterizePolygon(
    polygon: List<Pair<Float, Float>>,
    width: Int,
    height: Int,
    outInside: BooleanArray,
  ) {
    val m = polygon.size
    val px = FloatArray(m) { (polygon[it].first.coerceIn(0f, 1f)) * (width - 1) }
    val py = FloatArray(m) { (polygon[it].second.coerceIn(0f, 1f)) * (height - 1) }

    val interX = FloatArray(m)
    for (y in 0 until height) {
      val yf = y.toFloat() + 0.5f
      var nodes = 0
      var j = m - 1
      for (i in 0 until m) {
        val yi = py[i]
        val yj = py[j]
        if ((yi < yf && yj >= yf) || (yj < yf && yi >= yf)) {
          val denom = (yj - yi)
          if (abs(denom) > 1e-5f) {
            interX[nodes++] = px[i] + (yf - yi) / denom * (px[j] - px[i])
          }
        }
        j = i
      }
      if (nodes >= 2) {
        // Insertion sort small intersection array
        for (k in 1 until nodes) {
          val v = interX[k]
          var t = k - 1
          while (t >= 0 && interX[t] > v) {
            interX[t + 1] = interX[t]
            t--
          }
          interX[t + 1] = v
        }
        val row = y * width
        var idx = 0
        while (idx + 1 < nodes) {
          val x0 = max(0, interX[idx].toInt())
          val x1 = min(width - 1, interX[idx + 1].toInt())
          for (x in x0..x1) outInside[row + x] = true
          idx += 2
        }
      }
    }
  }

  private fun cleanupSmallIslandsAndHoles(
    fgMask: BooleanArray,
    forcedFg: BooleanArray,
    forcedBg: BooleanArray,
    width: Int,
    height: Int,
  ) {
    val n = width * height
    val visited = BooleanArray(n)
    val queue = IntArray(n)
    val components = ArrayList<Pair<IntArray, Boolean>>()
    var maxSize = 0

    for (start in 0 until n) {
      if (!fgMask[start] || visited[start]) continue
      var head = 0
      var tail = 0
      queue[tail++] = start
      visited[start] = true
      var touchesKeep = false
      while (head < tail) {
        val cur = queue[head++]
        if (forcedFg[cur]) touchesKeep = true
        val x = cur % width
        val y = cur / width
        if (x > 0 && fgMask[cur - 1] && !visited[cur - 1]) {
          visited[cur - 1] = true; queue[tail++] = cur - 1
        }
        if (x < width - 1 && fgMask[cur + 1] && !visited[cur + 1]) {
          visited[cur + 1] = true; queue[tail++] = cur + 1
        }
        if (y > 0 && fgMask[cur - width] && !visited[cur - width]) {
          visited[cur - width] = true; queue[tail++] = cur - width
        }
        if (y < height - 1 && fgMask[cur + width] && !visited[cur + width]) {
          visited[cur + width] = true; queue[tail++] = cur + width
        }
      }
      if (tail > maxSize) maxSize = tail
      components.add(queue.copyOf(tail) to touchesKeep)
    }

    val minKeepSize = max(24, (maxSize * 0.12f).toInt())
    fgMask.fill(false)
    for ((compIndices, touchesKeep) in components) {
      if (compIndices.size >= minKeepSize || touchesKeep) {
        for (idx in compIndices) fgMask[idx] = true
      }
    }

    // Fill tiny enclosed background speckles unless explicitly erased
    val outsideBg = BooleanArray(n)
    var tail = 0
    for (x in 0 until width) {
      if (!fgMask[x]) {
        outsideBg[x] = true; queue[tail++] = x
      }
      val bot = (height - 1) * width + x
      if (!fgMask[bot] && !outsideBg[bot]) {
        outsideBg[bot] = true; queue[tail++] = bot
      }
    }
    for (y in 0 until height) {
      val leftIdx = y * width
      if (!fgMask[leftIdx] && !outsideBg[leftIdx]) {
        outsideBg[leftIdx] = true; queue[tail++] = leftIdx
      }
      val rightIdx = leftIdx + width - 1
      if (!fgMask[rightIdx] && !outsideBg[rightIdx]) {
        outsideBg[rightIdx] = true; queue[tail++] = rightIdx
      }
    }

    var head = 0
    while (head < tail) {
      val cur = queue[head++]
      val x = cur % width
      val y = cur / width
      if (x > 0 && !fgMask[cur - 1] && !outsideBg[cur - 1]) {
        outsideBg[cur - 1] = true; queue[tail++] = cur - 1
      }
      if (x < width - 1 && !fgMask[cur + 1] && !outsideBg[cur + 1]) {
        outsideBg[cur + 1] = true; queue[tail++] = cur + 1
      }
      if (y > 0 && !fgMask[cur - width] && !outsideBg[cur - width]) {
        outsideBg[cur - width] = true; queue[tail++] = cur - width
      }
      if (y < height - 1 && !fgMask[cur + width] && !outsideBg[cur + width]) {
        outsideBg[cur + width] = true; queue[tail++] = cur + width
      }
    }

    // Only fill small holes (< 0.6% of total area) and never override user Erase strokes
    val maxHoleFill = max(16, (n * 0.006f).toInt())
    visited.fill(false)
    for (start in 0 until n) {
      if (fgMask[start] || outsideBg[start] || visited[start]) continue
      var hHead = 0
      var hTail = 0
      queue[hTail++] = start
      visited[start] = true
      var touchesErase = false
      while (hHead < hTail) {
        val cur = queue[hHead++]
        if (forcedBg[cur]) touchesErase = true
        val x = cur % width
        val y = cur / width
        if (x > 0 && !fgMask[cur - 1] && !outsideBg[cur - 1] && !visited[cur - 1]) {
          visited[cur - 1] = true; queue[hTail++] = cur - 1
        }
        if (x < width - 1 && !fgMask[cur + 1] && !outsideBg[cur + 1] && !visited[cur + 1]) {
          visited[cur + 1] = true; queue[hTail++] = cur + 1
        }
        if (y > 0 && !fgMask[cur - width] && !outsideBg[cur - width] && !visited[cur - width]) {
          visited[cur - width] = true; queue[hTail++] = cur - width
        }
        if (y < height - 1 && !fgMask[cur + width] && !outsideBg[cur + width] && !visited[cur + width]) {
          visited[cur + width] = true; queue[hTail++] = cur + width
        }
      }
      if (!touchesErase && hTail <= maxHoleFill) {
        for (k in 0 until hTail) fgMask[queue[k]] = true
      }
    }
  }

  private fun featherMaskToAlpha(
    fgMask: BooleanArray,
    srcAlpha: IntArray,
    width: Int,
    height: Int,
  ): IntArray {
    val n = width * height
    val out = IntArray(n)
    for (y in 0 until height) {
      val row = y * width
      for (x in 0 until width) {
        val i = row + x
        var sum = 0
        var count = 0
        for (dy in -1..1) {
          val ny = y + dy
          if (ny !in 0 until height) continue
          val nRow = ny * width
          for (dx in -1..1) {
            val nx = x + dx
            if (nx !in 0 until width) continue
            val weight = if (dx == 0 && dy == 0) 2 else 1
            if (fgMask[nRow + nx]) sum += 255 * weight
            count += weight
          }
        }
        val smooth = sum / max(1, count)
        val merged =
          if (fgMask[i]) max(smooth, 180)
          else if (smooth < 36) 0 else smooth
        out[i] = ((merged * srcAlpha[i]) / 255).coerceIn(0, 255)
      }
    }
    return out
  }

  /**
   * Exact linear-time O(W * H) 2D Euclidean Distance Transform (Felzenszwalb & Huttenlocher).
   * Returns pixel distance to the nearest true element in [seeds].
   */
  fun computeEuclideanDistanceField(seeds: BooleanArray, width: Int, height: Int): FloatArray {
    val n = width * height
    val f = FloatArray(n) { i -> if (seeds[i]) 0f else INF_DIST }
    val colPassed = FloatArray(n)
    val v = IntArray(max(width, height))
    val z = FloatArray(max(width, height) + 1)

    // Transform columns first
    val colIn = FloatArray(height)
    val colOut = FloatArray(height)
    for (x in 0 until width) {
      for (y in 0 until height) colIn[y] = f[y * width + x]
      edt1d(colIn, colOut, height, v, z)
      for (y in 0 until height) colPassed[y * width + x] = colOut[y]
    }

    // Transform rows next
    val rowIn = FloatArray(width)
    val rowOut = FloatArray(width)
    val distOut = FloatArray(n)
    for (y in 0 until height) {
      val row = y * width
      for (x in 0 until width) rowIn[x] = colPassed[row + x]
      edt1d(rowIn, rowOut, width, v, z)
      for (x in 0 until width) distOut[row + x] = sqrt(max(0f, rowOut[x]))
    }
    return distOut
  }

  private fun edt1d(f: FloatArray, d: FloatArray, len: Int, v: IntArray, z: FloatArray) {
    var k = 0
    v[0] = 0
    z[0] = -INF_DIST
    z[1] = INF_DIST
    for (q in 1 until len) {
      val fq = f[q]
      if (fq >= INF_DIST * 0.5f) continue
      var s: Float
      while (true) {
        val vk = v[k]
        s = ((fq + q * q) - (f[vk] + vk * vk)) / (2f * (q - vk))
        if (s <= z[k]) {
          k--
          if (k < 0) {
            k = 0
            v[0] = q
            z[0] = -INF_DIST
            z[1] = INF_DIST
            break
          }
        } else {
          k++
          v[k] = q
          z[k] = s
          z[k + 1] = INF_DIST
          break
        }
      }
    }
    k = 0
    for (q in 0 until len) {
      while (z[k + 1] < q) k++
      val vk = v[k]
      val dq = (q - vk).toFloat()
      d[q] = dq * dq + f[vk]
    }
  }

  /**
   * Renders an organic die-cut sticker border + optional antique gold trim + soft drop shadow
   * around the alpha cutout, optionally cropping tightly to non-transparent bounds.
   */
  fun applyDieCutStickerBorder(
    pixels: IntArray,
    alpha: IntArray,
    width: Int,
    height: Int,
    border: StickerBorderOptions,
    trimToBounds: Boolean = true,
  ): Triple<IntArray, Int, Int> {
    val borderRadius = if (border.enabled) border.thicknessPx.coerceIn(0, 40) else 0
    val shadowPad = if (border.shadowEnabled) 10 else 0
    val pad = borderRadius + shadowPad + 4

    val cw = width + pad * 2
    val ch = height + pad * 2
    val cn = cw * ch

    val fgSeeds = BooleanArray(cn)
    val srcPixels = IntArray(cn)
    val srcAlpha = IntArray(cn)

    for (y in 0 until height) {
      val sRow = y * width
      val dRow = (y + pad) * cw + pad
      for (x in 0 until width) {
        val sIdx = sRow + x
        val aVal = alpha[sIdx].coerceIn(0, 255)
        srcPixels[dRow + x] = (pixels[sIdx] and 0x00FFFFFF) or (aVal shl 24)
        srcAlpha[dRow + x] = aVal
        if (aVal >= 96) fgSeeds[dRow + x] = true
      }
    }

    val borderArgb = parseColorHex(border.colorHex, 0xFFFFFDF8.toInt())
    val borderR = (borderArgb ushr 16) and 0xFF
    val borderG = (borderArgb ushr 8) and 0xFF
    val borderB = borderArgb and 0xFF

    val goldArgb = 0xFFC79A3E.toInt()
    val goldR = (goldArgb ushr 16) and 0xFF
    val goldG = (goldArgb ushr 8) and 0xFF
    val goldB = goldArgb and 0xFF

    val distField = computeEuclideanDistanceField(fgSeeds, cw, ch)
    val outPixels = IntArray(cn)

    val shadowDx = 4
    val shadowDy = 5

    for (y in 0 until ch) {
      val row = y * cw
      for (x in 0 until cw) {
        val idx = row + x
        val sa = srcAlpha[idx]
        val d = distField[idx]

        // Soft drop shadow beneath border silhouette
        var underShadowAlpha = 0
        if (border.shadowEnabled) {
          val sx = x - shadowDx
          val sy = y - shadowDy
          if (sx in 0 until cw && sy in 0 until ch) {
            val sDist = distField[sy * cw + sx] - borderRadius.toFloat()
            if (sDist <= 8f) {
              val t = ((8f - sDist) / 8f).coerceIn(0f, 1f)
              underShadowAlpha = (t * t * 72f).roundToInt().coerceIn(0, 80)
            }
          }
        }

        // Die-cut border layer
        var borderLayerAlpha = 0
        var bR = borderR
        var bG = borderG
        var bB = borderB

        if (borderRadius > 0) {
          val edgeDist = d - borderRadius.toFloat()
          if (edgeDist <= 1.2f) {
            borderLayerAlpha =
              if (edgeDist <= -0.2f) 255
              else ((1.2f - edgeDist) / 1.4f * 255f).roundToInt().coerceIn(0, 255)

            // Optional inner antique gold hairline trim along border outer edge
            if (border.doubleGoldTrim && abs(edgeDist + 2.2f) <= 1.2f) {
              bR = goldR
              bG = goldG
              bB = goldB
            }
          }
        }

        // Composite: Shadow -> Border -> Subject Foreground
        var compA = underShadowAlpha
        var compR = 24
        var compG = 18
        var compB = 14

        if (borderLayerAlpha > 0) {
          val fa = borderLayerAlpha / 255f
          val ba = (compA / 255f) * (1f - fa)
          val outA = fa + ba
          if (outA > 1e-4f) {
            compR = ((bR * fa + compR * ba) / outA).roundToInt().coerceIn(0, 255)
            compG = ((bG * fa + compG * ba) / outA).roundToInt().coerceIn(0, 255)
            compB = ((bB * fa + compB * ba) / outA).roundToInt().coerceIn(0, 255)
            compA = (outA * 255f).roundToInt().coerceIn(0, 255)
          }
        }

        if (sa > 0) {
          val sp = srcPixels[idx]
          val sr = (sp ushr 16) and 0xFF
          val sg = (sp ushr 8) and 0xFF
          val sb = sp and 0xFF
          val fa = sa / 255f
          val ba = (compA / 255f) * (1f - fa)
          val outA = fa + ba
          if (outA > 1e-4f) {
            compR = ((sr * fa + compR * ba) / outA).roundToInt().coerceIn(0, 255)
            compG = ((sg * fa + compG * ba) / outA).roundToInt().coerceIn(0, 255)
            compB = ((sb * fa + compB * ba) / outA).roundToInt().coerceIn(0, 255)
            compA = (outA * 255f).roundToInt().coerceIn(0, 255)
          }
        }

        outPixels[idx] = (compA shl 24) or (compR shl 16) or (compG shl 8) or compB
      }
    }

    if (!trimToBounds) return Triple(outPixels, cw, ch)

    var minX = cw
    var minY = ch
    var maxX = -1
    var maxY = -1
    for (y in 0 until ch) {
      val row = y * cw
      for (x in 0 until cw) {
        if (((outPixels[row + x] ushr 24) and 0xFF) > 2) {
          if (x < minX) minX = x
          if (x > maxX) maxX = x
          if (y < minY) minY = y
          if (y > maxY) maxY = y
        }
      }
    }

    if (maxX < minX || maxY < minY) {
      return Triple(outPixels, cw, ch)
    }

    val trimPad = 4
    val tx0 = max(0, minX - trimPad)
    val ty0 = max(0, minY - trimPad)
    val tx1 = min(cw - 1, maxX + trimPad)
    val ty1 = min(ch - 1, maxY + trimPad)
    val tw = tx1 - tx0 + 1
    val th = ty1 - ty0 + 1
    val trimmed = IntArray(tw * th)
    for (y in 0 until th) {
      System.arraycopy(outPixels, (ty0 + y) * cw + tx0, trimmed, y * tw, tw)
    }
    return Triple(trimmed, tw, th)
  }

  private fun parseColorHex(hex: String, fallback: Int): Int =
    runCatching {
      val clean = hex.trim().removePrefix("#")
      when (clean.length) {
        6 -> (0xFF000000.toInt()) or clean.toInt(16)
        8 -> clean.toLong(16).toInt()
        else -> fallback
      }
    }.getOrDefault(fallback)
}
