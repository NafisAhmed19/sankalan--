package com.example.features.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatAlignLeft
import androidx.compose.material.icons.automirrored.outlined.FormatAlignRight
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FormatAlignCenter
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.GroupWork
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.i18n.LocalStrings
import com.example.core.model.CanvasElement
import com.example.core.model.Page
import com.example.features.bookshelf.parseHexColor
import com.example.ui.theme.LocalSankalanPalette
import com.example.ui.theme.SankalanSpacing
import com.example.ui.theme.SankalanTokens
import com.example.ui.theme.resolveEditorFontFamily
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextElementEditorDialog(
  initialElement: CanvasElement?,
  onDismiss: () -> Unit,
  onSaveText: (
    text: String,
    fontId: String,
    fontSize: Float,
    colorHex: String,
    align: String,
    bold: Boolean,
    italic: Boolean,
    underline: Boolean,
  ) -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current

  var text by remember { mutableStateOf(initialElement?.text.orEmpty()) }
  var fontId by remember { mutableStateOf(initialElement?.fontId ?: "noto_sans_bn") }
  var fontSize by remember { mutableFloatStateOf(initialElement?.fontSize ?: 15f) }
  var colorHex by remember { mutableStateOf(initialElement?.colorHex ?: "#302C29") }
  var align by remember { mutableStateOf(initialElement?.align ?: "left") }
  var bold by remember { mutableStateOf(initialElement?.bold ?: false) }
  var italic by remember { mutableStateOf(initialElement?.italic ?: false) }
  var underline by remember { mutableStateOf(initialElement?.underline ?: false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = palette.cardSurface,
    titleContentColor = palette.textPrimary,
    textContentColor = palette.textPrimary,
    title = { Text(strings.editorEditTextTitle) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
      ) {
        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          placeholder = { Text(strings.editorTextPlaceholder) },
          minLines = 3,
          maxLines = 6,
          modifier = Modifier.fillMaxWidth().testTag("editor_text_input"),
        )

        // Formatting Controls (Bold / Italic / Underline / Align)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            IconButton(onClick = { bold = !bold }) {
              Icon(
                Icons.Outlined.FormatBold,
                contentDescription = "Bold",
                tint = if (bold) palette.goldAccent else palette.textSecondary,
              )
            }
            IconButton(onClick = { italic = !italic }) {
              Icon(
                Icons.Outlined.FormatItalic,
                contentDescription = "Italic",
                tint = if (italic) palette.goldAccent else palette.textSecondary,
              )
            }
            IconButton(onClick = { underline = !underline }) {
              Icon(
                Icons.Outlined.FormatUnderlined,
                contentDescription = "Underline",
                tint = if (underline) palette.goldAccent else palette.textSecondary,
              )
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            IconButton(onClick = { align = "left" }) {
              Icon(
                Icons.AutoMirrored.Outlined.FormatAlignLeft,
                contentDescription = "Align Left",
                tint = if (align == "left") palette.goldAccent else palette.textSecondary,
              )
            }
            IconButton(onClick = { align = "center" }) {
              Icon(
                Icons.Outlined.FormatAlignCenter,
                contentDescription = "Align Center",
                tint = if (align == "center") palette.goldAccent else palette.textSecondary,
              )
            }
            IconButton(onClick = { align = "right" }) {
              Icon(
                Icons.AutoMirrored.Outlined.FormatAlignRight,
                contentDescription = "Align Right",
                tint = if (align == "right") palette.goldAccent else palette.textSecondary,
              )
            }
          }
        }

        // Size Slider
        Column {
          Text(
            text = "Size: ${fontSize.toInt()}sp",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary,
          )
          Slider(
            value = fontSize,
            onValueChange = { fontSize = it },
            valueRange = 11f..30f,
            colors =
              SliderDefaults.colors(
                thumbColor = palette.goldAccent,
                activeTrackColor = palette.goldAccent,
              ),
          )
        }

        // Ink Swatches
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          EDITOR_INK_SWATCHES.forEach { hex ->
            val selected = colorHex == hex
            Box(
              modifier =
                Modifier
                  .size(28.dp)
                  .clip(CircleShape)
                  .background(parseHexColor(hex))
                  .border(
                    width = if (selected) 2.5.dp else 1.dp,
                    color = if (selected) palette.goldHighlight else palette.borderSubtle,
                    shape = CircleShape,
                  )
                  .clickable(role = Role.RadioButton) { colorHex = hex }
            )
          }
        }

        // Font Selector (Bangla + English handwriting fonts)
        Text(
          text = strings.editorFontLabel,
          style = MaterialTheme.typography.labelLarge,
          color = palette.goldAccent,
        )
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.xs),
        ) {
          EDITOR_FONT_CHOICES.forEach { choice ->
            val selected = fontId == choice.id
            Surface(
              shape = MaterialTheme.shapes.small,
              color = if (selected) palette.goldAccent else palette.shelfSurface,
              border = BorderStroke(1.dp, if (selected) palette.goldAccent else palette.borderSubtle),
              modifier =
                Modifier
                  .clip(MaterialTheme.shapes.small)
                  .clickable(role = Role.RadioButton) { fontId = choice.id },
            ) {
              Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text(
                  text = choice.displayName,
                  style = MaterialTheme.typography.labelSmall,
                  color = if (selected) SankalanTokens.Walnut900 else palette.textPrimary,
                )
                Text(
                  text = choice.sample,
                  fontFamily = resolveEditorFontFamily(choice.id),
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                  fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                  textDecoration = if (underline) TextDecoration.Underline else TextDecoration.None,
                  color = if (selected) SankalanTokens.Walnut900 else palette.textSecondary,
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSaveText(
            text.trim(),
            fontId,
            fontSize,
            colorHex,
            align,
            bold,
            italic,
            underline,
          )
        },
        enabled = text.trim().isNotEmpty(),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = palette.goldAccent,
            contentColor = SankalanTokens.Walnut900,
          ),
        modifier = Modifier.testTag("editor_text_submit"),
      ) {
        Text(strings.save)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(strings.cancel, color = palette.textSecondary)
      }
    },
  )
}

@Composable
fun PhotoCropAndFrameDialog(
  photoFilePath: String,
  initialPayload: ImageFramePayload = ImageFramePayload(),
  onDismiss: () -> Unit,
  onConfirm: (ImageFramePayload) -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  var frameStyle by remember { mutableStateOf(PhotoFrameStyle.fromId(initialPayload.frameStyle)) }
  var cropZoom by remember { mutableFloatStateOf(initialPayload.cropZoom) }
  var cropOffsetX by remember { mutableFloatStateOf(initialPayload.cropOffsetX) }
  var cropOffsetY by remember { mutableFloatStateOf(initialPayload.cropOffsetY) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = palette.cardSurface,
    titleContentColor = palette.textPrimary,
    textContentColor = palette.textPrimary,
    title = { Text(strings.editorCropAndFrame) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Live Cropped Frame Preview
        val previewClip =
          when (frameStyle) {
            PhotoFrameStyle.TORN_PAPER -> TornPaperShape
            PhotoFrameStyle.ROUNDED -> RoundedCornerShape(14.dp)
            PhotoFrameStyle.OVAL -> CircleShape
            PhotoFrameStyle.POLAROID,
            PhotoFrameStyle.CLEAN -> RoundedCornerShape(4.dp)
          }

        Box(
          modifier =
            Modifier
              .size(168.dp)
              .clip(previewClip)
              .background(if (frameStyle == PhotoFrameStyle.POLAROID) SankalanTokens.Cream else SankalanTokens.PaperShadow)
              .padding(
                start = if (frameStyle == PhotoFrameStyle.POLAROID) 10.dp else 0.dp,
                end = if (frameStyle == PhotoFrameStyle.POLAROID) 10.dp else 0.dp,
                top = if (frameStyle == PhotoFrameStyle.POLAROID) 10.dp else 0.dp,
                bottom = if (frameStyle == PhotoFrameStyle.POLAROID) 24.dp else 0.dp,
              ),
        ) {
          AsyncImage(
            model = File(photoFilePath),
            contentDescription = "Photo preview",
            contentScale = ContentScale.Crop,
            modifier =
              Modifier
                .fillMaxSize()
                .graphicsLayer {
                  scaleX = cropZoom
                  scaleY = cropZoom
                  translationX = cropOffsetX * 80f
                  translationY = cropOffsetY * 80f
                },
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          listOf(
            PhotoFrameStyle.CLEAN to strings.editorFrameClean,
            PhotoFrameStyle.POLAROID to strings.editorFramePolaroid,
            PhotoFrameStyle.TORN_PAPER to strings.editorFrameTornPaper,
            PhotoFrameStyle.ROUNDED to strings.editorFrameRounded,
            PhotoFrameStyle.OVAL to strings.editorFrameOval,
          ).forEach { (style, label) ->
            FilterChip(
              selected = frameStyle == style,
              onClick = { frameStyle = style },
              label = { Text(label) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = palette.goldAccent,
                  selectedLabelColor = SankalanTokens.Walnut900,
                ),
            )
          }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "${strings.editorCropZoom}: ${(cropZoom * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary,
          )
          Slider(
            value = cropZoom,
            onValueChange = { cropZoom = it },
            valueRange = 1.0f..2.6f,
            colors = SliderDefaults.colors(thumbColor = palette.goldAccent, activeTrackColor = palette.goldAccent),
          )
          Slider(
            value = cropOffsetX,
            onValueChange = { cropOffsetX = it },
            valueRange = -0.8f..0.8f,
            colors = SliderDefaults.colors(thumbColor = palette.goldAccent, activeTrackColor = palette.goldAccent),
          )
          Slider(
            value = cropOffsetY,
            onValueChange = { cropOffsetY = it },
            valueRange = -0.8f..0.8f,
            colors = SliderDefaults.colors(thumbColor = palette.goldAccent, activeTrackColor = palette.goldAccent),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm(
            ImageFramePayload(
              frameStyle = frameStyle.id,
              cropZoom = cropZoom,
              cropOffsetX = cropOffsetX,
              cropOffsetY = cropOffsetY,
            )
          )
        },
        colors =
          ButtonDefaults.buttonColors(
            containerColor = palette.goldAccent,
            contentColor = SankalanTokens.Walnut900,
          ),
        modifier = Modifier.testTag("editor_photo_confirm"),
      ) {
        Text(strings.save)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.cancel, color = palette.textSecondary) }
    },
  )
}

@Composable
fun LayerManagerPanel(
  elements: List<CanvasElement>,
  selectedElementId: String?,
  onSelectElement: (String) -> Unit,
  onBringForward: (String) -> Unit,
  onSendBackward: (String) -> Unit,
  onToggleLock: (String) -> Unit,
  onToggleHidden: (String) -> Unit,
  onOpacityChange: (String, Float) -> Unit,
  onGroupElements: (List<String>) -> Unit,
  onUngroupElement: (String) -> Unit,
  onDeleteElement: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val orderedTopDown = remember(elements) { elements.sortedByDescending { it.zIndex } }
  var multiSelectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
  ) {
    // Group / Ungroup action bar
    if (multiSelectedIds.size >= 2 || orderedTopDown.any { it.id == selectedElementId && it.groupId != null }) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.xs),
      ) {
        if (multiSelectedIds.size >= 2) {
          OutlinedButton(
            onClick = {
              onGroupElements(multiSelectedIds.toList())
              multiSelectedIds = emptySet()
            },
            border = BorderStroke(1.dp, palette.borderGold),
            shape = MaterialTheme.shapes.small,
          ) {
            Icon(Icons.Outlined.GroupWork, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("${strings.editorGroupSelected} (${multiSelectedIds.size})")
          }
        }
        val selGroup = orderedTopDown.firstOrNull { it.id == selectedElementId }?.groupId
        if (selGroup != null) {
          OutlinedButton(
            onClick = { onUngroupElement(selGroup) },
            border = BorderStroke(1.dp, palette.borderSubtle),
            shape = MaterialTheme.shapes.small,
          ) {
            Text(strings.editorUngroup)
          }
        }
      }
    }

    if (orderedTopDown.isEmpty()) {
      Text(
        text = strings.editorLayersEmpty,
        style = MaterialTheme.typography.bodySmall,
        color = palette.textSecondary,
        modifier = Modifier.padding(SankalanSpacing.sm),
      )
    } else {
      orderedTopDown.forEachIndexed { idx, el ->
        val isSelected = el.id == selectedElementId
        val typeLabel =
          when (el.type) {
            "text" -> "${strings.editorLayerText}: ${el.text.orEmpty()}"
            "stroke" -> strings.editorLayerStroke
            "image" -> strings.editorLayerPhoto
            "shape" -> "${strings.editorLayerShape}: ${el.assetId.orEmpty()}"
            "tape" -> strings.studioCatTape
            "frame" -> strings.studioCatFrames
            "label" -> "${strings.studioCatLabels}: ${el.text.orEmpty()}"
            "sticker" -> strings.studioCatStamps
            "voice" -> "${strings.voiceNoteLabel}: ${el.text.orEmpty()}"
            else -> el.type
          }

        Surface(
          shape = MaterialTheme.shapes.small,
          color = if (isSelected) palette.shelfSurface else palette.background.copy(alpha = 0.7f),
          border = BorderStroke(1.dp, if (isSelected) palette.goldAccent else palette.borderSubtle),
          modifier =
            Modifier
              .fillMaxWidth()
              .clickable { onSelectElement(el.id) }
              .testTag("layer_row_${el.id}"),
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Checkbox(
                checked = el.id in multiSelectedIds,
                onCheckedChange = { checked ->
                  multiSelectedIds = if (checked) multiSelectedIds + el.id else multiSelectedIds - el.id
                },
                colors = CheckboxDefaults.colors(checkedColor = palette.goldAccent),
                modifier = Modifier.size(28.dp),
              )
              Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(
                  text = typeLabel,
                  style = MaterialTheme.typography.labelMedium,
                  color = palette.textPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                if (el.groupId != null) {
                  Text(
                    text = "Group ${el.groupId.take(4)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.goldAccent,
                  )
                }
              }

              IconButton(onClick = { onBringForward(el.id) }, enabled = idx > 0, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Outlined.ArrowUpward, contentDescription = strings.editorBringForward, modifier = Modifier.size(16.dp))
              }
              IconButton(
                onClick = { onSendBackward(el.id) },
                enabled = idx < orderedTopDown.lastIndex,
                modifier = Modifier.size(30.dp),
              ) {
                Icon(Icons.Outlined.ArrowDownward, contentDescription = strings.editorSendBackward, modifier = Modifier.size(16.dp))
              }
              IconButton(onClick = { onToggleLock(el.id) }, modifier = Modifier.size(30.dp)) {
                Icon(
                  imageVector = if (el.locked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                  contentDescription = strings.editorLockLayer,
                  tint = if (el.locked) palette.goldAccent else palette.textSecondary,
                  modifier = Modifier.size(16.dp),
                )
              }
              IconButton(onClick = { onToggleHidden(el.id) }, modifier = Modifier.size(30.dp)) {
                Icon(
                  imageVector = if (el.hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                  contentDescription = strings.editorHideLayer,
                  tint = if (el.hidden) palette.textSecondary else palette.textPrimary,
                  modifier = Modifier.size(16.dp),
                )
              }
              IconButton(onClick = { onDeleteElement(el.id) }, modifier = Modifier.size(30.dp)) {
                Icon(
                  imageVector = Icons.Outlined.DeleteOutline,
                  contentDescription = strings.delete,
                  tint = if (palette.isDark) SankalanTokens.ErrorDark else SankalanTokens.ErrorLight,
                  modifier = Modifier.size(16.dp),
                )
              }
            }

            if (isSelected) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = "${strings.editorOpacity} ${(el.opacity * 100).toInt()}%",
                  style = MaterialTheme.typography.labelSmall,
                  color = palette.textSecondary,
                )
                Slider(
                  value = el.opacity,
                  onValueChange = { onOpacityChange(el.id, it) },
                  valueRange = 0.15f..1.0f,
                  colors = SliderDefaults.colors(thumbColor = palette.goldAccent, activeTrackColor = palette.goldAccent),
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PageInfoPanel(
  page: Page,
  onUpdateMetadata: (title: String?, dateIso: String?, mood: String?, pinned: Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val moods = listOf("Peaceful", "Joyful", "Reflective", "Grateful", "Focused", "Nostalgic")

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
  ) {
    OutlinedTextField(
      value = page.title.orEmpty(),
      onValueChange = { onUpdateMetadata(it, page.dateIso, page.mood, page.pinned) },
      label = { Text(strings.editorPageTitleHint) },
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("editor_page_title_input"),
    )

    OutlinedTextField(
      value = page.dateIso.orEmpty(),
      onValueChange = { onUpdateMetadata(page.title, it, page.mood, page.pinned) },
      label = { Text(strings.editorDateLabel) },
      placeholder = { Text("2026-11-13") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(text = strings.pinPageLabel, style = MaterialTheme.typography.labelLarge, color = palette.textPrimary)
      Switch(
        checked = page.pinned,
        onCheckedChange = { onUpdateMetadata(page.title, page.dateIso, page.mood, it) },
        colors =
          SwitchDefaults.colors(
            checkedThumbColor = SankalanTokens.Walnut900,
            checkedTrackColor = palette.goldAccent,
          ),
      )
    }

    Text(text = strings.editorMoodLabel, style = MaterialTheme.typography.labelLarge, color = palette.goldAccent)
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      moods.forEach { mood ->
        val selected = page.mood == mood
        FilterChip(
          selected = selected,
          onClick = { onUpdateMetadata(page.title, page.dateIso, if (selected) null else mood, page.pinned) },
          label = { Text(mood) },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = palette.goldAccent,
              selectedLabelColor = SankalanTokens.Walnut900,
            ),
        )
      }
    }
  }
}
