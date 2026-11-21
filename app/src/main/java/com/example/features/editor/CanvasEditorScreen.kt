package com.example.features.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatAlignLeft
import androidx.compose.material.icons.automirrored.outlined.FormatAlignRight
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.BorderColor
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatAlignCenter
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.LocalStrings
import com.example.core.i18n.formatNumber
import com.example.core.model.AppLanguage
import com.example.core.model.CanvasElement
import com.example.core.model.Diary
import com.example.core.model.NumeralStyle
import com.example.core.model.Page
import com.example.core.model.SaveStatus
import com.example.features.bookshelf.parseHexColor
import com.example.features.studio.PageTemplate
import com.example.features.studio.PaperSelectorPanel
import com.example.features.studio.SafeTemplateConfirmDialog
import com.example.features.studio.StickerCutoutEditorDialog
import com.example.features.studio.StickerCutoutEngine
import com.example.features.studio.StudioAssetPackPanel
import com.example.features.studio.StudioUserStore
import com.example.features.studio.TemplateSelectorPanel
import com.example.features.studio.UserUploadedAsset
import com.example.features.studio.VoiceRecorderPanel
import com.example.features.studio.applyTemplateToPage
import com.example.features.studio.findPaperPreset
import com.example.ui.components.SankalanAtmosphericBackdrop
import com.example.ui.components.SaveStatusPill
import com.example.ui.theme.LocalSankalanPalette
import com.example.ui.theme.SankalanSpacing
import com.example.ui.theme.SankalanTokens
import com.example.ui.theme.resolveEditorFontFamily
import java.util.UUID
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun CanvasEditorScreen(
  diary: Diary,
  initialPage: Page,
  pageNumber: Int,
  language: AppLanguage,
  numeralStyle: NumeralStyle,
  saveStatus: SaveStatus,
  onRetrySave: () -> Unit,
  onPersistPage: (Page) -> Unit,
  onDismiss: () -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val context = LocalContext.current
  val isBangla = language == AppLanguage.BN_BD

  val studioStore = remember(context) { StudioUserStore(context) }
  var favoriteAssetIds by remember { mutableStateOf(studioStore.loadFavorites()) }
  var userUploads by remember { mutableStateOf(studioStore.loadUploads()) }

  val voiceManager = remember(context) { VoiceNoteManager(context) }
  var playingVoiceElementId by remember { mutableStateOf<String?>(null) }
  DisposableEffect(voiceManager) { onDispose { voiceManager.release() } }

  val history = remember(initialPage.id) { UndoRedoHistory(initialPage) }
  var workingPage by remember(initialPage.id) { mutableStateOf(initialPage) }
  var historyTick by remember { mutableIntStateOf(0) } // triggers recomposition for canUndo/canRedo

  var toolMode by remember { mutableStateOf(EditorToolMode.SELECT) }
  var isInspectorCollapsed by remember { mutableStateOf(false) }
  var selectedElementId by remember { mutableStateOf<String?>(null) }
  var inlineEditingElementId by remember { mutableStateOf<String?>(null) }

  // Word-style default text formatting (applies when creating new inline text boxes or modifying selected text)
  var defaultTextFontId by remember { mutableStateOf(if (isBangla) "noto_sans_bn" else "inter") }
  var defaultTextSize by remember { mutableFloatStateOf(15f) }
  var defaultTextBold by remember { mutableStateOf(false) }
  var defaultTextItalic by remember { mutableStateOf(false) }
  var defaultTextUnderline by remember { mutableStateOf(false) }
  var defaultTextAlign by remember { mutableStateOf("left") }
  var defaultTextColorHex by remember {
    mutableStateOf(
      if (findPaperPreset(initialPage.background.presetId).isDarkPaper) "#F6EFE2" else "#302C29"
    )
  }

  // Drawing / Stylus state
  var activePen by remember { mutableStateOf(PenKind.PEN) }
  var penColorHex by remember {
    mutableStateOf(
      if (findPaperPreset(initialPage.background.presetId).isDarkPaper) "#F6EFE2" else "#302C29"
    )
  }
  var strokeSizeRatio by remember { mutableFloatStateOf(0.009f) }
  var stylusOnlyPalmRejection by remember { mutableStateOf(false) }
  var activeStroke by remember { mutableStateOf<StrokePayload?>(null) }

  // GPU-only live gesture transform & canvas size
  var liveTransform by remember { mutableStateOf<LiveElementTransform?>(null) }
  var canvasPxSize by remember { mutableStateOf(IntSize(1, 1)) }

  // Dialog states
  var pendingPhotoForCrop by remember { mutableStateOf<Pair<String, CanvasElement?>?>(null) }
  var pendingSafeTemplate by remember { mutableStateOf<PageTemplate?>(null) }
  var pendingStickerEditor by remember { mutableStateOf<Pair<android.graphics.Bitmap, UserUploadedAsset?>?>(null) }

  fun commitAndPersist(nextPage: Page) {
    history.commit(nextPage)
    workingPage = nextPage
    historyTick++
    onPersistPage(nextPage)
  }

  fun commitInlineEditingIfActive() {
    val activeId = inlineEditingElementId ?: return
    val activeEl = workingPage.elements.firstOrNull { it.id == activeId }
    val cleanedPage =
      if (activeEl != null && activeEl.text.isNullOrBlank()) {
        workingPage.copy(elements = workingPage.elements.filterNot { it.id == activeId })
      } else {
        workingPage
      }
    inlineEditingElementId = null
    if (cleanedPage.elements.none { it.id == selectedElementId }) {
      selectedElementId = null
    }
    commitAndPersist(cleanedPage)
  }

  fun startInlineTextOnElement(el: CanvasElement) {
    if (inlineEditingElementId != null && inlineEditingElementId != el.id) {
      commitInlineEditingIfActive()
    }
    selectedElementId = if (isPageBodyElement(el)) null else el.id
    inlineEditingElementId = el.id
  }

  fun focusOrCreatePageBodyInline() {
    val existingBody = workingPage.elements.firstOrNull { isPageBodyElement(it) && !it.locked }
    if (existingBody != null) {
      if (inlineEditingElementId != null && inlineEditingElementId != existingBody.id) {
        commitInlineEditingIfActive()
      }
      selectedElementId = null
      inlineEditingElementId = existingBody.id
      toolMode = EditorToolMode.TEXT
      return
    }
    commitInlineEditingIfActive()
    val hasHeader =
      !workingPage.dateIso.isNullOrBlank() ||
        !workingPage.mood.isNullOrBlank() ||
        !workingPage.title.isNullOrBlank() ||
        workingPage.pinned
    val startY = if (hasHeader) 0.165f else 0.06f
    val newEl =
      CanvasElement(
        id = UUID.randomUUID().toString(),
        type = "text",
        assetId = PAGE_BODY_ASSET_ID,
        x = 0.065f,
        y = startY,
        w = 0.87f,
        h = (0.94f - startY).coerceAtLeast(0.55f),
        text = "",
        fontId = defaultTextFontId,
        fontSize = defaultTextSize,
        colorHex = defaultTextColorHex,
        align = defaultTextAlign,
        bold = defaultTextBold,
        italic = defaultTextItalic,
        underline = defaultTextUnderline,
        zIndex = 0,
      )
    workingPage = workingPage.copy(elements = workingPage.elements + newEl)
    selectedElementId = null
    inlineEditingElementId = newEl.id
    toolMode = EditorToolMode.TEXT
  }

  fun createDiagramTextBox() {
    commitInlineEditingIfActive()
    val newEl =
      CanvasElement(
        id = UUID.randomUUID().toString(),
        type = "text",
        assetId = DIAGRAM_BOX_ASSET_ID,
        x = 0.22f,
        y = 0.36f,
        w = 0.48f,
        h = 0.16f,
        text = "",
        fontId = defaultTextFontId,
        fontSize = defaultTextSize,
        colorHex = defaultTextColorHex,
        align = "center",
        bold = defaultTextBold,
        italic = defaultTextItalic,
        underline = defaultTextUnderline,
        zIndex = nextZIndex(workingPage),
      )
    workingPage = workingPage.copy(elements = workingPage.elements + newEl)
    selectedElementId = newEl.id
    inlineEditingElementId = newEl.id
    toolMode = EditorToolMode.TEXT
  }

  fun performUndo() {
    commitInlineEditingIfActive()
    val prev = history.undo() ?: return
    workingPage = prev
    selectedElementId = null
    inlineEditingElementId = null
    historyTick++
    onPersistPage(prev)
  }

  fun performRedo() {
    commitInlineEditingIfActive()
    val next = history.redo() ?: return
    workingPage = next
    selectedElementId = null
    inlineEditingElementId = null
    historyTick++
    onPersistPage(next)
  }

  // Zero-permission Photo Picker (Play Policy compliant)
  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        val localPath = copyPickedPhotoToInternalMedia(context, uri)
        if (localPath != null) {
          pendingPhotoForCrop = localPath to null
        }
      }
    }

  val pickUserAssetLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        val decoded = StickerCutoutEngine.decodeUriWithExifCorrection(context, uri, maxDim = 960)
        if (decoded != null) {
          pendingStickerEditor = decoded to null
        }
      }
    }

  fun closeEditorCleanly() {
    commitInlineEditingIfActive()
    voiceManager.release()
    playingVoiceElementId = null
    onPersistPage(workingPage)
    onDismiss()
  }

  BackHandler(enabled = pendingStickerEditor == null) {
    if (inlineEditingElementId != null) {
      commitInlineEditingIfActive()
    } else {
      closeEditorCleanly()
    }
  }

  val selectedElement =
    remember(workingPage.elements, selectedElementId) {
      workingPage.elements.firstOrNull { it.id == selectedElementId }
    }

  val activeFormattingTextElement =
    remember(workingPage.elements, inlineEditingElementId, selectedElementId) {
      val id = inlineEditingElementId ?: selectedElementId
      workingPage.elements.firstOrNull { it.id == id && it.type == "text" }
    }

  val inspectorBlock: @Composable () -> Unit = {
    EditorInspectorContent(
      toolMode = toolMode,
      workingPage = workingPage,
      selectedElement = selectedElement,
      isBangla = isBangla,
      activePen = activePen,
      penColorHex = penColorHex,
      strokeSizeRatio = strokeSizeRatio,
      stylusOnlyPalmRejection = stylusOnlyPalmRejection,
      favoriteAssetIds = favoriteAssetIds,
      userUploads = userUploads,
      voiceManager = voiceManager,
      playingVoiceElementId = playingVoiceElementId,
      onPenChange = { activePen = it },
      onColorChange = { penColorHex = it },
      onStrokeSizeChange = { strokeSizeRatio = it },
      onStylusOnlyChange = { stylusOnlyPalmRejection = it },
      onWriteOnPageClick = { focusOrCreatePageBodyInline() },
      onAddDiagramBoxClick = { createDiagramTextBox() },
      onOpenStickerStudio = {
        commitInlineEditingIfActive()
        toolMode = EditorToolMode.STUDIO_ASSETS
        isInspectorCollapsed = false
      },
      onStartDrawMode = {
        commitInlineEditingIfActive()
        toolMode = EditorToolMode.DRAW
        isInspectorCollapsed = false
      },
      onEditSelectedText = { el -> startInlineTextOnElement(el) },
      onPickPhotoClick = {
        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
      },
      onEditPhotoCrop = { el ->
        el.blobRef?.let { path -> pendingPhotoForCrop = path to el }
      },
      onAddShape = { shapeKind ->
        val newShape =
          CanvasElement(
            id = UUID.randomUUID().toString(),
            type = "shape",
            x = 0.34f,
            y = 0.36f,
            w = 0.28f,
            h = if (shapeKind == ShapeKind.DIVIDER_RULE) 0.06f else 0.20f,
            colorHex = penColorHex,
            assetId = shapeKind.id,
            zIndex = nextZIndex(workingPage),
          )
        selectedElementId = newShape.id
        commitAndPersist(workingPage.copy(elements = workingPage.elements + newShape))
      },
      onToggleFavorite = { id -> favoriteAssetIds = studioStore.toggleFavorite(id) },
      onInsertStudioAsset = { asset ->
        val el =
          CanvasElement(
            id = UUID.randomUUID().toString(),
            type = asset.elementType,
            x = 0.25f,
            y = 0.28f,
            w = asset.defaultW,
            h = asset.defaultH,
            zIndex = nextZIndex(workingPage),
            assetId = asset.id,
            text = if (isBangla) asset.defaultTextBn ?: asset.defaultTextEn else asset.defaultTextEn,
            colorHex = asset.colorHex,
          )
        selectedElementId = el.id
        commitAndPersist(workingPage.copy(elements = workingPage.elements + el))
      },
      onInsertUserUpload = { upload ->
        val el =
          CanvasElement(
            id = UUID.randomUUID().toString(),
            type = "sticker",
            x = 0.28f,
            y = 0.30f,
            w = 0.34f,
            h = 0.24f,
            zIndex = nextZIndex(workingPage),
            assetId = upload.id,
            blobRef = upload.filePath,
            text = upload.title,
          )
        selectedElementId = el.id
        commitAndPersist(workingPage.copy(elements = workingPage.elements + el))
      },
      onPickUserUpload = {
        pickUserAssetLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
      },
      onEditUserUpload = { upload ->
        val src = studioStore.loadSourceBitmapForUpload(upload)
        if (src != null) pendingStickerEditor = src to upload
      },
      onApplyBackground = { nextBg ->
        val preset = findPaperPreset(nextBg.presetId)
        if (preset.isDarkPaper && penColorHex == "#302C29") {
          penColorHex = preset.defaultInkHex
        } else if (!preset.isDarkPaper && penColorHex == "#F6EFE2") {
          penColorHex = "#302C29"
        }
        commitAndPersist(workingPage.copy(background = nextBg))
      },
      onSelectTemplate = { tpl ->
        if (workingPage.elements.isNotEmpty()) {
          pendingSafeTemplate = tpl
        } else {
          selectedElementId = null
          commitAndPersist(applyTemplateToPage(workingPage, tpl, replaceExisting = true, isBangla = isBangla))
          toolMode = EditorToolMode.SELECT
        }
      },
      onAttachVoiceNote = { filePath, durationSec ->
        val el =
          CanvasElement(
            id = UUID.randomUUID().toString(),
            type = "voice",
            x = 0.14f,
            y = 0.72f,
            w = 0.72f,
            h = 0.11f,
            blobRef = filePath,
            fontSize = durationSec.toFloat(),
            text = strings.voiceNoteLabel,
            zIndex = nextZIndex(workingPage),
          )
        selectedElementId = el.id
        commitAndPersist(workingPage.copy(elements = workingPage.elements + el))
      },
      onPlayVoiceElement = { el ->
        voiceManager.togglePlay(el.id, el.blobRef) { activeId ->
          playingVoiceElementId = activeId
        }
      },
      onRotateSelected = { el ->
        val next =
          workingPage.elements.map {
            if (it.id == el.id) it.copy(rotation = (it.rotation + 15f) % 360f) else it
          }
        commitAndPersist(workingPage.copy(elements = next))
      },
      onDeleteSelected = { el ->
        if (playingVoiceElementId == el.id) {
          voiceManager.stopPlayback()
          playingVoiceElementId = null
        }
        selectedElementId = null
        commitAndPersist(
          workingPage.copy(elements = workingPage.elements.filterNot { it.id == el.id })
        )
      },
      onSelectLayer = { id -> selectedElementId = id },
      onBringForward = { id ->
        val reordered = shiftLayerZOrder(workingPage.elements, id, forward = true)
        commitAndPersist(workingPage.copy(elements = reordered))
      },
      onSendBackward = { id ->
        val reordered = shiftLayerZOrder(workingPage.elements, id, forward = false)
        commitAndPersist(workingPage.copy(elements = reordered))
      },
      onToggleLock = { id ->
        val next =
          workingPage.elements.map { if (it.id == id) it.copy(locked = !it.locked) else it }
        commitAndPersist(workingPage.copy(elements = next))
      },
      onToggleHidden = { id ->
        val next =
          workingPage.elements.map { if (it.id == id) it.copy(hidden = !it.hidden) else it }
        commitAndPersist(workingPage.copy(elements = next))
      },
      onOpacityChange = { id, alpha ->
        val next =
          workingPage.elements.map { if (it.id == id) it.copy(opacity = alpha) else it }
        commitAndPersist(workingPage.copy(elements = next))
      },
      onGroupElements = { ids ->
        val gid = UUID.randomUUID().toString()
        val next =
          workingPage.elements.map { if (it.id in ids) it.copy(groupId = gid) else it }
        commitAndPersist(workingPage.copy(elements = next))
      },
      onUngroupElement = { gid ->
        val next =
          workingPage.elements.map { if (it.groupId == gid) it.copy(groupId = null) else it }
        commitAndPersist(workingPage.copy(elements = next))
      },
      onUpdateMetadata = { title, dateIso, mood, pinned ->
        commitAndPersist(
          workingPage.copy(title = title, dateIso = dateIso, mood = mood, pinned = pinned)
        )
      },
    )
  }

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = {},
        )
  ) {
    Surface(
      color = palette.background,
      contentColor = palette.textPrimary,
      modifier = Modifier.fillMaxSize(),
    ) {
      SankalanAtmosphericBackdrop(
        modifier = Modifier.fillMaxSize(),
        showOrnamentFrame = false,
      ) {
        Column(
          modifier =
            Modifier
              .fillMaxSize()
              .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
        // 1. Top Editor Bar (Close, Page/Save pill, Undo, Redo, Done)
        Surface(
          color = palette.shelfSurface,
          border = BorderStroke(1.dp, palette.borderGold),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .padding(horizontal = SankalanSpacing.sm, vertical = SankalanSpacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(
              onClick = { closeEditorCleanly() },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("editor_close_button"),
            ) {
              Icon(Icons.Outlined.Close, contentDescription = strings.closeLabel, tint = palette.textPrimary)
            }

            Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
              Text(
                text = "${diary.title} · ${strings.pageNumberPrefix} ${formatNumber(pageNumber, numeralStyle)}",
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              SaveStatusPill(status = saveStatus, onRetry = onRetrySave)
            }

            // Read historyTick so canUndo/canRedo stay reactive
            val canUndoNow = remember(historyTick) { history.canUndo }
            val canRedoNow = remember(historyTick) { history.canRedo }

            IconButton(
              onClick = { performUndo() },
              enabled = canUndoNow,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("editor_undo_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.Undo,
                contentDescription = strings.editorUndo,
                tint = if (canUndoNow) palette.goldAccent else palette.textSecondary.copy(alpha = 0.35f),
              )
            }
            IconButton(
              onClick = { performRedo() },
              enabled = canRedoNow,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("editor_redo_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.Redo,
                contentDescription = strings.editorRedo,
                tint = if (canRedoNow) palette.goldAccent else palette.textSecondary.copy(alpha = 0.35f),
              )
            }

            Button(
              onClick = { closeEditorCleanly() },
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = palette.goldAccent,
                  contentColor = SankalanTokens.Walnut900,
                ),
              shape = MaterialTheme.shapes.small,
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier.minimumInteractiveComponentSize().testTag("editor_done_button"),
            ) {
              Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = strings.editorDone,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        // 2. Word-Style Top Formatting Ribbon (visible whenever Text tool or text box is active)
        val showWordFormattingBar =
          toolMode == EditorToolMode.TEXT ||
            inlineEditingElementId != null ||
            activeFormattingTextElement != null
        if (showWordFormattingBar) {
          WordStyleTextFormattingBar(
            activeElement = activeFormattingTextElement,
            defaultFontId = defaultTextFontId,
            defaultFontSize = defaultTextSize,
            defaultBold = defaultTextBold,
            defaultItalic = defaultTextItalic,
            defaultUnderline = defaultTextUnderline,
            defaultAlign = defaultTextAlign,
            defaultColorHex = defaultTextColorHex,
            isInlineTyping = inlineEditingElementId != null,
            onCommitTyping = { commitInlineEditingIfActive() },
            onUpdateFormatting = { fontId, fontSize, bold, italic, underline, align, colorHex ->
              defaultTextFontId = fontId
              defaultTextSize = fontSize
              defaultTextBold = bold
              defaultTextItalic = italic
              defaultTextUnderline = underline
              defaultTextAlign = align
              defaultTextColorHex = colorHex

              val targetId = activeFormattingTextElement?.id
              if (targetId != null) {
                val updated =
                  workingPage.elements.map { el ->
                    if (el.id == targetId) {
                      el.copy(
                        fontId = fontId,
                        fontSize = fontSize,
                        bold = bold,
                        italic = italic,
                        underline = underline,
                        align = align,
                        colorHex = colorHex,
                      )
                    } else {
                      el
                    }
                  }
                if (inlineEditingElementId != null) {
                  workingPage = workingPage.copy(elements = updated)
                } else {
                  commitAndPersist(workingPage.copy(elements = updated))
                }
              }
            },
          )
        }

        // 3. Adaptive Workspace + Floating Draggable Collapsible Quick-Tool Orb Overlay
        var workspacePxSize by remember { mutableStateOf(IntSize(1, 1)) }
        var isOrbExpanded by remember { mutableStateOf(false) }

        Box(
          modifier =
            Modifier
              .weight(1f)
              .fillMaxWidth()
              .onSizeChanged { workspacePxSize = it },
        ) {
          BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideWorkspace = maxWidth >= 720.dp
            val phoneInspectorMaxHeight = (maxHeight * 0.30f).coerceIn(96.dp, 200.dp)

            val canvasStageContent: @Composable () -> Unit = {
              InteractiveCanvasStage(
                workingPage = workingPage,
                numeralStyle = numeralStyle,
                toolMode = toolMode,
                selectedElementId = selectedElementId,
                inlineEditingElementId = inlineEditingElementId,
                inlinePlaceholder = strings.editorInlineTextHint,
                activePen = activePen,
                penColorHex = penColorHex,
                strokeSizeRatio = strokeSizeRatio,
                stylusOnlyPalmRejection = stylusOnlyPalmRejection,
                activeStroke = activeStroke,
                liveTransform = liveTransform,
                canvasPxSize = canvasPxSize,
                playingVoiceElementId = playingVoiceElementId,
                onVoiceElementClick = { el ->
                  voiceManager.togglePlay(el.id, el.blobRef) { activeId ->
                    playingVoiceElementId = activeId
                  }
                },
                onCanvasSizeChanged = { canvasPxSize = it },
                onSelectElement = { id ->
                  if (inlineEditingElementId != null && id != inlineEditingElementId) {
                    commitInlineEditingIfActive()
                  }
                  selectedElementId = id
                },
                onStartInlineEdit = { el -> startInlineTextOnElement(el) },
                onFocusPageBody = { focusOrCreatePageBodyInline() },
                onInlineTextChange = { elId, nextText ->
                  workingPage =
                    workingPage.copy(
                      elements =
                        workingPage.elements.map { if (it.id == elId) it.copy(text = nextText) else it }
                    )
                },
                onCommitInlineText = { commitInlineEditingIfActive() },
                onLiveTransformChange = { nextLt -> liveTransform = nextLt },
                onErasePreviewElements = { remaining ->
                  workingPage = workingPage.copy(elements = remaining)
                },
                onCommitElements = { finalElements ->
                  liveTransform = null
                  commitAndPersist(workingPage.copy(elements = finalElements))
                },
                onActiveStrokeChange = { activeStroke = it },
                onCommitStroke = { strokeEl ->
                  activeStroke = null
                  commitAndPersist(workingPage.copy(elements = workingPage.elements + strokeEl))
                },
              )
            }

            if (isWideWorkspace) {
              Row(modifier = Modifier.fillMaxSize()) {
                Box(
                  modifier = Modifier.weight(1.35f).fillMaxHeight().padding(SankalanSpacing.lg),
                  contentAlignment = Alignment.Center,
                ) {
                  canvasStageContent()
                }

                Surface(
                  color = palette.cardSurface,
                  border = BorderStroke(1.dp, palette.borderGold),
                  modifier = Modifier.weight(0.9f).fillMaxHeight(),
                ) {
                  Column(modifier = Modifier.fillMaxSize().padding(SankalanSpacing.md)) {
                    EditorModeTabsStrip(
                      toolMode = toolMode,
                      onSelectMode = { nextMode ->
                        if (nextMode != EditorToolMode.TEXT && nextMode != EditorToolMode.SELECT) {
                          commitInlineEditingIfActive()
                        }
                        toolMode = nextMode
                      },
                    )
                    Spacer(modifier = Modifier.height(SankalanSpacing.sm))
                    Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                      inspectorBlock()
                    }
                  }
                }
              }
            } else {
              Column(modifier = Modifier.fillMaxSize()) {
                Box(
                  modifier =
                    Modifier
                      .weight(1f)
                      .fillMaxWidth()
                      .heightIn(min = 120.dp)
                      .padding(horizontal = SankalanSpacing.md, vertical = SankalanSpacing.xs),
                  contentAlignment = Alignment.Center,
                ) {
                  canvasStageContent()
                }

                Surface(
                  color = palette.cardSurface,
                  border = BorderStroke(1.dp, palette.borderGold),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(
                    modifier =
                      Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SankalanSpacing.sm, vertical = SankalanSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      Box(modifier = Modifier.weight(1f)) {
                        EditorModeTabsStrip(
                          toolMode = toolMode,
                          onSelectMode = { nextMode ->
                            if (nextMode != EditorToolMode.TEXT && nextMode != EditorToolMode.SELECT) {
                              commitInlineEditingIfActive()
                            }
                            toolMode = nextMode
                            isInspectorCollapsed = false
                          },
                        )
                      }
                      IconButton(
                        onClick = { isInspectorCollapsed = !isInspectorCollapsed },
                        modifier =
                          Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("editor_toggle_inspector_button"),
                      ) {
                        Icon(
                          imageVector =
                            if (isInspectorCollapsed) Icons.Outlined.KeyboardArrowUp
                            else Icons.Outlined.KeyboardArrowDown,
                          contentDescription = null,
                          tint = palette.goldAccent,
                        )
                      }
                    }

                    AnimatedVisibility(visible = !isInspectorCollapsed) {
                      Box(
                        modifier =
                          Modifier
                            .fillMaxWidth()
                            .heightIn(max = phoneInspectorMaxHeight)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = SankalanSpacing.xs, vertical = SankalanSpacing.xs)
                            .padding(bottom = 6.dp),
                      ) {
                        inspectorBlock()
                      }
                    }
                  }
                }
              }
            }
          }

          // Floating Draggable & Collapsible Quick-Tool Orb
          FloatingQuickToolOrb(
            containerSize = workspacePxSize,
            isExpanded = isOrbExpanded,
            toolMode = toolMode,
            activePen = activePen,
            stylusOnly = stylusOnlyPalmRejection,
            canUndo = remember(historyTick) { history.canUndo },
            canRedo = remember(historyTick) { history.canRedo },
            onToggleExpand = { isOrbExpanded = !isOrbExpanded },
            onSelectPenTool = { pen ->
              commitInlineEditingIfActive()
              activePen = pen
              toolMode = EditorToolMode.DRAW
              isOrbExpanded = false
            },
            onSelectTextTool = {
              isOrbExpanded = false
              focusOrCreatePageBodyInline()
            },
            onSelectStickerTool = {
              commitInlineEditingIfActive()
              toolMode = EditorToolMode.STUDIO_ASSETS
              isInspectorCollapsed = false
              isOrbExpanded = false
            },
            onSelectMoveTool = {
              commitInlineEditingIfActive()
              toolMode = EditorToolMode.SELECT
              isOrbExpanded = false
            },
            onToggleStylusOnly = { stylusOnlyPalmRejection = !stylusOnlyPalmRejection },
            onUndo = { performUndo() },
            onRedo = { performRedo() },
          )
        }
      }
      }
    }

    // Smart Sticker Cutout & Border Editor Overlay (rendered inside root Box over CanvasEditorScreen)
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
            studioStore.saveEditedSticker(
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
            userUploads = studioStore.loadUploads()
            if (existingUpload == null) {
              val el =
                CanvasElement(
                  id = UUID.randomUUID().toString(),
                  type = "sticker",
                  x = 0.28f,
                  y = 0.30f,
                  w = 0.34f,
                  h = 0.24f,
                  zIndex = nextZIndex(workingPage),
                  assetId = saved.id,
                  blobRef = saved.filePath,
                  text = saved.title,
                )
              selectedElementId = el.id
              commitAndPersist(workingPage.copy(elements = workingPage.elements + el))
            } else {
              val updatedElements =
                workingPage.elements.map { el ->
                  if (el.assetId == saved.id) el.copy(blobRef = saved.filePath, text = saved.title)
                  else el
                }
              commitAndPersist(workingPage.copy(elements = updatedElements))
            }
          }
        },
      )
    }
  }

  // Photo Crop & Frame Dialog
  if (pendingPhotoForCrop != null) {
    val (path, editingImage) = pendingPhotoForCrop!!
    val initialFrame =
      editingImage?.let { CanvasPayloadCodec.decodeImageFrame(it.strokePointsJson) } ?: ImageFramePayload()
    PhotoCropAndFrameDialog(
      photoFilePath = path,
      initialPayload = initialFrame,
      onDismiss = { pendingPhotoForCrop = null },
      onConfirm = { payload ->
        val encoded = CanvasPayloadCodec.encodeImageFrame(payload)
        val updatedElements =
          if (editingImage == null) {
            val newImg =
              CanvasElement(
                id = UUID.randomUUID().toString(),
                type = "image",
                x = 0.18f,
                y = 0.22f,
                w = 0.54f,
                h = 0.36f,
                blobRef = path,
                strokePointsJson = encoded,
                zIndex = nextZIndex(workingPage),
              )
            selectedElementId = newImg.id
            workingPage.elements + newImg
          } else {
            workingPage.elements.map {
              if (it.id == editingImage.id) it.copy(strokePointsJson = encoded) else it
            }
          }
        commitAndPersist(workingPage.copy(elements = updatedElements))
        pendingPhotoForCrop = null
      },
    )
  }

  // Safe Template Replacement vs Overlay Confirmation Dialog
  if (pendingSafeTemplate != null) {
    val tpl = pendingSafeTemplate!!
    SafeTemplateConfirmDialog(
      templateName = if (isBangla) tpl.nameBn else tpl.nameEn,
      onDismiss = { pendingSafeTemplate = null },
      onReplace = {
        selectedElementId = null
        commitAndPersist(applyTemplateToPage(workingPage, tpl, replaceExisting = true, isBangla = isBangla))
        pendingSafeTemplate = null
        toolMode = EditorToolMode.SELECT
      },
      onOverlay = {
        selectedElementId = null
        commitAndPersist(applyTemplateToPage(workingPage, tpl, replaceExisting = false, isBangla = isBangla))
        pendingSafeTemplate = null
        toolMode = EditorToolMode.SELECT
      },
    )
  }
}

@Composable
private fun WordStyleTextFormattingBar(
  activeElement: CanvasElement?,
  defaultFontId: String,
  defaultFontSize: Float,
  defaultBold: Boolean,
  defaultItalic: Boolean,
  defaultUnderline: Boolean,
  defaultAlign: String,
  defaultColorHex: String,
  isInlineTyping: Boolean,
  onCommitTyping: () -> Unit,
  onUpdateFormatting: (
    fontId: String,
    fontSize: Float,
    bold: Boolean,
    italic: Boolean,
    underline: Boolean,
    align: String,
    colorHex: String,
  ) -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  var showFontMenu by remember { mutableStateOf(false) }

  val fontId = activeElement?.fontId ?: defaultFontId
  val fontSize = activeElement?.fontSize ?: defaultFontSize
  val bold = activeElement?.bold ?: defaultBold
  val italic = activeElement?.italic ?: defaultItalic
  val underline = activeElement?.underline ?: defaultUnderline
  val align = activeElement?.align ?: defaultAlign
  val colorHex = activeElement?.colorHex ?: defaultColorHex

  val currentFontChoice =
    remember(fontId) { EDITOR_FONT_CHOICES.firstOrNull { it.id == fontId } ?: EDITOR_FONT_CHOICES.first() }

  Surface(
    color = palette.cardSurface,
    border = BorderStroke(1.dp, palette.borderSubtle),
    modifier = Modifier.fillMaxWidth().testTag("word_style_text_toolbar"),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = SankalanSpacing.sm, vertical = 5.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Font Family Dropdown (Word-style)
      Box {
        Surface(
          shape = MaterialTheme.shapes.small,
          color = palette.shelfSurface,
          border = BorderStroke(1.dp, palette.borderGold),
          modifier =
            Modifier
              .clip(MaterialTheme.shapes.small)
              .clickable { showFontMenu = true }
              .testTag("word_bar_font_picker"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
          ) {
            Text(
              text = currentFontChoice.displayName,
              fontFamily = resolveEditorFontFamily(currentFontChoice.id),
              fontSize = 12.sp,
              color = palette.textPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Icon(
              imageVector = Icons.Outlined.ArrowDropDown,
              contentDescription = strings.editorFontLabel,
              tint = palette.goldAccent,
              modifier = Modifier.size(16.dp),
            )
          }
        }
        DropdownMenu(
          expanded = showFontMenu,
          onDismissRequest = { showFontMenu = false },
        ) {
          EDITOR_FONT_CHOICES.forEach { choice ->
            DropdownMenuItem(
              text = {
                Text(
                  text = "${choice.displayName} — ${choice.sample}",
                  fontFamily = resolveEditorFontFamily(choice.id),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              },
              onClick = {
                showFontMenu = false
                onUpdateFormatting(choice.id, fontSize, bold, italic, underline, align, colorHex)
              },
            )
          }
        }
      }

      // Font Size - / +
      Surface(
        shape = MaterialTheme.shapes.small,
        color = palette.shelfSurface,
        border = BorderStroke(1.dp, palette.borderSubtle),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier =
              Modifier
                .clickable {
                  val nextSize = (fontSize - 1.5f).coerceIn(10f, 36f)
                  onUpdateFormatting(fontId, nextSize, bold, italic, underline, align, colorHex)
                }
                .padding(horizontal = 8.dp, vertical = 6.dp),
          ) {
            Text("−", fontWeight = FontWeight.Bold, color = palette.goldAccent, fontSize = 13.sp)
          }
          Text(
            text = "${fontSize.roundToInt()}",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textPrimary,
            modifier = Modifier.padding(horizontal = 4.dp),
          )
          Box(
            modifier =
              Modifier
                .clickable {
                  val nextSize = (fontSize + 1.5f).coerceIn(10f, 36f)
                  onUpdateFormatting(fontId, nextSize, bold, italic, underline, align, colorHex)
                }
                .padding(horizontal = 8.dp, vertical = 6.dp),
          ) {
            Text("+", fontWeight = FontWeight.Bold, color = palette.goldAccent, fontSize = 13.sp)
          }
        }
      }

      // Bold / Italic / Underline pills
      WordFormatIconToggle(
        selected = bold,
        icon = Icons.Outlined.FormatBold,
        contentDesc = strings.editorBoldLabel,
        onClick = { onUpdateFormatting(fontId, fontSize, !bold, italic, underline, align, colorHex) },
      )
      WordFormatIconToggle(
        selected = italic,
        icon = Icons.Outlined.FormatItalic,
        contentDesc = strings.editorItalicLabel,
        onClick = { onUpdateFormatting(fontId, fontSize, bold, !italic, underline, align, colorHex) },
      )
      WordFormatIconToggle(
        selected = underline,
        icon = Icons.Outlined.FormatUnderlined,
        contentDesc = strings.editorUnderlineLabel,
        onClick = { onUpdateFormatting(fontId, fontSize, bold, italic, !underline, align, colorHex) },
      )

      // Alignment pills
      WordFormatIconToggle(
        selected = align == "left",
        icon = Icons.AutoMirrored.Outlined.FormatAlignLeft,
        contentDesc = strings.editorAlignLeft,
        onClick = { onUpdateFormatting(fontId, fontSize, bold, italic, underline, "left", colorHex) },
      )
      WordFormatIconToggle(
        selected = align == "center",
        icon = Icons.Outlined.FormatAlignCenter,
        contentDesc = strings.editorAlignCenter,
        onClick = { onUpdateFormatting(fontId, fontSize, bold, italic, underline, "center", colorHex) },
      )
      WordFormatIconToggle(
        selected = align == "right",
        icon = Icons.AutoMirrored.Outlined.FormatAlignRight,
        contentDesc = strings.editorAlignRight,
        onClick = { onUpdateFormatting(fontId, fontSize, bold, italic, underline, "right", colorHex) },
      )

      // Compact Ink Color Swatches
      EDITOR_INK_SWATCHES.take(7).forEach { hex ->
        val selected = colorHex.equals(hex, ignoreCase = true)
        Box(
          modifier =
            Modifier
              .size(22.dp)
              .clip(CircleShape)
              .background(parseHexColor(hex))
              .border(
                width = if (selected) 2.2.dp else 1.dp,
                color = if (selected) palette.goldHighlight else palette.borderSubtle,
                shape = CircleShape,
              )
              .clickable {
                onUpdateFormatting(fontId, fontSize, bold, italic, underline, align, hex)
              },
        )
      }

      if (isInlineTyping) {
        Surface(
          shape = MaterialTheme.shapes.small,
          color = palette.goldAccent,
          modifier =
            Modifier
              .clip(MaterialTheme.shapes.small)
              .clickable(onClick = onCommitTyping)
              .testTag("word_bar_done_typing"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.Check,
              contentDescription = strings.editorDone,
              tint = SankalanTokens.Walnut900,
              modifier = Modifier.size(15.dp),
            )
            Text(
              text = strings.editorDone,
              style = MaterialTheme.typography.labelSmall,
              color = SankalanTokens.Walnut900,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun WordFormatIconToggle(
  selected: Boolean,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  contentDesc: String,
  onClick: () -> Unit,
) {
  val palette = LocalSankalanPalette.current
  Surface(
    shape = MaterialTheme.shapes.small,
    color = if (selected) palette.goldAccent else palette.shelfSurface,
    border = BorderStroke(1.dp, if (selected) palette.goldAccent else palette.borderSubtle),
    modifier = Modifier.clip(MaterialTheme.shapes.small).clickable(onClick = onClick),
  ) {
    Box(modifier = Modifier.padding(6.dp), contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = contentDesc,
        tint = if (selected) SankalanTokens.Walnut900 else palette.textPrimary,
        modifier = Modifier.size(16.dp),
      )
    }
  }
}

/**
 * Floating draggable & collapsible Quick-Tool Orb giving 1-tap access to Pen/Stylus,
 * Pencil, Highlighter, Eraser, Inline Typing, Move & Spin, Palm Rejection & Undo/Redo.
 */
@Composable
private fun FloatingQuickToolOrb(
  containerSize: IntSize,
  isExpanded: Boolean,
  toolMode: EditorToolMode,
  activePen: PenKind,
  stylusOnly: Boolean,
  canUndo: Boolean,
  canRedo: Boolean,
  onToggleExpand: () -> Unit,
  onSelectPenTool: (PenKind) -> Unit,
  onSelectTextTool: () -> Unit,
  onSelectStickerTool: () -> Unit,
  onSelectMoveTool: () -> Unit,
  onToggleStylusOnly: () -> Unit,
  onUndo: () -> Unit,
  onRedo: () -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val density = LocalDensity.current
  val orbPx = with(density) { 54.dp.toPx() }
  val marginPx = with(density) { 14.dp.toPx() }

  var initializedPos by remember { mutableStateOf(false) }
  var offsetX by remember { mutableFloatStateOf(0f) }
  var offsetY by remember { mutableFloatStateOf(0f) }

  LaunchedEffect(containerSize) {
    if (containerSize.width > 10 && containerSize.height > 10) {
      val maxX = (containerSize.width - orbPx - marginPx).coerceAtLeast(marginPx)
      val maxY = (containerSize.height - orbPx - marginPx).coerceAtLeast(marginPx)
      if (!initializedPos) {
        offsetX = maxX
        offsetY = (containerSize.height * 0.42f).coerceIn(marginPx, maxY)
        initializedPos = true
      } else {
        offsetX = offsetX.coerceIn(marginPx, maxX)
        offsetY = offsetY.coerceIn(marginPx, maxY)
      }
    }
  }

  if (containerSize.width <= 10 || containerSize.height <= 10) return

  val onRightSide = offsetX > containerSize.width * 0.5f
  val openUpwards = offsetY > containerSize.height * 0.52f
  val gapPx = with(density) { 8.dp.toPx() }
  val topPadDp = with(density) { (offsetY + orbPx + gapPx).coerceAtLeast(marginPx).toDp() }
  val bottomPadDp = with(density) { (containerSize.height - offsetY + gapPx).coerceAtLeast(marginPx).toDp() }
  val startPadDp = with(density) { offsetX.coerceAtLeast(marginPx).toDp() }
  val endPadDp = with(density) { (containerSize.width - offsetX - orbPx).coerceAtLeast(marginPx).toDp() }

  Box(modifier = Modifier.fillMaxSize()) {
    val panelAlignment =
      when {
        openUpwards && onRightSide -> Alignment.BottomEnd
        openUpwards && !onRightSide -> Alignment.BottomStart
        !openUpwards && onRightSide -> Alignment.TopEnd
        else -> Alignment.TopStart
      }
    val panelPadding =
      PaddingValues(
        start = if (!onRightSide) startPadDp else 12.dp,
        end = if (onRightSide) endPadDp else 12.dp,
        top = if (!openUpwards) topPadDp else 12.dp,
        bottom = if (openUpwards) bottomPadDp else 12.dp,
      )

    Box(
      modifier = Modifier.fillMaxSize().padding(panelPadding),
      contentAlignment = panelAlignment,
    ) {
      AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
      ) {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = SankalanTokens.Walnut900.copy(alpha = 0.96f),
          border = BorderStroke(1.2.dp, palette.goldAccent),
          shadowElevation = 10.dp,
          modifier = Modifier.testTag("floating_quick_tool_panel"),
        ) {
          Column(
            modifier = Modifier.padding(10.dp).heightIn(max = 340.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = strings.editorQuickOrbLabel,
              style = MaterialTheme.typography.labelSmall,
              color = SankalanTokens.GoldHighlight,
              modifier = Modifier.padding(horizontal = 4.dp),
            )
            QuickOrbToolRow(
              icon = Icons.Outlined.Edit,
              label = strings.editorQuickOrbPen,
              selected = toolMode == EditorToolMode.DRAW && activePen == PenKind.PEN,
              onClick = { onSelectPenTool(PenKind.PEN) },
            )
            QuickOrbToolRow(
              icon = Icons.Outlined.Draw,
              label = strings.editorQuickOrbPencil,
              selected = toolMode == EditorToolMode.DRAW && activePen == PenKind.PENCIL,
              onClick = { onSelectPenTool(PenKind.PENCIL) },
            )
            QuickOrbToolRow(
              icon = Icons.Outlined.BorderColor,
              label = strings.editorQuickOrbHighlighter,
              selected = toolMode == EditorToolMode.DRAW && activePen == PenKind.HIGHLIGHTER,
              onClick = { onSelectPenTool(PenKind.HIGHLIGHTER) },
            )
            QuickOrbToolRow(
              icon = Icons.Outlined.CleaningServices,
              label = strings.editorQuickOrbEraser,
              selected = toolMode == EditorToolMode.DRAW && activePen == PenKind.ERASER,
              onClick = { onSelectPenTool(PenKind.ERASER) },
            )
            QuickOrbToolRow(
              icon = Icons.Outlined.TextFields,
              label = strings.editorQuickOrbText,
              selected = toolMode == EditorToolMode.TEXT,
              onClick = onSelectTextTool,
            )
            QuickOrbToolRow(
              icon = Icons.Outlined.AutoAwesome,
              label = strings.editorQuickOrbSticker,
              selected = toolMode == EditorToolMode.STUDIO_ASSETS,
              onClick = onSelectStickerTool,
              modifier = Modifier.testTag("editor_orb_sticker_btn"),
            )
            QuickOrbToolRow(
              icon = Icons.Outlined.OpenWith,
              label = strings.editorQuickOrbSelect,
              selected = toolMode == EditorToolMode.SELECT,
              onClick = onSelectMoveTool,
            )
            QuickOrbToolRow(
              icon = Icons.Outlined.AutoFixHigh,
              label = strings.editorQuickOrbStylusToggle,
              selected = stylusOnly,
              onClick = onToggleStylusOnly,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = SankalanTokens.Walnut800,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(enabled = canUndo, onClick = onUndo),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Outlined.Undo,
                  contentDescription = strings.editorUndo,
                  tint = if (canUndo) SankalanTokens.GoldHighlight else SankalanTokens.Muted,
                  modifier = Modifier.padding(8.dp).size(18.dp),
                )
              }
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = SankalanTokens.Walnut800,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(enabled = canRedo, onClick = onRedo),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Outlined.Redo,
                  contentDescription = strings.editorRedo,
                  tint = if (canRedo) SankalanTokens.GoldHighlight else SankalanTokens.Muted,
                  modifier = Modifier.padding(8.dp).size(18.dp),
                )
              }
            }
          }
        }
      }
    }

    // Main circular draggable & collapsible Quick-Tool Orb
    Surface(
      shape = CircleShape,
      color = if (isExpanded) SankalanTokens.Walnut900 else palette.goldAccent,
      border = BorderStroke(2.dp, if (isExpanded) palette.goldAccent else SankalanTokens.Walnut900),
      shadowElevation = 8.dp,
      modifier =
        Modifier
          .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
          .size(54.dp)
          .pointerInput(containerSize) {
            detectDragGestures { change, dragAmount ->
              change.consume()
              val maxX = (containerSize.width - orbPx - marginPx).coerceAtLeast(marginPx)
              val maxY = (containerSize.height - orbPx - marginPx).coerceAtLeast(marginPx)
              offsetX = (offsetX + dragAmount.x).coerceIn(marginPx, maxX)
              offsetY = (offsetY + dragAmount.y).coerceIn(marginPx, maxY)
            }
          }
          .clickable(role = Role.Button, onClick = onToggleExpand)
          .testTag("floating_quick_tool_orb"),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = if (isExpanded) Icons.Outlined.Close else Icons.Outlined.Build,
          contentDescription = strings.editorQuickOrbLabel,
          tint = if (isExpanded) palette.goldAccent else SankalanTokens.Walnut900,
          modifier = Modifier.size(24.dp),
        )
      }
    }
  }
}

@Composable
private fun QuickOrbToolRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (selected) SankalanTokens.AntiqueGold else SankalanTokens.Walnut800,
    modifier = modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (selected) SankalanTokens.Walnut900 else SankalanTokens.GoldHighlight,
        modifier = Modifier.size(16.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) SankalanTokens.Walnut900 else SankalanTokens.Cream,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun InteractiveCanvasStage(
  workingPage: Page,
  numeralStyle: NumeralStyle,
  toolMode: EditorToolMode,
  selectedElementId: String?,
  inlineEditingElementId: String?,
  inlinePlaceholder: String,
  activePen: PenKind,
  penColorHex: String,
  strokeSizeRatio: Float,
  stylusOnlyPalmRejection: Boolean,
  activeStroke: StrokePayload?,
  liveTransform: LiveElementTransform?,
  canvasPxSize: IntSize,
  playingVoiceElementId: String?,
  onVoiceElementClick: (CanvasElement) -> Unit,
  onCanvasSizeChanged: (IntSize) -> Unit,
  onSelectElement: (String?) -> Unit,
  onStartInlineEdit: (CanvasElement) -> Unit,
  onFocusPageBody: () -> Unit,
  onInlineTextChange: (String, String) -> Unit,
  onCommitInlineText: () -> Unit,
  onLiveTransformChange: (LiveElementTransform?) -> Unit,
  onErasePreviewElements: (List<CanvasElement>) -> Unit,
  onCommitElements: (List<CanvasElement>) -> Unit,
  onActiveStrokeChange: (StrokePayload?) -> Unit,
  onCommitStroke: (CanvasElement) -> Unit,
) {
  val currentPage by rememberUpdatedState(workingPage)
  val currentSelectedId by rememberUpdatedState(selectedElementId)
  val currentToolMode by rememberUpdatedState(toolMode)
  val currentActivePen by rememberUpdatedState(activePen)
  val currentPenColor by rememberUpdatedState(penColorHex)
  val currentStrokeSize by rememberUpdatedState(strokeSizeRatio)
  val currentStylusOnly by rememberUpdatedState(stylusOnlyPalmRejection)
  val currentLiveTransform by rememberUpdatedState(liveTransform)

  val onSelectLatest by rememberUpdatedState(onSelectElement)
  val onStartInlineLatest by rememberUpdatedState(onStartInlineEdit)
  val onFocusPageBodyLatest by rememberUpdatedState(onFocusPageBody)
  val onLiveTransformLatest by rememberUpdatedState(onLiveTransformChange)
  val onErasePreviewLatest by rememberUpdatedState(onErasePreviewElements)
  val onCommitElementsLatest by rememberUpdatedState(onCommitElements)
  val onActiveStrokeLatest by rememberUpdatedState(onActiveStrokeChange)
  val onCommitStrokeLatest by rememberUpdatedState(onCommitStroke)

  Box(
    modifier =
      Modifier
        .onSizeChanged(onCanvasSizeChanged)
        .pointerInput(toolMode, inlineEditingElementId, canvasPxSize) {
          if (toolMode != EditorToolMode.DRAW && inlineEditingElementId == null) {
            detectTapGestures(
              onTap = { tapOffset ->
                val cw = canvasPxSize.width.coerceAtLeast(1).toFloat()
                val ch = canvasPxSize.height.coerceAtLeast(1).toFloat()
                val aspect = cw / ch
                val nx = tapOffset.x / cw
                val ny = tapOffset.y / ch
                val elements = currentPage.elements
                val selId = currentSelectedId
                val hit =
                  elements
                    .filter { !it.hidden && !it.locked && it.type != "stroke" && !isPageBodyElement(it) }
                    .sortedByDescending { it.zIndex }
                    .firstOrNull {
                      CanvasTransformMath.hitTestElement(
                        nx = nx,
                        ny = ny,
                        el = it,
                        canvasAspect = aspect,
                        isSelected = it.id == selId,
                      ) != null
                    }
                if (currentToolMode == EditorToolMode.TEXT) {
                  if (hit != null && hit.type == "text") {
                    onStartInlineLatest(hit)
                  } else if (hit == null) {
                    onFocusPageBodyLatest()
                  } else {
                    onSelectLatest(hit.id)
                  }
                } else {
                  onSelectLatest(hit?.id)
                }
              },
              onDoubleTap = { tapOffset ->
                val cw = canvasPxSize.width.coerceAtLeast(1).toFloat()
                val ch = canvasPxSize.height.coerceAtLeast(1).toFloat()
                val aspect = cw / ch
                val nx = tapOffset.x / cw
                val ny = tapOffset.y / ch
                val elements = currentPage.elements
                val selId = currentSelectedId
                val hitBox =
                  elements
                    .filter { !it.hidden && !it.locked && isDiagramTextBox(it) }
                    .sortedByDescending { it.zIndex }
                    .firstOrNull {
                      CanvasTransformMath.hitTestElement(nx, ny, it, aspect, it.id == selId) != null
                    }
                if (hitBox != null) {
                  onStartInlineLatest(hitBox)
                } else {
                  val nonTextHit =
                    elements
                      .filter { !it.hidden && !it.locked && it.type != "stroke" && !isPageBodyElement(it) }
                      .any {
                        CanvasTransformMath.hitTestElement(nx, ny, it, aspect, it.id == selId) != null
                      }
                  if (!nonTextHit) {
                    onFocusPageBodyLatest()
                  }
                }
              },
            )
          }
        }
        .pointerInput(toolMode, inlineEditingElementId, canvasPxSize) {
          if (toolMode != EditorToolMode.DRAW && inlineEditingElementId == null) {
            awaitEachGesture {
              val down = awaitFirstDown(requireUnconsumed = false)
              val cw = canvasPxSize.width.coerceAtLeast(1).toFloat()
              val ch = canvasPxSize.height.coerceAtLeast(1).toFloat()
              val aspect = cw / ch
              val startNx = down.position.x / cw
              val startNy = down.position.y / ch
              val startElements = currentPage.elements
              val selId = currentSelectedId

              val currentSel =
                startElements.firstOrNull {
                  it.id == selId && !it.locked && !it.hidden && !isPageBodyElement(it)
                }
              var initialZone: ElementHitZone? =
                if (currentSel != null) {
                  CanvasTransformMath.hitTestElement(startNx, startNy, currentSel, aspect, isSelected = true)
                } else {
                  null
                }
              val target =
                if (initialZone != null && currentSel != null) {
                  currentSel
                } else {
                  val found =
                    startElements
                      .filter { !it.hidden && !it.locked && it.type != "stroke" && !isPageBodyElement(it) }
                      .sortedByDescending { it.zIndex }
                      .firstOrNull { el ->
                        CanvasTransformMath.hitTestElement(startNx, startNy, el, aspect, isSelected = false) != null
                      }
                  if (found != null) initialZone = ElementHitZone.BODY
                  found
                }

              val activeTarget = target ?: currentSel ?: return@awaitEachGesture
              val hitZone = initialZone ?: ElementHitZone.BODY
              val movedIds =
                if (activeTarget.groupId != null) {
                  startElements.filter { it.groupId == activeTarget.groupId && !it.locked }.map { it.id }.toSet()
                } else {
                  setOf(activeTarget.id)
                }
              val siblings = startElements.filter { it.id !in movedIds }

              var didModify = false
              var totalDragDist = 0f
              var dragDx = 0f
              var dragDy = 0f
              var resizeAccumPxX = 0f
              var resizeAccumPxY = 0f
              var pinchZoom = 1f
              var pinchRot = 0f
              var pinchPanX = 0f
              var pinchPanY = 0f
              var latestTransform: LiveElementTransform? = null

              while (true) {
                val event = awaitPointerEvent()
                val pressedChanges = event.changes.filter { it.pressed }
                if (pressedChanges.isEmpty()) break

                if (pressedChanges.size >= 2) {
                  // 2-finger multi-touch pinch-resize + rotate + pan on selected element via GPU graphicsLayer
                  val zoom = event.calculateZoom()
                  val rotDelta = event.calculateRotation()
                  val pan = event.calculatePan()
                  val distMoved = kotlin.math.abs(zoom - 1f) + kotlin.math.abs(rotDelta) + hypot(pan.x, pan.y)
                  if (distMoved > 0.002f) {
                    event.changes.forEach { it.consume() }
                    if (!didModify) {
                      onSelectLatest(activeTarget.id)
                    }
                    didModify = true
                    pinchZoom = (pinchZoom * zoom).coerceIn(0.25f, 3.5f)
                    pinchRot += rotDelta
                    pinchPanX += pan.x / cw
                    pinchPanY += pan.y / ch
                    val clampedW = (activeTarget.w * pinchZoom).coerceIn(0.08f, 0.98f)
                    val clampedH = (activeTarget.h * pinchZoom).coerceIn(0.04f, 0.94f)
                    val lt =
                      LiveElementTransform(
                        targetIds = setOf(activeTarget.id),
                        anchorElementId = activeTarget.id,
                        dxNorm = pinchPanX,
                        dyNorm = pinchPanY,
                        scaleX = clampedW / activeTarget.w.coerceAtLeast(0.02f),
                        scaleY = clampedH / activeTarget.h.coerceAtLeast(0.02f),
                        rotationDeltaDeg = pinchRot,
                        showRotationBadge = kotlin.math.abs(rotDelta) > 0.05f || kotlin.math.abs(pinchRot) > 0.5f,
                      )
                    latestTransform = lt
                    onLiveTransformLatest(lt)
                  }
                } else if (target != null) {
                  // 1-finger drag / rotate-handle / resize-handle via GPU graphicsLayer (zero page recomposition)
                  val change = pressedChanges.first()
                  val delta = change.position - change.previousPosition
                  totalDragDist += hypot(delta.x, delta.y)
                  if (totalDragDist > viewConfiguration.touchSlop * 0.4f || didModify) {
                    change.consume()
                    if (!didModify) {
                      onSelectLatest(activeTarget.id)
                    }
                    didModify = true

                    when (hitZone) {
                      ElementHitZone.ROTATE_HANDLE -> {
                        val centerPxX = (activeTarget.x + activeTarget.w * 0.5f) * cw
                        val centerPxY = (activeTarget.y + activeTarget.h * 0.5f) * ch
                        val nextAngle =
                          CanvasTransformMath.computeRotationDegreesFromTop(
                            pointerX = change.position.x,
                            pointerY = change.position.y,
                            centerX = centerPxX,
                            centerY = centerPxY,
                          )
                        val lt =
                          LiveElementTransform(
                            targetIds = setOf(activeTarget.id),
                            anchorElementId = activeTarget.id,
                            rotationOverrideDeg = nextAngle,
                            showRotationBadge = true,
                          )
                        latestTransform = lt
                        onLiveTransformLatest(lt)
                      }
                      ElementHitZone.RESIZE_HANDLE -> {
                        resizeAccumPxX += delta.x
                        resizeAccumPxY += delta.y
                        val rad = Math.toRadians(-activeTarget.rotation.toDouble())
                        val cosR = cos(rad).toFloat()
                        val sinR = sin(rad).toFloat()
                        val localDx = (resizeAccumPxX * cosR - resizeAccumPxY * sinR) / cw
                        val localDy = (resizeAccumPxX * sinR + resizeAccumPxY * cosR) / ch
                        val newW = (activeTarget.w + localDx).coerceIn(0.08f, 0.98f)
                        val newH = (activeTarget.h + localDy).coerceIn(0.04f, 0.94f)
                        val dwPx = (newW - activeTarget.w) * cw * 0.5f
                        val dhPx = (newH - activeTarget.h) * ch * 0.5f
                        val posRad = Math.toRadians(activeTarget.rotation.toDouble())
                        val shiftDx = (dwPx * cos(posRad) - dhPx * sin(posRad)).toFloat() / cw
                        val shiftDy = (dwPx * sin(posRad) + dhPx * cos(posRad)).toFloat() / ch
                        val lt =
                          LiveElementTransform(
                            targetIds = setOf(activeTarget.id),
                            anchorElementId = activeTarget.id,
                            dxNorm = shiftDx,
                            dyNorm = shiftDy,
                            scaleX = newW / activeTarget.w.coerceAtLeast(0.02f),
                            scaleY = newH / activeTarget.h.coerceAtLeast(0.02f),
                          )
                        latestTransform = lt
                        onLiveTransformLatest(lt)
                      }
                      ElementHitZone.BODY -> {
                        dragDx += delta.x / cw
                        dragDy += delta.y / ch
                        val snap =
                          CanvasSnapEngine.snapPosition(
                            candidateX = activeTarget.x + dragDx,
                            candidateY = activeTarget.y + dragDy,
                            elementW = activeTarget.w,
                            elementH = activeTarget.h,
                            otherElements = siblings,
                          )
                        val appliedDx = snap.snappedX - activeTarget.x
                        val appliedDy = snap.snappedY - activeTarget.y
                        val lt =
                          LiveElementTransform(
                            targetIds = movedIds,
                            anchorElementId = activeTarget.id,
                            dxNorm = appliedDx,
                            dyNorm = appliedDy,
                            verticalGuides = snap.verticalGuides,
                            horizontalGuides = snap.horizontalGuides,
                          )
                        latestTransform = lt
                        onLiveTransformLatest(lt)
                      }
                    }
                  }
                }
              }

              val finalTransform = latestTransform
              onLiveTransformLatest(null)
              if (didModify && finalTransform != null) {
                val committed = CanvasTransformMath.commitLiveTransform(startElements, finalTransform)
                onCommitElementsLatest(committed)
              }
            }
          }
        }
        .pointerInput(toolMode, canvasPxSize) {
          if (toolMode == EditorToolMode.DRAW) {
            awaitEachGesture {
              val down = awaitFirstDown(requireUnconsumed = false)
              // Palm Rejection check: when stylusOnlyPalmRejection is true, only accept Stylus or Eraser pointers!
              if (currentStylusOnly && down.type != PointerType.Stylus && down.type != PointerType.Eraser) {
                return@awaitEachGesture
              }
              down.consume()
              val w = canvasPxSize.width.coerceAtLeast(1).toFloat()
              val h = canvasPxSize.height.coerceAtLeast(1).toFloat()

              if (currentActivePen == PenKind.ERASER || down.type == PointerType.Eraser) {
                var currentElements = currentPage.elements
                var erasedAny = false
                fun eraseNear(px: Float, py: Float) {
                  val nx = (px / w).coerceIn(0f, 1f)
                  val ny = (py / h).coerceIn(0f, 1f)
                  val filtered =
                    currentElements.filterNot { el ->
                      if (el.type != "stroke" || el.locked) false
                      else {
                        val payload = CanvasPayloadCodec.decodeStroke(el.strokePointsJson)
                        val hit =
                          payload?.points?.any { pt -> hypot(pt.x - nx, pt.y - ny) < 0.045f } == true
                        if (hit) erasedAny = true
                        hit
                      }
                    }
                  if (filtered.size != currentElements.size) {
                    currentElements = filtered
                    onErasePreviewLatest(filtered)
                  }
                }
                eraseNear(down.position.x, down.position.y)
                while (true) {
                  val event = awaitPointerEvent()
                  val change = event.changes.firstOrNull { it.id == down.id } ?: break
                  if (!change.pressed) break
                  change.consume()
                  eraseNear(change.position.x, change.position.y)
                }
                if (erasedAny) {
                  onCommitElementsLatest(currentElements)
                }
              } else {
                val points =
                  mutableListOf(
                    StrokePoint(
                      x = (down.position.x / w).coerceIn(0f, 1f),
                      y = (down.position.y / h).coerceIn(0f, 1f),
                      pressure = down.pressure.coerceIn(0.2f, 1.3f),
                    )
                  )
                var currentPayload =
                  StrokePayload(
                    tool = currentActivePen.id,
                    colorHex = currentPenColor,
                    widthRatio = currentStrokeSize,
                    opacity = if (currentActivePen == PenKind.HIGHLIGHTER) 0.35f else 1f,
                    points = points.toList(),
                  )
                onActiveStrokeLatest(currentPayload)

                while (true) {
                  val event = awaitPointerEvent()
                  val change = event.changes.firstOrNull { it.id == down.id } ?: break
                  if (!change.pressed) break
                  change.consume()
                  val nx = (change.position.x / w).coerceIn(0f, 1f)
                  val ny = (change.position.y / h).coerceIn(0f, 1f)
                  val last = points.last()
                  if (hypot(nx - last.x, ny - last.y) > 0.004f) {
                    points.add(StrokePoint(nx, ny, change.pressure.coerceIn(0.2f, 1.3f)))
                    currentPayload = currentPayload.copy(points = points.toList())
                    onActiveStrokeLatest(currentPayload)
                  }
                }

                val strokeEl =
                  CanvasElement(
                    id = UUID.randomUUID().toString(),
                    type = "stroke",
                    x = 0f,
                    y = 0f,
                    w = 1f,
                    h = 1f,
                    strokePointsJson = CanvasPayloadCodec.encodeStroke(currentPayload),
                    zIndex = nextZIndex(currentPage),
                  )
                onCommitStrokeLatest(strokeEl)
              }
            }
          }
        },
    contentAlignment = Alignment.Center,
  ) {
    ParchmentPageSurface(
      page = workingPage,
      numeralStyle = numeralStyle,
      selectedElementId = if (toolMode == EditorToolMode.DRAW) null else selectedElementId,
      inlineEditingElementId = inlineEditingElementId,
      inlinePlaceholder = inlinePlaceholder,
      onInlineTextChange = onInlineTextChange,
      activeStrokePreview = activeStroke,
      liveTransformProvider = { currentLiveTransform },
      playingVoiceElementId = playingVoiceElementId,
      onVoiceElementClick = onVoiceElementClick,
    )

    if (inlineEditingElementId != null) {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = SankalanTokens.Walnut900.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, SankalanTokens.AntiqueGold),
        modifier =
          Modifier
            .align(Alignment.BottomEnd)
            .padding(8.dp)
            .clickable(onClick = onCommitInlineText),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = SankalanTokens.GoldHighlight,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text = LocalStrings.current.editorDone,
            fontSize = 11.sp,
            color = SankalanTokens.Cream,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }
  }
}

@Composable
private fun EditorModeTabsStrip(
  toolMode: EditorToolMode,
  onSelectMode: (EditorToolMode) -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val modes =
    listOf(
      Triple(EditorToolMode.SELECT, strings.editorToolSelect, Icons.Outlined.OpenWith),
      Triple(EditorToolMode.TEXT, strings.editorToolText, Icons.Outlined.TextFields),
      Triple(EditorToolMode.DRAW, strings.editorToolDraw, Icons.Outlined.Brush),
      Triple(EditorToolMode.PHOTO, strings.editorToolPhoto, Icons.Outlined.AddPhotoAlternate),
      Triple(EditorToolMode.STUDIO_ASSETS, strings.editorToolStudioAssets, Icons.Outlined.AutoAwesome),
      Triple(EditorToolMode.PAPER, strings.editorToolPaper, Icons.Outlined.Description),
      Triple(EditorToolMode.TEMPLATES, strings.editorToolTemplates, Icons.Outlined.DashboardCustomize),
      Triple(EditorToolMode.VOICE, strings.editorToolVoice, Icons.Outlined.Mic),
      Triple(EditorToolMode.SHAPES, strings.editorToolShapes, Icons.Outlined.Category),
      Triple(EditorToolMode.LAYERS, strings.editorToolLayers, Icons.Outlined.Layers),
      Triple(EditorToolMode.PAGE_INFO, strings.editorToolPageInfo, Icons.Outlined.Info),
    )

  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    modes.forEach { (mode, label, icon) ->
      val selected = toolMode == mode
      Surface(
        shape = MaterialTheme.shapes.small,
        color = if (selected) palette.goldAccent else palette.shelfSurface,
        border = BorderStroke(1.dp, if (selected) palette.goldAccent else palette.borderSubtle),
        modifier =
          Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Tab) { onSelectMode(mode) }
            .testTag("editor_tab_${mode.name.lowercase()}"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) SankalanTokens.Walnut900 else palette.goldAccent,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) SankalanTokens.Walnut900 else palette.textPrimary,
          )
        }
      }
    }
  }
}

@Composable
private fun EditorInspectorContent(
  toolMode: EditorToolMode,
  workingPage: Page,
  selectedElement: CanvasElement?,
  isBangla: Boolean,
  activePen: PenKind,
  penColorHex: String,
  strokeSizeRatio: Float,
  stylusOnlyPalmRejection: Boolean,
  favoriteAssetIds: Set<String>,
  userUploads: List<com.example.features.studio.UserUploadedAsset>,
  voiceManager: VoiceNoteManager,
  playingVoiceElementId: String?,
  onPenChange: (PenKind) -> Unit,
  onColorChange: (String) -> Unit,
  onStrokeSizeChange: (Float) -> Unit,
  onStylusOnlyChange: (Boolean) -> Unit,
  onWriteOnPageClick: () -> Unit,
  onAddDiagramBoxClick: () -> Unit,
  onOpenStickerStudio: () -> Unit,
  onStartDrawMode: () -> Unit,
  onEditSelectedText: (CanvasElement) -> Unit,
  onPickPhotoClick: () -> Unit,
  onEditPhotoCrop: (CanvasElement) -> Unit,
  onAddShape: (ShapeKind) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onInsertStudioAsset: (com.example.features.studio.StudioAsset) -> Unit,
  onInsertUserUpload: (com.example.features.studio.UserUploadedAsset) -> Unit,
  onPickUserUpload: () -> Unit,
  onEditUserUpload: (com.example.features.studio.UserUploadedAsset) -> Unit,
  onApplyBackground: (com.example.core.model.PageBackground) -> Unit,
  onSelectTemplate: (PageTemplate) -> Unit,
  onAttachVoiceNote: (filePath: String, durationSeconds: Int) -> Unit,
  onPlayVoiceElement: (CanvasElement) -> Unit,
  onRotateSelected: (CanvasElement) -> Unit,
  onDeleteSelected: (CanvasElement) -> Unit,
  onSelectLayer: (String) -> Unit,
  onBringForward: (String) -> Unit,
  onSendBackward: (String) -> Unit,
  onToggleLock: (String) -> Unit,
  onToggleHidden: (String) -> Unit,
  onOpacityChange: (String, Float) -> Unit,
  onGroupElements: (List<String>) -> Unit,
  onUngroupElement: (String) -> Unit,
  onUpdateMetadata: (String?, String?, String?, Boolean) -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current

  when (toolMode) {
    EditorToolMode.SELECT -> {
      if (selectedElement == null) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Button(
              onClick = onOpenStickerStudio,
              colors = ButtonDefaults.buttonColors(containerColor = palette.goldAccent, contentColor = SankalanTokens.Walnut900),
              shape = MaterialTheme.shapes.small,
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.minimumInteractiveComponentSize().testTag("editor_quick_add_sticker"),
            ) {
              Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(strings.editorQuickAddSticker, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }

            OutlinedButton(
              onClick = onPickUserUpload,
              border = BorderStroke(1.dp, palette.borderGold),
              shape = MaterialTheme.shapes.small,
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier.minimumInteractiveComponentSize().testTag("editor_quick_sticker_from_photo"),
            ) {
              Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(strings.editorQuickStickerFromPhoto, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }

            OutlinedButton(
              onClick = onWriteOnPageClick,
              border = BorderStroke(1.dp, palette.borderSubtle),
              shape = MaterialTheme.shapes.small,
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier.minimumInteractiveComponentSize().testTag("editor_quick_add_text"),
            ) {
              Icon(Icons.Outlined.TextFields, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(strings.editorWriteOnPage, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }

            OutlinedButton(
              onClick = onPickPhotoClick,
              border = BorderStroke(1.dp, palette.borderSubtle),
              shape = MaterialTheme.shapes.small,
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier.minimumInteractiveComponentSize().testTag("editor_quick_add_photo"),
            ) {
              Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(strings.editorToolPhoto, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }

            OutlinedButton(
              onClick = onStartDrawMode,
              border = BorderStroke(1.dp, palette.borderSubtle),
              shape = MaterialTheme.shapes.small,
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(Icons.Outlined.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(strings.editorToolDraw, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
          }

          Text(
            text = strings.editorRotateHandleHint,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary,
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = strings.editorRotateHandleHint,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary,
          )
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (selectedElement.type == "text") {
              Button(
                onClick = { onEditSelectedText(selectedElement) },
                colors = ButtonDefaults.buttonColors(containerColor = palette.goldAccent, contentColor = SankalanTokens.Walnut900),
              ) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.editPage)
              }
            }
            if (selectedElement.type == "image") {
              Button(
                onClick = { onEditPhotoCrop(selectedElement) },
                colors = ButtonDefaults.buttonColors(containerColor = palette.goldAccent, contentColor = SankalanTokens.Walnut900),
              ) {
                Icon(Icons.Outlined.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.editorCropAndFrame)
              }
            }
            if (selectedElement.type == "voice") {
              val isPlaying = selectedElement.id == playingVoiceElementId
              Button(
                onClick = { onPlayVoiceElement(selectedElement) },
                colors = ButtonDefaults.buttonColors(containerColor = palette.goldAccent, contentColor = SankalanTokens.Walnut900),
              ) {
                Icon(
                  imageVector = if (isPlaying) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isPlaying) strings.voiceStopPlayLabel else strings.voicePlayLabel)
              }
            }
            OutlinedButton(
              onClick = { onRotateSelected(selectedElement) },
              border = BorderStroke(1.dp, palette.borderGold),
            ) {
              Icon(Icons.Outlined.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("${strings.editorRotate} +15°")
            }
            OutlinedButton(
              onClick = { onDeleteSelected(selectedElement) },
              border = BorderStroke(1.dp, palette.borderSubtle),
            ) {
              Icon(
                Icons.Outlined.DeleteOutline,
                contentDescription = strings.delete,
                tint = if (palette.isDark) SankalanTokens.ErrorDark else SankalanTokens.ErrorLight,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(strings.delete)
            }
          }
        }
      }
    }
    EditorToolMode.TEXT -> {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Section 1: Natural Line-by-Line Page Writing (Cursor inside page margins)
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = strings.editorWriteOnPage,
              style = MaterialTheme.typography.labelLarge,
              color = palette.goldAccent,
            )
            Text(
              text = strings.editorPageWritingHint,
              style = MaterialTheme.typography.bodySmall,
              color = palette.textSecondary,
            )
          }
          Button(
            onClick = onWriteOnPageClick,
            colors = ButtonDefaults.buttonColors(containerColor = palette.goldAccent, contentColor = SankalanTokens.Walnut900),
            modifier = Modifier.minimumInteractiveComponentSize().testTag("editor_write_on_page_button"),
          ) {
            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(strings.editorWriteOnPage, maxLines = 1)
          }
        }

        // Section 2: Separate Diagram & Floating Text Box System
        Surface(
          shape = MaterialTheme.shapes.small,
          color = palette.shelfSurface,
          border = BorderStroke(1.dp, palette.borderSubtle),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = strings.editorDiagramSectionTitle,
              style = MaterialTheme.typography.labelMedium,
              color = palette.textPrimary,
            )
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              OutlinedButton(
                onClick = onAddDiagramBoxClick,
                border = BorderStroke(1.dp, palette.borderGold),
                modifier = Modifier.minimumInteractiveComponentSize().testTag("editor_add_diagram_box_button"),
              ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.editorAddDiagramBox, maxLines = 1)
              }
              if (selectedElement != null && isDiagramTextBox(selectedElement)) {
                OutlinedButton(
                  onClick = { onEditSelectedText(selectedElement) },
                  border = BorderStroke(1.dp, palette.borderSubtle),
                  modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                  Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(strings.editorEditTextTitle, maxLines = 1)
                }
              }
            }
          }
        }
      }
    }
    EditorToolMode.DRAW -> {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          listOf(
            PenKind.PEN to strings.editorPenInk,
            PenKind.PENCIL to strings.editorPenPencil,
            PenKind.MARKER to strings.editorPenMarker,
            PenKind.HIGHLIGHTER to strings.editorPenHighlighter,
            PenKind.ERASER to strings.editorEraser,
          ).forEach { (kind, label) ->
            FilterChip(
              selected = activePen == kind,
              onClick = { onPenChange(kind) },
              label = { Text(label) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = palette.goldAccent,
                  selectedLabelColor = SankalanTokens.Walnut900,
                ),
            )
          }
        }

        if (activePen != PenKind.ERASER) {
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            EDITOR_INK_SWATCHES.forEach { hex ->
              val selected = penColorHex == hex
              Box(
                modifier =
                  Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(parseHexColor(hex))
                    .border(
                      width = if (selected) 2.5.dp else 1.dp,
                      color = if (selected) palette.goldHighlight else palette.borderSubtle,
                      shape = CircleShape,
                    )
                    .clickable { onColorChange(hex) }
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.editorStrokeSize, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
            Slider(
              value = strokeSizeRatio,
              onValueChange = onStrokeSizeChange,
              valueRange = 0.004f..0.028f,
              colors = SliderDefaults.colors(thumbColor = palette.goldAccent, activeTrackColor = palette.goldAccent),
              modifier = Modifier.weight(1f),
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(strings.editorStylusOnly, style = MaterialTheme.typography.labelMedium, color = palette.textPrimary)
            Text(strings.editorStylusOnlyHint, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
          }
          Switch(
            checked = stylusOnlyPalmRejection,
            onCheckedChange = onStylusOnlyChange,
            colors = SwitchDefaults.colors(checkedThumbColor = SankalanTokens.Walnut900, checkedTrackColor = palette.goldAccent),
          )
        }
      }
    }
    EditorToolMode.PHOTO -> {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
          onClick = onPickPhotoClick,
          colors = ButtonDefaults.buttonColors(containerColor = palette.goldAccent, contentColor = SankalanTokens.Walnut900),
          modifier = Modifier.testTag("editor_pick_photo_button"),
        ) {
          Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(strings.editorPickPhoto)
        }
        if (selectedElement?.type == "image") {
          OutlinedButton(onClick = { onEditPhotoCrop(selectedElement) }) {
            Text(strings.editorCropAndFrame)
          }
        }
      }
    }
    EditorToolMode.STUDIO_ASSETS -> {
      StudioAssetPackPanel(
        isBangla = isBangla,
        favoriteIds = favoriteAssetIds,
        userUploads = userUploads,
        onToggleFavorite = onToggleFavorite,
        onInsertAsset = onInsertStudioAsset,
        onInsertUpload = onInsertUserUpload,
        onPickUserUpload = onPickUserUpload,
        onEditUpload = onEditUserUpload,
      )
    }
    EditorToolMode.PAPER -> {
      PaperSelectorPanel(
        currentBackground = workingPage.background,
        isBangla = isBangla,
        favoriteIds = favoriteAssetIds,
        onToggleFavorite = onToggleFavorite,
        onApplyBackground = onApplyBackground,
      )
    }
    EditorToolMode.TEMPLATES -> {
      TemplateSelectorPanel(
        isBangla = isBangla,
        favoriteIds = favoriteAssetIds,
        onToggleFavorite = onToggleFavorite,
        onSelectTemplate = onSelectTemplate,
      )
    }
    EditorToolMode.VOICE -> {
      VoiceRecorderPanel(
        voiceManager = voiceManager,
        existingVoiceElements = workingPage.elements.filter { it.type == "voice" },
        playingVoiceId = playingVoiceElementId,
        onAttachVoiceNote = onAttachVoiceNote,
        onPlayVoiceElement = onPlayVoiceElement,
      )
    }
    EditorToolMode.SHAPES -> {
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ShapeKind.entries.forEach { kind ->
          OutlinedButton(
            onClick = { onAddShape(kind) },
            border = BorderStroke(1.dp, palette.borderGold),
          ) {
            Text(kind.label)
          }
        }
      }
    }
    EditorToolMode.LAYERS -> {
      LayerManagerPanel(
        elements = workingPage.elements,
        selectedElementId = selectedElement?.id,
        onSelectElement = onSelectLayer,
        onBringForward = onBringForward,
        onSendBackward = onSendBackward,
        onToggleLock = onToggleLock,
        onToggleHidden = onToggleHidden,
        onOpacityChange = onOpacityChange,
        onGroupElements = onGroupElements,
        onUngroupElement = onUngroupElement,
        onDeleteElement = { id ->
          workingPage.elements.firstOrNull { it.id == id }?.let(onDeleteSelected)
        },
      )
    }
    EditorToolMode.PAGE_INFO -> {
      PageInfoPanel(page = workingPage, onUpdateMetadata = onUpdateMetadata)
    }
  }
}

private fun shiftLayerZOrder(
  elements: List<CanvasElement>,
  targetId: String,
  forward: Boolean,
): List<CanvasElement> {
  val sorted = elements.sortedBy { it.zIndex }.toMutableList()
  val idx = sorted.indexOfFirst { it.id == targetId }
  if (idx < 0) return elements
  val swapIdx = if (forward) idx + 1 else idx - 1
  if (swapIdx !in sorted.indices) return elements
  val item = sorted.removeAt(idx)
  sorted.add(swapIdx, item)
  return sorted.mapIndexed { orderIdx, el -> el.copy(zIndex = orderIdx) }
}
