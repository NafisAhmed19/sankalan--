package com.example.features.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.i18n.formatDigits
import com.example.core.model.CanvasElement
import com.example.core.model.NumeralStyle
import com.example.core.model.Page
import com.example.features.bookshelf.parseHexColor
import com.example.features.studio.StudioAssetGraphic
import com.example.features.studio.VoiceMemoryElementCard
import com.example.features.studio.findPaperPreset
import com.example.ui.theme.NotoSerifBengaliFontFamily
import com.example.ui.theme.SankalanTokens
import com.example.ui.theme.resolveEditorFontFamily
import java.io.File
import kotlin.math.roundToInt

/**
 * Procedural Torn Paper / Deckle Edge Clip Shape for scrapbooking photos and notes.
 */
val TornPaperShape = GenericShape { size, _ ->
  val teeth = 16
  val stepX = size.width / teeth
  val stepY = size.height / (teeth + 4)
  moveTo(0f, 3f)
  for (i in 0 until teeth) {
    val jitter = if (i % 2 == 0) 4f else 0f
    lineTo((i + 0.5f) * stepX, jitter)
    lineTo((i + 1f) * stepX, 3f)
  }
  for (j in 0 until teeth + 4) {
    val jitter = if (j % 2 == 0) size.width - 4f else size.width
    lineTo(jitter, (j + 0.5f) * stepY)
    lineTo(size.width - 2f, (j + 1f) * stepY)
  }
  for (i in teeth downTo 1) {
    val jitter = if (i % 2 == 0) size.height - 4f else size.height
    lineTo((i - 0.5f) * stepX, jitter)
    lineTo((i - 1f) * stepX, size.height - 2f)
  }
  for (j in (teeth + 4) downTo 1) {
    val jitter = if (j % 2 == 0) 4f else 0f
    lineTo(jitter, (j - 0.5f) * stepY)
    lineTo(2f, (j - 1f) * stepY)
  }
  close()
}

/**
 * Unified Physical Parchment Page Renderer shared across:
 * - Layered Canvas Editor (Step 7)
 * - Reading Mode Single & Spread views (Step 6)
 */
@Composable
fun ParchmentPageSurface(
  page: Page,
  numeralStyle: NumeralStyle,
  modifier: Modifier = Modifier,
  selectedElementId: String? = null,
  inlineEditingElementId: String? = null,
  inlinePlaceholder: String = "",
  onInlineTextChange: ((elementId: String, newText: String) -> Unit)? = null,
  showRotationAngleBadge: Boolean = false,
  activeStrokePreview: StrokePayload? = null,
  verticalGuides: List<Float> = emptyList(),
  horizontalGuides: List<Float> = emptyList(),
  liveTransformProvider: () -> LiveElementTransform? = { null },
  playingVoiceElementId: String? = null,
  onVoiceElementClick: ((CanvasElement) -> Unit)? = null,
) {
  val preset = findPaperPreset(page.background.presetId)
  val paperBg = parseHexColor(page.background.tintHex, parseHexColor(preset.tintHex, SankalanTokens.Paper))
  val headerInk = if (preset.isDarkPaper) SankalanTokens.Cream else SankalanTokens.Ink
  val lineInk = if (preset.isDarkPaper) SankalanTokens.AntiqueGold.copy(alpha = 0.24f) else SankalanTokens.Ink.copy(alpha = 0.12f)
  val pageEdgeShape =
    when (page.background.edgeStyle) {
      "torn" -> TornPaperShape
      "deckle" -> RoundedCornerShape(topStart = 12.dp, topEnd = 6.dp, bottomEnd = 14.dp, bottomStart = 8.dp)
      else -> RoundedCornerShape(10.dp)
    }
  val sortedElements = remember(page.elements) { page.elements.filter { !it.hidden }.sortedBy { it.zIndex } }

  BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
    val fitByHeight =
      constraints.hasBoundedHeight &&
        constraints.maxHeight > 0 &&
        constraints.maxWidth > 0 &&
        (constraints.maxWidth.toFloat() / constraints.maxHeight.toFloat()) > 0.72f
    Surface(
      shape = pageEdgeShape,
      color = paperBg.copy(alpha = page.background.opacity.coerceIn(0.6f, 1f)),
      border = BorderStroke(1.2.dp, SankalanTokens.AntiqueGold.copy(alpha = 0.65f)),
      shadowElevation = 6.dp,
      modifier = Modifier.aspectRatio(0.72f, matchHeightConstraintsFirst = fitByHeight),
    ) {
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val density = LocalDensity.current
      val canvasW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
      val canvasH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
      val scaleFactor = (canvasW / 360.dp.value / 2.2f).coerceIn(0.45f, 1.85f)

      // 1. Paper grain / ruled / dot-grid / graph / tea-stained / kraft / airmail / obsidian markings
      Canvas(modifier = Modifier.fillMaxSize()) {
        when (preset.pattern) {
          "ruled" -> {
            var y = size.height * 0.14f
            val step = size.height * 0.055f
            while (y < size.height * 0.94f) {
              drawLine(
                color = lineInk,
                start = Offset(size.width * 0.06f, y),
                end = Offset(size.width * 0.94f, y),
                strokeWidth = 0.9.dp.toPx(),
              )
              y += step
            }
          }
          "dot_grid" -> {
            val stepX = size.width * 0.06f
            val stepY = size.height * 0.045f
            var y = size.height * 0.10f
            while (y < size.height * 0.94f) {
              var x = size.width * 0.08f
              while (x < size.width * 0.93f) {
                drawCircle(
                  color = lineInk.copy(alpha = (lineInk.alpha * 1.4f).coerceAtMost(0.45f)),
                  radius = 1.dp.toPx(),
                  center = Offset(x, y),
                )
                x += stepX
              }
              y += stepY
            }
          }
          "graph" -> {
            val step = size.width * 0.065f
            var x = size.width * 0.06f
            while (x < size.width * 0.94f) {
              drawLine(color = lineInk, start = Offset(x, size.height * 0.06f), end = Offset(x, size.height * 0.94f), strokeWidth = 0.7.dp.toPx())
              x += step
            }
            var y = size.height * 0.06f
            while (y < size.height * 0.94f) {
              drawLine(color = lineInk, start = Offset(size.width * 0.06f, y), end = Offset(size.width * 0.94f, y), strokeWidth = 0.7.dp.toPx())
              y += step
            }
          }
          "tea_stained" -> {
            drawCircle(
              color = SankalanTokens.Leather.copy(alpha = 0.08f),
              radius = size.width * 0.34f,
              center = Offset(size.width * 0.85f, size.height * 0.14f),
            )
            drawCircle(
              color = SankalanTokens.Leather.copy(alpha = 0.06f),
              radius = size.width * 0.28f,
              center = Offset(size.width * 0.16f, size.height * 0.86f),
            )
          }
          "kraft" -> {
            var y = size.height * 0.05f
            val step = size.height * 0.07f
            while (y < size.height) {
              drawLine(
                color = SankalanTokens.Walnut700.copy(alpha = 0.09f),
                start = Offset(0f, y),
                end = Offset(size.width, y + 4.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
              )
              y += step
            }
          }
          "airmail" -> {
            val stripeW = 14.dp.toPx()
            var x = 8.dp.toPx()
            var flip = true
            while (x < size.width - 8.dp.toPx()) {
              drawLine(
                color = if (flip) SankalanTokens.Terracotta.copy(alpha = 0.5f) else Color(0xFF2A4365).copy(alpha = 0.5f),
                start = Offset(x, 6.dp.toPx()),
                end = Offset((x + stripeW * 0.7f).coerceAtMost(size.width - 8.dp.toPx()), 6.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
              )
              x += stripeW
              flip = !flip
            }
          }
          "obsidian_gold" -> {
            val inset = 10.dp.toPx()
            drawRect(
              color = SankalanTokens.AntiqueGold.copy(alpha = 0.45f),
              topLeft = Offset(inset, inset),
              size = Size(size.width - inset * 2, size.height - inset * 2),
              style = Stroke(width = 1.dp.toPx()),
            )
          }
          else -> {
            // Subtle laid paper vertical shimmer lines
            val step = size.width * 0.08f
            var x = step
            while (x < size.width) {
              drawLine(
                color = (if (preset.isDarkPaper) SankalanTokens.AntiqueGold else SankalanTokens.PaperShadow).copy(alpha = 0.18f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 0.7.dp.toPx(),
              )
              x += step
            }
          }
        }

        // Physical left spine binding gutter shadow & warm paper edge vignette
        drawRect(
          brush =
            Brush.horizontalGradient(
              colors =
                listOf(
                  Color.Black.copy(alpha = if (preset.isDarkPaper) 0.28f else 0.11f),
                  Color.Transparent,
                ),
              startX = 0f,
              endX = size.width * 0.065f,
            ),
          topLeft = Offset.Zero,
          size = Size(size.width * 0.065f, size.height),
        )
        // Soft bottom/right aged paper shadow depth
        drawRect(
          brush =
            Brush.verticalGradient(
              colors =
                listOf(
                  Color.Transparent,
                  (if (preset.isDarkPaper) Color.Black else SankalanTokens.Leather).copy(alpha = 0.055f),
                ),
              startY = size.height * 0.85f,
              endY = size.height,
            ),
          topLeft = Offset(0f, size.height * 0.85f),
          size = Size(size.width, size.height * 0.15f),
        )
      }

      // 2. Optional Page Header (Date, Mood, Pin, Title)
      if (!page.dateIso.isNullOrBlank() || !page.mood.isNullOrBlank() || !page.title.isNullOrBlank() || page.pinned) {
        Column(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
              if (!page.dateIso.isNullOrBlank()) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = if (preset.isDarkPaper) SankalanTokens.AntiqueGold else SankalanTokens.Leather,
                    modifier = Modifier.size(11.dp),
                  )
                  Text(
                    text = formatDigits(page.dateIso, numeralStyle),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = headerInk,
                  )
                }
              }
              if (!page.mood.isNullOrBlank()) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Mood,
                    contentDescription = null,
                    tint = if (preset.isDarkPaper) SankalanTokens.GoldHighlight else SankalanTokens.Sage,
                    modifier = Modifier.size(11.dp),
                  )
                  Text(
                    text = page.mood,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (preset.isDarkPaper) SankalanTokens.GoldHighlight else SankalanTokens.Sage,
                  )
                }
              }
            }
            if (page.pinned) {
              Icon(
                imageVector = Icons.Outlined.PushPin,
                contentDescription = null,
                tint = SankalanTokens.Terracotta,
                modifier = Modifier.size(13.dp),
              )
            }
          }
          if (!page.title.isNullOrBlank()) {
            Text(
              text = page.title,
              fontFamily = NotoSerifBengaliFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = (15f * scaleFactor.coerceAtLeast(0.85f)).sp,
              color = headerInk,
              maxLines = 1,
            )
            Box(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .height(1.dp)
                  .background(SankalanTokens.AntiqueGold.copy(alpha = 0.35f))
            )
          }
        }
      }

      // 3. Layered Canvas Elements (sorted by zIndex)
      sortedElements.forEach { el ->
        when (el.type) {
          "stroke" -> {
            val payload = remember(el.strokePointsJson) { CanvasPayloadCodec.decodeStroke(el.strokePointsJson) }
            if (payload != null && payload.points.isNotEmpty()) {
              Canvas(modifier = Modifier.fillMaxSize().alpha(el.opacity)) {
                drawStrokePayload(payload)
              }
            }
          }
          else -> {
            val px = (el.x * canvasW).roundToInt()
            val py = (el.y * canvasH).roundToInt()
            val pwDp = with(density) { (el.w * canvasW).toDp() }
            val phDp = with(density) { (el.h * canvasH).toDp() }
            val isPageBody = isPageBodyElement(el)
            val isDiagramBox = isDiagramTextBox(el) && el.assetId == DIAGRAM_BOX_ASSET_ID
            val isSelected = !isPageBody && el.id == selectedElementId
            val isInlineEditing = el.id == inlineEditingElementId && el.type == "text" && onInlineTextChange != null

            Box(
              modifier =
                Modifier
                  .offset { IntOffset(px, py) }
                  .width(pwDp.coerceAtLeast(24.dp))
                  .height(phDp.coerceAtLeast(20.dp))
                  .graphicsLayer {
                    val lt = if (!isPageBody) liveTransformProvider() else null
                    if (lt != null && el.id in lt.targetIds) {
                      translationX = lt.dxNorm * canvasW
                      translationY = lt.dyNorm * canvasH
                      if (el.id == lt.anchorElementId) {
                        scaleX = lt.scaleX
                        scaleY = lt.scaleY
                        rotationZ = lt.rotationOverrideDeg ?: (el.rotation + lt.rotationDeltaDeg)
                      } else {
                        rotationZ = el.rotation
                      }
                    } else {
                      rotationZ = el.rotation
                    }
                    transformOrigin = TransformOrigin.Center
                  }
                  .alpha(el.opacity)
                  .then(
                    when {
                      isInlineEditing && isPageBody -> Modifier
                      isInlineEditing ->
                        Modifier
                          .background(
                            if (preset.isDarkPaper) SankalanTokens.Walnut900.copy(alpha = 0.25f)
                            else SankalanTokens.AntiqueGold.copy(alpha = 0.08f),
                            RoundedCornerShape(6.dp),
                          )
                          .border(
                            width = 1.6.dp,
                            color = SankalanTokens.AntiqueGold,
                            shape = RoundedCornerShape(6.dp),
                          )
                      isSelected ->
                        Modifier.border(
                          width = 1.8.dp,
                          color = SankalanTokens.AntiqueGold,
                          shape = RoundedCornerShape(5.dp),
                        )
                      isDiagramBox ->
                        Modifier
                          .background(
                            if (preset.isDarkPaper) SankalanTokens.Walnut900.copy(alpha = 0.16f)
                            else SankalanTokens.AntiqueGold.copy(alpha = 0.06f),
                            RoundedCornerShape(6.dp),
                          )
                          .border(
                            width = 0.9.dp,
                            color = SankalanTokens.AntiqueGold.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(6.dp),
                          )
                      else -> Modifier
                    }
                  ),
            ) {
              when (el.type) {
                "text" -> {
                  if (isInlineEditing) {
                    RenderInlineTextEditor(
                      el = el,
                      scaleFactor = scaleFactor,
                      placeholder = inlinePlaceholder,
                      isDarkPaper = preset.isDarkPaper,
                      isPageBody = isPageBody,
                      onTextChange = { nextTxt -> onInlineTextChange?.invoke(el.id, nextTxt) },
                    )
                  } else {
                    RenderTextElement(el, scaleFactor, isPageBody = isPageBody)
                  }
                }
                "image" -> RenderImageElement(el)
                "shape" -> RenderShapeElement(el)
                "tape", "frame", "label", "sticker" ->
                  StudioAssetGraphic(
                    assetId = el.assetId,
                    elementType = el.type,
                    labelText = el.text,
                    customTintHex = el.colorHex,
                    blobRef = el.blobRef,
                  )
                "voice" ->
                  VoiceMemoryElementCard(
                    element = el,
                    isPlaying = el.id == playingVoiceElementId,
                    modifier =
                      if (onVoiceElementClick != null) {
                        Modifier.clickable { onVoiceElementClick(el) }
                      } else {
                        Modifier
                      },
                  )
              }

              if (isSelected && !isInlineEditing) {
                // Top-center rotation stem & enlarged gold rotate handle knob
                Box(
                  modifier =
                    Modifier
                      .align(Alignment.TopCenter)
                      .offset(y = (-30).dp),
                  contentAlignment = Alignment.TopCenter,
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                      shape = CircleShape,
                      color = SankalanTokens.AntiqueGold,
                      border = BorderStroke(1.6.dp, SankalanTokens.Walnut900),
                      shadowElevation = 4.dp,
                      modifier = Modifier.size(24.dp),
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(
                          imageVector = Icons.Outlined.RotateRight,
                          contentDescription = null,
                          tint = SankalanTokens.Walnut900,
                          modifier = Modifier.size(15.dp),
                        )
                      }
                    }
                    Box(
                      modifier =
                        Modifier
                          .width(1.8.dp)
                          .height(8.dp)
                          .background(SankalanTokens.AntiqueGold)
                    )
                  }
                }

                // Live rotation degree pill when rotating or rotated
                val liveTransform = liveTransformProvider()
                val liveAngle =
                  if (liveTransform != null && liveTransform.anchorElementId == el.id) {
                    liveTransform.rotationOverrideDeg ?: (el.rotation + liveTransform.rotationDeltaDeg)
                  } else {
                    el.rotation
                  }
                val liveRotating =
                  showRotationAngleBadge || (liveTransform != null && liveTransform.showRotationBadge && liveTransform.anchorElementId == el.id)
                if (liveRotating || kotlin.math.abs(liveAngle) > 0.5f) {
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SankalanTokens.Walnut900.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, SankalanTokens.AntiqueGold),
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-24).dp),
                  ) {
                    Text(
                      text = "${(((liveAngle % 360f) + 360f) % 360f).roundToInt()}°",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = SankalanTokens.GoldHighlight,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                  }
                }

                // Enlarged visual bottom-right resize knob
                Box(
                  modifier =
                    Modifier
                      .align(Alignment.BottomEnd)
                      .offset(x = 7.dp, y = 7.dp)
                      .size(18.dp)
                      .background(SankalanTokens.AntiqueGold, CircleShape)
                      .border(1.6.dp, SankalanTokens.Walnut900, CircleShape)
                )
              }
            }
          }
        }
      }

      // 4. Live In-Progress Ink Stroke Preview
      if (activeStrokePreview != null && activeStrokePreview.points.isNotEmpty()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          drawStrokePayload(activeStrokePreview)
        }
      }

      // 5. Magnetic Snapping Alignment Guides (Gold dashed hairlines read inside Draw phase)
      Canvas(modifier = Modifier.fillMaxSize()) {
        val lt = liveTransformProvider()
        val vList = lt?.verticalGuides ?: verticalGuides
        val hList = lt?.horizontalGuides ?: horizontalGuides
        if (vList.isNotEmpty() || hList.isNotEmpty()) {
          val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
          vList.forEach { gx ->
            drawLine(
              color = SankalanTokens.AntiqueGold,
              start = Offset(gx * size.width, 0f),
              end = Offset(gx * size.width, size.height),
              strokeWidth = 1.2.dp.toPx(),
              pathEffect = dash,
            )
          }
          hList.forEach { gy ->
            drawLine(
              color = SankalanTokens.AntiqueGold,
              start = Offset(0f, gy * size.height),
              end = Offset(size.width, gy * size.height),
              strokeWidth = 1.2.dp.toPx(),
              pathEffect = dash,
            )
          }
        }
      }
    }
  }
}
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStrokePayload(payload: StrokePayload) {
  val pen = PenKind.fromId(payload.tool)
  val baseColor = parseHexColor(payload.colorHex, SankalanTokens.Ink)
  val alpha =
    when (pen) {
      PenKind.HIGHLIGHTER -> 0.34f
      PenKind.PENCIL -> 0.72f
      PenKind.MARKER -> 0.90f
      else -> payload.opacity.coerceIn(0.1f, 1f)
    }
  val widthMultiplier =
    when (pen) {
      PenKind.HIGHLIGHTER -> 2.4f
      PenKind.MARKER -> 1.65f
      PenKind.PENCIL -> 0.8f
      else -> 1.0f
    }
  val pts = payload.points
  if (pts.isEmpty()) return
  if (pts.size == 1) {
    val r = (payload.widthRatio * size.width * widthMultiplier * (0.6f + pts[0].pressure * 0.6f)).coerceAtLeast(1.5f)
    drawCircle(
      color = baseColor.copy(alpha = alpha),
      radius = r / 2f,
      center = Offset(pts[0].x * size.width, pts[0].y * size.height),
    )
    return
  }

  // Smooth natural handwriting via quadratic midpoint curves (for pressure-uniform segments or subtle pressure feel)
  val avgPressure = pts.map { it.pressure }.average().toFloat().coerceIn(0.25f, 1.35f)
  val smoothW = (payload.widthRatio * size.width * widthMultiplier * (0.65f + avgPressure * 0.55f)).coerceAtLeast(1.5f)
  if (pts.size >= 3 && pen != PenKind.MARKER) {
    val path = Path().apply {
      moveTo(pts[0].x * size.width, pts[0].y * size.height)
      for (i in 1 until pts.lastIndex) {
        val x0 = pts[i].x * size.width
        val y0 = pts[i].y * size.height
        val x1 = pts[i + 1].x * size.width
        val y1 = pts[i + 1].y * size.height
        quadraticTo(x0, y0, (x0 + x1) * 0.5f, (y0 + y1) * 0.5f)
      }
      lineTo(pts.last().x * size.width, pts.last().y * size.height)
    }
    drawPath(
      path = path,
      color = baseColor.copy(alpha = alpha),
      style = Stroke(width = smoothW, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    return
  }

  for (i in 0 until pts.lastIndex) {
    val p0 = pts[i]
    val p1 = pts[i + 1]
    val segPressure = ((p0.pressure + p1.pressure) * 0.5f).coerceIn(0.25f, 1.35f)
    val strokeW =
      (payload.widthRatio * size.width * widthMultiplier * (0.65f + segPressure * 0.55f))
        .coerceAtLeast(1.5f)
    drawLine(
      color = baseColor.copy(alpha = alpha),
      start = Offset(p0.x * size.width, p0.y * size.height),
      end = Offset(p1.x * size.width, p1.y * size.height),
      strokeWidth = strokeW,
      cap = if (pen == PenKind.MARKER) StrokeCap.Square else StrokeCap.Round,
    )
  }
}

@Composable
private fun RenderInlineTextEditor(
  el: CanvasElement,
  scaleFactor: Float,
  placeholder: String,
  isDarkPaper: Boolean,
  isPageBody: Boolean,
  onTextChange: (String) -> Unit,
) {
  val fontFamily = resolveEditorFontFamily(el.fontId)
  val baseSize = (el.fontSize ?: 15f) * scaleFactor.coerceIn(0.65f, 1.35f)
  val ink = parseHexColor(el.colorHex ?: if (isDarkPaper) "#F6EFE2" else "#302C29", SankalanTokens.Ink)
  val align =
    when (el.align) {
      "center" -> TextAlign.Center
      "right" -> TextAlign.End
      else -> TextAlign.Start
    }
  val textStyle =
    TextStyle(
      fontFamily = fontFamily,
      fontSize = baseSize.sp,
      lineHeight = (baseSize * if (isPageBody) 1.62f else 1.50f).sp,
      fontWeight = if (el.bold) FontWeight.Bold else FontWeight.Normal,
      fontStyle = if (el.italic) FontStyle.Italic else FontStyle.Normal,
      textDecoration = if (el.underline) TextDecoration.Underline else TextDecoration.None,
      color = ink,
      textAlign = align,
    )
  val focusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current

  LaunchedEffect(el.id) {
    focusRequester.requestFocus()
    keyboardController?.show()
  }

  val baseModifier =
    if (isPageBody) {
      Modifier.fillMaxSize().padding(horizontal = 2.dp, vertical = 1.dp).verticalScroll(rememberScrollState())
    } else {
      Modifier.fillMaxSize().padding(5.dp)
    }

  BasicTextField(
    value = el.text.orEmpty(),
    onValueChange = onTextChange,
    textStyle = textStyle,
    cursorBrush = SolidColor(ink),
    modifier = baseModifier.focusRequester(focusRequester),
    decorationBox = { innerTextField ->
      Box(modifier = Modifier.fillMaxSize()) {
        if (el.text.isNullOrEmpty() && placeholder.isNotEmpty()) {
          Text(
            text = placeholder,
            style = textStyle.copy(color = ink.copy(alpha = 0.42f)),
          )
        }
        innerTextField()
      }
    },
  )
}

@Composable
private fun RenderTextElement(
  el: CanvasElement,
  scaleFactor: Float,
  isPageBody: Boolean = false,
) {
  val fontFamily = resolveEditorFontFamily(el.fontId)
  val baseSize = (el.fontSize ?: 15f) * scaleFactor.coerceIn(0.65f, 1.35f)
  val ink = parseHexColor(el.colorHex ?: "#302C29", SankalanTokens.Ink)
  val align =
    when (el.align) {
      "center" -> TextAlign.Center
      "right" -> TextAlign.End
      else -> TextAlign.Start
    }
  Text(
    text = el.text.orEmpty(),
    style =
      TextStyle(
        fontFamily = fontFamily,
        fontSize = baseSize.sp,
        lineHeight = (baseSize * if (isPageBody) 1.62f else 1.50f).sp,
        fontWeight = if (el.bold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (el.italic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (el.underline) TextDecoration.Underline else TextDecoration.None,
        color = ink,
        textAlign = align,
      ),
    modifier =
      if (isPageBody) Modifier.fillMaxSize().padding(horizontal = 2.dp, vertical = 1.dp)
      else Modifier.fillMaxSize().padding(5.dp),
  )
}

@Composable
private fun RenderImageElement(el: CanvasElement) {
  val framePayload = remember(el.strokePointsJson) { CanvasPayloadCodec.decodeImageFrame(el.strokePointsJson) }
  val frameStyle = PhotoFrameStyle.fromId(framePayload.frameStyle)
  val model: Any? =
    remember(el.blobRef) {
      el.blobRef?.let { path ->
        val file = File(path)
        if (file.exists()) file else path
      }
    }

  val clipShape =
    when (frameStyle) {
      PhotoFrameStyle.TORN_PAPER -> TornPaperShape
      PhotoFrameStyle.ROUNDED -> RoundedCornerShape(12.dp)
      PhotoFrameStyle.OVAL -> CircleShape
      PhotoFrameStyle.POLAROID,
      PhotoFrameStyle.CLEAN -> RoundedCornerShape(3.dp)
    }

  if (frameStyle == PhotoFrameStyle.POLAROID) {
    Surface(
      shape = RoundedCornerShape(4.dp),
      color = SankalanTokens.Cream,
      border = BorderStroke(1.dp, SankalanTokens.PaperShadow),
      shadowElevation = 4.dp,
      modifier = Modifier.fillMaxSize(),
    ) {
      Column(modifier = Modifier.fillMaxSize().padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 14.dp)) {
        AsyncImage(
          model = model,
          contentDescription = el.text ?: "Diary photo",
          contentScale = ContentScale.Crop,
          modifier =
            Modifier
              .weight(1f)
              .fillMaxWidth()
              .clip(RoundedCornerShape(2.dp))
              .graphicsLayer {
                scaleX = framePayload.cropZoom
                scaleY = framePayload.cropZoom
                translationX = framePayload.cropOffsetX * 80f
                translationY = framePayload.cropOffsetY * 80f
              },
        )
      }
    }
  } else {
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .shadow(if (frameStyle == PhotoFrameStyle.TORN_PAPER) 4.dp else 2.dp, clipShape)
          .clip(clipShape)
          .background(SankalanTokens.PaperShadow),
    ) {
      AsyncImage(
        model = model,
        contentDescription = el.text ?: "Diary photo",
        contentScale = ContentScale.Crop,
        modifier =
          Modifier
            .fillMaxSize()
            .graphicsLayer {
              scaleX = framePayload.cropZoom
              scaleY = framePayload.cropZoom
              translationX = framePayload.cropOffsetX * 80f
              translationY = framePayload.cropOffsetY * 80f
            },
      )
    }
  }
}

@Composable
private fun RenderShapeElement(el: CanvasElement) {
  val shapeColor = parseHexColor(el.colorHex ?: "#B08A4F", SankalanTokens.AntiqueGold)
  Canvas(modifier = Modifier.fillMaxSize()) {
    when (el.assetId) {
      ShapeKind.CIRCLE.id -> {
        drawCircle(color = shapeColor.copy(alpha = 0.25f))
        drawCircle(color = shapeColor, style = Stroke(width = 2.dp.toPx()))
      }
      ShapeKind.ROUNDED_CARD.id -> {
        drawRoundRect(
          color = shapeColor.copy(alpha = 0.16f),
          cornerRadius = CornerRadius(10.dp.toPx()),
        )
        drawRoundRect(
          color = shapeColor,
          cornerRadius = CornerRadius(10.dp.toPx()),
          style = Stroke(width = 1.5.dp.toPx()),
        )
      }
      ShapeKind.RIBBON_BANNER.id -> {
        val path =
          Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width * 0.92f, size.height * 0.5f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            lineTo(size.width * 0.08f, size.height * 0.5f)
            close()
          }
        drawPath(path, color = shapeColor.copy(alpha = 0.85f))
      }
      ShapeKind.DIVIDER_RULE.id -> {
        val midY = size.height / 2f
        drawLine(
          color = shapeColor,
          start = Offset(0f, midY),
          end = Offset(size.width, midY),
          strokeWidth = 2.dp.toPx(),
          cap = StrokeCap.Round,
        )
        drawCircle(color = shapeColor, radius = 3.5.dp.toPx(), center = Offset(size.width / 2f, midY))
      }
      ShapeKind.HEART.id -> {
        val w = size.width
        val h = size.height
        val path =
          Path().apply {
            moveTo(w * 0.5f, h * 0.88f)
            cubicTo(w * 0.1f, h * 0.62f, w * 0.05f, h * 0.22f, w * 0.3f, h * 0.18f)
            cubicTo(w * 0.44f, h * 0.16f, w * 0.5f, h * 0.30f, w * 0.5f, h * 0.36f)
            cubicTo(w * 0.5f, h * 0.30f, w * 0.56f, h * 0.16f, w * 0.7f, h * 0.18f)
            cubicTo(w * 0.95f, h * 0.22f, w * 0.9f, h * 0.62f, w * 0.5f, h * 0.88f)
            close()
          }
        drawPath(path, color = shapeColor.copy(alpha = 0.82f))
      }
      else -> {
        // 5-point star / emblem
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerR = size.minDimension * 0.46f
        val innerR = outerR * 0.42f
        val path = Path()
        for (i in 0 until 10) {
          val angle = Math.toRadians(-90.0 + i * 36.0)
          val r = if (i % 2 == 0) outerR else innerR
          val x = cx + (r * kotlin.math.cos(angle)).toFloat()
          val y = cy + (r * kotlin.math.sin(angle)).toFloat()
          if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color = shapeColor.copy(alpha = 0.85f))
      }
    }
  }
}
