package com.example.features.memorylane

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.LocalStrings
import com.example.core.i18n.formatDigits
import com.example.core.i18n.formatNumber
import com.example.core.model.CoverStyle
import com.example.core.model.Diary
import com.example.core.model.DiaryWithPages
import com.example.core.model.NumeralStyle
import com.example.core.model.Page
import com.example.features.bookshelf.BookCoverArt
import com.example.features.bookshelf.parseHexColor
import com.example.ui.theme.LocalSankalanPalette
import com.example.ui.theme.NotoSerifBengaliFontFamily
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.SankalanSpacing
import com.example.ui.theme.SankalanTokens
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class MemoryLaneReason {
  PINNED,
  ON_THIS_DAY,
  CURATED,
}

data class MemoryLaneEntry(
  val diaryWithPages: DiaryWithPages,
  val page: Page,
  val pageIndex: Int,
  val reason: MemoryLaneReason,
  val weight: Int,
)

/**
 * Pure 3D geometry & lighting state for a single frame of a spine-hinged page-turn animation.
 */
data class MemoryPageTurnFrame(
  val showBackFace: Boolean,
  val leafOnLeftHalf: Boolean,
  val rotationYDeg: Float,
  val pivotX: Float,
  val shadowAlpha: Float,
  val highlightAlpha: Float,
  val liftScale: Float,
  val liftTranslationYDp: Float,
)

object MemoryPageTurnMath {
  /**
   * Computes Compose `graphicsLayer` rotation, spine pivot, specular curl lighting, and subtle
   * vertical lift for a realistic 2-sided 3D page turn across the book's center spine.
   *
   * @param progress normalized animation/drag progress in `0f..1f`
   * @param direction `+1` for forward (right page flips onto left page), `-1` for backward
   */
  fun computeFrame(progress: Float, direction: Int): MemoryPageTurnFrame {
    val p = progress.coerceIn(0f, 1f)
    val arc = sin(p * PI).toFloat().coerceIn(0f, 1f)
    val forward = direction >= 0

    val showBackFace: Boolean
    val leafOnLeftHalf: Boolean
    val rotationYDeg: Float
    val pivotX: Float

    if (forward) {
      if (p < 0.5f) {
        val phase = p / 0.5f
        showBackFace = false
        leafOnLeftHalf = false
        rotationYDeg = -90f * phase
        pivotX = 0f
      } else {
        val phase = (p - 0.5f) / 0.5f
        showBackFace = true
        leafOnLeftHalf = true
        rotationYDeg = 90f * (1f - phase)
        pivotX = 1f
      }
    } else {
      if (p < 0.5f) {
        val phase = p / 0.5f
        showBackFace = false
        leafOnLeftHalf = true
        rotationYDeg = 90f * phase
        pivotX = 1f
      } else {
        val phase = (p - 0.5f) / 0.5f
        showBackFace = true
        leafOnLeftHalf = false
        rotationYDeg = -90f * (1f - phase)
        pivotX = 0f
      }
    }

    return MemoryPageTurnFrame(
      showBackFace = showBackFace,
      leafOnLeftHalf = leafOnLeftHalf,
      rotationYDeg = rotationYDeg,
      pivotX = pivotX,
      shadowAlpha = 0.36f * arc,
      highlightAlpha = 0.44f * arc,
      liftScale = 1f + 0.026f * arc,
      liftTranslationYDp = -4.5f * arc,
    )
  }
}

private data class MemoryFlipPair(
  val outgoing: MemoryLaneEntry,
  val incoming: MemoryLaneEntry,
  val direction: Int,
  val targetIndex: Int,
  val wrapSeedIncrement: Long,
)

private const val ROUND_SEED_STEP = 0x517CC1B727220A95L
private val PAGE_TURN_EASING = CubicBezierEasing(0.32f, 0.05f, 0.18f, 1.0f)

object MemoryLaneCurator {
  fun currentMonthDayIsoSuffix(calendar: Calendar = Calendar.getInstance()): String =
    String.format(
      Locale.US,
      "-%02d-%02d",
      calendar.get(Calendar.MONTH) + 1,
      calendar.get(Calendar.DAY_OF_MONTH),
    )

  /**
   * Builds weighted curated memories strictly from NON-archived diaries & NON-deleted,
   * NON-blank pages:
   * - Pinned pages get +3 priority weight
   * - On-This-Day (-MM-DD match) pages get +4 priority weight
   * - All other pages with real content get +1 base weight
   */
  fun buildCuratedMemories(
    diaries: List<DiaryWithPages>,
    monthDaySuffix: String = currentMonthDayIsoSuffix(),
  ): List<MemoryLaneEntry> {
    val candidates = mutableListOf<MemoryLaneEntry>()
    for (bundle in diaries) {
      if (bundle.diary.archived) continue
      val activePages = bundle.pages.filter { !it.isDeleted }.sortedBy { it.order }
      activePages.forEachIndexed { idx, page ->
        if (!page.hasContent) return@forEachIndexed
        val isOnThisDay = !page.dateIso.isNullOrBlank() && page.dateIso.endsWith(monthDaySuffix)
        var weight = 1
        if (page.pinned) weight += 3
        if (isOnThisDay) weight += 4
        val reason =
          when {
            isOnThisDay -> MemoryLaneReason.ON_THIS_DAY
            page.pinned -> MemoryLaneReason.PINNED
            else -> MemoryLaneReason.CURATED
          }
        candidates.add(
          MemoryLaneEntry(
            diaryWithPages = bundle,
            page = page,
            pageIndex = idx,
            reason = reason,
            weight = weight,
          )
        )
      }
    }
    return candidates.sortedWith(
      compareByDescending<MemoryLaneEntry> { it.weight }.thenByDescending { it.page.updatedAt }
    )
  }

  /**
   * Builds a rotating Memory Lane feed across ALL non-archived diaries in the library:
   * - When multiple diaries exist, emits exactly ONE entry per diary so journals cycle one-by-one.
   * - Each diary picks one random non-deleted page (preferring content pages, falling back to any active page)
   *   seeded by [roundSeed] so every pass/session surfaces fresh memories across all books.
   * - When only a single non-archived diary exists, surfaces its active entries in shuffled order.
   */
  fun buildRotatingLibraryFeed(
    diaries: List<DiaryWithPages>,
    roundSeed: Long = System.currentTimeMillis(),
    monthDaySuffix: String = currentMonthDayIsoSuffix(),
  ): List<MemoryLaneEntry> {
    val nonArchived = diaries.filter { !it.diary.archived }
    if (nonArchived.isEmpty()) return emptyList()

    fun toEntry(bundle: DiaryWithPages, page: Page, pageIdx: Int): MemoryLaneEntry {
      val isOnThisDay = !page.dateIso.isNullOrBlank() && page.dateIso.endsWith(monthDaySuffix)
      var weight = 1
      if (page.pinned) weight += 3
      if (isOnThisDay) weight += 4
      val reason =
        when {
          isOnThisDay -> MemoryLaneReason.ON_THIS_DAY
          page.pinned -> MemoryLaneReason.PINNED
          else -> MemoryLaneReason.CURATED
        }
      return MemoryLaneEntry(
        diaryWithPages = bundle,
        page = page,
        pageIndex = pageIdx,
        reason = reason,
        weight = weight,
      )
    }

    if (nonArchived.size == 1) {
      val onlyBundle = nonArchived.first()
      val activePages = onlyBundle.pages.filter { !it.isDeleted }.sortedBy { it.order }
      val contentIndexed =
        activePages.mapIndexedNotNull { idx, page -> if (page.hasContent) idx to page else null }
      val pool =
        if (contentIndexed.isNotEmpty()) contentIndexed
        else activePages.firstOrNull()?.let { listOf(0 to it) }.orEmpty()
      if (pool.isEmpty()) return emptyList()
      return pool.shuffled(Random(roundSeed)).map { (idx, page) -> toEntry(onlyBundle, page, idx) }
    }

    val perBookEntries =
      nonArchived.mapNotNull { bundle ->
        val activePages = bundle.pages.filter { !it.isDeleted }.sortedBy { it.order }
        if (activePages.isEmpty()) return@mapNotNull null
        val contentIndexed =
          activePages.mapIndexedNotNull { idx, page -> if (page.hasContent) idx to page else null }
        val pool =
          if (contentIndexed.isNotEmpty()) contentIndexed
          else activePages.mapIndexed { idx, p -> idx to p }
        val bookRng = Random(roundSeed xor (bundle.diary.id.hashCode().toLong() * -7046029254386353131L))
        val (pickedIdx, pickedPage) = pool[bookRng.nextInt(pool.size)]
        toEntry(bundle, pickedPage, pickedIdx)
      }

    return perBookEntries.shuffled(Random(roundSeed))
  }
}

@Composable
fun MemoryLaneStage(
  activeDiaries: List<DiaryWithPages>,
  numeralStyle: NumeralStyle,
  reduceMotion: Boolean,
  liteMode: Boolean,
  onOpenMemory: (DiaryWithPages, Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val coroutineScope = rememberCoroutineScope()

  var roundSeed by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
  val memories =
    remember(activeDiaries, roundSeed) {
      MemoryLaneCurator.buildRotatingLibraryFeed(activeDiaries, roundSeed = roundSeed)
    }
  var currentIndex by rememberSaveable { mutableIntStateOf(0) }
  var isPlaying by rememberSaveable { mutableStateOf(true) }

  if (currentIndex >= memories.size && memories.isNotEmpty()) {
    currentIndex = 0
  }

  val useStaticCarousel = reduceMotion || liteMode
  val currentEntry = memories.getOrNull(currentIndex)

  // Initial cover-open hinge + 3D spine-hinged page-turn animation state
  val coverOpenProgress = remember { Animatable(if (useStaticCarousel) 1f else 0f) }
  val turnProgress = remember { Animatable(0f) }
  var hasCompletedCoverReveal by remember { mutableStateOf(useStaticCarousel) }
  var activeFlip by remember { mutableStateOf<MemoryFlipPair?>(null) }
  var isDragging by remember { mutableStateOf(false) }
  var flipJob by remember { mutableStateOf<Job?>(null) }
  var flipToken by remember { mutableIntStateOf(0) }
  var stageWidthPx by remember { mutableIntStateOf(600) }

  // Keep latest snapshots available inside drag gestures without restarting pointerInput
  val latestMemories by rememberUpdatedState(memories)
  val latestIndex by rememberUpdatedState(currentIndex)
  val latestSeed by rememberUpdatedState(roundSeed)
  val latestDiaries by rememberUpdatedState(activeDiaries)

  fun resolveTargetForStep(
    baseFeed: List<MemoryLaneEntry>,
    baseIdx: Int,
    baseSeed: Long,
    step: Int,
  ): Triple<MemoryLaneEntry, Int, Long>? {
    if (baseFeed.isEmpty()) return null
    val rawNext = baseIdx + step
    return when {
      rawNext >= baseFeed.size -> {
        val nextSeed = baseSeed + ROUND_SEED_STEP
        val nextFeed = MemoryLaneCurator.buildRotatingLibraryFeed(latestDiaries, roundSeed = nextSeed)
        val entry = nextFeed.firstOrNull() ?: baseFeed.first()
        Triple(entry, 0, ROUND_SEED_STEP)
      }
      rawNext < 0 -> {
        val wrappedIdx = baseFeed.lastIndex
        Triple(baseFeed[wrappedIdx], wrappedIdx, 0L)
      }
      else -> Triple(baseFeed[rawNext], rawNext, 0L)
    }
  }

  fun advanceMemoryStatic(step: Int) {
    val feed = latestMemories
    if (feed.isEmpty()) return
    val next = latestIndex + step
    if (next >= feed.size) {
      currentIndex = 0
      roundSeed += ROUND_SEED_STEP
    } else if (next < 0) {
      currentIndex = feed.lastIndex
    } else {
      currentIndex = next
    }
  }

  fun triggerPageTurn(step: Int) {
    val feed = latestMemories
    if (feed.size <= 1) return
    if (useStaticCarousel) {
      advanceMemoryStatic(step)
      return
    }

    val myToken = ++flipToken
    flipJob?.cancel()

    // If a previous flip was mid-flight, commit its destination immediately so rapid clicks chain smoothly
    val interrupted = activeFlip
    var baseFeed = feed
    var baseIndex = latestIndex
    var baseSeed = latestSeed
    val outgoing: MemoryLaneEntry
    if (interrupted != null) {
      baseIndex = interrupted.targetIndex
      if (interrupted.wrapSeedIncrement != 0L) {
        baseSeed += interrupted.wrapSeedIncrement
        baseFeed = MemoryLaneCurator.buildRotatingLibraryFeed(latestDiaries, roundSeed = baseSeed)
      }
      currentIndex = baseIndex
      roundSeed = baseSeed
      outgoing = interrupted.incoming
    } else {
      outgoing = baseFeed.getOrNull(baseIndex) ?: return
    }

    val resolved = resolveTargetForStep(baseFeed, baseIndex, baseSeed, step) ?: return
    val (incoming, targetIdx, wrapInc) = resolved
    val pair =
      MemoryFlipPair(
        outgoing = outgoing,
        incoming = incoming,
        direction = if (step >= 0) 1 else -1,
        targetIndex = targetIdx,
        wrapSeedIncrement = wrapInc,
      )
    activeFlip = pair
    hasCompletedCoverReveal = true

    flipJob =
      coroutineScope.launch {
        coverOpenProgress.snapTo(1f)
        turnProgress.snapTo(0f)
        turnProgress.animateTo(
          targetValue = 1f,
          animationSpec = tween(durationMillis = 820, easing = PAGE_TURN_EASING),
        )
        if (flipToken == myToken) {
          currentIndex = pair.targetIndex
          if (pair.wrapSeedIncrement != 0L) {
            roundSeed += pair.wrapSeedIncrement
          }
          turnProgress.snapTo(0f)
          activeFlip = null
        }
      }
  }

  // Reset cover reveal when diary collection changes from empty -> populated
  LaunchedEffect(memories.isEmpty(), useStaticCarousel) {
    if (useStaticCarousel) {
      hasCompletedCoverReveal = true
      coverOpenProgress.snapTo(1f)
      activeFlip = null
      turnProgress.snapTo(0f)
    } else if (!memories.isEmpty() && !hasCompletedCoverReveal) {
      coverOpenProgress.snapTo(0f)
      coverOpenProgress.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 740, easing = FastOutSlowInEasing),
      )
      hasCompletedCoverReveal = true
    }
  }

  // Auto-play timer: cleanly pauses while dragging, paused by user, or mid-flip
  LaunchedEffect(
    memories.size,
    currentIndex,
    roundSeed,
    isPlaying,
    isDragging,
    activeFlip,
    hasCompletedCoverReveal,
    useStaticCarousel,
  ) {
    if (memories.isEmpty()) return@LaunchedEffect
    if (useStaticCarousel) {
      if (isPlaying && memories.size > 1) {
        delay(5500L)
        advanceMemoryStatic(1)
      }
      return@LaunchedEffect
    }
    if (hasCompletedCoverReveal && isPlaying && !isDragging && activeFlip == null && memories.size > 1) {
      delay(4400L)
      triggerPageTurn(1)
    }
  }

  val displayedEntry = activeFlip?.outgoing ?: currentEntry

  Surface(
    shape = MaterialTheme.shapes.medium,
    color = palette.shelfSurface.copy(alpha = 0.86f),
    border = BorderStroke(1.dp, palette.borderGold),
    shadowElevation = 6.dp,
    modifier = modifier.fillMaxWidth().testTag("home_memory_lane_card"),
  ) {
    Column(
      modifier = Modifier.padding(SankalanSpacing.lg),
      verticalArrangement = Arrangement.spacedBy(SankalanSpacing.md),
    ) {
      // Header + Play/Pause & Prev/Next controls
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f).padding(end = SankalanSpacing.sm),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
        ) {
          Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = palette.goldAccent,
          )
          Column {
            Text(
              text = strings.memoryLaneTitle,
              style = MaterialTheme.typography.titleLarge,
              color = palette.textPrimary,
            )
            Text(
              text = strings.memoryLaneSubtitle,
              style = MaterialTheme.typography.bodyMedium,
              color = palette.textSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        if (memories.isNotEmpty()) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            IconButton(
              onClick = { triggerPageTurn(-1) },
              enabled = memories.size > 1,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("memory_lane_prev"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = strings.memoryLanePrev,
                tint = palette.goldAccent,
              )
            }
            IconButton(
              onClick = { isPlaying = !isPlaying },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("memory_lane_play_pause"),
            ) {
              Icon(
                imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = if (isPlaying) strings.memoryLanePause else strings.memoryLanePlay,
                tint = palette.goldAccent,
              )
            }
            IconButton(
              onClick = { triggerPageTurn(1) },
              enabled = memories.size > 1,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("memory_lane_next"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = strings.memoryLaneNext,
                tint = palette.goldAccent,
              )
            }
          }
        }
      }

      if (displayedEntry == null) {
        Surface(
          shape = MaterialTheme.shapes.small,
          color = palette.background.copy(alpha = 0.72f),
          border = BorderStroke(1.dp, palette.borderSubtle),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = strings.memoryLaneNoMemories,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(SankalanSpacing.xl),
          )
        }
      } else {
        // Illuminated Memory Lane Vitrine Stage with Metallic Gold Framing & Interactive Swipe-to-Turn
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .height(228.dp)
              .onSizeChanged { stageWidthPx = it.width.coerceAtLeast(320) }
              .clip(MaterialTheme.shapes.small)
              .border(
                width = 1.dp,
                brush =
                  Brush.linearGradient(
                    colors =
                      listOf(
                        SankalanTokens.AntiqueGold.copy(alpha = 0.45f),
                        SankalanTokens.GoldHighlight.copy(alpha = 0.65f),
                        SankalanTokens.AntiqueGold.copy(alpha = 0.35f),
                      )
                  ),
                shape = MaterialTheme.shapes.small,
              )
              .background(
                Brush.verticalGradient(
                  colors = listOf(Color(0xFF1F1511), Color(0xFF160E0B), Color(0xFF281B15))
                )
              )
              .pointerInput(useStaticCarousel) {
                if (useStaticCarousel) return@pointerInput
                var totalDragX = 0f
                var activeDragPair: MemoryFlipPair? = null
                detectHorizontalDragGestures(
                  onDragStart = {
                    if (latestMemories.size <= 1) return@detectHorizontalDragGestures
                    flipJob?.cancel()
                    ++flipToken
                    isDragging = true
                    hasCompletedCoverReveal = true
                    totalDragX = 0f
                    activeDragPair = null
                  },
                  onHorizontalDrag = { change, dragAmount ->
                    if (latestMemories.size <= 1) return@detectHorizontalDragGestures
                    change.consume()
                    totalDragX += dragAmount
                    val desiredDir = if (totalDragX <= 0f) 1 else -1
                    if (activeDragPair == null || activeDragPair?.direction != desiredDir) {
                      val current = latestMemories.getOrNull(latestIndex) ?: return@detectHorizontalDragGestures
                      val resolved =
                        resolveTargetForStep(
                          baseFeed = latestMemories,
                          baseIdx = latestIndex,
                          baseSeed = latestSeed,
                          step = desiredDir,
                        ) ?: return@detectHorizontalDragGestures
                      activeDragPair =
                        MemoryFlipPair(
                          outgoing = current,
                          incoming = resolved.first,
                          direction = desiredDir,
                          targetIndex = resolved.second,
                          wrapSeedIncrement = resolved.third,
                        )
                      activeFlip = activeDragPair
                    }
                    val sweepSpan = (stageWidthPx * 0.62f).coerceAtLeast(180f)
                    val dragFraction = (abs(totalDragX) / sweepSpan).coerceIn(0f, 1f)
                    coroutineScope.launch {
                      coverOpenProgress.snapTo(1f)
                      turnProgress.snapTo(dragFraction)
                    }
                  },
                  onDragEnd = {
                    val pair = activeDragPair
                    if (pair == null) {
                      isDragging = false
                      activeFlip = null
                      return@detectHorizontalDragGestures
                    }
                    val myToken = ++flipToken
                    val shouldCommit = turnProgress.value >= 0.24f
                    flipJob =
                      coroutineScope.launch {
                        if (shouldCommit) {
                          turnProgress.animateTo(
                            targetValue = 1f,
                            animationSpec =
                              spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                              ),
                          )
                          if (flipToken == myToken) {
                            currentIndex = pair.targetIndex
                            if (pair.wrapSeedIncrement != 0L) {
                              roundSeed += pair.wrapSeedIncrement
                            }
                            turnProgress.snapTo(0f)
                            activeFlip = null
                          }
                        } else {
                          turnProgress.animateTo(
                            targetValue = 0f,
                            animationSpec =
                              spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMedium,
                              ),
                          )
                          if (flipToken == myToken) {
                            activeFlip = null
                          }
                        }
                        isDragging = false
                      }
                  },
                  onDragCancel = {
                    val myToken = ++flipToken
                    flipJob =
                      coroutineScope.launch {
                        turnProgress.animateTo(
                          targetValue = 0f,
                          animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        )
                        if (flipToken == myToken) {
                          activeFlip = null
                        }
                        isDragging = false
                      }
                  },
                )
              }
              .clickable(role = Role.Button) {
                val target = activeFlip?.incoming ?: currentEntry
                if (target != null) {
                  onOpenMemory(target.diaryWithPages, target.pageIndex)
                }
              }
              .testTag("memory_lane_stage_open"),
          contentAlignment = Alignment.Center,
        ) {
          // Stage spotlight, brass corner vitrine trim & pedestal shelf rail
          Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
              brush =
                Brush.radialGradient(
                  colors =
                    listOf(
                      SankalanTokens.GoldHighlight.copy(alpha = 0.22f),
                      SankalanTokens.AntiqueGold.copy(alpha = 0.08f),
                      Color.Transparent,
                    ),
                  center = Offset(size.width * 0.5f, size.height * 0.08f),
                  radius = size.width * 0.58f,
                )
            )
            // Subtle inner dashed gold frame inside stage
            val pad = 6.dp.toPx()
            drawRoundRect(
              color = SankalanTokens.AntiqueGold.copy(alpha = 0.22f),
              topLeft = Offset(pad, pad),
              size = Size(size.width - pad * 2, size.height - pad * 2),
              cornerRadius = CornerRadius(6.dp.toPx()),
              style =
                Stroke(
                  width = 0.75.dp.toPx(),
                  pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f),
                ),
            )
            // Brass shelf rail beneath book
            val railY = size.height - 13.dp.toPx()
            drawLine(
              brush =
                Brush.horizontalGradient(
                  colors =
                    listOf(
                      Color.Transparent,
                      SankalanTokens.GoldHighlight.copy(alpha = 0.65f),
                      SankalanTokens.AntiqueGold.copy(alpha = 0.70f),
                      Color.Transparent,
                    )
                ),
              start = Offset(16.dp.toPx(), railY),
              end = Offset(size.width - 16.dp.toPx(), railY),
              strokeWidth = 1.6.dp.toPx(),
            )
          }

          if (useStaticCarousel) {
            AnimatedContent(
              targetState = displayedEntry,
              transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(200)) },
              label = "memoryStaticCarousel",
            ) { entry ->
              MemoryOpenSpreadBook(
                outgoingEntry = entry,
                incomingEntry = null,
                turnProgress = 0f,
                turnDirection = 1,
                coverOpenProgress = 1f,
                numeralStyle = numeralStyle,
                modifier = Modifier.padding(horizontal = SankalanSpacing.md, vertical = SankalanSpacing.md),
              )
            }
          } else {
            Box(
              modifier = Modifier.padding(horizontal = SankalanSpacing.md, vertical = SankalanSpacing.md),
              contentAlignment = Alignment.Center,
            ) {
              MemoryOpenSpreadBook(
                outgoingEntry = activeFlip?.outgoing ?: displayedEntry,
                incomingEntry = activeFlip?.incoming,
                turnProgress = turnProgress.value,
                turnDirection = activeFlip?.direction ?: 1,
                coverOpenProgress = coverOpenProgress.value,
                numeralStyle = numeralStyle,
              )
            }
          }
        }

        // Bottom indicator dots + Open Memory action
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            val dotCount = memories.size.coerceAtMost(7)
            val displayDotIdx = (activeFlip?.targetIndex ?: currentIndex) % dotCount
            for (i in 0 until dotCount) {
              val active = i == displayDotIdx
              Box(
                modifier =
                  Modifier
                    .size(if (active) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (active) palette.goldAccent else palette.borderSubtle)
              )
            }
            if (memories.size > 7) {
              Text(
                text = "+${formatNumber(memories.size - 7, numeralStyle)}",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
              )
            }
          }

          TextButton(
            onClick = {
              val target = activeFlip?.incoming ?: currentEntry
              if (target != null) {
                onOpenMemory(target.diaryWithPages, target.pageIndex)
              }
            },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("memory_lane_open_button"),
          ) {
            Text(text = strings.memoryLaneReadMemory, color = palette.goldAccent)
          }
        }
      }
    }
  }
}

@Composable
private fun MemoryOpenSpreadBook(
  outgoingEntry: MemoryLaneEntry,
  incomingEntry: MemoryLaneEntry?,
  turnProgress: Float,
  turnDirection: Int,
  coverOpenProgress: Float,
  numeralStyle: NumeralStyle,
  modifier: Modifier = Modifier,
) {
  val isTurning = incomingEntry != null && turnProgress > 0.002f && turnProgress < 0.998f
  val turnFrame =
    if (isTurning) MemoryPageTurnMath.computeFrame(turnProgress, turnDirection) else null

  Box(
    modifier = modifier.fillMaxWidth().height(186.dp),
    contentAlignment = Alignment.Center,
  ) {
    if (coverOpenProgress < 0.45f) {
      // Initial reveal: Detailed tactile BookCoverArt swinging open on its spine hinge
      val coverPhase = (coverOpenProgress / 0.45f).coerceIn(0f, 1f)
      BookCoverArt(
        diary = outgoingEntry.diaryWithPages.diary,
        pageCount = outgoingEntry.diaryWithPages.pages.size,
        numeralStyle = numeralStyle,
        isBack = false,
        modifier =
          Modifier
            .width(126.dp)
            .height(174.dp)
            .graphicsLayer {
              rotationY = -88f * coverPhase
              cameraDistance = 12f * density
              transformOrigin = TransformOrigin(0f, 0.5f)
              alpha = (1f - coverPhase * 0.25f).coerceIn(0.65f, 1f)
            },
      )
    } else {
      // Unfold from 0.45f -> 1f on first entry, then 1f during resting & 3D page turns
      val unfoldFraction =
        if (coverOpenProgress < 1f) ((coverOpenProgress - 0.45f) / 0.55f).coerceIn(0f, 1f) else 1f
      val spreadScale = (0.86f + 0.14f * unfoldFraction).coerceIn(0.86f, 1f)
      val spreadAlpha = (0.35f + 0.65f * unfoldFraction).coerceIn(0.35f, 1f)

      // Smoothly transition outer binding colors across diaries during page turn
      val outCoverColor = parseHexColor(outgoingEntry.diaryWithPages.diary.cover.colorHex, SankalanTokens.Leather)
      val outAccentColor = parseHexColor(outgoingEntry.diaryWithPages.diary.cover.accentHex, SankalanTokens.AntiqueGold)
      val blendT =
        if (incomingEntry != null && turnProgress > 0f) {
          ((turnProgress - 0.25f) / 0.5f).coerceIn(0f, 1f)
        } else {
          0f
        }
      val coverColor =
        if (incomingEntry != null && blendT > 0f) {
          val inCoverColor =
            parseHexColor(incomingEntry.diaryWithPages.diary.cover.colorHex, SankalanTokens.Leather)
          lerp(outCoverColor, inCoverColor, blendT)
        } else {
          outCoverColor
        }
      val accentColor =
        if (incomingEntry != null && blendT > 0f) {
          val inAccentColor =
            parseHexColor(incomingEntry.diaryWithPages.diary.cover.accentHex, SankalanTokens.AntiqueGold)
          lerp(outAccentColor, inAccentColor, blendT)
        } else {
          outAccentColor
        }
      val activeDiaryForTexture: Diary =
        if (incomingEntry != null && turnProgress >= 0.5f) incomingEntry.diaryWithPages.diary
        else outgoingEntry.diaryWithPages.diary

      val metallicBorderBrush =
        Brush.linearGradient(
          colors =
            listOf(
              SankalanTokens.GoldHighlight,
              accentColor,
              SankalanTokens.AntiqueGold,
              SankalanTokens.GoldHighlight,
            )
        )

      // Determine which entry sits on the static Left / Right pages beneath the turning leaf
      val baseLeftEntry: MemoryLaneEntry =
        when {
          !isTurning -> outgoingEntry
          turnDirection >= 0 -> outgoingEntry // Forward: left shows outgoing until turning leaf lands
          else -> incomingEntry!! // Backward: left reveals incoming beneath lifting leaf
        }
      val baseRightEntry: MemoryLaneEntry =
        when {
          !isTurning -> outgoingEntry
          turnDirection >= 0 -> incomingEntry!! // Forward: right reveals incoming beneath lifting leaf
          else -> outgoingEntry // Backward: right stays outgoing until turning leaf lands
        }

      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(176.dp)
            .graphicsLayer {
              scaleX = spreadScale
              scaleY = spreadScale
              alpha = spreadAlpha
              transformOrigin = TransformOrigin.Center
            },
      ) {
        // Layer 1: Hardcover gilded binding + static base spread pages + cast page-turn shadow
        Box(
          modifier =
            Modifier
              .matchParentSize()
              .shadow(12.dp, RoundedCornerShape(10.dp))
              .clip(RoundedCornerShape(10.dp))
              .background(coverColor)
              .border(1.8.dp, metallicBorderBrush, RoundedCornerShape(10.dp)),
        ) {
          BindingBoardOrnamentCanvas(
            coverStyle = activeDiaryForTexture.cover.style,
            metallicBorderBrush = metallicBorderBrush,
            modifier = Modifier.fillMaxSize(),
          )

          Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 7.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            // Left fore-edge stacked page thickness lines
            ForeEdgeStack(isLeft = true)

            // Left Page (weight 1f matches turning leaf width accurately)
            Box(
              modifier =
                Modifier
                  .weight(1f)
                  .fillMaxHeight()
                  .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)),
            ) {
              SpreadLeftPageContent(entry = baseLeftEntry, numeralStyle = numeralStyle)
              if (turnFrame != null && turnFrame.leafOnLeftHalf && turnFrame.shadowAlpha > 0.01f) {
                CastSpineShadowOverlay(shadowAlpha = turnFrame.shadowAlpha, anchorOnRight = true)
              }
            }

            // Center 3D Book Spine Valley + Binding Stitch Threads
            CenterSpineValley()

            // Right Page (weight 1f matches turning leaf width accurately)
            Box(
              modifier =
                Modifier
                  .weight(1f)
                  .fillMaxHeight()
                  .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)),
            ) {
              SpreadRightPageContent(entry = baseRightEntry)
              if (turnFrame != null && !turnFrame.leafOnLeftHalf && turnFrame.shadowAlpha > 0.01f) {
                CastSpineShadowOverlay(shadowAlpha = turnFrame.shadowAlpha, anchorOnRight = false)
              }
            }

            // Right fore-edge stacked page thickness lines
            ForeEdgeStack(isLeft = false)
          }
        }

        // Layer 2: Unclipped 3D Spine-Hinged Turning Leaf (Compose animation primitives)
        if (turnFrame != null && incomingEntry != null) {
          val leafEntry = if (turnFrame.showBackFace) incomingEntry else outgoingEntry
          val leafClipShape =
            if (turnFrame.leafOnLeftHalf) RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
            else RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)

          Row(
            modifier = Modifier.matchParentSize().padding(horizontal = 7.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Spacer(modifier = Modifier.width(4.dp))

            if (turnFrame.leafOnLeftHalf) {
              Box(
                modifier =
                  Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .graphicsLayer {
                      rotationY = turnFrame.rotationYDeg
                      cameraDistance = 12f * density
                      transformOrigin = TransformOrigin(turnFrame.pivotX, 0.5f)
                      scaleX = turnFrame.liftScale
                      scaleY = turnFrame.liftScale
                      translationY = turnFrame.liftTranslationYDp * density
                      shadowElevation = (10f * sin(turnProgress * PI).toFloat()).coerceAtLeast(1f)
                    }
                    .clip(leafClipShape),
              ) {
                SpreadLeftPageContent(entry = leafEntry, numeralStyle = numeralStyle)
                TurningLeafCurlOverlay(
                  highlightAlpha = turnFrame.highlightAlpha,
                  hingeOnRight = true,
                )
              }
            } else {
              Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (!turnFrame.leafOnLeftHalf) {
              Box(
                modifier =
                  Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .graphicsLayer {
                      rotationY = turnFrame.rotationYDeg
                      cameraDistance = 12f * density
                      transformOrigin = TransformOrigin(turnFrame.pivotX, 0.5f)
                      scaleX = turnFrame.liftScale
                      scaleY = turnFrame.liftScale
                      translationY = turnFrame.liftTranslationYDp * density
                      shadowElevation = (10f * sin(turnProgress * PI).toFloat()).coerceAtLeast(1f)
                    }
                    .clip(leafClipShape),
              ) {
                SpreadRightPageContent(entry = leafEntry)
                TurningLeafCurlOverlay(
                  highlightAlpha = turnFrame.highlightAlpha,
                  hingeOnRight = false,
                )
              }
            } else {
              Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.width(4.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun BindingBoardOrnamentCanvas(
  coverStyle: CoverStyle,
  metallicBorderBrush: Brush,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    when (coverStyle) {
      CoverStyle.LUXURY -> {
        val step = 8.dp.toPx()
        var row = 0
        var y = 4.dp.toPx()
        while (y < size.height) {
          var x = if (row % 2 == 0) 4.dp.toPx() else 8.dp.toPx()
          while (x < size.width) {
            drawCircle(Color.Black.copy(alpha = 0.16f), 1.1.dp.toPx(), Offset(x, y))
            x += step
          }
          y += step
          row++
        }
      }
      CoverStyle.ARTISTIC -> {
        val step = 5.dp.toPx()
        var y = step
        while (y < size.height) {
          drawLine(Color.White.copy(alpha = 0.06f), Offset(0f, y), Offset(size.width, y), 0.8.dp.toPx())
          y += step
        }
      }
      CoverStyle.MINIMAL -> Unit
    }

    // Top & bottom center woven spine headbands
    val midX = size.width * 0.5f
    val bandW = 14.dp.toPx()
    drawRoundRect(
      brush = metallicBorderBrush,
      topLeft = Offset(midX - bandW * 0.5f, 1.dp.toPx()),
      size = Size(bandW, 5.dp.toPx()),
      cornerRadius = CornerRadius(2.dp.toPx()),
    )
    drawRoundRect(
      brush = metallicBorderBrush,
      topLeft = Offset(midX - bandW * 0.5f, size.height - 6.dp.toPx()),
      size = Size(bandW, 5.dp.toPx()),
      cornerRadius = CornerRadius(2.dp.toPx()),
    )

    // Brass corner guard mounts & stud rivets on all 4 binding corners
    val cm = 3.dp.toPx()
    val cLen = 14.dp.toPx()
    val cStroke = 1.6.dp.toPx()
    val studR = 1.5.dp.toPx()
    drawLine(SankalanTokens.GoldHighlight, Offset(cm, cm), Offset(cm + cLen, cm), cStroke)
    drawLine(SankalanTokens.GoldHighlight, Offset(cm, cm), Offset(cm, cm + cLen), cStroke)
    drawCircle(SankalanTokens.GoldHighlight, studR, Offset(cm + 4.dp.toPx(), cm + 4.dp.toPx()))

    drawLine(SankalanTokens.GoldHighlight, Offset(size.width - cm, cm), Offset(size.width - cm - cLen, cm), cStroke)
    drawLine(SankalanTokens.GoldHighlight, Offset(size.width - cm, cm), Offset(size.width - cm, cm + cLen), cStroke)
    drawCircle(SankalanTokens.GoldHighlight, studR, Offset(size.width - cm - 4.dp.toPx(), cm + 4.dp.toPx()))

    drawLine(SankalanTokens.GoldHighlight, Offset(cm, size.height - cm), Offset(cm + cLen, size.height - cm), cStroke)
    drawLine(SankalanTokens.GoldHighlight, Offset(cm, size.height - cm), Offset(cm, size.height - cm - cLen), cStroke)
    drawCircle(SankalanTokens.GoldHighlight, studR, Offset(cm + 4.dp.toPx(), size.height - cm - 4.dp.toPx()))

    drawLine(SankalanTokens.GoldHighlight, Offset(size.width - cm, size.height - cm), Offset(size.width - cm - cLen, size.height - cm), cStroke)
    drawLine(SankalanTokens.GoldHighlight, Offset(size.width - cm, size.height - cm), Offset(size.width - cm, size.height - cm - cLen), cStroke)
    drawCircle(SankalanTokens.GoldHighlight, studR, Offset(size.width - cm - 4.dp.toPx(), size.height - cm - 4.dp.toPx()))
  }
}

@Composable
private fun ForeEdgeStack(isLeft: Boolean) {
  val colors =
    if (isLeft) listOf(SankalanTokens.AntiqueGold.copy(alpha = 0.65f), SankalanTokens.PaperShadow)
    else listOf(SankalanTokens.PaperShadow, SankalanTokens.AntiqueGold.copy(alpha = 0.65f))
  val shape =
    if (isLeft) RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp)
    else RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp)
  Box(
    modifier =
      Modifier
        .width(4.dp)
        .fillMaxHeight()
        .background(Brush.horizontalGradient(colors = colors), shape)
  )
}

@Composable
private fun CenterSpineValley() {
  Box(
    modifier =
      Modifier
        .width(10.dp)
        .fillMaxHeight()
        .background(
          Brush.horizontalGradient(
            colors =
              listOf(
                SankalanTokens.PaperShadow,
                Color(0xFF6B523D),
                Color(0xFF3E2A1E),
                Color(0xFF6B523D),
                SankalanTokens.PaperShadow,
              )
          )
        )
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = size.width * 0.5f
      var y = 14.dp.toPx()
      val step = 18.dp.toPx()
      while (y < size.height - 14.dp.toPx()) {
        drawLine(
          color = SankalanTokens.GoldHighlight.copy(alpha = 0.78f),
          start = Offset(cx, y),
          end = Offset(cx, y + 7.dp.toPx()),
          strokeWidth = 1.4.dp.toPx(),
        )
        y += step
      }
    }
  }
}

@Composable
private fun CastSpineShadowOverlay(
  shadowAlpha: Float,
  anchorOnRight: Boolean,
) {
  Canvas(modifier = Modifier.fillMaxSize()) {
    val colors =
      if (anchorOnRight) {
        listOf(
          Color.Transparent,
          Color.Black.copy(alpha = shadowAlpha * 0.35f),
          Color.Black.copy(alpha = shadowAlpha),
        )
      } else {
        listOf(
          Color.Black.copy(alpha = shadowAlpha),
          Color.Black.copy(alpha = shadowAlpha * 0.35f),
          Color.Transparent,
        )
      }
    drawRect(brush = Brush.horizontalGradient(colors = colors))
  }
}

@Composable
private fun TurningLeafCurlOverlay(
  highlightAlpha: Float,
  hingeOnRight: Boolean,
) {
  Canvas(modifier = Modifier.fillMaxSize()) {
    // Soft spine fold shade + warm ivory-gold curved highlight sheen toward outer edge
    val bandColors =
      if (hingeOnRight) {
        listOf(
          SankalanTokens.GoldHighlight.copy(alpha = highlightAlpha * 0.65f),
          SankalanTokens.Cream.copy(alpha = highlightAlpha),
          Color.Transparent,
          Color.Black.copy(alpha = highlightAlpha * 0.32f),
        )
      } else {
        listOf(
          Color.Black.copy(alpha = highlightAlpha * 0.32f),
          Color.Transparent,
          SankalanTokens.Cream.copy(alpha = highlightAlpha),
          SankalanTokens.GoldHighlight.copy(alpha = highlightAlpha * 0.65f),
        )
      }
    drawRect(brush = Brush.horizontalGradient(colors = bandColors))

    // Crisp illuminated paper thickness edge on the lifting margin
    val outerX = if (hingeOnRight) 1.dp.toPx() else size.width - 1.dp.toPx()
    drawLine(
      color = SankalanTokens.Cream.copy(alpha = (0.45f + highlightAlpha).coerceAtMost(0.95f)),
      start = Offset(outerX, 2.dp.toPx()),
      end = Offset(outerX, size.height - 2.dp.toPx()),
      strokeWidth = 1.8.dp.toPx(),
    )
  }
}

@Composable
private fun SpreadLeftPageContent(
  entry: MemoryLaneEntry,
  numeralStyle: NumeralStyle,
) {
  val strings = LocalStrings.current
  val diary = entry.diaryWithPages.diary
  val page = entry.page
  val coverColor = parseHexColor(diary.cover.colorHex, SankalanTokens.Leather)
  val accentColor = parseHexColor(diary.cover.accentHex, SankalanTokens.AntiqueGold)
  val paperBg = parseHexColor(page.background.tintHex, SankalanTokens.Paper)
  val reasonLabel =
    when (entry.reason) {
      MemoryLaneReason.ON_THIS_DAY -> strings.memoryLaneOnThisDayLabel
      MemoryLaneReason.PINNED -> strings.memoryLanePinnedLabel
      MemoryLaneReason.CURATED -> strings.memoryLaneReflectionLabel
    }

  Box(modifier = Modifier.fillMaxSize().background(paperBg)) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      // Metallic gold inner bookplate frame
      val pad = 4.5.dp.toPx()
      drawRoundRect(
        color = accentColor.copy(alpha = 0.58f),
        topLeft = Offset(pad, pad),
        size = Size(size.width - pad * 2, size.height - pad * 2),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = Stroke(width = 0.85.dp.toPx()),
      )
      // Right-edge spine gutter shadow
      drawRect(
        brush =
          Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.14f)),
            startX = size.width * 0.86f,
            endX = size.width,
          ),
        topLeft = Offset(size.width * 0.86f, 0f),
        size = Size(size.width * 0.14f, size.height),
      )
    }

    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = 9.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = accentColor.copy(alpha = 0.22f),
          border = BorderStroke(0.75.dp, accentColor.copy(alpha = 0.55f)),
          modifier = Modifier.weight(1f, fill = false).padding(end = 4.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
          ) {
            Icon(
              imageVector =
                if (entry.reason == MemoryLaneReason.PINNED) Icons.Outlined.PushPin
                else Icons.Outlined.CalendarToday,
              contentDescription = null,
              tint = SankalanTokens.Leather,
              modifier = Modifier.size(10.dp),
            )
            Text(
              text = reasonLabel,
              style = MaterialTheme.typography.labelSmall,
              fontSize = 9.sp,
              color = SankalanTokens.Ink,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Surface(
          shape = CircleShape,
          color = coverColor,
          border = BorderStroke(1.dp, accentColor.copy(alpha = 0.85f)),
          modifier = Modifier.size(22.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = diary.cover.monogram.take(2),
              fontFamily = NotoSerifBengaliFontFamily,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = SankalanTokens.GoldHighlight,
            )
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
          text = diary.title,
          fontFamily = PlayfairDisplayFontFamily,
          fontWeight = FontWeight.Bold,
          style = MaterialTheme.typography.labelLarge,
          color = SankalanTokens.Leather,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        if (!page.dateIso.isNullOrBlank()) {
          Text(
            text = formatDigits(page.dateIso, numeralStyle),
            style = MaterialTheme.typography.labelSmall,
            color = SankalanTokens.Muted,
          )
        }
      }

      Text(
        text = "${strings.pageNumberPrefix} ${formatNumber(entry.pageIndex + 1, numeralStyle)}",
        style = MaterialTheme.typography.labelSmall,
        color = SankalanTokens.Leather.copy(alpha = 0.78f),
        fontWeight = FontWeight.Medium,
      )
    }
  }
}

@Composable
private fun SpreadRightPageContent(
  entry: MemoryLaneEntry,
) {
  val diary = entry.diaryWithPages.diary
  val page = entry.page
  val accentColor = parseHexColor(diary.cover.accentHex, SankalanTokens.AntiqueGold)
  val paperBg = parseHexColor(page.background.tintHex, SankalanTokens.Paper)
  val excerpt = page.elements.firstOrNull { !it.hidden && !it.text.isNullOrBlank() }?.text.orEmpty()

  Box(modifier = Modifier.fillMaxSize().background(paperBg)) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      // Left-edge spine gutter shadow
      drawRect(
        brush =
          Brush.horizontalGradient(
            colors = listOf(Color.Black.copy(alpha = 0.16f), Color.Transparent),
            startX = 0f,
            endX = size.width * 0.12f,
          ),
        topLeft = Offset.Zero,
        size = Size(size.width * 0.12f, size.height),
      )
      // Metallic gold border frame + subtle ruled writing lines
      val pad = 4.5.dp.toPx()
      drawRoundRect(
        color = accentColor.copy(alpha = 0.58f),
        topLeft = Offset(pad, pad),
        size = Size(size.width - pad * 2, size.height - pad * 2),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = Stroke(width = 0.85.dp.toPx()),
      )
      var ly = 42.dp.toPx()
      while (ly < size.height - 16.dp.toPx()) {
        drawLine(
          color = SankalanTokens.Ink.copy(alpha = 0.08f),
          start = Offset(10.dp.toPx(), ly),
          end = Offset(size.width - 10.dp.toPx(), ly),
          strokeWidth = 0.7.dp.toPx(),
        )
        ly += 17.dp.toPx()
      }
      // Gold corner arc motif on top-right
      val arcR = 9.dp.toPx()
      drawArc(
        color = SankalanTokens.AntiqueGold.copy(alpha = 0.68f),
        startAngle = 90f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(size.width - pad - arcR, pad - arcR),
        size = Size(arcR * 2, arcR * 2),
        style = Stroke(width = 1.dp.toPx()),
      )
      // Crimson/Gold bookmark ribbon tail near bottom-right
      val rx = size.width - 18.dp.toPx()
      val ribbonPath =
        Path().apply {
          moveTo(rx, size.height - 18.dp.toPx())
          lineTo(rx + 8.dp.toPx(), size.height - 18.dp.toPx())
          lineTo(rx + 8.dp.toPx(), size.height - 1.dp.toPx())
          lineTo(rx + 4.dp.toPx(), size.height - 5.dp.toPx())
          lineTo(rx, size.height - 1.dp.toPx())
          close()
        }
      drawPath(ribbonPath, color = SankalanTokens.Terracotta.copy(alpha = 0.86f))
    }

    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 9.dp),
      verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      if (!page.title.isNullOrBlank()) {
        Text(
          text = page.title,
          fontFamily = NotoSerifBengaliFontFamily,
          fontWeight = FontWeight.SemiBold,
          style = MaterialTheme.typography.titleSmall,
          color = SankalanTokens.Ink,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Text(
        text = excerpt.ifBlank { page.mood.orEmpty() },
        style = MaterialTheme.typography.bodySmall,
        color = SankalanTokens.Ink.copy(alpha = 0.86f),
        maxLines = 5,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
