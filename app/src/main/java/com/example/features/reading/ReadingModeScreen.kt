package com.example.features.reading

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.ZoomOutMap
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.i18n.LocalStrings
import com.example.core.i18n.formatNumber
import com.example.core.model.AppSettings
import com.example.core.model.DiaryWithPages
import com.example.core.model.NumeralStyle
import com.example.core.model.ReadingLayout
import com.example.features.bookshelf.BookCoverArt
import com.example.features.editor.ParchmentPageSurface
import com.example.features.editor.VoiceNoteManager
import com.example.ui.theme.LocalSankalanPalette
import com.example.ui.theme.SankalanSpacing
import com.example.ui.theme.SankalanTokens
import kotlin.math.abs
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun ReadingModeScreen(
  diaryWithPages: DiaryWithPages,
  settings: AppSettings,
  initialPageId: String?,
  onDismiss: () -> Unit,
  onOpenPageManager: (currentPageId: String?) -> Unit,
  onRequestEditPage: (diaryId: String, pageId: String) -> Unit,
  onRequestExport: (activePageId: String?) -> Unit = {},
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val diary = diaryWithPages.diary
  val pages = remember(diaryWithPages.pages) { diaryWithPages.pages.sortedBy { it.order } }

  var activeLayout by remember(settings.readingLayout) { mutableStateOf(settings.readingLayout) }
  var preservedPageId by remember(diary.id, initialPageId) { mutableStateOf(initialPageId) }

  val slots =
    remember(diary, pages, activeLayout) {
      ReadingSlotBuilder.buildSlots(diary = diary, activePages = pages, layout = activeLayout)
    }

  var currentSlotIndex by remember(diary.id) {
    mutableIntStateOf(ReadingSlotBuilder.findSlotIndexForPageId(slots, initialPageId))
  }

  // Re-sync slot index whenever slots rebuild (layout switch or Page Manager reorder) while preserving Page.id!
  LaunchedEffect(slots, preservedPageId) {
    val targetIdx = ReadingSlotBuilder.findSlotIndexForPageId(slots, preservedPageId)
    if (preservedPageId != null) {
      currentSlotIndex = targetIdx.coerceIn(0, slots.lastIndex)
    } else if (currentSlotIndex > slots.lastIndex) {
      currentSlotIndex = slots.lastIndex.coerceAtLeast(0)
    }
  }

  val currentSlot = slots.getOrNull(currentSlotIndex)
  val currentPageId = ReadingSlotBuilder.primaryPageId(currentSlot)

  // Offline paper-turn SoundPool player & voice memory clip player
  val soundPlayer = remember { PageTurnSoundPlayer(context) }
  val voiceManager = remember(context) { VoiceNoteManager(context) }
  var playingVoiceElementId by remember { mutableStateOf<String?>(null) }
  DisposableEffect(Unit) {
    onDispose {
      soundPlayer.release()
      voiceManager.release()
    }
  }

  var isFullscreen by remember { mutableStateOf(false) }
  var zoomScale by remember { mutableFloatStateOf(1f) }
  var panOffset by remember { mutableStateOf(Offset.Zero) }

  // Interactive Page-Curl drag state (-1f..+1f; negative = curling forward to next slot)
  val curlProgress = remember { Animatable(0f) }
  var dragStartYFraction by remember { mutableFloatStateOf(0.5f) }
  var transitionJob by remember { mutableStateOf<Job?>(null) }
  val useCurl = settings.pageTurnAnimation && !settings.liteMode && !settings.reducedMotion

  val latestSlotIndex = rememberUpdatedState(currentSlotIndex)
  val latestSlots = rememberUpdatedState(slots)
  val latestUseCurl = rememberUpdatedState(useCurl)
  val latestZoomScale = rememberUpdatedState(zoomScale)
  val latestPanOffset = rememberUpdatedState(panOffset)

  fun goToSlot(newIndex: Int) {
    val slotList = latestSlots.value
    val curIdx = latestSlotIndex.value
    val clamped = newIndex.coerceIn(0, slotList.lastIndex.coerceAtLeast(0))
    if (clamped == curIdx || slotList.isEmpty()) {
      transitionJob?.cancel()
      transitionJob = scope.launch { curlProgress.animateTo(0f, tween(160)) }
      return
    }
    val direction = if (clamped > curIdx) -1f else 1f
    transitionJob?.cancel()
    transitionJob =
      scope.launch {
        val remainingRatio = (1f - abs(curlProgress.value)).coerceIn(0.15f, 1f)
        val durationMs = ((if (latestUseCurl.value) 230 else 160) * remainingRatio).toInt().coerceIn(80, 250)
        curlProgress.animateTo(
          targetValue = direction,
          animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
        )
        currentSlotIndex = clamped
        preservedPageId = ReadingSlotBuilder.primaryPageId(slotList[clamped])
        zoomScale = 1f
        panOffset = Offset.Zero
        voiceManager.stopPlayback()
        playingVoiceElementId = null
        soundPlayer.playIfEnabled(settings.pageTurnSound)
        curlProgress.snapTo(0f)
      }
  }

  BackHandler {
    if (isFullscreen) {
      isFullscreen = false
    } else {
      onDismiss()
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      color = palette.background,
      contentColor = palette.textPrimary,
      modifier = Modifier.fillMaxSize(),
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          // Top Reading HUD
          AnimatedVisibility(
            visible = !isFullscreen,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
          ) {
            Surface(
              color = palette.shelfSurface,
              border = BorderStroke(1.dp, palette.borderGold),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SankalanSpacing.md, vertical = SankalanSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                IconButton(
                  onClick = onDismiss,
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("reading_close_button"),
                ) {
                  Icon(Icons.Outlined.Close, contentDescription = strings.closeLabel, tint = palette.textPrimary)
                }

                Column(modifier = Modifier.weight(1f).padding(horizontal = SankalanSpacing.xs)) {
                  Text(
                    text = diary.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = formatSlotLabel(currentSlot, settings.numeralStyle),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.goldAccent,
                  )
                }

                // Single / Spread Toggle
                OutlinedButton(
                  onClick = {
                    val savedId = currentPageId
                    activeLayout =
                      if (activeLayout == ReadingLayout.SINGLE) ReadingLayout.SPREAD
                      else ReadingLayout.SINGLE
                    preservedPageId = savedId
                  },
                  border = BorderStroke(1.dp, palette.borderGold),
                  shape = MaterialTheme.shapes.small,
                  contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("reading_layout_toggle"),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                    contentDescription = strings.readingLayoutLabel,
                    tint = palette.goldAccent,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text =
                      if (activeLayout == ReadingLayout.SINGLE) strings.singlePageLayout
                      else strings.twoPageSpreadLayout,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textPrimary,
                  )
                }

                // Page Manager Shortcut
                IconButton(
                  onClick = { onOpenPageManager(currentPageId) },
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("reading_open_page_manager"),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.CollectionsBookmark,
                    contentDescription = strings.pageManager,
                    tint = palette.goldAccent,
                  )
                }

                // Edit Current Page (launches full layered CanvasEditorScreen preserving exact Page.id)
                if (currentPageId != null) {
                  IconButton(
                    onClick = { onRequestEditPage(diary.id, currentPageId) },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("reading_edit_page_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Edit,
                      contentDescription = strings.editPage,
                      tint = palette.goldAccent,
                    )
                  }
                }

                // Export Current Diary / Page
                IconButton(
                  onClick = { onRequestExport(currentPageId) },
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("reading_export_button"),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.PictureAsPdf,
                    contentDescription = strings.exportDiaryTitle,
                    tint = palette.goldAccent,
                  )
                }

                // Fullscreen Toggle
                IconButton(
                  onClick = { isFullscreen = true },
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("reading_fullscreen_button"),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Fullscreen,
                    contentDescription = strings.toggleFullscreenLabel,
                    tint = palette.textPrimary,
                  )
                }
              }
            }
          }

          // Center Interactive Book Stage (Unified Corner/Edge Drag Curl + Pinch Zoom + Double-Tap Zoom)
          Box(
            modifier =
              Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 980.dp)
                .padding(
                  horizontal = if (isFullscreen) SankalanSpacing.sm else SankalanSpacing.md,
                  vertical = if (isFullscreen) SankalanSpacing.sm else SankalanSpacing.md,
                )
                .testTag("reading_book_stage")
                .pointerInput(Unit) {
                  awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPos = down.position
                    val stageW = size.width.coerceAtLeast(1).toFloat()
                    val stageH = size.height.coerceAtLeast(1).toFloat()
                    val startYFrac = (startPos.y / stageH).coerceIn(0.1f, 0.9f)
                    val nearCornerOrEdge = startPos.x <= stageW * 0.24f || startPos.x >= stageW * 0.76f
                    val velocityTracker = VelocityTracker().apply {
                      addPosition(down.uptimeMillis, startPos)
                    }
                    var mode = 0 // 0=undecided, 1=page_turn, 2=pan_zoomed, 3=pinch_zoom
                    while (true) {
                      val event = awaitPointerEvent()
                      val pressed = event.changes.filter { it.pressed }
                      if (pressed.isEmpty()) break

                      if (pressed.size >= 2) {
                        mode = 3
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        val newZoom = (latestZoomScale.value * zoomChange).coerceIn(1f, 3f)
                        zoomScale = newZoom
                        panOffset =
                          if (newZoom <= 1.02f) Offset.Zero
                          else {
                            val maxPanX = size.width * (newZoom - 1f) * 0.45f
                            val maxPanY = size.height * (newZoom - 1f) * 0.45f
                            Offset(
                              x = (latestPanOffset.value.x + panChange.x).coerceIn(-maxPanX, maxPanX),
                              y = (latestPanOffset.value.y + panChange.y).coerceIn(-maxPanY, maxPanY),
                            )
                          }
                        event.changes.forEach { it.consume() }
                        if (abs(curlProgress.value) > 0.005f) {
                          transitionJob?.cancel()
                          transitionJob = scope.launch { curlProgress.animateTo(0f, tween(120)) }
                        }
                      } else {
                        val change = pressed.first()
                        val delta = change.positionChange()
                        if (mode == 3) {
                          // Stay in pinch mode until all fingers lift
                          change.consume()
                        } else if (latestZoomScale.value > 1.05f) {
                          val totalOffset = change.position - startPos
                          if (mode == 2 || totalOffset.getDistance() > viewConfiguration.touchSlop) {
                            mode = 2
                            val curZoom = latestZoomScale.value
                            val maxPanX = size.width * (curZoom - 1f) * 0.45f
                            val maxPanY = size.height * (curZoom - 1f) * 0.45f
                            panOffset =
                              Offset(
                                x = (latestPanOffset.value.x + delta.x).coerceIn(-maxPanX, maxPanX),
                                y = (latestPanOffset.value.y + delta.y).coerceIn(-maxPanY, maxPanY),
                              )
                            change.consume()
                          }
                        } else {
                          if (mode == 0) {
                            val totalOffset = change.position - startPos
                            if (totalOffset.getDistance() > viewConfiguration.touchSlop &&
                              PageTurnPhysics.isPageTurnDragSlope(totalOffset.x, totalOffset.y, nearCornerOrEdge)
                            ) {
                              mode = 1
                              dragStartYFraction = startYFrac
                              transitionJob?.cancel()
                              val lastIdx = latestSlots.value.lastIndex
                              val curIdx = latestSlotIndex.value
                              val next =
                                PageTurnPhysics.computeDragProgress(
                                  dragDeltaPx = totalOffset.x,
                                  stageWidthPx = stageW,
                                  currentProgress = curlProgress.value,
                                  canFlipForward = curIdx < lastIdx,
                                  canFlipBackward = curIdx > 0,
                                )
                              scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                curlProgress.snapTo(next)
                              }
                              velocityTracker.addPosition(change.uptimeMillis, change.position)
                              change.consume()
                            }
                          } else if (mode == 1) {
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            change.consume()
                            val lastIdx = latestSlots.value.lastIndex
                            val curIdx = latestSlotIndex.value
                            val next =
                              PageTurnPhysics.computeDragProgress(
                                dragDeltaPx = delta.x,
                                stageWidthPx = stageW,
                                currentProgress = curlProgress.value,
                                canFlipForward = curIdx < lastIdx,
                                canFlipBackward = curIdx > 0,
                              )
                            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                              curlProgress.snapTo(next)
                            }
                          }
                        }
                      }
                    }

                    if (mode == 1) {
                      val velocityX = velocityTracker.calculateVelocity().x
                      val curIdx = latestSlotIndex.value
                      val lastIdx = latestSlots.value.lastIndex
                      val targetIdx =
                        PageTurnPhysics.resolveSwipeOutcome(
                          progress = curlProgress.value,
                          velocityX = velocityX,
                          currentSlotIndex = curIdx,
                          lastSlotIndex = lastIdx,
                        )
                      if (targetIdx != curIdx) {
                        goToSlot(targetIdx)
                      } else {
                        transitionJob?.cancel()
                        transitionJob =
                          scope.launch {
                            curlProgress.animateTo(
                              targetValue = 0f,
                              animationSpec = spring(stiffness = 520f, dampingRatio = 0.84f),
                            )
                          }
                      }
                    }
                  }
                }
                .pointerInput(Unit) {
                  detectTapGestures(
                    onDoubleTap = {
                      if (latestZoomScale.value > 1.05f) {
                        zoomScale = 1f
                        panOffset = Offset.Zero
                      } else {
                        zoomScale = 1.85f
                      }
                    },
                    onTap = { tapOffset ->
                      val curIdx = latestSlotIndex.value
                      val lastIdx = latestSlots.value.lastIndex
                      val w = size.width.toFloat().coerceAtLeast(1f)
                      val h = size.height.toFloat().coerceAtLeast(1f)
                      val tappedRightCorner =
                        latestZoomScale.value <= 1.05f &&
                          tapOffset.x >= w * 0.78f &&
                          tapOffset.y >= h * 0.78f &&
                          curIdx < lastIdx
                      val tappedLeftCorner =
                        latestZoomScale.value <= 1.05f &&
                          tapOffset.x <= w * 0.22f &&
                          tapOffset.y >= h * 0.78f &&
                          curIdx > 0
                      when {
                        tappedRightCorner -> {
                          dragStartYFraction = 0.85f
                          goToSlot(curIdx + 1)
                        }
                        tappedLeftCorner -> {
                          dragStartYFraction = 0.85f
                          goToSlot(curIdx - 1)
                        }
                        else -> isFullscreen = !isFullscreen
                      }
                    },
                  )
                },
            contentAlignment = Alignment.Center,
          ) {
            val dragNorm = curlProgress.value
            val absCurl = abs(dragNorm).coerceIn(0f, 1f)
            val underneathSlot =
              when {
                dragNorm < -0.002f -> slots.getOrNull(currentSlotIndex + 1)
                dragNorm > 0.002f -> slots.getOrNull(currentSlotIndex - 1)
                else -> null
              }
            val onVoiceClick: (com.example.core.model.CanvasElement) -> Unit = { el ->
              voiceManager.togglePlay(el.id, el.blobRef) { activeId ->
                playingVoiceElementId = activeId
              }
            }

            // Underneath revealing page (next or previous slot revealed while peeling top page)
            if (underneathSlot != null) {
              Box(
                modifier =
                  Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                      val revealScale = 0.96f + 0.04f * absCurl
                      scaleX = revealScale
                      scaleY = revealScale
                      alpha = (0.62f + 0.38f * absCurl).coerceIn(0f, 1f)
                    },
                contentAlignment = Alignment.Center,
              ) {
                SlotContentRenderer(
                  slot = underneathSlot,
                  numeralStyle = settings.numeralStyle,
                  playingVoiceElementId = playingVoiceElementId,
                  onVoiceElementClick = onVoiceClick,
                )
                Box(
                  modifier =
                    Modifier
                      .fillMaxSize()
                      .background(Color.Black.copy(alpha = (1f - absCurl) * 0.26f))
                )
              }
            }

            // Active top page (follows finger drag 1:1 from any corner or edge)
            Box(
              modifier =
                Modifier
                  .fillMaxSize()
                  .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = panOffset.x + dragNorm * size.width * 0.92f
                    translationY = panOffset.y
                    if (useCurl && absCurl > 0.002f) {
                      rotationY = dragNorm * 24f
                      rotationZ = dragNorm * (dragStartYFraction - 0.5f) * 14f
                      cameraDistance = 16f * density
                      transformOrigin =
                        if (dragNorm < 0f) TransformOrigin(0f, dragStartYFraction)
                        else TransformOrigin(1f, dragStartYFraction)
                      alpha = (1f - ((absCurl - 0.72f).coerceAtLeast(0f) / 0.28f) * 0.65f).coerceIn(0.35f, 1f)
                    } else if (!useCurl && absCurl > 0.002f) {
                      alpha = (1f - absCurl * 0.35f).coerceIn(0.45f, 1f)
                    }
                  },
              contentAlignment = Alignment.Center,
            ) {
              SlotContentRenderer(
                slot = currentSlot,
                numeralStyle = settings.numeralStyle,
                playingVoiceElementId = playingVoiceElementId,
                onVoiceElementClick = onVoiceClick,
              )

              // Realistic Page Curl Fold Highlight + Cast Drop Shadow Overlay
              if (useCurl && absCurl > 0.02f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                  val curlsFromRight = dragNorm < 0f
                  val curlX =
                    if (curlsFromRight) size.width * (1f - absCurl * 0.55f)
                    else size.width * (absCurl * 0.55f)
                  val bandW = 48.dp.toPx() * (0.45f + absCurl * 0.55f)

                  // Warm folded back-paper edge highlight & shadow
                  drawRect(
                    brush =
                      Brush.horizontalGradient(
                        colors =
                          listOf(
                            Color.Transparent,
                            SankalanTokens.Cream.copy(alpha = 0.55f * absCurl),
                            SankalanTokens.PaperShadow.copy(alpha = 0.72f * absCurl),
                            Color.Black.copy(alpha = 0.30f * absCurl),
                            Color.Transparent,
                          ),
                        startX = (curlX - bandW).coerceAtLeast(0f),
                        endX = (curlX + bandW).coerceAtMost(size.width),
                      ),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, size.height),
                  )
                }
              }
            }

            // Visual Corner Dog-Ear & Left-Edge Curl Cues when unzoomed and at rest
            if (zoomScale <= 1.05f && absCurl < 0.03f) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                val cornerSize = 26.dp.toPx()
                if (currentSlotIndex < slots.lastIndex) {
                  val brShadow =
                    Path().apply {
                      moveTo(size.width - cornerSize - 4.dp.toPx(), size.height)
                      lineTo(size.width, size.height - cornerSize - 4.dp.toPx())
                      lineTo(size.width, size.height)
                      close()
                    }
                  drawPath(brShadow, color = Color.Black.copy(alpha = 0.22f))
                  val brFold =
                    Path().apply {
                      moveTo(size.width - cornerSize, size.height)
                      lineTo(size.width - cornerSize * 0.24f, size.height - cornerSize * 0.24f)
                      lineTo(size.width, size.height - cornerSize)
                      close()
                    }
                  drawPath(brFold, color = SankalanTokens.AntiqueGold.copy(alpha = 0.85f))
                }
                if (currentSlotIndex > 0) {
                  val blShadow =
                    Path().apply {
                      moveTo(0f, size.height - cornerSize - 4.dp.toPx())
                      lineTo(cornerSize + 4.dp.toPx(), size.height)
                      lineTo(0f, size.height)
                      close()
                    }
                  drawPath(blShadow, color = Color.Black.copy(alpha = 0.18f))
                  val blFold =
                    Path().apply {
                      moveTo(0f, size.height - cornerSize)
                      lineTo(cornerSize * 0.24f, size.height - cornerSize * 0.24f)
                      lineTo(cornerSize, size.height)
                      close()
                    }
                  drawPath(blFold, color = SankalanTokens.AntiqueGold.copy(alpha = 0.72f))
                }
              }
            }

            // Floating Fullscreen Exit + Zoom Reset Chips
            if (zoomScale > 1.05f || isFullscreen) {
              Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(SankalanSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.xs),
              ) {
                if (zoomScale > 1.05f) {
                  Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.shelfSurface.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, palette.borderGold),
                    onClick = {
                      zoomScale = 1f
                      panOffset = Offset.Zero
                    },
                    modifier = Modifier.testTag("reading_reset_zoom"),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      Icon(Icons.Outlined.ZoomOutMap, contentDescription = null, tint = palette.goldAccent, modifier = Modifier.size(16.dp))
                      Text(text = strings.resetZoomLabel, style = MaterialTheme.typography.labelSmall)
                    }
                  }
                }
                if (isFullscreen) {
                  Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.shelfSurface.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, palette.borderGold),
                    onClick = { isFullscreen = false },
                    modifier = Modifier.testTag("reading_exit_fullscreen"),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.FullscreenExit,
                      contentDescription = strings.toggleFullscreenLabel,
                      tint = palette.goldAccent,
                      modifier = Modifier.padding(8.dp).size(18.dp),
                    )
                  }
                }
              }
            }
          }

          // Bottom Prev / Hint / Next Controls
          AnimatedVisibility(
            visible = !isFullscreen,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
          ) {
            Column(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .widthIn(max = 980.dp)
                  .padding(horizontal = SankalanSpacing.lg, vertical = SankalanSpacing.sm),
              verticalArrangement = Arrangement.spacedBy(SankalanSpacing.xs),
            ) {
              Text(
                text = strings.tapToToggleHudHint,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                OutlinedButton(
                  onClick = { goToSlot(currentSlotIndex - 1) },
                  enabled = currentSlotIndex > 0,
                  border = BorderStroke(1.dp, palette.borderGold),
                  shape = MaterialTheme.shapes.small,
                  modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("reading_prev_button"),
                ) {
                  Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(text = strings.prevPageLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                  onClick = { goToSlot(0) },
                  enabled = currentSlotIndex != 0,
                  border = BorderStroke(1.dp, palette.borderSubtle),
                  shape = MaterialTheme.shapes.small,
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("reading_cover_button"),
                ) {
                  Text(text = strings.coverLabel)
                }

                Button(
                  onClick = { goToSlot(currentSlotIndex + 1) },
                  enabled = currentSlotIndex < slots.lastIndex,
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = palette.goldAccent,
                      contentColor = SankalanTokens.Walnut900,
                    ),
                  shape = MaterialTheme.shapes.small,
                  modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("reading_next_button"),
                ) {
                  Text(text = strings.nextPageLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  Spacer(modifier = Modifier.width(6.dp))
                  Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SlotContentRenderer(
  slot: ReadingSlot?,
  numeralStyle: NumeralStyle,
  playingVoiceElementId: String?,
  onVoiceElementClick: (com.example.core.model.CanvasElement) -> Unit,
) {
  when (slot) {
    is ReadingSlot.FrontCover -> {
      Box(modifier = Modifier.fillMaxSize().widthIn(max = 560.dp), contentAlignment = Alignment.Center) {
        BookCoverArt(
          diary = slot.diary,
          pageCount = slot.totalPages,
          numeralStyle = numeralStyle,
          isBack = false,
          modifier = Modifier.aspectRatio(0.72f, matchHeightConstraintsFirst = false),
        )
      }
    }
    is ReadingSlot.BackCover -> {
      Box(modifier = Modifier.fillMaxSize().widthIn(max = 560.dp), contentAlignment = Alignment.Center) {
        BookCoverArt(
          diary = slot.diary,
          pageCount = slot.totalPages,
          numeralStyle = numeralStyle,
          isBack = true,
          modifier = Modifier.aspectRatio(0.72f, matchHeightConstraintsFirst = false),
        )
      }
    }
    is ReadingSlot.SinglePage -> {
      Box(modifier = Modifier.fillMaxSize().widthIn(max = 560.dp), contentAlignment = Alignment.Center) {
        ParchmentPageSurface(
          page = slot.page,
          numeralStyle = numeralStyle,
          playingVoiceElementId = playingVoiceElementId,
          onVoiceElementClick = onVoiceElementClick,
        )
      }
    }
    is ReadingSlot.SpreadPages -> {
      Row(
        modifier =
          Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(SankalanTokens.Walnut800)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
          ParchmentPageSurface(
            page = slot.leftPage,
            numeralStyle = numeralStyle,
            playingVoiceElementId = playingVoiceElementId,
            onVoiceElementClick = onVoiceElementClick,
          )
        }
        // Realistic Center Book Spine Crease
        Box(
          modifier =
            Modifier
              .width(8.dp)
              .fillMaxHeight()
              .background(
                Brush.horizontalGradient(
                  colors = listOf(SankalanTokens.PaperShadow, Color(0xFF7B634C), SankalanTokens.PaperShadow)
                )
              )
        )
        Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
          ParchmentPageSurface(
            page = slot.rightPage,
            numeralStyle = numeralStyle,
            playingVoiceElementId = playingVoiceElementId,
            onVoiceElementClick = onVoiceElementClick,
          )
        }
      }
    }
    null -> Unit
  }
}

@Composable
private fun formatSlotLabel(slot: ReadingSlot?, numeralStyle: NumeralStyle): String {
  val strings = LocalStrings.current
  return when (slot) {
    is ReadingSlot.FrontCover -> strings.frontCoverTag
    is ReadingSlot.BackCover -> strings.backCoverTag
    is ReadingSlot.SinglePage ->
      "${strings.pageNumberPrefix} ${formatNumber(slot.pageIndex + 1, numeralStyle)} / ${formatNumber(slot.totalPages, numeralStyle)}"
    is ReadingSlot.SpreadPages ->
      "${strings.spreadLabel} ${formatNumber(slot.leftIndex + 1, numeralStyle)}–${formatNumber(slot.rightIndex + 1, numeralStyle)} / ${formatNumber(slot.totalPages, numeralStyle)}"
    null -> ""
  }
}
