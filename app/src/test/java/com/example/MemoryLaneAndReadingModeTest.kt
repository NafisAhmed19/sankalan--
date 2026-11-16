package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.data.DiaryRepository
import com.example.core.data.local.SankalanDatabase
import com.example.core.model.CanvasElement
import com.example.core.model.Diary
import com.example.core.model.DiaryInvariants
import com.example.core.model.DiaryWithPages
import com.example.core.model.Page
import com.example.core.model.ReadingLayout
import com.example.features.memorylane.MemoryLaneCurator
import com.example.features.memorylane.MemoryLaneReason
import com.example.features.reading.PageTurnPhysics
import com.example.features.reading.ReadingSlot
import com.example.features.reading.ReadingSlotBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MemoryLaneAndReadingModeTest {

  private lateinit var context: Context
  private lateinit var database: SankalanDatabase
  private lateinit var repository: DiaryRepository

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context.getSharedPreferences("sankalan_seed_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    database =
      Room.inMemoryDatabaseBuilder(context, SankalanDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    repository = DiaryRepository(context = context, dao = database.diaryDao())
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `memory lane excludes archived books and blank pages while prioritizing pinned and on-this-day`() {
    val activePages =
      listOf(
        Page(
          id = "p-curated",
          diaryId = "d-active",
          order = 0,
          dateIso = "2026-01-10",
          title = "Regular entry",
          pinned = false,
          elements = listOf(CanvasElement(id = "e1", type = "text", text = "Hello")),
        ),
        Page(
          id = "p-blank",
          diaryId = "d-active",
          order = 1,
          title = null,
          elements = emptyList(),
        ),
        Page(
          id = "p-pinned",
          diaryId = "d-active",
          order = 2,
          dateIso = "2026-02-05",
          title = "Pinned memory",
          pinned = true,
        ),
        Page(
          id = "p-otd",
          diaryId = "d-active",
          order = 3,
          dateIso = "2024-11-13",
          title = "On This Day reflection",
          pinned = false,
        ),
        Page(id = "p-5", diaryId = "d-active", order = 4),
      )

    val archivedDiary =
      DiaryWithPages(
        diary = Diary(id = "d-arch", title = "Archived", pageIds = listOf("pa-1"), archived = true),
        pages =
          listOf(
            Page(
              id = "pa-1",
              diaryId = "d-arch",
              order = 0,
              dateIso = "2024-11-13",
              title = "Should be excluded",
              pinned = true,
            )
          ),
      )

    val activeDiary =
      DiaryWithPages(
        diary = Diary(id = "d-active", title = "Active", pageIds = activePages.map { it.id }),
        pages = activePages,
      )

    val curated =
      MemoryLaneCurator.buildCuratedMemories(
        diaries = listOf(archivedDiary, activeDiary),
        monthDaySuffix = "-11-13",
      )

    assertEquals(3, curated.size)
    assertFalse(curated.any { it.diaryWithPages.diary.id == "d-arch" })
    assertFalse(curated.any { it.page.id == "p-blank" })
    assertEquals("p-otd", curated[0].page.id)
    assertEquals(MemoryLaneReason.ON_THIS_DAY, curated[0].reason)
    assertEquals("p-pinned", curated[1].page.id)
    assertEquals(MemoryLaneReason.PINNED, curated[1].reason)
    assertEquals("p-curated", curated[2].page.id)
    assertEquals(MemoryLaneReason.CURATED, curated[2].reason)
  }

  @Test
  fun `reading slot builder follows D-15 order for odd and even page counts and preserves Page id`() {
    val pages6 = (0 until 6).map { idx -> Page(id = "id-$idx", diaryId = "d1", order = idx) }
    val diary6 = Diary(id = "d1", title = "Book 6", pageIds = pages6.map { it.id })

    val singleSlots = ReadingSlotBuilder.buildSlots(diary6, pages6, ReadingLayout.SINGLE)
    assertEquals(8, singleSlots.size) // Cover + 6 pages + Back
    assertTrue(singleSlots[0] is ReadingSlot.FrontCover)
    assertTrue(singleSlots[1] is ReadingSlot.SinglePage)
    assertTrue(singleSlots[7] is ReadingSlot.BackCover)

    val spreadSlots6 = ReadingSlotBuilder.buildSlots(diary6, pages6, ReadingLayout.SPREAD)
    // Cover, Page 1 alone, Spread(2,3), Spread(4,5), Page 6 alone, Back Cover -> 6 slots
    assertEquals(6, spreadSlots6.size)
    assertTrue(spreadSlots6[0] is ReadingSlot.FrontCover)
    assertEquals(0, (spreadSlots6[1] as ReadingSlot.SinglePage).pageIndex)
    assertEquals(1, (spreadSlots6[2] as ReadingSlot.SpreadPages).leftIndex)
    assertEquals(2, (spreadSlots6[2] as ReadingSlot.SpreadPages).rightIndex)
    assertEquals(3, (spreadSlots6[3] as ReadingSlot.SpreadPages).leftIndex)
    assertEquals(4, (spreadSlots6[3] as ReadingSlot.SpreadPages).rightIndex)
    assertEquals(5, (spreadSlots6[4] as ReadingSlot.SinglePage).pageIndex)
    assertTrue(spreadSlots6[5] is ReadingSlot.BackCover)

    // Verify preserving Page id across layout toggle
    val slotForPage4InSpread = ReadingSlotBuilder.findSlotIndexForPageId(spreadSlots6, "id-3")
    assertEquals(3, slotForPage4InSpread)
    val slotForPage4InSingle = ReadingSlotBuilder.findSlotIndexForPageId(singleSlots, "id-3")
    assertEquals(4, slotForPage4InSingle)

    // 5 pages spread: Cover -> Page 1 -> (2,3) -> (4,5) -> Back Cover
    val pages5 = pages6.take(5)
    val spreadSlots5 = ReadingSlotBuilder.buildSlots(diary6, pages5, ReadingLayout.SPREAD)
    assertEquals(5, spreadSlots5.size)
    assertTrue(spreadSlots5[4] is ReadingSlot.BackCover)
  }

  @Test
  fun `page manager reorder preserves Page ids and soft-delete enforces min 5 and recovery`() = runBlocking {
    repository.seedDemoDiaryIfFirstLaunch()
    val demo = repository.allDiariesFlow.first().first()
    val originalIds = demo.pages.map { it.id }
    assertEquals(6, originalIds.size)

    // Reorder: move last page to first while keeping exact Page IDs
    val reorderedIds = listOf(originalIds.last()) + originalIds.dropLast(1)
    assertTrue(repository.reorderPages(demo.diary.id, reorderedIds).isSuccess)

    val afterReorder = repository.allDiariesFlow.first().first()
    assertEquals(reorderedIds, afterReorder.pages.map { it.id })
    assertTrue(DiaryInvariants.validateDiaryPages(afterReorder.diary, afterReorder.pages).isSuccess)

    // Soft-delete 1 page (down from 6 -> 5): succeeds and moves to Recently Deleted
    val deletedTargetId = afterReorder.pages[1].id
    assertTrue(repository.softDeletePage(demo.diary.id, deletedTargetId).isSuccess)
    val afterDelete = repository.allDiariesFlow.first().first()
    assertEquals(5, afterDelete.pages.size)
    assertEquals(1, afterDelete.deletedPages.size)
    assertEquals(deletedTargetId, afterDelete.deletedPages.first().id)

    // Attempt to delete when active count == 5 must fail!
    val secondDelete = repository.softDeletePage(demo.diary.id, afterDelete.pages.first().id)
    assertTrue(secondDelete.isFailure)
    assertEquals(5, repository.allDiariesFlow.first().first().pages.size)

    // Restore from Recently Deleted recovers exact Page.id back to active pages (5 -> 6)
    assertTrue(repository.restoreDeletedPage(demo.diary.id, deletedTargetId).isSuccess)
    val afterRestore = repository.allDiariesFlow.first().first()
    assertEquals(6, afterRestore.pages.size)
    assertTrue(afterRestore.deletedPages.isEmpty())
    assertTrue(afterRestore.pages.any { it.id == deletedTargetId })

    // Pin toggle & duplicate & reset
    assertTrue(repository.togglePinPage(demo.diary.id, deletedTargetId).isSuccess)
    assertTrue(repository.duplicatePage(demo.diary.id, deletedTargetId).isSuccess)
    assertEquals(7, repository.allDiariesFlow.first().first().pages.size)
    assertTrue(repository.resetPage(demo.diary.id, deletedTargetId).isSuccess)
    val resetPage = repository.allDiariesFlow.first().first().pages.first { it.id == deletedTargetId }
    assertTrue(resetPage.elements.isEmpty())
    assertFalse(resetPage.pinned)
  }

  @Test
  fun `page turn physics accepts diagonal corner drags and flips on distance or velocity`() {
    // Diagonal corner drag near edge/corner must be accepted even if vertical delta is sizable
    assertTrue(PageTurnPhysics.isPageTurnDragSlope(dx = -28f, dy = -60f, startedNearCornerOrEdge = true))
    assertTrue(PageTurnPhysics.isPageTurnDragSlope(dx = 40f, dy = 20f, startedNearCornerOrEdge = false))
    assertFalse(PageTurnPhysics.isPageTurnDragSlope(dx = 6f, dy = 80f, startedNearCornerOrEdge = false))

    // Elastic resistance when trying to drag past FrontCover (index 0) backward
    val blockedBack =
      PageTurnPhysics.computeDragProgress(
        dragDeltaPx = 300f,
        stageWidthPx = 400f,
        currentProgress = 0f,
        canFlipForward = true,
        canFlipBackward = false,
      )
    assertTrue(blockedBack in 0f..0.20f)

    // Unblocked left-drag advances progress smoothly
    val forwardProgress =
      PageTurnPhysics.computeDragProgress(
        dragDeltaPx = -120f,
        stageWidthPx = 400f,
        currentProgress = 0f,
        canFlipForward = true,
        canFlipBackward = true,
      )
    assertTrue(forwardProgress < -0.25f)

    // Distance flip forward & backward
    assertEquals(2, PageTurnPhysics.resolveSwipeOutcome(progress = -0.20f, velocityX = 0f, currentSlotIndex = 1, lastSlotIndex = 5))
    assertEquals(0, PageTurnPhysics.resolveSwipeOutcome(progress = 0.20f, velocityX = 0f, currentSlotIndex = 1, lastSlotIndex = 5))

    // Quick flick flips even at small drag progress
    assertEquals(2, PageTurnPhysics.resolveSwipeOutcome(progress = -0.06f, velocityX = -520f, currentSlotIndex = 1, lastSlotIndex = 5))
    assertEquals(0, PageTurnPhysics.resolveSwipeOutcome(progress = 0.06f, velocityX = 520f, currentSlotIndex = 1, lastSlotIndex = 5))

    // Tiny accidental nudge stays on current page
    assertEquals(1, PageTurnPhysics.resolveSwipeOutcome(progress = -0.05f, velocityX = -50f, currentSlotIndex = 1, lastSlotIndex = 5))
  }
}
