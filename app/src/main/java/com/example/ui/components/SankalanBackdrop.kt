package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.core.i18n.LocalStrings
import com.example.core.model.SaveStatus
import com.example.ui.theme.LocalSankalanPalette
import com.example.ui.theme.SankalanTokens

/**
 * Subtle, low-noise vintage library backdrop applied across primary screens:
 * - Warm radial library lamp glow from top
 * - Ultra-light 3-4% woodgrain / laid-paper vertical ribs
 * - Restrained antique-gold bookplate corner filigree
 */
@Composable
fun SankalanAtmosphericBackdrop(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  val palette = LocalSankalanPalette.current

  Box(modifier = modifier.fillMaxSize()) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      // Base surface fill
      drawRect(color = palette.background)

      // Soft warm library lamp ambient radial glow at top-center
      drawRect(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                if (palette.isDark) SankalanTokens.AntiqueGold.copy(alpha = 0.08f)
                else SankalanTokens.PaperShadow.copy(alpha = 0.28f),
                Color.Transparent,
              ),
            center = Offset(size.width * 0.5f, size.height * 0.08f),
            radius = size.maxDimension * 0.65f,
          )
      )

      // Ultra-light vertical woodgrain / laid paper texture lines (restrained, low-noise)
      val stripeColor =
        if (palette.isDark) SankalanTokens.Cream.copy(alpha = 0.025f)
        else SankalanTokens.Walnut700.copy(alpha = 0.03f)
      val step = 28.dp.toPx()
      var x = step * 0.5f
      var index = 0
      while (x < size.width) {
        val alphaFactor = if (index % 3 == 0) 1.3f else 0.7f
        drawLine(
          color = stripeColor.copy(alpha = stripeColor.alpha * alphaFactor),
          start = Offset(x, 0f),
          end = Offset(x, size.height),
          strokeWidth = 1.dp.toPx(),
        )
        x += step
        index++
      }

      // Delicate antique-gold corner bookplate filigree
      val filigreeColor = palette.goldAccent.copy(alpha = if (palette.isDark) 0.16f else 0.20f)
      val margin = 12.dp.toPx()
      val arm = 22.dp.toPx()
      val stroke = Stroke(width = 1.dp.toPx())

      // Top-left L-filigree
      drawLine(filigreeColor, Offset(margin, margin), Offset(margin + arm, margin), stroke.width)
      drawLine(filigreeColor, Offset(margin, margin), Offset(margin, margin + arm), stroke.width)
      // Top-right L-filigree
      drawLine(
        filigreeColor,
        Offset(size.width - margin, margin),
        Offset(size.width - margin - arm, margin),
        stroke.width,
      )
      drawLine(
        filigreeColor,
        Offset(size.width - margin, margin),
        Offset(size.width - margin, margin + arm),
        stroke.width,
      )
      // Bottom-left L-filigree
      drawLine(
        filigreeColor,
        Offset(margin, size.height - margin),
        Offset(margin + arm, size.height - margin),
        stroke.width,
      )
      drawLine(
        filigreeColor,
        Offset(margin, size.height - margin),
        Offset(margin, size.height - margin - arm),
        stroke.width,
      )
      // Bottom-right L-filigree
      drawLine(
        filigreeColor,
        Offset(size.width - margin, size.height - margin),
        Offset(size.width - margin - arm, size.height - margin),
        stroke.width,
      )
      drawLine(
        filigreeColor,
        Offset(size.width - margin, size.height - margin),
        Offset(size.width - margin, size.height - margin - arm),
        stroke.width,
      )
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
