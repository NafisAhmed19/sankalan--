package com.example.features.reading

import com.example.core.model.Diary
import com.example.core.model.Page
import com.example.core.model.ReadingLayout

/**
 * Canonical Reading Slots implementing Decision Record D-15:
 * Cover -> Page 1 alone -> 2-page spreads (2–3, 4–5...) -> remaining odd last page alone -> Back Cover.
 */
sealed interface ReadingSlot {
  data class FrontCover(val diary: Diary, val totalPages: Int) : ReadingSlot
  data class SinglePage(val page: Page, val pageIndex: Int, val totalPages: Int) : ReadingSlot
  data class SpreadPages(
    val leftPage: Page,
    val leftIndex: Int,
    val rightPage: Page,
    val rightIndex: Int,
    val totalPages: Int,
  ) : ReadingSlot
  data class BackCover(val diary: Diary, val totalPages: Int) : ReadingSlot
}

object ReadingSlotBuilder {
  fun buildSlots(
    diary: Diary,
    activePages: List<Page>,
    layout: ReadingLayout,
  ): List<ReadingSlot> {
    val ordered = activePages.filter { !it.isDeleted }.sortedBy { it.order }
    val total = ordered.size
    val slots = mutableListOf<ReadingSlot>()
    slots.add(ReadingSlot.FrontCover(diary = diary, totalPages = total))

    if (ordered.isEmpty()) {
      slots.add(ReadingSlot.BackCover(diary = diary, totalPages = 0))
      return slots
    }

    if (layout == ReadingLayout.SINGLE) {
      ordered.forEachIndexed { idx, page ->
        slots.add(ReadingSlot.SinglePage(page = page, pageIndex = idx, totalPages = total))
      }
    } else {
      // Page 1 alone (index 0) per traditional hardcover opening
      slots.add(ReadingSlot.SinglePage(page = ordered[0], pageIndex = 0, totalPages = total))
      var idx = 1
      while (idx < ordered.size) {
        if (idx + 1 < ordered.size) {
          slots.add(
            ReadingSlot.SpreadPages(
              leftPage = ordered[idx],
              leftIndex = idx,
              rightPage = ordered[idx + 1],
              rightIndex = idx + 1,
              totalPages = total,
            )
          )
          idx += 2
        } else {
          // Odd remaining last page sits alone before Back Cover
          slots.add(
            ReadingSlot.SinglePage(
              page = ordered[idx],
              pageIndex = idx,
              totalPages = total,
            )
          )
          idx += 1
        }
      }
    }

    slots.add(ReadingSlot.BackCover(diary = diary, totalPages = total))
    return slots
  }

  fun findSlotIndexForPageId(slots: List<ReadingSlot>, pageId: String?): Int {
    if (pageId == null) return 0
    val matchIdx =
      slots.indexOfFirst { slot ->
        when (slot) {
          is ReadingSlot.SinglePage -> slot.page.id == pageId
          is ReadingSlot.SpreadPages -> slot.leftPage.id == pageId || slot.rightPage.id == pageId
          is ReadingSlot.FrontCover,
          is ReadingSlot.BackCover -> false
        }
      }
    return if (matchIdx >= 0) matchIdx else 0
  }

  fun primaryPageId(slot: ReadingSlot?): String? =
    when (slot) {
      is ReadingSlot.SinglePage -> slot.page.id
      is ReadingSlot.SpreadPages -> slot.leftPage.id
      is ReadingSlot.FrontCover,
      is ReadingSlot.BackCover,
      null -> null
    }
}

/**
 * Pure physics & gesture threshold helpers for corner/edge finger page-turning.
 */
object PageTurnPhysics {
  fun isPageTurnDragSlope(
    dx: Float,
    dy: Float,
    startedNearCornerOrEdge: Boolean,
  ): Boolean {
    val absDx = kotlin.math.abs(dx)
    val absDy = kotlin.math.abs(dy)
    if (absDx <= 1.5f) return false
    // Corner/side drags frequently move diagonally; allow steeper diagonal strokes near edges/corners.
    val slopeRatio = if (startedNearCornerOrEdge) 0.35f else 0.55f
    return absDx >= absDy * slopeRatio
  }

  fun computeDragProgress(
    dragDeltaPx: Float,
    stageWidthPx: Float,
    currentProgress: Float,
    canFlipForward: Boolean,
    canFlipBackward: Boolean,
  ): Float {
    val normDelta = dragDeltaPx / (stageWidthPx.coerceAtLeast(1f) * 0.80f)
    val raw = currentProgress + normDelta
    return when {
      raw < 0f && !canFlipForward -> (currentProgress + normDelta * 0.22f).coerceIn(-0.20f, 0.20f)
      raw > 0f && !canFlipBackward -> (currentProgress + normDelta * 0.22f).coerceIn(-0.20f, 0.20f)
      else -> raw.coerceIn(-0.98f, 0.98f)
    }
  }

  fun resolveSwipeOutcome(
    progress: Float,
    velocityX: Float,
    currentSlotIndex: Int,
    lastSlotIndex: Int,
  ): Int {
    if (lastSlotIndex <= 0) return currentSlotIndex
    val clampedCur = currentSlotIndex.coerceIn(0, lastSlotIndex)
    return when {
      clampedCur < lastSlotIndex &&
        (progress <= -0.16f || (progress <= -0.04f && velocityX <= -360f)) -> clampedCur + 1
      clampedCur > 0 &&
        (progress >= 0.16f || (progress >= 0.04f && velocityX >= 360f)) -> clampedCur - 1
      else -> clampedCur
    }
  }
}

