package com.example.features.studio

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.core.i18n.LocalStrings
import com.example.core.model.CanvasElement
import com.example.core.model.Page
import com.example.core.model.PageBackground
import com.example.features.bookshelf.parseHexColor
import com.example.features.editor.VoiceNoteManager
import com.example.features.editor.formatDurationMmSs
import com.example.ui.theme.LocalSankalanPalette
import com.example.ui.theme.SankalanSpacing
import com.example.ui.theme.SankalanTokens
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaperSelectorPanel(
  currentBackground: PageBackground,
  isBangla: Boolean,
  favoriteIds: Set<String>,
  onToggleFavorite: (String) -> Unit,
  onApplyBackground: (PageBackground) -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current

  Column(verticalArrangement = Arrangement.spacedBy(SankalanSpacing.sm)) {
    // Edge Style Cut Chips
    Text(text = strings.paperEdgeStyleLabel, style = MaterialTheme.typography.labelLarge, color = palette.goldAccent)
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      listOf(
        "clean_cut" to strings.paperEdgeClean,
        "deckle" to strings.paperEdgeDeckle,
        "torn" to strings.paperEdgeTorn,
      ).forEach { (edgeId, label) ->
        FilterChip(
          selected = currentBackground.edgeStyle == edgeId,
          onClick = { onApplyBackground(currentBackground.copy(edgeStyle = edgeId)) },
          label = { Text(label) },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = palette.goldAccent,
              selectedLabelColor = SankalanTokens.Walnut900,
            ),
        )
      }
    }

    // Opacity slider
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        text = "${strings.editorOpacity}: ${(currentBackground.opacity * 100).toInt()}%",
        style = MaterialTheme.typography.labelSmall,
        color = palette.textSecondary,
      )
      Slider(
        value = currentBackground.opacity,
        onValueChange = { onApplyBackground(currentBackground.copy(opacity = it)) },
        valueRange = 0.65f..1.0f,
        colors = SliderDefaults.colors(thumbColor = palette.goldAccent, activeTrackColor = palette.goldAccent),
        modifier = Modifier.weight(1f),
      )
    }

    // 22 Paper Preset Swatches
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      PAPER_PRESETS.forEach { preset ->
        val selected = currentBackground.presetId == preset.id
        val isFav = preset.id in favoriteIds
        Surface(
          shape = MaterialTheme.shapes.small,
          color = palette.cardSurface,
          border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) palette.goldAccent else palette.borderSubtle),
          modifier =
            Modifier
              .width(126.dp)
              .clip(MaterialTheme.shapes.small)
              .clickable(role = Role.RadioButton) {
                onApplyBackground(
                  currentBackground.copy(
                    presetId = preset.id,
                    tintHex = preset.tintHex,
                    edgeStyle = preset.defaultEdge,
                  )
                )
              }
              .testTag("paper_preset_${preset.id}"),
        ) {
          Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier =
                  Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(parseHexColor(preset.tintHex))
                    .border(1.dp, palette.borderGold, CircleShape)
              )
              IconButton(
                onClick = { onToggleFavorite(preset.id) },
                modifier = Modifier.size(24.dp),
              ) {
                Icon(
                  imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                  contentDescription = strings.studioCatFavorites,
                  tint = if (isFav) palette.goldAccent else palette.textSecondary,
                  modifier = Modifier.size(16.dp),
                )
              }
            }
            Text(
              text = if (isBangla) preset.nameBn else preset.nameEn,
              style = MaterialTheme.typography.labelSmall,
              color = palette.textPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@Composable
fun TemplateSelectorPanel(
  isBangla: Boolean,
  favoriteIds: Set<String>,
  onToggleFavorite: (String) -> Unit,
  onSelectTemplate: (PageTemplate) -> Unit,
) {
  val palette = LocalSankalanPalette.current

  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
  ) {
    PAGE_TEMPLATES.forEach { tpl ->
      val isFav = tpl.id in favoriteIds
      Surface(
        shape = MaterialTheme.shapes.small,
        color = palette.cardSurface,
        border = BorderStroke(1.dp, palette.borderGold),
        modifier =
          Modifier
            .width(168.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button) { onSelectTemplate(tpl) }
            .testTag("template_card_${tpl.id}"),
      ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier =
                Modifier
                  .size(width = 32.dp, height = 18.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(parseHexColor(tpl.background.tintHex))
                  .border(1.dp, palette.borderGold, RoundedCornerShape(4.dp))
            )
            IconButton(
              onClick = { onToggleFavorite(tpl.id) },
              modifier = Modifier.size(24.dp),
            ) {
              Icon(
                imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = if (isFav) palette.goldAccent else palette.textSecondary,
                modifier = Modifier.size(16.dp),
              )
            }
          }
          Text(
            text = if (isBangla) tpl.nameBn else tpl.nameEn,
            style = MaterialTheme.typography.labelLarge,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = if (isBangla) tpl.subtitleBn else tpl.subtitleEn,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioAssetPackPanel(
  isBangla: Boolean,
  favoriteIds: Set<String>,
  userUploads: List<UserUploadedAsset>,
  onToggleFavorite: (String) -> Unit,
  onInsertAsset: (StudioAsset) -> Unit,
  onInsertUpload: (UserUploadedAsset) -> Unit,
  onPickUserUpload: () -> Unit,
  onEditUpload: (UserUploadedAsset) -> Unit = {},
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  var activeFilter by remember { mutableStateOf(StudioCategory.WASHI_TAPE) }

  Column(verticalArrangement = Arrangement.spacedBy(SankalanSpacing.sm)) {
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      listOf(
        StudioCategory.WASHI_TAPE to strings.studioCatTape,
        StudioCategory.FRAMES to strings.studioCatFrames,
        StudioCategory.LABELS to strings.studioCatLabels,
        StudioCategory.STAMPS to strings.studioCatStamps,
        StudioCategory.BOTANICAL_FLORALS to strings.studioCatFlorals,
        StudioCategory.EPHEMERA to strings.studioCatEphemera,
        StudioCategory.SEASONAL to strings.studioCatSeasonal,
        StudioCategory.MY_UPLOADS to strings.studioCatUploads,
        StudioCategory.FAVORITES to strings.studioCatFavorites,
      ).forEach { (cat, label) ->
        FilterChip(
          selected = activeFilter == cat,
          onClick = { activeFilter = cat },
          label = { Text(label, maxLines = 1) },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = palette.goldAccent,
              selectedLabelColor = SankalanTokens.Walnut900,
            ),
        )
      }
    }

    if (activeFilter == StudioCategory.MY_UPLOADS) {
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedButton(
          onClick = onPickUserUpload,
          border = BorderStroke(1.dp, palette.borderGold),
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(strings.studioUploadAssetButton)
        }

        userUploads.forEach { upload ->
          Surface(
            shape = MaterialTheme.shapes.small,
            color = palette.cardSurface,
            border = BorderStroke(1.dp, palette.borderSubtle),
            modifier =
              Modifier
                .width(96.dp)
                .clip(MaterialTheme.shapes.small)
                .clickable(role = Role.Button) { onInsertUpload(upload) },
          ) {
            Column(
              modifier = Modifier.padding(6.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(
                  onClick = { onEditUpload(upload) },
                  modifier = Modifier.size(24.dp),
                ) {
                  Icon(
                    Icons.Outlined.Edit,
                    contentDescription = strings.stickerEditExistingBtn,
                    tint = palette.goldAccent,
                    modifier = Modifier.size(14.dp),
                  )
                }
              }
              Box(modifier = Modifier.size(56.dp)) {
                StudioAssetGraphic(
                  assetId = upload.id,
                  elementType = "sticker",
                  labelText = upload.title,
                  blobRef = upload.filePath,
                )
              }
              Text(upload.title, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
        }
      }
    } else {
      val visibleAssets =
        remember(activeFilter, favoriteIds) {
          when (activeFilter) {
            StudioCategory.FAVORITES -> ALL_STUDIO_ASSETS.filter { it.id in favoriteIds }
            else -> ALL_STUDIO_ASSETS.filter { it.category == activeFilter }
          }
        }
      if (visibleAssets.isEmpty()) {
        Text(
          text = strings.studioFavoritesEmpty,
          style = MaterialTheme.typography.bodySmall,
          color = palette.textSecondary,
        )
      } else {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          visibleAssets.forEach { asset ->
            val isFav = asset.id in favoriteIds
            Surface(
              shape = MaterialTheme.shapes.small,
              color = palette.cardSurface,
              border = BorderStroke(1.dp, palette.borderSubtle),
              modifier =
                Modifier
                  .width(116.dp)
                  .clip(MaterialTheme.shapes.small)
                  .clickable(role = Role.Button) { onInsertAsset(asset) }
                  .testTag("editor_asset_${asset.id}"),
            ) {
              Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.End,
                ) {
                  IconButton(
                    onClick = { onToggleFavorite(asset.id) },
                    modifier = Modifier.size(22.dp),
                  ) {
                    Icon(
                      imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                      contentDescription = null,
                      tint = if (isFav) palette.goldAccent else palette.textSecondary,
                      modifier = Modifier.size(15.dp),
                    )
                  }
                }
                Box(modifier = Modifier.width(88.dp).height(44.dp)) {
                  StudioAssetGraphic(
                    assetId = asset.id,
                    elementType = asset.elementType,
                    labelText = if (isBangla) asset.defaultTextBn ?: asset.defaultTextEn else asset.defaultTextEn,
                  )
                }
                Text(
                  text = if (isBangla) asset.nameBn else asset.nameEn,
                  style = MaterialTheme.typography.labelSmall,
                  color = palette.textPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun VoiceRecorderPanel(
  voiceManager: VoiceNoteManager,
  existingVoiceElements: List<CanvasElement>,
  playingVoiceId: String?,
  onAttachVoiceNote: (filePath: String, durationSeconds: Int) -> Unit,
  onPlayVoiceElement: (CanvasElement) -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val context = LocalContext.current

  var hasPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  var isRecording by remember { mutableStateOf(false) }
  var elapsedSec by remember { mutableIntStateOf(0) }

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasPermission = granted
      if (granted && !isRecording) {
        if (voiceManager.startRecording()) {
          isRecording = true
          elapsedSec = 0
        }
      }
    }

  LaunchedEffect(isRecording) {
    while (isRecording) {
      delay(1000L)
      elapsedSec++
    }
  }

  Column(verticalArrangement = Arrangement.spacedBy(SankalanSpacing.sm)) {
    if (!hasPermission) {
      Surface(
        shape = MaterialTheme.shapes.small,
        color = palette.cardSurface,
        border = BorderStroke(1.dp, palette.borderGold),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(SankalanSpacing.sm),
          horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = strings.voicePermissionNeeded,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
            modifier = Modifier.weight(1f),
          )
          Button(
            onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            colors =
              ButtonDefaults.buttonColors(
                containerColor = palette.goldAccent,
                contentColor = SankalanTokens.Walnut900,
              ),
            shape = MaterialTheme.shapes.small,
          ) {
            Text(strings.voiceGrantPermission, style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    } else {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Button(
          onClick = {
            if (isRecording) {
              val saved = voiceManager.stopRecording(discard = false)
              isRecording = false
              elapsedSec = 0
              if (saved != null) {
                onAttachVoiceNote(saved.first, saved.second)
              }
            } else {
              if (voiceManager.startRecording()) {
                isRecording = true
                elapsedSec = 0
              }
            }
          },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = if (isRecording) SankalanTokens.Terracotta else palette.goldAccent,
              contentColor = if (isRecording) SankalanTokens.Cream else SankalanTokens.Walnut900,
            ),
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("editor_voice_record_button"),
        ) {
          Icon(
            imageVector = if (isRecording) Icons.Outlined.Stop else Icons.Outlined.Mic,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isRecording) "${strings.voiceRecordStop} (${formatDurationMmSs(elapsedSec)})" else strings.voiceRecordStart,
            style = MaterialTheme.typography.labelMedium,
          )
        }

        if (isRecording) {
          Text(
            text = strings.voiceRecordingStatus,
            style = MaterialTheme.typography.labelSmall,
            color = SankalanTokens.Terracotta,
          )
        }
      }
    }

    if (existingVoiceElements.isNotEmpty()) {
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        existingVoiceElements.forEach { el ->
          val playing = el.id == playingVoiceId
          OutlinedButton(
            onClick = { onPlayVoiceElement(el) },
            border = BorderStroke(1.dp, if (playing) palette.goldAccent else palette.borderSubtle),
            shape = MaterialTheme.shapes.small,
          ) {
            Icon(
              imageVector = if (playing) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
              contentDescription = null,
              tint = palette.goldAccent,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Outlined.GraphicEq, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${el.text ?: strings.voiceNoteLabel} (${formatDurationMmSs(el.fontSize?.toInt() ?: 1)})",
              style = MaterialTheme.typography.labelSmall,
            )
          }
        }
      }
    }
  }
}

@Composable
fun SafeTemplateConfirmDialog(
  templateName: String,
  onDismiss: () -> Unit,
  onReplace: () -> Unit,
  onOverlay: () -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = palette.cardSurface,
    titleContentColor = palette.textPrimary,
    textContentColor = palette.textSecondary,
    title = { Text("${strings.templateSafeTitle} ($templateName)") },
    text = { Text(strings.templateSafeBody) },
    confirmButton = {
      Button(
        onClick = onOverlay,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = palette.goldAccent,
            contentColor = SankalanTokens.Walnut900,
          ),
        modifier = Modifier.testTag("template_safe_overlay_btn"),
      ) {
        Text(strings.templateOverlayOption)
      }
    },
    dismissButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(onClick = onDismiss) { Text(strings.cancel, color = palette.textSecondary) }
        OutlinedButton(
          onClick = onReplace,
          border = BorderStroke(1.dp, palette.borderGold),
          modifier = Modifier.testTag("template_safe_replace_btn"),
        ) {
          Text(strings.templateReplaceOption, color = palette.goldAccent)
        }
      }
    },
  )
}
