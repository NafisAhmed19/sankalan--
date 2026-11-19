package com.example.features.studio

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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.graphics.Bitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.data.DiaryRepository
import com.example.core.i18n.LocalStrings
import com.example.core.i18n.formatNumber
import com.example.core.model.AppLanguage
import com.example.core.model.CanvasElement
import com.example.core.model.DiaryWithPages
import com.example.core.model.NumeralStyle
import com.example.core.model.Page
import com.example.core.model.PageBackground
import com.example.features.bookshelf.parseHexColor
import com.example.ui.components.SankalanAtmosphericBackdrop
import com.example.ui.theme.LocalSankalanPalette
import com.example.ui.theme.SankalanSpacing
import com.example.ui.theme.SankalanTokens
import java.util.UUID

sealed interface StudioApplyAction {
  data class ApplyPaper(val preset: PaperPreset) : StudioApplyAction
  data class ApplyTemplate(val template: PageTemplate) : StudioApplyAction
  data class InsertPackAsset(val asset: StudioAsset) : StudioApplyAction
  data class InsertUpload(val upload: UserUploadedAsset) : StudioApplyAction
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioScreen(
  language: AppLanguage,
  numeralStyle: NumeralStyle,
  activeDiaries: List<DiaryWithPages>,
  onApplyToDiaryPage: (diaryId: String, updatedPage: Page) -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val context = LocalContext.current
  val isBangla = language == AppLanguage.BN_BD

  val store = remember(context) { StudioUserStore(context) }
  var favoriteIds by remember { mutableStateOf(store.loadFavorites()) }
  var userUploads by remember { mutableStateOf(store.loadUploads()) }

  var searchQuery by rememberSaveable { mutableStateOf("") }
  var selectedCategory by rememberSaveable { mutableStateOf(StudioCategory.ALL) }
  var pendingApplyAction by remember { mutableStateOf<StudioApplyAction?>(null) }
  var pendingSafeTemplateTarget by remember { mutableStateOf<Triple<String, Page, PageTemplate>?>(null) }
  var pendingStickerEditor by remember { mutableStateOf<Pair<Bitmap, UserUploadedAsset?>?>(null) }

  val pickAssetLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        val decoded = StickerCutoutEngine.decodeUriWithExifCorrection(context, uri, maxDim = 960)
        if (decoded != null) {
          pendingStickerEditor = decoded to null
        }
      }
    }

  val normQuery = DiaryRepository.normalizeForSearch(searchQuery)

  val visiblePapers =
    remember(selectedCategory, normQuery, favoriteIds) {
      if (selectedCategory != StudioCategory.ALL &&
        selectedCategory != StudioCategory.PAPERS &&
        selectedCategory != StudioCategory.FAVORITES
      ) {
        emptyList()
      } else {
        PAPER_PRESETS.filter { paper ->
          val favOk = selectedCategory != StudioCategory.FAVORITES || paper.id in favoriteIds
          val qOk =
            normQuery.isEmpty() ||
              DiaryRepository.normalizeForSearch(paper.nameEn).contains(normQuery) ||
              DiaryRepository.normalizeForSearch(paper.nameBn).contains(normQuery)
          favOk && qOk
        }
      }
    }

  val visibleTemplates =
    remember(selectedCategory, normQuery, favoriteIds) {
      if (selectedCategory != StudioCategory.ALL &&
        selectedCategory != StudioCategory.TEMPLATES &&
        selectedCategory != StudioCategory.FAVORITES
      ) {
        emptyList()
      } else {
        PAGE_TEMPLATES.filter { tpl ->
          val favOk = selectedCategory != StudioCategory.FAVORITES || tpl.id in favoriteIds
          val qOk =
            normQuery.isEmpty() ||
              DiaryRepository.normalizeForSearch(tpl.nameEn).contains(normQuery) ||
              DiaryRepository.normalizeForSearch(tpl.nameBn).contains(normQuery)
          favOk && qOk
        }
      }
    }

  val visibleAssets =
    remember(selectedCategory, normQuery, favoriteIds) {
      val packCategories = setOf(
        StudioCategory.ALL,
        StudioCategory.WASHI_TAPE,
        StudioCategory.FRAMES,
        StudioCategory.LABELS,
        StudioCategory.STAMPS,
        StudioCategory.BOTANICAL_FLORALS,
        StudioCategory.EPHEMERA,
        StudioCategory.SEASONAL,
        StudioCategory.FAVORITES,
      )
      if (selectedCategory !in packCategories) {
        emptyList()
      } else {
        ALL_STUDIO_ASSETS.filter { asset ->
          val catOk =
            when (selectedCategory) {
              StudioCategory.ALL -> true
              StudioCategory.FAVORITES -> asset.id in favoriteIds
              else -> asset.category == selectedCategory
            }
          val qOk =
            normQuery.isEmpty() ||
              DiaryRepository.normalizeForSearch(asset.nameEn).contains(normQuery) ||
              DiaryRepository.normalizeForSearch(asset.nameBn).contains(normQuery)
          catOk && qOk
        }
      }
    }

  val visibleUploads =
    remember(selectedCategory, normQuery, userUploads) {
      if (selectedCategory != StudioCategory.ALL && selectedCategory != StudioCategory.MY_UPLOADS) {
        emptyList()
      } else {
        userUploads.filter { u ->
          normQuery.isEmpty() || DiaryRepository.normalizeForSearch(u.title).contains(normQuery)
        }
      }
    }

  SankalanAtmosphericBackdrop(modifier = modifier) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
      val cols = if (maxWidth >= 700.dp) 3 else 2

      LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 1080.dp),
        contentPadding = PaddingValues(horizontal = SankalanSpacing.lg, vertical = SankalanSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(SankalanSpacing.md),
      ) {
        // Header + Zero-permission User Sticker Upload CTA
        item {
          Column(verticalArrangement = Arrangement.spacedBy(SankalanSpacing.sm)) {
            Text(
              text = strings.studioPreviewTitle,
              style = MaterialTheme.typography.headlineMedium,
              color = palette.textPrimary,
            )
            Text(
              text = strings.studioPreviewBody,
              style = MaterialTheme.typography.bodyMedium,
              color = palette.textSecondary,
            )
            OutlinedButton(
              onClick = {
                pickAssetLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
              },
              border = BorderStroke(1.dp, palette.borderGold),
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("studio_upload_button"),
            ) {
              Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = palette.goldAccent, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(strings.studioUploadAssetButton, color = palette.textPrimary)
            }
          }
        }

        // Search
        item {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(strings.studioSearchHint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = palette.goldAccent) },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { searchQuery = "" }) {
                  Icon(Icons.Outlined.Close, contentDescription = strings.closeLabel)
                }
              }
            },
            singleLine = true,
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = palette.goldAccent,
                unfocusedBorderColor = palette.borderSubtle,
                focusedTextColor = palette.textPrimary,
                unfocusedTextColor = palette.textPrimary,
              ),
            modifier = Modifier.fillMaxWidth().testTag("studio_search_input"),
          )
        }

        // Category Filter Chips
        item {
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(SankalanSpacing.xs),
          ) {
            val chipColors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = palette.goldAccent,
                selectedLabelColor = SankalanTokens.Walnut900,
                containerColor = palette.shelfSurface,
                labelColor = palette.textPrimary,
              )
            listOf(
              StudioCategory.ALL to strings.studioCatAll,
              StudioCategory.PAPERS to strings.studioCatPapers,
              StudioCategory.TEMPLATES to strings.studioCatTemplates,
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
                selected = selectedCategory == cat,
                onClick = { selectedCategory = cat },
                label = { Text(label, maxLines = 1) },
                colors = chipColors,
                modifier = Modifier.testTag("studio_cat_${cat.name.lowercase()}"),
              )
            }
          }
        }

        // Templates Section
        if (visibleTemplates.isNotEmpty()) {
          item {
            Text(text = strings.studioCatTemplates, style = MaterialTheme.typography.titleMedium, color = palette.goldAccent)
          }
          items(visibleTemplates.chunked(cols), key = { "tpl_${it.first().id}" }) { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm)) {
              row.forEach { tpl ->
                val isFav = tpl.id in favoriteIds
                Surface(
                  shape = MaterialTheme.shapes.medium,
                  color = palette.cardSurface,
                  border = BorderStroke(1.dp, palette.borderGold),
                  modifier =
                    Modifier
                      .weight(1f)
                      .clip(MaterialTheme.shapes.medium)
                      .clickable(role = Role.Button) { pendingApplyAction = StudioApplyAction.ApplyTemplate(tpl) },
                ) {
                  Column(modifier = Modifier.padding(SankalanSpacing.sm), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                      Box(
                        modifier =
                          Modifier
                            .size(width = 36.dp, height = 20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(parseHexColor(tpl.background.tintHex))
                            .border(1.dp, palette.borderGold, RoundedCornerShape(4.dp))
                      )
                      IconButton(
                        onClick = { favoriteIds = store.toggleFavorite(tpl.id) },
                        modifier = Modifier.size(28.dp),
                      ) {
                        Icon(
                          imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                          contentDescription = null,
                          tint = if (isFav) palette.goldAccent else palette.textSecondary,
                          modifier = Modifier.size(18.dp),
                        )
                      }
                    }
                    Text(
                      text = if (isBangla) tpl.nameBn else tpl.nameEn,
                      style = MaterialTheme.typography.titleSmall,
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
              repeat(cols - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
          }
        }

        // Papers Section (22 Presets)
        if (visiblePapers.isNotEmpty()) {
          item {
            Text(text = strings.studioCatPapers, style = MaterialTheme.typography.titleMedium, color = palette.goldAccent)
          }
          items(visiblePapers.chunked(cols), key = { "paper_${it.first().id}" }) { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm)) {
              row.forEach { preset ->
                val isFav = preset.id in favoriteIds
                Surface(
                  shape = MaterialTheme.shapes.small,
                  color = palette.cardSurface,
                  border = BorderStroke(1.dp, palette.borderSubtle),
                  modifier =
                    Modifier
                      .weight(1f)
                      .clip(MaterialTheme.shapes.small)
                      .clickable(role = Role.Button) { pendingApplyAction = StudioApplyAction.ApplyPaper(preset) },
                ) {
                  Row(
                    modifier = Modifier.padding(SankalanSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Box(
                      modifier =
                        Modifier
                          .size(28.dp)
                          .clip(CircleShape)
                          .background(parseHexColor(preset.tintHex))
                          .border(1.dp, palette.borderGold, CircleShape)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = if (isBangla) preset.nameBn else preset.nameEn,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      Text(
                        text = preset.defaultEdge.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        maxLines = 1,
                      )
                    }
                    IconButton(
                      onClick = { favoriteIds = store.toggleFavorite(preset.id) },
                      modifier = Modifier.size(28.dp),
                    ) {
                      Icon(
                        imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        tint = if (isFav) palette.goldAccent else palette.textSecondary,
                        modifier = Modifier.size(16.dp),
                      )
                    }
                  }
                }
              }
              repeat(cols - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
          }
        }

        // Asset Pack 1 (Tapes, Frames, Labels, Stamps)
        if (visibleAssets.isNotEmpty()) {
          item {
            Text(text = strings.editorToolStudioAssets, style = MaterialTheme.typography.titleMedium, color = palette.goldAccent)
          }
          items(visibleAssets.chunked(cols), key = { "asset_${it.first().id}" }) { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm)) {
              row.forEach { asset ->
                val isFav = asset.id in favoriteIds
                Surface(
                  shape = MaterialTheme.shapes.small,
                  color = palette.cardSurface,
                  border = BorderStroke(1.dp, palette.borderSubtle),
                  modifier =
                    Modifier
                      .weight(1f)
                      .clip(MaterialTheme.shapes.small)
                      .clickable(role = Role.Button) { pendingApplyAction = StudioApplyAction.InsertPackAsset(asset) },
                ) {
                  Column(
                    modifier = Modifier.padding(SankalanSpacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                      IconButton(onClick = { favoriteIds = store.toggleFavorite(asset.id) }, modifier = Modifier.size(24.dp)) {
                        Icon(
                          imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                          contentDescription = null,
                          tint = if (isFav) palette.goldAccent else palette.textSecondary,
                          modifier = Modifier.size(16.dp),
                        )
                      }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
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
              repeat(cols - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
          }
        }

        // My Uploads
        if (selectedCategory == StudioCategory.MY_UPLOADS || visibleUploads.isNotEmpty()) {
          item {
            Text(text = strings.studioCatUploads, style = MaterialTheme.typography.titleMedium, color = palette.goldAccent)
          }
          if (visibleUploads.isEmpty()) {
            item {
              Text(text = strings.studioUploadsEmpty, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            }
          } else {
            items(visibleUploads.chunked(cols), key = { "upload_${it.first().id}" }) { row ->
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm)) {
                row.forEach { upload ->
                  Surface(
                    shape = MaterialTheme.shapes.small,
                    color = palette.cardSurface,
                    border = BorderStroke(1.dp, palette.borderSubtle),
                    modifier =
                      Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.small)
                        .clickable(role = Role.Button) { pendingApplyAction = StudioApplyAction.InsertUpload(upload) },
                  ) {
                    Column(
                      modifier = Modifier.padding(SankalanSpacing.sm),
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                      ) {
                        IconButton(
                          onClick = {
                            val src = store.loadSourceBitmapForUpload(upload)
                            if (src != null) pendingStickerEditor = src to upload
                          },
                          modifier = Modifier.size(28.dp).testTag("studio_edit_upload_${upload.id}"),
                        ) {
                          Icon(
                            Icons.Outlined.Edit,
                            contentDescription = strings.stickerEditExistingBtn,
                            tint = palette.goldAccent,
                            modifier = Modifier.size(16.dp),
                          )
                        }
                        IconButton(
                          onClick = { userUploads = store.deleteUpload(upload.id) },
                          modifier = Modifier.size(28.dp),
                        ) {
                          Icon(Icons.Outlined.DeleteOutline, contentDescription = strings.delete, modifier = Modifier.size(16.dp))
                        }
                      }
                      Box(modifier = Modifier.size(64.dp)) {
                        StudioAssetGraphic(assetId = upload.id, elementType = "sticker", blobRef = upload.filePath)
                      }
                      Text(upload.title, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                  }
                }
                repeat(cols - row.size) { Spacer(modifier = Modifier.weight(1f)) }
              }
            }
          }
        }

        if (selectedCategory == StudioCategory.FAVORITES &&
          visiblePapers.isEmpty() &&
          visibleTemplates.isEmpty() &&
          visibleAssets.isEmpty()
        ) {
          item {
            Surface(
              shape = MaterialTheme.shapes.medium,
              color = palette.cardSurface,
              border = BorderStroke(1.dp, palette.borderSubtle),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = strings.studioFavoritesEmpty,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
                modifier = Modifier.padding(SankalanSpacing.lg),
              )
            }
          }
        }
      }
    }
  }

  // Choose Target Diary & Page Dialog
  if (pendingApplyAction != null && activeDiaries.isNotEmpty()) {
    val action = pendingApplyAction!!
    PickDiaryAndPageDialog(
      diaries = activeDiaries,
      numeralStyle = numeralStyle,
      onDismiss = { pendingApplyAction = null },
      onSelectTarget = { diaryId, page ->
        when (action) {
          is StudioApplyAction.ApplyPaper -> {
            val nextPage =
              page.copy(
                background =
                  page.background.copy(
                    presetId = action.preset.id,
                    tintHex = action.preset.tintHex,
                    edgeStyle = action.preset.defaultEdge,
                  )
              )
            onApplyToDiaryPage(diaryId, nextPage)
            pendingApplyAction = null
          }
          is StudioApplyAction.ApplyTemplate -> {
            if (page.elements.isNotEmpty()) {
              pendingSafeTemplateTarget = Triple(diaryId, page, action.template)
              pendingApplyAction = null
            } else {
              onApplyToDiaryPage(diaryId, applyTemplateToPage(page, action.template, replaceExisting = true, isBangla = isBangla))
              pendingApplyAction = null
            }
          }
          is StudioApplyAction.InsertPackAsset -> {
            val nextZ = (page.elements.maxOfOrNull { it.zIndex } ?: 0) + 1
            val newEl =
              CanvasElement(
                id = UUID.randomUUID().toString(),
                type = action.asset.elementType,
                x = 0.25f,
                y = 0.28f,
                w = action.asset.defaultW,
                h = action.asset.defaultH,
                zIndex = nextZ,
                assetId = action.asset.id,
                text = if (isBangla) action.asset.defaultTextBn ?: action.asset.defaultTextEn else action.asset.defaultTextEn,
                colorHex = action.asset.colorHex,
              )
            onApplyToDiaryPage(diaryId, page.copy(elements = page.elements + newEl))
            pendingApplyAction = null
          }
          is StudioApplyAction.InsertUpload -> {
            val nextZ = (page.elements.maxOfOrNull { it.zIndex } ?: 0) + 1
            val newEl =
              CanvasElement(
                id = UUID.randomUUID().toString(),
                type = "sticker",
                x = 0.28f,
                y = 0.30f,
                w = 0.34f,
                h = 0.24f,
                zIndex = nextZ,
                assetId = action.upload.id,
                blobRef = action.upload.filePath,
                text = action.upload.title,
              )
            onApplyToDiaryPage(diaryId, page.copy(elements = page.elements + newEl))
            pendingApplyAction = null
          }
        }
      },
    )
  }

  if (pendingSafeTemplateTarget != null) {
    val (diaryId, page, tpl) = pendingSafeTemplateTarget!!
    SafeTemplateConfirmDialog(
      templateName = if (isBangla) tpl.nameBn else tpl.nameEn,
      onDismiss = { pendingSafeTemplateTarget = null },
      onReplace = {
        onApplyToDiaryPage(diaryId, applyTemplateToPage(page, tpl, replaceExisting = true, isBangla = isBangla))
        pendingSafeTemplateTarget = null
      },
      onOverlay = {
        onApplyToDiaryPage(diaryId, applyTemplateToPage(page, tpl, replaceExisting = false, isBangla = isBangla))
        pendingSafeTemplateTarget = null
      },
    )
  }

  if (pendingStickerEditor != null) {
    val (sourceBmp, existingUpload) = pendingStickerEditor!!
    StickerCutoutEditorDialog(
      sourceBitmap = sourceBmp,
      initialTitle = existingUpload?.title ?: "My Sticker",
      onDismiss = {
        pendingStickerEditor = null
      },
      onSaveSticker = { finalStickerBmp, title ->
        val saved =
          store.saveEditedSticker(
            stickerBitmap = finalStickerBmp,
            sourceBitmap = sourceBmp,
            title = title,
            existingAssetId = existingUpload?.id,
          )
        if (finalStickerBmp !== sourceBmp && !finalStickerBmp.isRecycled) {
          finalStickerBmp.recycle()
        }
        pendingStickerEditor = null
        if (saved != null) {
          userUploads = store.loadUploads()
          selectedCategory = StudioCategory.MY_UPLOADS
        }
      },
    )
  }
}

@Composable
private fun PickDiaryAndPageDialog(
  diaries: List<DiaryWithPages>,
  numeralStyle: NumeralStyle,
  onDismiss: () -> Unit,
  onSelectTarget: (diaryId: String, page: Page) -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  var selectedDiaryId by remember { mutableStateOf(diaries.first().diary.id) }
  val selectedDiary = diaries.firstOrNull { it.diary.id == selectedDiaryId } ?: diaries.first()
  var selectedPageId by remember(selectedDiary.diary.id) {
    mutableStateOf(selectedDiary.pages.firstOrNull()?.id.orEmpty())
  }
  val selectedPage = selectedDiary.pages.firstOrNull { it.id == selectedPageId } ?: selectedDiary.pages.firstOrNull()

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = palette.cardSurface,
    titleContentColor = palette.textPrimary,
    textContentColor = palette.textPrimary,
    title = { Text(strings.studioPickTargetPage) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(SankalanSpacing.sm)) {
        Column(
          modifier = Modifier.fillMaxWidth().heightIn(max = 176.dp).verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          diaries.forEach { bundle ->
            val active = bundle.diary.id == selectedDiary.diary.id
            Surface(
              shape = MaterialTheme.shapes.small,
              color = if (active) palette.goldAccent.copy(alpha = 0.22f) else palette.shelfSurface,
              border = BorderStroke(1.dp, if (active) palette.goldAccent else palette.borderSubtle),
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clip(MaterialTheme.shapes.small)
                  .clickable(role = Role.RadioButton) { selectedDiaryId = bundle.diary.id },
            ) {
              Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoStories, contentDescription = null, tint = palette.goldAccent, modifier = Modifier.size(18.dp))
                Text(bundle.diary.title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
              }
            }
          }
        }

        if (selectedDiary.pages.isNotEmpty()) {
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            selectedDiary.pages.forEachIndexed { idx, p ->
              val isChosen = p.id == selectedPage?.id
              FilterChip(
                selected = isChosen,
                onClick = { selectedPageId = p.id },
                label = { Text("${strings.pageNumberPrefix} ${formatNumber(idx + 1, numeralStyle)}") },
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
    },
    confirmButton = {
      Button(
        onClick = {
          if (selectedPage != null) onSelectTarget(selectedDiary.diary.id, selectedPage)
        },
        enabled = selectedPage != null,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = palette.goldAccent,
            contentColor = SankalanTokens.Walnut900,
          ),
      ) {
        Text(strings.studioUseInDiary)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.cancel, color = palette.textSecondary) }
    },
  )
}
