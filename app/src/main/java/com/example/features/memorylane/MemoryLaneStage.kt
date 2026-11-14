package com.example.features.memorylane

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.core.model.DiaryWithPages
import com.example.core.model.NumeralStyle
import com.example.core.model.Page
import com.example.features.bookshelf.parseHexColor
import com.example.ui.theme.LocalSankalanPalette
import com.example.ui.theme.NotoSerifBengaliFontFamily
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.SankalanSpacing
import com.example.ui.theme.SankalanTokens
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay

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

  val memories = remember(activeDiaries) { MemoryLaneCurator.buildCuratedMemories(activeDiaries) }
  var currentIndex by rememberSaveable { mutableIntStateOf(0) }
  var isPlaying by rememberSaveable { mutableStateOf(true) }

  if (currentIndex >= memories.size && memories.isNotEmpty()) {
    currentIndex = 0
  }

  val useStaticCarousel = reduceMotion || liteMode
  val currentEntry = memories.getOrNull(currentIndex)

  // Book glide + 3D cover-open choreography:
  // slideX: +1f (right) -> 0f (center) -> -1f (left)
  // openProgress: 0f (closed book) -> 1f (open 2-page spread)
  val slideFraction = remember { Animatable(0f) }
  val spreadOpenFraction = remember { Animatable(1f) }

  LaunchedEffect(memories.size, currentIndex, isPlaying, useStaticCarousel) {
    if (memories.isEmpty()) return@LaunchedEffect
    if (useStaticCarousel) {
      slideFraction.snapTo(0f)
      spreadOpenFraction.snapTo(1f)
      if (isPlaying && memories.size > 1) {
        delay(5500L)
        currentIndex = (currentIndex + 1) % memories.size
      }
      return@LaunchedEffect
    }

    // Enter gliding from right
    slideFraction.snapTo(0.85f)
    spreadOpenFraction.snapTo(0f)
    slideFraction.animateTo(
      targetValue = 0f,
      animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing),
    )
    // Open into a 2-page warm parchment spread in the center
    spreadOpenFraction.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing),
    )

    if (isPlaying && memories.size > 1) {
      delay(4200L)
      // Gently close book cover and glide out left
      spreadOpenFraction.animateTo(
        targetValue = 0f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
      )
      slideFraction.animateTo(
        targetValue = -0.9f,
        animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing),
      )
      currentIndex = (currentIndex + 1) % memories.size
    }
  }

  Surface(
    shape = MaterialTheme.shapes.medium,
    color = palette.shelfSurface.copy(alpha = 0.95f),
    border = BorderStroke(1.dp, palette.borderGold),
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
              onClick = {
                currentIndex = if (currentIndex == 0) memories.lastIndex else currentIndex - 1
              },
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
              onClick = { currentIndex = (currentIndex + 1) % memories.size },
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

      if (currentEntry == null) {
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
        // Illuminated Memory Lane Stage
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .height(216.dp)
              .clip(MaterialTheme.shapes.small)
              .background(
                Brush.verticalGradient(
                  colors = listOf(palette.background, Color(0xFF1A110E), palette.cardSurface)
                )
              )
              .clickable(role = Role.Button) {
                onOpenMemory(currentEntry.diaryWithPages, currentEntry.pageIndex)
              }
              .testTag("memory_lane_stage_open"),
          contentAlignment = Alignment.Center,
        ) {
          // Stage floor rail & ambient spotlight
          Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
              brush =
                Brush.radialGradient(
                  colors = listOf(SankalanTokens.AntiqueGold.copy(alpha = 0.16f), Color.Transparent),
                  center = Offset(size.width * 0.5f, size.height * 0.12f),
                  radius = size.width * 0.55f,
                )
            )
            val railY = size.height - 16.dp.toPx()
            drawLine(
              color = SankalanTokens.AntiqueGold.copy(alpha = 0.45f),
              start = Offset(18.dp.toPx(), railY),
              end = Offset(size.width - 18.dp.toPx(), railY),
              strokeWidth = 2.dp.toPx(),
            )
          }

          if (useStaticCarousel) {
            AnimatedContent(
              targetState = currentEntry,
              transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(200)) },
              label = "memoryStaticCarousel",
            ) { entry ->
              MemoryOpenSpreadBook(
                entry = entry,
                openProgress = 1f,
                numeralStyle = numeralStyle,
                modifier = Modifier.padding(horizontal = SankalanSpacing.md, vertical = SankalanSpacing.md),
              )
            }
          } else {
            val slidePxOffset = slideFraction.value * 240f
            val alpha = (1f - kotlin.math.abs(slideFraction.value) * 0.7f).coerceIn(0.15f, 1f)
            Box(
              modifier =
                Modifier
                  .padding(horizontal = SankalanSpacing.md, vertical = SankalanSpacing.md)
                  .graphicsLayer {
                    translationX = slidePxOffset * density
                    this.alpha = alpha
                  },
              contentAlignment = Alignment.Center,
            ) {
              MemoryOpenSpreadBook(
                entry = currentEntry,
                openProgress = spreadOpenFraction.value,
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
            for (i in 0 until dotCount) {
              val active = i == (currentIndex % dotCount)
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
            onClick = { onOpenMemory(currentEntry.diaryWithPages, currentEntry.pageIndex) },
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
  entry: MemoryLaneEntry,
  openProgress: Float,
  numeralStyle: NumeralStyle,
  modifier: Modifier = Modifier,
) {
  val strings = LocalStrings.current
  val diary = entry.diaryWithPages.diary
  val page = entry.page
  val coverColor = parseHexColor(diary.cover.colorHex, SankalanTokens.Leather)
  val accentColor = parseHexColor(diary.cover.accentHex, SankalanTokens.AntiqueGold)
  val paperBg = parseHexColor(page.background.tintHex, SankalanTokens.Paper)
  val excerpt = page.elements.firstOrNull { !it.hidden && !it.text.isNullOrBlank() }?.text.orEmpty()

  val reasonLabel =
    when (entry.reason) {
      MemoryLaneReason.ON_THIS_DAY -> strings.memoryLaneOnThisDayLabel
      MemoryLaneReason.PINNED -> strings.memoryLanePinnedLabel
      MemoryLaneReason.CURATED -> strings.memoryLaneReflectionLabel
    }

  Box(
    modifier = modifier.fillMaxWidth().height(172.dp),
    contentAlignment = Alignment.Center,
  ) {
    if (openProgress < 0.25f) {
      // Closed traveling volume silhouette
      Surface(
        shape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp, topStart = 4.dp, bottomStart = 4.dp),
        color = coverColor,
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.8f)),
        shadowElevation = 8.dp,
        modifier = Modifier.width(126.dp).height(162.dp),
      ) {
        Column(
          modifier = Modifier.fillMaxSize().padding(12.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = diary.cover.monogram,
            fontFamily = NotoSerifBengaliFontFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
          )
          Text(
            text = diary.title,
            fontFamily = PlayfairDisplayFontFamily,
            fontSize = 13.sp,
            color = SankalanTokens.Cream,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${formatNumber(entry.diaryWithPages.pages.size, numeralStyle)} ${strings.pagesUnit}",
            style = MaterialTheme.typography.labelSmall,
            color = accentColor.copy(alpha = 0.8f),
          )
        }
      }
    } else {
      // Open 2-Page Warm Parchment Spread inside leather binding
      val spreadScale = (0.72f + 0.28f * openProgress).coerceIn(0.72f, 1f)
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(164.dp)
            .graphicsLayer {
              scaleX = spreadScale
              scaleY = spreadScale
              transformOrigin = TransformOrigin.Center
            }
            .shadow(10.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(coverColor)
            .border(1.5.dp, accentColor.copy(alpha = 0.72f), RoundedCornerShape(10.dp))
            .padding(6.dp),
      ) {
        // Left Page: Book Metadata, Reason & Date
        Box(
          modifier =
            Modifier
              .weight(0.95f)
              .fillMaxHeight()
              .clip(RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp))
              .background(paperBg)
              .padding(10.dp),
        ) {
          Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = accentColor.copy(alpha = 0.20f),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  imageVector =
                    if (entry.reason == MemoryLaneReason.PINNED) Icons.Outlined.PushPin
                    else Icons.Outlined.CalendarToday,
                  contentDescription = null,
                  tint = SankalanTokens.Leather,
                  modifier = Modifier.size(11.dp),
                )
                Text(
                  text = reasonLabel,
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 10.sp,
                  color = SankalanTokens.Ink,
                  maxLines = 1,
                )
              }
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
              Text(
                text = diary.title,
                fontFamily = PlayfairDisplayFontFamily,
                fontWeight = FontWeight.SemiBold,
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
              color = SankalanTokens.Muted,
            )
          }
        }

        // Center book spine shadow seam
        Box(
          modifier =
            Modifier
              .width(6.dp)
              .fillMaxHeight()
              .background(
                Brush.horizontalGradient(
                  colors = listOf(SankalanTokens.PaperShadow, Color(0xFF8C735B), SankalanTokens.PaperShadow)
                )
              )
        )

        // Right Page: Memory Heading & Content Excerpt
        Box(
          modifier =
            Modifier
              .weight(1.25f)
              .fillMaxHeight()
              .clip(RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp))
              .background(paperBg)
              .padding(10.dp),
        ) {
          Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
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
              color = SankalanTokens.Ink.copy(alpha = 0.85f),
              maxLines = 5,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}
