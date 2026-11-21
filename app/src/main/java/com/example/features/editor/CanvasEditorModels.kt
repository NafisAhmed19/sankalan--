package com.example.features.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.core.model.CanvasElement
import com.example.core.model.Page
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.abs

enum class EditorToolMode {
  SELECT,
  TEXT,
  DRAW,
  PHOTO,
  STUDIO_ASSETS,
  PAPER,
  TEMPLATES,
  VOICE,
  SHAPES,
  LAYERS,
  PAGE_INFO,
}

enum class PenKind(val id: String) {
  PEN("pen"),
  PENCIL("pencil"),
  MARKER("marker"),
  HIGHLIGHTER("highlighter"),
  ERASER("eraser");

  companion object {
    fun fromId(id: String?): PenKind = entries.firstOrNull { it.id == id } ?: PEN
  }
}

enum class PhotoFrameStyle(val id: String) {
  CLEAN("clean"),
  POLAROID("polaroid"),
  TORN_PAPER("torn_paper"),
  ROUNDED("rounded"),
  OVAL("oval");

  companion object {
    fun fromId(id: String?): PhotoFrameStyle = entries.firstOrNull { it.id == id } ?: CLEAN
  }
}

enum class ShapeKind(val id: String, val label: String) {
  CIRCLE("circle", "Circle"),
  ROUNDED_CARD("rounded_card", "Card"),
  RIBBON_BANNER("ribbon", "Ribbon"),
  DIVIDER_RULE("divider", "Divider"),
  HEART("heart", "Heart"),
  STAR("star", "Star"),
}

data class EditorFontChoice(
  val id: String,
  val displayName: String,
  val sample: String,
)

val EDITOR_FONT_CHOICES =
  listOf(
    EditorFontChoice("noto_sans_bn", "Noto Sans বাংলা", "সংকলন · স্মৃতির খাতা"),
    EditorFontChoice("noto_serif_bn", "Noto Serif বাংলা", "জীবন চলার ছাপ"),
    EditorFontChoice("atma", "Atma (বাংলা হাতের লেখা)", "মনের ডায়েরি"),
    EditorFontChoice("galada", "Galada (বাংলা ক্যালিগ্রাফি)", "শ্রদ্ধা ও ভালোবাসা"),
    EditorFontChoice("inter", "Inter Clean", "Quiet evening notes"),
    EditorFontChoice("playfair", "Playfair Literary", "Timeless Volume"),
    EditorFontChoice("caveat", "Caveat Handwriting", "Handwritten memory"),
  )

val EDITOR_INK_SWATCHES =
  listOf(
    "#302C29", // Charcoal Ink
    "#593B2B", // Walnut Leather
    "#B08A4F", // Antique Gold
    "#A83E2B", // Terracotta / Crimson
    "#5C7052", // Botanical Sage
    "#2A4365", // Midnight Indigo
    "#6B3FA0", // Royal Plum
    "#E9C46A", // Warm Highlighter Amber
    "#F6EFE2", // Warm Ivory Ink (for Dark Papers)
  )

data class StrokePoint(
  val x: Float,
  val y: Float,
  val pressure: Float = 0.5f,
)

data class StrokePayload(
  val tool: String = PenKind.PEN.id,
  val colorHex: String = "#302C29",
  val widthRatio: Float = 0.008f,
  val opacity: Float = 1f,
  val points: List<StrokePoint> = emptyList(),
)

data class ImageFramePayload(
  val frameStyle: String = PhotoFrameStyle.CLEAN.id,
  val cropZoom: Float = 1f,
  val cropOffsetX: Float = 0f,
  val cropOffsetY: Float = 0f,
)

object CanvasPayloadCodec {
  private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val strokeAdapter = moshi.adapter(StrokePayload::class.java)
  private val imageAdapter = moshi.adapter(ImageFramePayload::class.java)

  fun encodeStroke(payload: StrokePayload): String = strokeAdapter.toJson(payload)

  fun decodeStroke(json: String?): StrokePayload? {
    if (json.isNullOrBlank()) return null
    return runCatching { strokeAdapter.fromJson(json) }.getOrNull()
  }

  fun encodeImageFrame(payload: ImageFramePayload): String = imageAdapter.toJson(payload)

  fun decodeImageFrame(json: String?): ImageFramePayload =
    if (json.isNullOrBlank()) ImageFramePayload()
    else runCatching { imageAdapter.fromJson(json) }.getOrNull() ?: ImageFramePayload()
}

/**
 * Immutable Page Snapshot Undo/Redo history stack (step-scoped up to 40 states).
 */
class UndoRedoHistory<T>(
  initialState: T,
  private val maxDepth: Int = 40,
) {
  private val undoStack = ArrayDeque<T>()
  private val redoStack = ArrayDeque<T>()
  var current: T = initialState
    private set

  val canUndo: Boolean
    get() = undoStack.isNotEmpty()

  val canRedo: Boolean
    get() = redoStack.isNotEmpty()

  fun commit(nextState: T) {
    if (nextState == current) return
    undoStack.addLast(current)
    while (undoStack.size > maxDepth) {
      undoStack.removeFirst()
    }
    current = nextState
    redoStack.clear()
  }

  fun undo(): T? {
    if (undoStack.isEmpty()) return null
    redoStack.addLast(current)
    current = undoStack.removeLast()
    return current
  }

  fun redo(): T? {
    if (redoStack.isEmpty()) return null
    undoStack.addLast(current)
    current = redoStack.removeLast()
    return current
  }
}

data class SnapResult(
  val snappedX: Float,
  val snappedY: Float,
  val verticalGuides: List<Float>,
  val horizontalGuides: List<Float>,
)

object CanvasSnapEngine {
  private const val SNAP_THRESHOLD = 0.016f

  fun snapPosition(
    candidateX: Float,
    candidateY: Float,
    elementW: Float,
    elementH: Float,
    otherElements: List<CanvasElement>,
  ): SnapResult {
    val xTargets = mutableListOf(0.06f, 0.5f, 0.94f)
    val yTargets = mutableListOf(0.06f, 0.5f, 0.94f)
    otherElements.forEach { el ->
      if (!el.hidden) {
        xTargets.add(el.x)
        xTargets.add(el.x + el.w / 2f)
        xTargets.add(el.x + el.w)
        yTargets.add(el.y)
        yTargets.add(el.y + el.h / 2f)
        yTargets.add(el.y + el.h)
      }
    }

    var finalX = candidateX.coerceIn(-0.2f, 0.95f)
    val vGuides = mutableListOf<Float>()
    val myXs = listOf(finalX to 0f, (finalX + elementW / 2f) to (elementW / 2f), (finalX + elementW) to elementW)
    var bestDx = SNAP_THRESHOLD
    var snappedLineX: Float? = null
    for ((pos, offsetFromLeft) in myXs) {
      for (target in xTargets) {
        val diff = target - pos
        if (abs(diff) < bestDx) {
          bestDx = abs(diff)
          finalX = (target - offsetFromLeft).coerceIn(-0.2f, 0.95f)
          snappedLineX = target
        }
      }
    }
    if (snappedLineX != null) vGuides.add(snappedLineX)

    var finalY = candidateY.coerceIn(-0.1f, 0.95f)
    val hGuides = mutableListOf<Float>()
    val myYs = listOf(finalY to 0f, (finalY + elementH / 2f) to (elementH / 2f), (finalY + elementH) to elementH)
    var bestDy = SNAP_THRESHOLD
    var snappedLineY: Float? = null
    for ((pos, offsetFromTop) in myYs) {
      for (target in yTargets) {
        val diff = target - pos
        if (abs(diff) < bestDy) {
          bestDy = abs(diff)
          finalY = (target - offsetFromTop).coerceIn(-0.1f, 0.95f)
          snappedLineY = target
        }
      }
    }
    if (snappedLineY != null) hGuides.add(snappedLineY)

    return SnapResult(
      snappedX = finalX,
      snappedY = finalY,
      verticalGuides = vGuides,
      horizontalGuides = hGuides,
    )
  }
}

/**
 * Persists a zero-permission Photo Picker image locally inside app filesDir with downsampling
 * so images stay readable across app launches without requiring broad storage permissions.
 */
fun copyPickedPhotoToInternalMedia(context: Context, uri: Uri): String? {
  return runCatching {
    val mediaDir = File(context.filesDir, "diary_media").apply { mkdirs() }
    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { stream ->
      BitmapFactory.decodeStream(stream, null, boundsOpts)
    }
    val maxDim = 1280
    var sampleSize = 1
    val w = boundsOpts.outWidth
    val h = boundsOpts.outHeight
    if (w > maxDim || h > maxDim) {
      while (w / sampleSize > maxDim || h / sampleSize > maxDim) {
        sampleSize *= 2
      }
    }
    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val bitmap =
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, decodeOpts)
      } ?: return null

    val targetFile = File(mediaDir, "photo_${UUID.randomUUID()}.jpg")
    FileOutputStream(targetFile).use { out ->
      bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
    }
    bitmap.recycle()
    targetFile.absolutePath
  }.getOrNull()
}

fun nextZIndex(page: Page): Int =
  (page.elements.maxOfOrNull { it.zIndex } ?: 0) + 1

const val PAGE_BODY_ASSET_ID = "page_body"
const val DIAGRAM_BOX_ASSET_ID = "diagram_box"

fun isPageBodyElement(el: CanvasElement): Boolean =
  el.type == "text" && (
    el.assetId == PAGE_BODY_ASSET_ID ||
      el.id.endsWith("-text-1") ||
      (el.assetId == null && el.w >= 0.78f && el.h >= 0.45f && abs(el.rotation) < 0.5f)
    )

fun isDiagramTextBox(el: CanvasElement): Boolean =
  el.type == "text" && !isPageBodyElement(el)

data class LiveElementTransform(
  val targetIds: Set<String> = emptySet(),
  val anchorElementId: String? = null,
  val dxNorm: Float = 0f,
  val dyNorm: Float = 0f,
  val scaleX: Float = 1f,
  val scaleY: Float = 1f,
  val rotationOverrideDeg: Float? = null,
  val rotationDeltaDeg: Float = 0f,
  val showRotationBadge: Boolean = false,
  val verticalGuides: List<Float> = emptyList(),
  val horizontalGuides: List<Float> = emptyList(),
)

enum class ElementHitZone {
  BODY,
  ROTATE_HANDLE,
  RESIZE_HANDLE,
}

object CanvasTransformMath {
  /**
   * Computes normalized element-local coordinates relative to element center,
   * accounting for canvas aspect ratio so rotated handles align visually.
   */
  fun toRotatedLocal(
    nx: Float,
    ny: Float,
    el: CanvasElement,
    canvasAspect: Float,
  ): Pair<Float, Float> {
    val cx = el.x + el.w * 0.5f
    val cy = el.y + el.h * 0.5f
    val dxPx = (nx - cx) * canvasAspect
    val dyPx = ny - cy
    val rad = Math.toRadians(-el.rotation.toDouble())
    val cosR = kotlin.math.cos(rad).toFloat()
    val sinR = kotlin.math.sin(rad).toFloat()
    val localX = (dxPx * cosR - dyPx * sinR) / canvasAspect
    val localY = dxPx * sinR + dyPx * cosR
    return localX to localY
  }

  /**
   * Hit-tests a normalized canvas point against a possibly rotated element (and its active knobs).
   */
  fun hitTestElement(
    nx: Float,
    ny: Float,
    el: CanvasElement,
    canvasAspect: Float,
    isSelected: Boolean,
  ): ElementHitZone? {
    val (lx, ly) = toRotatedLocal(nx, ny, el, canvasAspect)
    val halfW = el.w * 0.5f
    val halfH = el.h * 0.5f
    if (isSelected) {
      // Top-center rotate handle sits above top edge (approx -halfH - 0.055f)
      val rotCenterY = -halfH - 0.055f
      if (abs(lx) <= 0.10f && abs(ly - rotCenterY) <= 0.085f) {
        return ElementHitZone.ROTATE_HANDLE
      }
      // Bottom-right resize handle sits at (halfW, halfH)
      if (abs(lx - halfW) <= 0.095f && abs(ly - halfH) <= 0.095f) {
        return ElementHitZone.RESIZE_HANDLE
      }
    }
    val pad = 0.02f
    if (lx in (-halfW - pad)..(halfW + pad) && ly in (-halfH - pad)..(halfH + pad)) {
      return ElementHitZone.BODY
    }
    return null
  }

  /**
   * Calculates clockwise angle in degrees [0, 360) when dragging the top-center rotate knob.
   */
  fun computeRotationDegreesFromTop(
    pointerX: Float,
    pointerY: Float,
    centerX: Float,
    centerY: Float,
  ): Float {
    val deg = Math.toDegrees(kotlin.math.atan2((pointerY - centerY).toDouble(), (pointerX - centerX).toDouble())).toFloat() + 90f
    return ((deg % 360f) + 360f) % 360f
  }

  fun commitLiveTransform(
    elements: List<CanvasElement>,
    transform: LiveElementTransform,
  ): List<CanvasElement> {
    if (transform.targetIds.isEmpty()) return elements
    return elements.map { item ->
      if (item.id !in transform.targetIds) {
        item
      } else if (item.id == transform.anchorElementId) {
        val newW = (item.w * transform.scaleX).coerceIn(0.08f, 0.98f)
        val newH = (item.h * transform.scaleY).coerceIn(0.04f, 0.94f)
        val cx = item.x + item.w * 0.5f + transform.dxNorm
        val cy = item.y + item.h * 0.5f + transform.dyNorm
        val nextRot = (((transform.rotationOverrideDeg ?: (item.rotation + transform.rotationDeltaDeg)) % 360f) + 360f) % 360f
        item.copy(
          x = (cx - newW * 0.5f).coerceIn(-0.25f, 0.96f),
          y = (cy - newH * 0.5f).coerceIn(-0.15f, 0.96f),
          w = newW,
          h = newH,
          rotation = nextRot,
        )
      } else {
        item.copy(
          x = (item.x + transform.dxNorm).coerceIn(-0.25f, 0.96f),
          y = (item.y + transform.dyNorm).coerceIn(-0.15f, 0.96f),
        )
      }
    }
  }

  fun applyPinchRotateAndPan(
    el: CanvasElement,
    zoomFactor: Float,
    rotateDeltaDeg: Float,
    panDxNorm: Float,
    panDyNorm: Float,
  ): CanvasElement {
    val safeZoom = zoomFactor.coerceIn(0.6f, 1.8f)
    val nextW = (el.w * safeZoom).coerceIn(0.08f, 0.98f)
    val nextH = (el.h * safeZoom).coerceIn(0.04f, 0.94f)
    val dw = nextW - el.w
    val dh = nextH - el.h
    val nextX = (el.x - dw * 0.5f + panDxNorm).coerceIn(-0.25f, 0.96f)
    val nextY = (el.y - dh * 0.5f + panDyNorm).coerceIn(-0.15f, 0.96f)
    val nextRot = (((el.rotation + rotateDeltaDeg) % 360f) + 360f) % 360f
    return el.copy(
      x = nextX,
      y = nextY,
      w = nextW,
      h = nextH,
      rotation = nextRot,
    )
  }
}

