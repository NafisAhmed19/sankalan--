package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.data.DiaryRepository
import com.example.core.data.local.SankalanDatabase
import com.example.core.model.CoverStyle
import com.example.core.model.Diary
import com.example.core.model.DiaryCover
import com.example.core.model.DiaryInvariants
import com.example.core.model.Page
import com.example.core.model.SaveStatus
import com.example.core.model.SpineStyle
import com.example.core.model.Visibility
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
class DiaryInvariantsAndRepoTest {

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
  fun `page count invariants enforce 5 to 200 bounds`() {
    assertFalse(DiaryInvariants.canDeletePages(currentActiveCount = 5))
    assertTrue(DiaryInvariants.canDeletePages(currentActiveCount = 6))
    assertTrue(DiaryInvariants.canAddPages(currentActiveCount = 199))
    assertFalse(DiaryInvariants.canAddPages(currentActiveCount = 200))
    assertEquals(5, DiaryInvariants.validateInitialPageCount(2))
    assertEquals(12, DiaryInvariants.validateInitialPageCount(12))
    assertEquals(200, DiaryInvariants.validateInitialPageCount(250))
  }

  @Test
  fun `validateDiaryPages rejects duplicate order or orphan pageIds`() {
    val pages =
      (0 until 5).map { idx ->
        Page(id = "p-$idx", diaryId = "d-1", order = idx)
      }
    val validDiary = Diary(id = "d-1", title = "Valid", pageIds = pages.map { it.id })
    assertTrue(DiaryInvariants.validateDiaryPages(validDiary, pages).isSuccess)

    val duplicateOrder = pages.toMutableList().apply { this[4] = this[4].copy(order = 0) }
    assertTrue(DiaryInvariants.validateDiaryPages(validDiary, duplicateOrder).isFailure)

    val mismatchedDiary = validDiary.copy(pageIds = listOf("p-0", "p-1", "p-2", "p-3", "missing"))
    assertTrue(DiaryInvariants.validateDiaryPages(mismatchedDiary, pages).isFailure)
  }

  @Test
  fun `normalizeForSearch unifies Bangla NFC forms and trims English case`() {
    // Decomposed ড + ় vs precomposed ড় (U+09DC)
    val decomposed = "\u09A1\u09BC\u09BE\u09AF\u09BC\u09C7\u09B0\u09BF"
    val precomposed = "\u09DC\u09BE\u09DF\u09C7\u09B0\u09BF"
    assertEquals(
      DiaryRepository.normalizeForSearch(precomposed),
      DiaryRepository.normalizeForSearch(decomposed),
    )
    assertEquals("autumn journal", DiaryRepository.normalizeForSearch("  Autumn JOURNAL "))
  }

  @Test
  fun `demo diary seeds 6 pages once and creates new journals with min 5 pages`() = runBlocking {
    repository.seedDemoDiaryIfFirstLaunch()
    repository.seedDemoDiaryIfFirstLaunch() // Should be idempotent

    val afterSeed = repository.allDiariesFlow.first()
    assertEquals(1, afterSeed.size)
    val demo = afterSeed.first()
    assertTrue(demo.diary.isDemo)
    assertEquals(Visibility.PRIVATE, demo.diary.visibility)
    assertEquals(6, demo.pages.size)
    assertTrue(DiaryInvariants.validateDiaryPages(demo.diary, demo.pages).isSuccess)

    // Create new diary requesting <5 pages; repository coerces to 5 active pages
    val createResult =
      repository.createDiary(
        title = "রাতের কাব্য",
        description = "Short poems",
        cover =
          DiaryCover(
            style = CoverStyle.ARTISTIC,
            colorHex = "#4E5E48",
            accentHex = "#F4EBDD",
            spineStyle = SpineStyle.STITCHED_CLOTH,
          ),
        initialPageCount = 3,
      )
    assertTrue(createResult.isSuccess)
    assertEquals(SaveStatus.Saved, repository.saveStatus.value)

    val all = repository.allDiariesFlow.first()
    assertEquals(2, all.size)
    val created = all.first { it.diary.id == createResult.getOrThrow() }
    assertEquals(5, created.pages.size)
    assertTrue(DiaryInvariants.validateDiaryPages(created.diary, created.pages).isSuccess)

    // Add dated page and verify dated query
    val datedRes =
      repository.addDatedPage(
        diaryId = created.diary.id,
        dateIso = "2026-11-15",
        title = "শীতের আগমন",
        bodyText = "প্রথম কুয়াশা",
      )
    assertTrue(datedRes.isSuccess)
    val datedPages = repository.allDatedPagesFlow.first()
    assertTrue(datedPages.any { it.dateIso == "2026-11-15" && it.title == "শীতের আগমন" })

    // Archive and restore
    repository.setArchived(created.diary.id, true)
    assertTrue(repository.allDiariesFlow.first().first { it.diary.id == created.diary.id }.diary.archived)
    repository.setArchived(created.diary.id, false)
    assertFalse(repository.allDiariesFlow.first().first { it.diary.id == created.diary.id }.diary.archived)
  }
}
