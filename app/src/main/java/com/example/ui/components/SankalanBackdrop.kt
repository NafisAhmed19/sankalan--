package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.i18n.LocalStrings
import com.example.core.model.SaveStatus
import com.example.ui.theme.LocalSankalanPalette
import com.example.ui.theme.SankalanTokens

/**
 * Rich vintage walnut study desk & warm brass lamp backdrop applied across primary screens:
 * - Photorealistic dark walnut woodgrain (or sunlit honey-oak in light mode)
 * - Calibrated atmospheric scrim preserving crisp text legibility
 * - Warm top-center antique brass desk lamp pool + soft peripheral edge vignette
 * - Delicate bookplate double hairline & corner Alpona diamond rosettes
 */
@Composable
fun SankalanAtmosphericBackdrop(
  modifier: Modifier = Modifier,
  showOrnamentFrame: Boolean = true,
  content: @Composable BoxScope.() -> Unit,
) {
  val palette = LocalSankalanPalette.current
  val backdropRes =
    if (palette.isDark) R.drawable.img_walnut_study_dark else R.drawable.img_oak_study_light

  Box(modifier = modifier.fillMaxSize().background(palette.background)) {
    // 1. Realistic woodgrain surface at high clarity so natural grain, knots & planks remain clearly visible
    Image(
      painter = painterResource(id = backdropRes),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      alpha = if (palette.isDark) 0.94f else 0.90f,
      modifier = Modifier.fillMaxSize(),
    )

    // 2. Calibrated light atmospheric scrim, warm lamp beam, plank joints & vintage bookplate details
    Canvas(modifier = Modifier.fillMaxSize()) {
      // Very light tint scrim preserving full woodgrain richness while keeping text crisp
      drawRect(
        color = palette.background.copy(alpha = if (palette.isDark) 0.14f else 0.18f)
      )

      // Subtle architectural vertical plank joints & fine wood highlight
      val seamShadow =
        if (palette.isDark) Color.Black.copy(alpha = 0.20f)
        else SankalanTokens.Walnut700.copy(alpha = 0.06f)
      val seamHighlight =
        if (palette.isDark) SankalanTokens.AntiqueGold.copy(alpha = 0.055f)
        else Color.White.copy(alpha = 0.18f)
      val plankStep = 108.dp.toPx()
      var px = plankStep * 0.6f
      while (px < size.width) {
        drawLine(
          color = seamShadow,
          start = Offset(px, 0f),
          end = Offset(px, size.height),
          strokeWidth = 1.dp.toPx(),
        )
        drawLine(
          color = seamHighlight,
          start = Offset(px + 1.2.dp.toPx(), 0f),
          end = Offset(px + 1.2.dp.toPx(), size.height),
          strokeWidth = 0.7.dp.toPx(),
        )
        px += plankStep
      }

      // Warm brass study lamp radial light pool from upper-center + soft diagonal shimmer
      drawRect(
        brush =
          Brush.radialGradient(
            colors =
              if (palette.isDark) {
                listOf(
                  SankalanTokens.GoldHighlight.copy(alpha = 0.18f),
                  SankalanTokens.AntiqueGold.copy(alpha = 0.09f),
                  Color.Transparent,
                )
              } else {
                listOf(
                  Color(0xFFFFFAEE).copy(alpha = 0.40f),
                  SankalanTokens.GoldHighlight.copy(alpha = 0.14f),
                  Color.Transparent,
                )
              },
            center = Offset(size.width * 0.5f, size.height * 0.05f),
            radius = size.maxDimension * 0.62f,
          )
      )

      // Restrained peripheral edge vignette
      drawRect(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                Color.Transparent,
                if (palette.isDark) Color.Black.copy(alpha = 0.28f)
                else SankalanTokens.Walnut800.copy(alpha = 0.09f),
              ),
            center = Offset(size.width * 0.5f, size.height * 0.45f),
            radius = size.maxDimension * 0.88f,
          )
      )

      if (showOrnamentFrame) {
        val filigreeColor =
          palette.goldAccent.copy(alpha = if (palette.isDark) 0.34f else 0.38f)
        val innerLineColor =
          palette.goldAccent.copy(alpha = if (palette.isDark) 0.16f else 0.20f)
        val watermarkColor =
          palette.goldAccent.copy(alpha = if (palette.isDark) 0.09f else 0.12f)

        // Subtle editorial Alpona mandala watermark arcs behind the upper desk area
        val mandalaCenter = Offset(size.width * 0.5f, size.height * 0.17f)
        val outerR = size.minDimension * 0.34f
        drawCircle(
          color = watermarkColor,
          radius = outerR,
          center = mandalaCenter,
          style =
            Stroke(
              width = 0.9.dp.toPx(),
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 10f), 0f),
            ),
        )
        drawCircle(
          color = watermarkColor.copy(alpha = watermarkColor.alpha * 0.85f),
          radius = outerR * 0.78f,
          center = mandalaCenter,
          style = Stroke(width = 0.7.dp.toPx()),
        )

        val margin = 9.dp.toPx()
        val innerMargin = 14.dp.toPx()
        val arm = 26.dp.toPx()
        val strokeW = 1.2.dp.toPx()

        // Delicate full-perimeter inner dashed bookplate line
        drawRect(
          color = innerLineColor,
          topLeft = Offset(innerMargin, innerMargin),
          size = Size(
            (size.width - innerMargin * 2).coerceAtLeast(0f),
            (size.height - innerMargin * 2).coerceAtLeast(0f),
          ),
          style =
            Stroke(
              width = 0.75.dp.toPx(),
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f),
            ),
        )

        // Crafted 4-corner L-filigree, inner arc & brass stud dot
        val arcRadius = 10.dp.toPx()
        val rivetOffset = 5.dp.toPx()
        val rivetRadius = 1.8.dp.toPx()

        // Top-left
        drawLine(filigreeColor, Offset(margin, margin), Offset(margin + arm, margin), strokeW)
        drawLine(filigreeColor, Offset(margin, margin), Offset(margin, margin + arm), strokeW)
        drawCircle(filigreeColor, rivetRadius, Offset(margin + rivetOffset, margin + rivetOffset))
        drawArc(
          color = filigreeColor.copy(alpha = filigreeColor.alpha * 0.85f),
          startAngle = 0f,
          sweepAngle = 90f,
          useCenter = false,
          topLeft = Offset(margin - arcRadius, margin - arcRadius),
          size = Size(arcRadius * 2, arcRadius * 2),
          style = Stroke(width = 0.9.dp.toPx()),
        )

        // Top-right
        drawLine(
          filigreeColor,
          Offset(size.width - margin, margin),
          Offset(size.width - margin - arm, margin),
          strokeW,
        )
        drawLine(
          filigreeColor,
          Offset(size.width - margin, margin),
          Offset(size.width - margin, margin + arm),
          strokeW,
        )
        drawCircle(
          filigreeColor,
          rivetRadius,
          Offset(size.width - margin - rivetOffset, margin + rivetOffset),
        )
        drawArc(
          color = filigreeColor.copy(alpha = filigreeColor.alpha * 0.85f),
          startAngle = 90f,
          sweepAngle = 90f,
          useCenter = false,
          topLeft = Offset(size.width - margin - arcRadius, margin - arcRadius),
          size = Size(arcRadius * 2, arcRadius * 2),
          style = Stroke(width = 0.9.dp.toPx()),
        )

        // Bottom-left
        drawLine(
          filigreeColor,
          Offset(margin, size.height - margin),
          Offset(margin + arm, size.height - margin),
          strokeW,
        )
        drawLine(
          filigreeColor,
          Offset(margin, size.height - margin),
          Offset(margin, size.height - margin - arm),
          strokeW,
        )
        drawCircle(
          filigreeColor,
          rivetRadius,
          Offset(margin + rivetOffset, size.height - margin - rivetOffset),
        )
        drawArc(
          color = filigreeColor.copy(alpha = filigreeColor.alpha * 0.85f),
          startAngle = 270f,
          sweepAngle = 90f,
          useCenter = false,
          topLeft = Offset(margin - arcRadius, size.height - margin - arcRadius),
          size = Size(arcRadius * 2, arcRadius * 2),
          style = Stroke(width = 0.9.dp.toPx()),
        )

        // Bottom-right
        drawLine(
          filigreeColor,
          Offset(size.width - margin, size.height - margin),
          Offset(size.width - margin - arm, size.height - margin),
          strokeW,
        )
        drawLine(
          filigreeColor,
          Offset(size.width - margin, size.height - margin),
          Offset(size.width - margin, size.height - margin - arm),
          strokeW,
        )
        drawCircle(
          filigreeColor,
          rivetRadius,
          Offset(size.width - margin - rivetOffset, size.height - margin - rivetOffset),
        )
        drawArc(
          color = filigreeColor.copy(alpha = filigreeColor.alpha * 0.85f),
          startAngle = 180f,
          sweepAngle = 90f,
          useCenter = false,
          topLeft = Offset(size.width - margin - arcRadius, size.height - margin - arcRadius),
          size = Size(arcRadius * 2, arcRadius * 2),
          style = Stroke(width = 0.9.dp.toPx()),
        )
      }
    }
    content()
  }
}

@Composable
fun SaveStatusPill(
  status: SaveStatus,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (status is SaveStatus.Idle) return
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current

  val (label, icon, tint) =
    when (status) {
      SaveStatus.Saving -> Triple(strings.saving, Icons.Outlined.Sync, palette.goldAccent)
      SaveStatus.Saved -> Triple(strings.saved, Icons.Outlined.CloudDone, palette.sage)
      is SaveStatus.Failed ->
        Triple(
          "${strings.saveFailed} · ${strings.retry}",
          Icons.Outlined.ErrorOutline,
          if (palette.isDark) SankalanTokens.ErrorDark else SankalanTokens.ErrorLight,
        )
      SaveStatus.Idle -> return
    }

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = palette.shelfSurface.copy(alpha = 0.92f),
    border = BorderStroke(1.dp, tint.copy(alpha = 0.55f)),
    modifier =
      modifier.let {
        if (status is SaveStatus.Failed) it.clickable(role = Role.Button, onClick = onRetry) else it
      },
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = tint,
        modifier = Modifier.size(14.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = palette.textPrimary,
      )
    }
  }
}
