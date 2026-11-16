package com.example.features.bookshelf

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.core.i18n.LocalStrings
import com.example.core.i18n.formatNumber
import com.example.core.model.CoverStyle
import com.example.core.model.Diary
import com.example.core.model.DiaryCover
import com.example.core.model.DiaryInvariants
import com.example.core.model.NumeralStyle
import com.example.core.model.SpineStyle
import com.example.ui.theme.LocalSankalanPalette
import com.example.ui.theme.SankalanSpacing
import com.example.ui.theme.SankalanTokens

private data class CoverSwatch(val hex: String, val accentHex: String)

private val COVER_SWATCHES =
  listOf(
    CoverSwatch("#593B2B", "#B08A4F"), // Classic Leather + Antique Gold
    CoverSwatch("#38271F", "#D4B477"), // Deep Walnut + Gold Highlight
    CoverSwatch("#5C2828", "#D4B477"), // Oxblood Burgundy
    CoverSwatch("#4E5E48", "#F4EBDD"), // Botanical Sage
    CoverSwatch("#8C4D38", "#F4EBDD"), // Warm Terracotta
    CoverSwatch("#27323F", "#B08A4F"), // Midnight Indigo
    CoverSwatch("#C9B79C", "#302C29"), // Minimal Linen
  )

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateJournalSheet(
  numeralStyle: NumeralStyle,
  onDismiss: () -> Unit,
  onCreate: (title: String, description: String?, cover: DiaryCover, pageCount: Int) -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var coverStyle by remember { mutableStateOf(CoverStyle.LUXURY) }
  var selectedSwatch by remember { mutableStateOf(COVER_SWATCHES.first()) }
  var spineStyle by remember { mutableStateOf(SpineStyle.RIBBED_GOLD) }
  var initialPages by remember { mutableIntStateOf(DiaryInvariants.MIN_PAGES) }

  val previewDiary =
    remember(title, description, coverStyle, selectedSwatch, spineStyle) {
      Diary(
        id = "preview",
        title = title.ifBlank { strings.brandName },
        description = description.takeIf { it.isNotBlank() },
        cover =
          DiaryCover(
            style = coverStyle,
            colorHex = selectedSwatch.hex,
            accentHex = selectedSwatch.accentHex,
            spineStyle = spineStyle,
            monogram = "ৎ",
          ),
        pageIds = emptyList(),
      )
    }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = palette.cardSurface,
    contentColor = palette.textPrimary,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = SankalanSpacing.lg, vertical = SankalanSpacing.md),
      verticalArrangement = Arrangement.spacedBy(SankalanSpacing.md),
    ) {
      Text(
        text = strings.createJournal,
        style = MaterialTheme.typography.headlineMedium,
        color = palette.textPrimary,
      )

      // Live Cover Preview
      Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        BookCoverArt(
          diary = previewDiary,
          pageCount = initialPages,
          numeralStyle = numeralStyle,
          isBack = false,
          modifier = Modifier.width(144.dp).aspectRatio(0.72f),
        )
      }

      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text(strings.diaryTitleHint) },
        singleLine = true,
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedBorderColor = palette.goldAccent,
            unfocusedBorderColor = palette.borderSubtle,
            focusedLabelColor = palette.goldAccent,
            focusedTextColor = palette.textPrimary,
            unfocusedTextColor = palette.textPrimary,
          ),
        modifier = Modifier.fillMaxWidth().testTag("create_diary_title_input"),
      )

      OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text(strings.diaryDescHint) },
        singleLine = true,
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedBorderColor = palette.goldAccent,
            unfocusedBorderColor = palette.borderSubtle,
            focusedLabelColor = palette.goldAccent,
            focusedTextColor = palette.textPrimary,
            unfocusedTextColor = palette.textPrimary,
          ),
        modifier = Modifier.fillMaxWidth(),
      )

      // Cover style (Luxury, Artistic, Minimal)
      Text(text = strings.coverStyleLabel, style = MaterialTheme.typography.labelLarge)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
      ) {
        listOf(
          CoverStyle.LUXURY to strings.coverLuxuryLabel,
          CoverStyle.ARTISTIC to strings.coverArtisticLabel,
          CoverStyle.MINIMAL to strings.coverMinimalLabel,
        ).forEach { (style, label) ->
          val selected = coverStyle == style
          Surface(
            shape = MaterialTheme.shapes.small,
            color = if (selected) palette.goldAccent else palette.shelfSurface,
            border = BorderStroke(1.dp, if (selected) palette.goldAccent else palette.borderSubtle),
            modifier =
              Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .clip(MaterialTheme.shapes.small)
                .clickable(role = Role.RadioButton) { coverStyle = style },
          ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
              Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) SankalanTokens.Walnut900 else palette.textPrimary,
              )
            }
          }
        }
      }

      // Binding colors
      Text(text = strings.coverColorLabel, style = MaterialTheme.typography.labelLarge)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.md),
        verticalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
      ) {
        COVER_SWATCHES.forEach { swatch ->
          val selected = selectedSwatch == swatch
          Box(
            modifier =
              Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(parseHexColor(swatch.hex))
                .border(
                  width = if (selected) 3.dp else 1.dp,
                  color = if (selected) palette.goldHighlight else palette.borderGold,
                  shape = CircleShape,
                )
                .clickable(role = Role.RadioButton) { selectedSwatch = swatch }
          )
        }
      }

      // Spine craft
      Text(text = strings.spineStyleLabel, style = MaterialTheme.typography.labelLarge)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
      ) {
        listOf(
          SpineStyle.RIBBED_GOLD to strings.spineRibbedGold,
          SpineStyle.SMOOTH_LEATHER to strings.spineSmoothLeather,
          SpineStyle.STITCHED_CLOTH to strings.spineStitchedCloth,
        ).forEach { (style, label) ->
          val selected = spineStyle == style
          Surface(
            shape = MaterialTheme.shapes.small,
            color = if (selected) palette.goldAccent else palette.shelfSurface,
            border = BorderStroke(1.dp, if (selected) palette.goldAccent else palette.borderSubtle),
            modifier =
              Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .clip(MaterialTheme.shapes.small)
                .clickable(role = Role.RadioButton) { spineStyle = style },
          ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp)) {
              Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) SankalanTokens.Walnut900 else palette.textPrimary,
                maxLines = 1,
              )
            }
          }
        }
      }

      // Starting pages (5..200 rule enforced)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(text = strings.initialPagesLabel, style = MaterialTheme.typography.labelLarge)
          Text(
            text = strings.initialPagesRuleHint,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          IconButton(
            onClick = { if (initialPages > DiaryInvariants.MIN_PAGES) initialPages-- },
            enabled = initialPages > DiaryInvariants.MIN_PAGES,
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(Icons.Outlined.Remove, contentDescription = null)
          }
          Text(
            text = formatNumber(initialPages, numeralStyle),
            style = MaterialTheme.typography.titleMedium,
            color = palette.goldAccent,
          )
          IconButton(
            onClick = { if (initialPages < 25) initialPages++ },
            enabled = initialPages < 25,
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
          }
        }
      }

      Button(
        onClick = {
          onCreate(
            title.trim(),
            description.trim().takeIf { it.isNotEmpty() },
            DiaryCover(
              style = coverStyle,
              colorHex = selectedSwatch.hex,
              accentHex = selectedSwatch.accentHex,
              spineStyle = spineStyle,
              monogram = "ৎ",
            ),
            initialPages,
          )
        },
        enabled = title.trim().isNotEmpty(),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = palette.goldAccent,
            contentColor = SankalanTokens.Walnut900,
          ),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("create_diary_submit"),
      ) {
        Text(text = strings.createButtonLabel, style = MaterialTheme.typography.labelLarge)
      }

      Spacer(modifier = Modifier.height(SankalanSpacing.lg))
    }
  }
}
