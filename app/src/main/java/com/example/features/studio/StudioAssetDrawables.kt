package com.example.features.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.model.CanvasElement
import com.example.features.bookshelf.parseHexColor
import com.example.features.editor.formatDurationMmSs
import com.example.ui.theme.NotoSerifBengaliFontFamily
import com.example.ui.theme.SankalanTokens
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

/**
 * Serrated horizontal washi tape edge shape.
 */
val SerratedTapeShape = GenericShape { size, _ ->
  val teeth = 7
  val stepY = size.height / teeth
  moveTo(0f, 0f)
  lineTo(size.width, 0f)
  for (i in 0 until teeth) {
    val x = if (i % 2 == 0) size.width - 5f else size.width
    lineTo(x, (i + 0.5f) * stepY)
  }
  lineTo(size.width, size.height)
  lineTo(0f, size.height)
  for (i in teeth downTo 1) {
    val x = if (i % 2 == 0) 5f else 0f
    lineTo(x, (i - 0.5f) * stepY)
  }
  close()
}

@Composable
fun StudioAssetGraphic(
  assetId: String?,
  elementType: String,
  labelText: String? = null,
  customTintHex: String? = null,
  blobRef: String? = null,
  modifier: Modifier = Modifier,
) {
  if (!blobRef.isNullOrBlank() && elementType != "voice") {
    val file = File(blobRef)
    AsyncImage(
      model = if (file.exists()) file else blobRef,
      contentDescription = labelText ?: "Uploaded sticker",
      contentScale = ContentScale.Fit,
      modifier = modifier.fillMaxSize(),
    )
    return
  }

  val catalogAsset = findStudioAsset(assetId)
  val primary = parseHexColor(customTintHex ?: catalogAsset?.colorHex ?: "#B08A4F", SankalanTokens.AntiqueGold)
  val secondary = parseHexColor(catalogAsset?.secondaryHex ?: "#F6EFE2", SankalanTokens.Paper)
  val crispOutline = Color(0xFF241A16)

  when (elementType) {
    "tape" -> {
      Box(modifier = modifier.fillMaxSize().clip(SerratedTapeShape)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          // Semi-translucent kraft/washi sheen with crisp borders
          drawRect(
            brush = Brush.verticalGradient(
              colors = listOf(primary.copy(alpha = 0.90f), primary.copy(alpha = 0.78f), primary.copy(alpha = 0.88f))
            )
          )
          when (assetId) {
            "tape_gingham_terra" -> {
              var x = 0f
              val step = 12.dp.toPx()
              while (x < size.width) {
                drawRect(
                  color = secondary.copy(alpha = 0.45f),
                  topLeft = Offset(x, 0f),
                  size = Size(step * 0.48f, size.height),
                )
                x += step
              }
              drawRect(
                color = secondary.copy(alpha = 0.34f),
                topLeft = Offset(0f, size.height * 0.25f),
                size = Size(size.width, size.height * 0.5f),
              )
            }
            "tape_sage_stripe" -> {
              var x = -size.height
              val step = 13.dp.toPx()
              while (x < size.width + size.height) {
                drawLine(
                  color = secondary.copy(alpha = 0.65f),
                  start = Offset(x, size.height),
                  end = Offset(x + size.height, 0f),
                  strokeWidth = 3.2.dp.toPx(),
                )
                x += step
              }
            }
            "tape_indigo_stars" -> {
              var x = 10.dp.toPx()
              while (x < size.width - 6.dp.toPx()) {
                val sy = size.height * 0.48f
                drawCircle(color = secondary, radius = 2.6.dp.toPx(), center = Offset(x, sy))
                drawLine(color = secondary, start = Offset(x - 4.dp.toPx(), sy), end = Offset(x + 4.dp.toPx(), sy), strokeWidth = 1.dp.toPx())
                drawLine(color = secondary, start = Offset(x, sy - 4.dp.toPx()), end = Offset(x, sy + 4.dp.toPx()), strokeWidth = 1.dp.toPx())
                x += 17.dp.toPx()
              }
            }
            "tape_floral_meadow" -> {
              var x = 11.dp.toPx()
              while (x < size.width - 8.dp.toPx()) {
                val fy = size.height * 0.5f
                for (p in 0 until 5) {
                  val rad = Math.toRadians(p * 72.0)
                  drawCircle(
                    color = secondary,
                    radius = 2.3.dp.toPx(),
                    center = Offset(x + (3.dp.toPx() * cos(rad)).toFloat(), fy + (3.dp.toPx() * sin(rad)).toFloat()),
                  )
                }
                drawCircle(color = SankalanTokens.AntiqueGold, radius = 2.dp.toPx(), center = Offset(x, fy))
                x += 19.dp.toPx()
              }
            }
            else -> {
              drawLine(
                color = secondary.copy(alpha = 0.72f),
                start = Offset(0f, size.height * 0.2f),
                end = Offset(size.width, size.height * 0.2f),
                strokeWidth = 1.2.dp.toPx(),
              )
              drawLine(
                color = secondary.copy(alpha = 0.72f),
                start = Offset(0f, size.height * 0.8f),
                end = Offset(size.width, size.height * 0.8f),
                strokeWidth = 1.2.dp.toPx(),
              )
            }
          }
          drawRect(color = crispOutline.copy(alpha = 0.22f), style = Stroke(width = 1.dp.toPx()))
        }
      }
    }

    "frame" -> {
      Canvas(modifier = modifier.fillMaxSize()) {
        val pad = 4.dp.toPx()
        when (assetId) {
          "frame_vintage_stamp" -> {
            val dash = PathEffect.dashPathEffect(floatArrayOf(9f, 5f), 0f)
            drawRoundRect(
              color = secondary.copy(alpha = 0.65f),
              cornerRadius = CornerRadius(4.dp.toPx()),
            )
            drawRoundRect(
              color = primary,
              topLeft = Offset(pad, pad),
              size = Size(size.width - pad * 2, size.height - pad * 2),
              cornerRadius = CornerRadius(3.dp.toPx()),
              style = Stroke(width = 2.6.dp.toPx(), pathEffect = dash),
            )
          }
          "frame_botanical_corner" -> {
            drawRoundRect(
              color = primary,
              topLeft = Offset(pad, pad),
              size = Size(size.width - pad * 2, size.height - pad * 2),
              cornerRadius = CornerRadius(8.dp.toPx()),
              style = Stroke(width = 2.2.dp.toPx()),
            )
            val leafR = 7.dp.toPx()
            listOf(
              Offset(pad + 7.dp.toPx(), pad + 7.dp.toPx()),
              Offset(size.width - pad - 7.dp.toPx(), pad + 7.dp.toPx()),
              Offset(pad + 7.dp.toPx(), size.height - pad - 7.dp.toPx()),
              Offset(size.width - pad - 7.dp.toPx(), size.height - pad - 7.dp.toPx()),
            ).forEach { corner ->
              drawCircle(color = secondary, radius = leafR * 0.72f, center = corner)
              drawCircle(color = primary, radius = leafR * 0.45f, center = corner)
            }
          }
          "frame_stitched_card" -> {
            drawRoundRect(
              color = secondary.copy(alpha = 0.72f),
              cornerRadius = CornerRadius(10.dp.toPx()),
            )
            drawRoundRect(
              color = primary,
              topLeft = Offset(pad, pad),
              size = Size(size.width - pad * 2, size.height - pad * 2),
              cornerRadius = CornerRadius(8.dp.toPx()),
              style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))),
            )
          }
          "frame_floral_vine" -> {
            drawRoundRect(
              color = primary,
              topLeft = Offset(pad, pad),
              size = Size(size.width - pad * 2, size.height - pad * 2),
              cornerRadius = CornerRadius(10.dp.toPx()),
              style = Stroke(width = 2.4.dp.toPx()),
            )
            val spots = listOf(
              Offset(pad + 10.dp.toPx(), pad + 10.dp.toPx()),
              Offset(size.width - pad - 10.dp.toPx(), pad + 10.dp.toPx()),
              Offset(pad + 10.dp.toPx(), size.height - pad - 10.dp.toPx()),
              Offset(size.width - pad - 10.dp.toPx(), size.height - pad - 10.dp.toPx()),
            )
            spots.forEach { c ->
              drawCircle(color = secondary, radius = 6.dp.toPx(), center = c)
              drawCircle(color = primary, radius = 2.6.dp.toPx(), center = c)
            }
          }
          "ephem_film_strip" -> {
            drawRoundRect(color = primary, cornerRadius = CornerRadius(4.dp.toPx()))
            val innerTop = 10.dp.toPx()
            val innerH = (size.height - innerTop * 2).coerceAtLeast(10f)
            drawRoundRect(
              color = secondary.copy(alpha = 0.34f),
              topLeft = Offset(10.dp.toPx(), innerTop),
              size = Size((size.width - 20.dp.toPx()).coerceAtLeast(10f), innerH),
              cornerRadius = CornerRadius(2.dp.toPx()),
            )
            var sx = 10.dp.toPx()
            while (sx < size.width - 10.dp.toPx()) {
              drawRect(color = secondary, topLeft = Offset(sx, 3.dp.toPx()), size = Size(6.dp.toPx(), 4.dp.toPx()))
              drawRect(color = secondary, topLeft = Offset(sx, size.height - 7.dp.toPx()), size = Size(6.dp.toPx(), 4.dp.toPx()))
              sx += 14.dp.toPx()
            }
          }
          "ephem_deckle_tab" -> {
            drawRoundRect(color = primary.copy(alpha = 0.72f), cornerRadius = CornerRadius(8.dp.toPx()))
            drawLine(color = secondary.copy(alpha = 0.75f), start = Offset(6.dp.toPx(), size.height * 0.25f), end = Offset(size.width - 6.dp.toPx(), size.height * 0.25f), strokeWidth = 1.6.dp.toPx())
            drawRoundRect(
              color = secondary,
              topLeft = Offset(pad, pad),
              size = Size(size.width - pad * 2, size.height - pad * 2),
              cornerRadius = CornerRadius(6.dp.toPx()),
              style = Stroke(width = 1.8.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))),
            )
          }
          else -> {
            drawRoundRect(
              color = primary,
              topLeft = Offset(pad, pad),
              size = Size(size.width - pad * 2, size.height - pad * 2),
              cornerRadius = CornerRadius(4.dp.toPx()),
              style = Stroke(width = 2.6.dp.toPx()),
            )
            val inner = pad * 2.2f
            drawRoundRect(
              color = primary.copy(alpha = 0.85f),
              topLeft = Offset(inner, inner),
              size = Size((size.width - inner * 2).coerceAtLeast(1f), (size.height - inner * 2).coerceAtLeast(1f)),
              cornerRadius = CornerRadius(2.dp.toPx()),
              style = Stroke(width = 1.2.dp.toPx()),
            )
          }
        }
      }
    }

    "label" -> {
      Surface(
        shape = RoundedCornerShape(7.dp),
        color = primary.copy(alpha = 0.96f),
        border = BorderStroke(1.4.dp, secondary.copy(alpha = 0.85f)),
        shadowElevation = 3.dp,
        modifier = modifier.fillMaxSize(),
      ) {
        Box(
          modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = labelText?.takeIf { it.isNotBlank() } ?: catalogAsset?.defaultTextEn ?: "Label",
            fontFamily = NotoSerifBengaliFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.5.sp,
            color = secondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
          )
        }
      }
    }

    else -> {
      // Crisp multi-layered vintage stamps, wax seals, florals, and heritage stickers
      Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val cx = size.width / 2f
          val cy = size.height / 2f
          val r = size.minDimension * 0.45f
          when (assetId) {
            "stamp_wax_sankalan" -> {
              // Shadow + scalloped organic wax lobes + raised rim + recessed center
              drawCircle(color = Color.Black.copy(alpha = 0.28f), radius = r * 1.04f, center = Offset(cx + 2.dp.toPx(), cy + 3.dp.toPx()))
              for (lobe in 0 until 12) {
                val rad = Math.toRadians(lobe * 30.0)
                drawCircle(
                  color = primary,
                  radius = r * 0.25f,
                  center = Offset(cx + (r * 0.82f * cos(rad)).toFloat(), cy + (r * 0.82f * sin(rad)).toFloat()),
                )
              }
              drawCircle(
                brush = Brush.radialGradient(
                  colors = listOf(primary.copy(alpha = 0.95f), primary, crispOutline.copy(alpha = 0.65f)),
                  center = Offset(cx - r * 0.2f, cy - r * 0.2f),
                  radius = r * 1.15f,
                ),
                radius = r * 0.92f,
                center = Offset(cx, cy),
              )
              drawCircle(
                color = secondary.copy(alpha = 0.88f),
                radius = r * 0.72f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.8.dp.toPx()),
              )
              drawCircle(
                color = crispOutline.copy(alpha = 0.4f),
                radius = r * 0.68f,
                center = Offset(cx, cy),
                style = Stroke(width = 0.8.dp.toPx()),
              )
            }
            "stamp_postage_dhaka" -> {
              drawRoundRect(
                color = secondary.copy(alpha = 0.85f),
                topLeft = Offset(cx - r * 1.05f, cy - r * 0.95f),
                size = Size(r * 2.1f, r * 1.9f),
                cornerRadius = CornerRadius(4.dp.toPx()),
              )
              val dash = PathEffect.dashPathEffect(floatArrayOf(7f, 4f), 0f)
              drawCircle(color = primary, radius = r * 0.86f, center = Offset(cx, cy), style = Stroke(width = 2.dp.toPx(), pathEffect = dash))
              drawCircle(color = primary, radius = r * 0.64f, center = Offset(cx, cy), style = Stroke(width = 1.4.dp.toPx()))
              for (lineIdx in -1..1) {
                val ly = cy + lineIdx * 4.dp.toPx()
                drawLine(color = primary.copy(alpha = 0.82f), start = Offset(cx - r * 1.02f, ly), end = Offset(cx + r * 1.02f, ly), strokeWidth = 1.2.dp.toPx())
              }
            }
            "stamp_coffee_ring" -> {
              drawCircle(color = primary.copy(alpha = 0.24f), radius = r * 0.90f, center = Offset(cx, cy))
              drawCircle(color = primary.copy(alpha = 0.78f), radius = r * 0.92f, center = Offset(cx, cy), style = Stroke(width = 4.dp.toPx()))
              drawCircle(color = primary.copy(alpha = 0.55f), radius = r * 0.83f, center = Offset(cx + 2.dp.toPx(), cy - 1.dp.toPx()), style = Stroke(width = 1.8.dp.toPx()))
              drawCircle(color = primary.copy(alpha = 0.65f), radius = 2.5.dp.toPx(), center = Offset(cx + r * 0.72f, cy + r * 0.55f))
              drawCircle(color = primary.copy(alpha = 0.50f), radius = 1.8.dp.toPx(), center = Offset(cx - r * 0.68f, cy - r * 0.58f))
            }
            "stamp_crescent_moon" -> {
              drawCircle(color = primary, radius = r * 0.94f, center = Offset(cx, cy))
              drawCircle(color = secondary, radius = r * 0.84f, center = Offset(cx, cy), style = Stroke(width = 1.2.dp.toPx()))
              drawCircle(color = secondary, radius = r * 0.46f, center = Offset(cx - r * 0.10f, cy))
              drawCircle(color = primary, radius = r * 0.37f, center = Offset(cx + r * 0.11f, cy - r * 0.06f))
              listOf(
                Offset(cx + r * 0.38f, cy - r * 0.34f) to 2.8.dp.toPx(),
                Offset(cx + r * 0.48f, cy + r * 0.15f) to 2.dp.toPx(),
                Offset(cx - r * 0.44f, cy - r * 0.42f) to 1.8.dp.toPx(),
              ).forEach { (pt, starR) ->
                drawCircle(color = secondary, radius = starR, center = pt)
              }
            }
            "stamp_pressed_leaf" -> {
              val leaf =
                Path().apply {
                  moveTo(cx, cy + r * 0.65f)
                  cubicTo(cx - r * 0.85f, cy + r * 0.25f, cx - r * 0.68f, cy - r * 0.62f, cx, cy - r * 0.88f)
                  cubicTo(cx + r * 0.68f, cy - r * 0.62f, cx + r * 0.85f, cy + r * 0.25f, cx, cy + r * 0.65f)
                  close()
                }
              drawPath(leaf, color = primary.copy(alpha = 0.75f))
              drawPath(leaf, color = crispOutline.copy(alpha = 0.65f), style = Stroke(width = 1.8.dp.toPx()))
              drawLine(color = secondary, start = Offset(cx, cy + r * 0.88f), end = Offset(cx, cy - r * 0.76f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
              for (v in -1..2) {
                val vy = cy + v * (r * 0.25f)
                drawLine(color = secondary.copy(alpha = 0.8f), start = Offset(cx, vy), end = Offset(cx - r * 0.34f, vy - r * 0.18f), strokeWidth = 1.2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color = secondary.copy(alpha = 0.8f), start = Offset(cx, vy), end = Offset(cx + r * 0.34f, vy - r * 0.18f), strokeWidth = 1.2.dp.toPx(), cap = StrokeCap.Round)
              }
            }
            "stamp_sunburst_badge" -> {
              for (a in 0 until 16) {
                val rad = Math.toRadians(a * 22.5)
                val endFrac = if (a % 2 == 0) 0.98f else 0.78f
                drawLine(
                  color = primary,
                  start = Offset(cx + (r * 0.46f * cos(rad)).toFloat(), cy + (r * 0.46f * sin(rad)).toFloat()),
                  end = Offset(cx + (r * endFrac * cos(rad)).toFloat(), cy + (r * endFrac * sin(rad)).toFloat()),
                  strokeWidth = 2.4.dp.toPx(),
                  cap = StrokeCap.Round,
                )
              }
              drawCircle(color = secondary, radius = r * 0.50f, center = Offset(cx, cy))
              drawCircle(color = primary, radius = r * 0.46f, center = Offset(cx, cy))
              drawCircle(color = secondary, radius = r * 0.34f, center = Offset(cx, cy), style = Stroke(width = 1.4.dp.toPx()))
            }
            "stamp_quill_emblem" -> {
              drawCircle(color = secondary.copy(alpha = 0.75f), radius = r * 0.90f, center = Offset(cx, cy))
              drawCircle(color = primary, radius = r * 0.90f, center = Offset(cx, cy), style = Stroke(width = 1.6.dp.toPx()))
              val feather =
                Path().apply {
                  moveTo(cx - r * 0.45f, cy + r * 0.65f)
                  lineTo(cx + r * 0.55f, cy - r * 0.62f)
                  cubicTo(cx + r * 0.25f, cy - r * 0.80f, cx - r * 0.42f, cy - r * 0.25f, cx - r * 0.32f, cy + r * 0.42f)
                  close()
                }
              drawPath(feather, color = primary.copy(alpha = 0.85f))
              drawPath(feather, color = crispOutline.copy(alpha = 0.6f), style = Stroke(width = 1.4.dp.toPx()))
              drawLine(color = secondary, start = Offset(cx - r * 0.48f, cy + r * 0.70f), end = Offset(cx + r * 0.50f, cy - r * 0.56f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
            }
            "stamp_verified_memory" -> {
              drawRoundRect(
                color = secondary.copy(alpha = 0.88f),
                topLeft = Offset(cx - r * 1.05f, cy - r * 0.62f),
                size = Size(r * 2.10f, r * 1.24f),
                cornerRadius = CornerRadius(6.dp.toPx()),
              )
              drawRoundRect(
                color = primary,
                topLeft = Offset(cx - r * 1.05f, cy - r * 0.62f),
                size = Size(r * 2.10f, r * 1.24f),
                cornerRadius = CornerRadius(6.dp.toPx()),
                style = Stroke(width = 2.4.dp.toPx()),
              )
              drawRoundRect(
                color = primary,
                topLeft = Offset(cx - r * 0.92f, cy - r * 0.49f),
                size = Size(r * 1.84f, r * 0.98f),
                cornerRadius = CornerRadius(4.dp.toPx()),
                style = Stroke(width = 1.dp.toPx()),
              )
            }
            "floral_shapla_lotus", "floral_peony_blush", "season_autumn_shiuli" -> {
              val petalCount = if (assetId == "season_autumn_shiuli") 6 else 8
              // Subtle backdrop halo so white/blush petals remain crisp on ivory parchment
              drawCircle(color = crispOutline.copy(alpha = 0.08f), radius = r * 0.92f, center = Offset(cx, cy))
              for (p in 0 until petalCount) {
                val deg = p * (360f / petalCount)
                rotate(degrees = deg, pivot = Offset(cx, cy)) {
                  drawOval(
                    color = primary,
                    topLeft = Offset(cx - r * 0.22f, cy - r * 0.92f),
                    size = Size(r * 0.44f, r * 0.66f),
                  )
                  drawOval(
                    color = crispOutline.copy(alpha = 0.45f),
                    topLeft = Offset(cx - r * 0.22f, cy - r * 0.92f),
                    size = Size(r * 0.44f, r * 0.66f),
                    style = Stroke(width = 1.2.dp.toPx()),
                  )
                }
              }
              drawCircle(color = secondary, radius = r * 0.30f, center = Offset(cx, cy))
              drawCircle(color = crispOutline.copy(alpha = 0.55f), radius = r * 0.30f, center = Offset(cx, cy), style = Stroke(width = 1.2.dp.toPx()))
              drawCircle(color = SankalanTokens.AntiqueGold, radius = r * 0.14f, center = Offset(cx, cy))
            }
            "floral_marigold_garland", "floral_gold_wreath" -> {
              drawCircle(color = secondary, radius = r * 0.80f, center = Offset(cx, cy), style = Stroke(width = 3.dp.toPx()))
              for (k in 0 until 12) {
                val rad = Math.toRadians(k * 30.0)
                val bx = cx + (r * 0.80f * cos(rad)).toFloat()
                val by = cy + (r * 0.80f * sin(rad)).toFloat()
                drawCircle(color = primary, radius = r * 0.22f, center = Offset(bx, by))
                drawCircle(color = crispOutline.copy(alpha = 0.4f), radius = r * 0.22f, center = Offset(bx, by), style = Stroke(width = 1.dp.toPx()))
                drawCircle(color = secondary, radius = r * 0.08f, center = Offset(bx, by))
              }
            }
            "floral_pressed_fern", "floral_bougainvillea", "season_spring_krishnochura" -> {
              drawLine(color = secondary, start = Offset(cx, cy + r * 0.88f), end = Offset(cx, cy - r * 0.82f), strokeWidth = 2.6.dp.toPx(), cap = StrokeCap.Round)
              for (step in -2..2) {
                val y = cy + step * (r * 0.29f)
                val lx = cx - r * 0.34f
                val rx = cx + r * 0.34f
                drawCircle(color = primary, radius = r * 0.24f, center = Offset(lx, y))
                drawCircle(color = crispOutline.copy(alpha = 0.45f), radius = r * 0.24f, center = Offset(lx, y), style = Stroke(width = 1.dp.toPx()))
                drawCircle(color = primary, radius = r * 0.24f, center = Offset(rx, y - r * 0.07f))
                drawCircle(color = crispOutline.copy(alpha = 0.45f), radius = r * 0.24f, center = Offset(rx, y - r * 0.07f), style = Stroke(width = 1.dp.toPx()))
              }
            }
            "season_monsoon_boat" -> {
              val boat =
                Path().apply {
                  moveTo(cx - r * 0.90f, cy + r * 0.16f)
                  lineTo(cx + r * 0.90f, cy + r * 0.16f)
                  lineTo(cx + r * 0.54f, cy + r * 0.66f)
                  lineTo(cx - r * 0.54f, cy + r * 0.66f)
                  close()
                }
              val sail =
                Path().apply {
                  moveTo(cx, cy - r * 0.72f)
                  lineTo(cx + r * 0.56f, cy + r * 0.14f)
                  lineTo(cx - r * 0.56f, cy + r * 0.14f)
                  close()
                }
              drawPath(boat, color = primary)
              drawPath(boat, color = crispOutline.copy(alpha = 0.65f), style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round))
              drawPath(sail, color = secondary)
              drawPath(sail, color = crispOutline.copy(alpha = 0.65f), style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round))
              drawLine(color = primary.copy(alpha = 0.75f), start = Offset(cx - r * 0.78f, cy + r * 0.80f), end = Offset(cx + r * 0.78f, cy + r * 0.80f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }
            "season_boishakh_alpona" -> {
              drawCircle(color = secondary, radius = r * 0.96f, center = Offset(cx, cy))
              drawCircle(color = primary, radius = r * 0.92f, center = Offset(cx, cy), style = Stroke(width = 2.6.dp.toPx()))
              for (p in 0 until 8) {
                val deg = p * 45f
                rotate(degrees = deg, pivot = Offset(cx, cy)) {
                  drawOval(
                    color = primary,
                    topLeft = Offset(cx - r * 0.13f, cy - r * 0.78f),
                    size = Size(r * 0.26f, r * 0.48f),
                  )
                }
              }
              drawCircle(color = secondary, radius = r * 0.30f, center = Offset(cx, cy))
              drawCircle(color = primary, radius = r * 0.24f, center = Offset(cx, cy))
            }
            "season_winter_tea", "season_golden_lantern" -> {
              drawCircle(color = secondary.copy(alpha = 0.7f), radius = r * 0.92f, center = Offset(cx, cy))
              drawRoundRect(
                color = primary,
                topLeft = Offset(cx - r * 0.52f, cy - r * 0.32f),
                size = Size(r * 1.04f, r * 0.86f),
                cornerRadius = CornerRadius(8.dp.toPx()),
              )
              drawRoundRect(
                color = crispOutline.copy(alpha = 0.6f),
                topLeft = Offset(cx - r * 0.52f, cy - r * 0.32f),
                size = Size(r * 1.04f, r * 0.86f),
                cornerRadius = CornerRadius(8.dp.toPx()),
                style = Stroke(width = 1.6.dp.toPx()),
              )
              drawCircle(color = secondary, radius = r * 0.22f, center = Offset(cx, cy + r * 0.08f))
              drawLine(color = primary, start = Offset(cx - r * 0.24f, cy - r * 0.50f), end = Offset(cx - r * 0.14f, cy - r * 0.84f), strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
              drawLine(color = primary, start = Offset(cx + r * 0.14f, cy - r * 0.50f), end = Offset(cx + r * 0.24f, cy - r * 0.84f), strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
            }
            else -> {
              drawCircle(color = primary.copy(alpha = 0.35f), radius = r, center = Offset(cx, cy))
              drawCircle(color = primary, radius = r, center = Offset(cx, cy), style = Stroke(width = 2.2.dp.toPx()))
            }
          }
        }
        val stampTitle = labelText?.takeIf { it.isNotBlank() } ?: catalogAsset?.defaultTextEn
        if (!stampTitle.isNullOrBlank()) {
          Text(
            text = stampTitle,
            fontFamily = NotoSerifBengaliFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = if (stampTitle.length <= 2) 17.sp else 9.5.sp,
            color = if (assetId == "stamp_wax_sankalan") secondary else primary,
            textAlign = TextAlign.Center,
            maxLines = 2,
          )
        }
      }
    }
  }
}

@Composable
fun VoiceMemoryElementCard(
  element: CanvasElement,
  isPlaying: Boolean,
  modifier: Modifier = Modifier,
) {
  val durationSec = element.fontSize?.toInt() ?: 5
  Surface(
    shape = RoundedCornerShape(24.dp),
    color = SankalanTokens.Walnut900.copy(alpha = 0.92f),
    border = BorderStroke(1.2.dp, SankalanTokens.AntiqueGold),
    shadowElevation = 3.dp,
    modifier = modifier.fillMaxSize(),
  ) {
    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Surface(
        shape = CircleShape,
        color = SankalanTokens.AntiqueGold,
        modifier = Modifier.size(28.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
            contentDescription = element.text ?: "Play voice note",
            tint = SankalanTokens.Walnut900,
            modifier = Modifier.size(18.dp),
          )
        }
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = element.text ?: "Voice Note",
          style = MaterialTheme.typography.labelSmall,
          color = SankalanTokens.Cream,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.GraphicEq,
            contentDescription = null,
            tint = SankalanTokens.AntiqueGold,
            modifier = Modifier.size(12.dp),
          )
          Text(
            text = formatDurationMmSs(durationSec),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = SankalanTokens.GoldHighlight,
          )
        }
      }
    }
  }
}
