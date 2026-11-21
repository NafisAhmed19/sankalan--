package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.model.CanvasElement
import com.example.core.model.Diary
import com.example.core.model.DiaryWithPages
import com.example.core.model.Page
import com.example.features.calendar.CalendarIcsExporter
import com.example.features.insights.DailyPromptCatalog
import com.example.features.insights.GlobalLibrarySearch
import com.example.features.insights.GlobalSearchHit
import com.example.features.insights.MoodTrackerAggregator
import com.example.features.security.AppLockManager
import com.example.features.security.PinHasher
import com.example.features.studio.ALL_STUDIO_ASSETS
import com.example.features.studio.ASSET_PACK_1
import com.example.features.studio.ASSET_PACK_2_FLORALS
import com.example.features.studio.ASSET_PACK_3_EPHEMERA_SEASONAL
import com.example.features.studio.StudioCategory
import com.example.features.studio.findStudioAsset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class InsightsSearchMoodAndLockTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context.getSharedPreferences("sankalan_lock_store", Context.MODE_PRIVATE).edit().clear().commit()
  }

  @Test
  fun `asset packs 2 and 3 extend catalog with unique ids and bilingual metadata`() {
    assertEquals(8, ASSET_PACK_2_FLORALS.size)
    assertEquals(10, ASSET_PACK_3_EPHEMERA_SEASONAL.size)
    assertEquals(
      ASSET_PACK_1.size + ASSET_PACK_2_FLORALS.size + ASSET_PACK_3_EPHEMERA_SEASONAL.size,
      ALL_STUDIO_ASSETS.size,
    )
    assertEquals(ALL_STUDIO_ASSETS.size, ALL_STUDIO_ASSETS.map { it.id }.toSet().size)

    assertTrue(ALL_STUDIO_ASSETS.any { it.category == StudioCategory.BOTANICAL_FLORALS && it.id == "floral_shapla_lotus" })
    assertTrue(ALL_STUDIO_ASSETS.any { it.category == StudioCategory.EPHEMERA && it.id == "ephem_film_strip" })
    assertTrue(ALL_STUDIO_ASSETS.any { it.category == StudioCategory.SEASONAL && it.id == "season_autumn_shiuli" })

    ALL_STUDIO_ASSETS.forEach { asset ->
      assertTrue(asset.nameEn.isNotBlank() && asset.nameBn.isNotBlank())
      assertTrue(asset.defaultW > 0f && asset.defaultH > 0f)
    }

    assertNotNull(findStudioAsset("season_monsoon_boat"))
    assertNull(findStudioAsset("non_existent_asset"))
  }

  @Test
  fun `mood tracker summarizes active logged pages and normalized mood fractions`() {
    val diary =
      DiaryWithPages(
        diary = Diary(id = "d1", title = "Autumn Notes", pageIds = listOf("p1", "p2", "p3", "p4")),
        pages =
          listOf(
            Page(
              id = "p1",
              diaryId = "d1",
              order = 0,
              dateIso = "2026-11-10",
              title = "One",
              mood = "Peaceful",
            ),
            Page(
              id = "p2",
              diaryId = "d1",
              order = 1,
              dateIso = "2026-11-11",
              title = "Two",
              mood = "Peaceful",
            ),
            Page(
              id = "p3",
              diaryId = "d1",
              order = 2,
              dateIso = "2026-11-11",
              title = "Three",
              mood = "Grateful",
            ),
            Page(
              id = "p4",
              diaryId = "d1",
              order = 3,
              elements = emptyList(), // blank page excluded from logged count
            ),
          ),
      )

    val summary = MoodTrackerAggregator.summarize(listOf(diary))
    assertEquals(3, summary.totalLoggedPages)
    assertEquals(2, summary.activeDaysCount)
    assertEquals(2, summary.moodBreakdown.size)
    assertEquals("Peaceful", summary.moodBreakdown[0].moodKey)
    assertEquals(2, summary.moodBreakdown[0].count)
    assertEquals(2f / 3f, summary.moodBreakdown[0].fraction, 0.001f)
    assertEquals("Grateful", summary.moodBreakdown[1].moodKey)
    assertEquals(1, summary.moodBreakdown[1].count)
  }

  @Test
  fun `daily prompt catalog returns valid prompts deterministically`() {
    assertTrue(DailyPromptCatalog.prompts.size >= 7)
    val today = DailyPromptCatalog.forToday(0)
    assertEquals(today.id, DailyPromptCatalog.forToday(DailyPromptCatalog.prompts.size).id)
    DailyPromptCatalog.prompts.forEach { prompt ->
      assertTrue(prompt.quoteEn.isNotBlank() && prompt.quoteBn.isNotBlank())
      assertTrue(prompt.promptEn.isNotBlank() && prompt.promptBn.isNotBlank())
    }
  }

  @Test
  fun `global search handles NFC Bangla conjuncts and English across diaries and pages`() {
    val pages =
      listOf(
        Page(
          id = "p1",
          diaryId = "d1",
          order = 0,
          dateIso = "2026-11-13",
          title = "কৃতজ্ঞতার পাতা",
          mood = "Grateful",
          elements =
            listOf(
              CanvasElement(
                id = "t1",
                type = "text",
                text = "সংকলন, স্মৃতি, শ্রদ্ধা, প্রিয়, স্বাধীনতা, দৃষ্টান্ত, কৃতজ্ঞতা, বইয়ের পাতা",
              )
            ),
        ),
        Page(
          id = "p2",
          diaryId = "d1",
          order = 1,
          title = "Tea Log",
          elements = listOf(CanvasElement(id = "t2", type = "text", text = "Evening rain by the window.")),
        ),
      )
    val bundle = DiaryWithPages(diary = Diary(id = "d1", title = "স্মৃতির খাতা · Autumn Journal", pageIds = pages.map { it.id }), pages = pages)

    val titleHits = GlobalLibrarySearch.search(listOf(bundle), "autumn")
    assertTrue(titleHits.any { it is GlobalSearchHit.DiaryHit && it.bundle.diary.id == "d1" })

    val banglaConjunctHits = GlobalLibrarySearch.search(listOf(bundle), "স্বাধীনতা")
    assertEquals(1, banglaConjunctHits.size)
    val pageHit = banglaConjunctHits.first() as GlobalSearchHit.PageHit
    assertEquals("p1", pageHit.page.id)
    assertEquals(0, pageHit.pageIndex)

    // Decomposed NFD-like Bengali query still matches NFC text
    val composedQueryHits = GlobalLibrarySearch.search(listOf(bundle), "বইয়ের পাতা")
    assertTrue(composedQueryHits.any { it is GlobalSearchHit.PageHit && it.page.id == "p1" })

    assertTrue(GlobalLibrarySearch.search(listOf(bundle), "   ").isEmpty())
  }

  @Test
  fun `ics exporter generates RFC 5545 calendar with recurring reminder and dated page milestones`() {
    val bundle =
      DiaryWithPages(
        diary = Diary(id = "d1", title = "My Archive", pageIds = listOf("p1")),
        pages =
          listOf(
            Page(
              id = "p1",
              diaryId = "d1",
              order = 0,
              dateIso = "2026-11-13",
              title = "Autumn Reflection",
              elements = listOf(CanvasElement(id = "e1", type = "text", text = "Quiet evening notes, warm tea.")),
            )
          ),
      )
    val ics =
      CalendarIcsExporter.buildIcs(
        diaries = listOf(bundle),
        datedPages = bundle.pages,
        reminderEnabled = true,
        reminderHour = 20,
        reminderMinute = 0,
        includeBanglaTitle = true,
      )

    assertTrue(ics.contains("BEGIN:VCALENDAR\r\n"))
    assertTrue(ics.contains("VERSION:2.0\r\n"))
    assertTrue(ics.contains("RRULE:FREQ=DAILY\r\n"))
    assertTrue(ics.contains("DTSTART;VALUE=DATE:20261113\r\n"))
    assertTrue(ics.contains("সংকলন-ৎ"))
    assertTrue(ics.endsWith("END:VCALENDAR\r\n"))
  }

  @Test
  fun `app lock hashes salted SHA-256 PINs, validates length, and clears safely`() {
    assertFalse(PinHasher.isValidPinFormat("123"))
    assertFalse(PinHasher.isValidPinFormat("12a4"))
    assertTrue(PinHasher.isValidPinFormat("2580"))
    assertTrue(PinHasher.isValidPinFormat("123456"))

    val lockManager = AppLockManager(context)
    assertFalse(lockManager.hasPin())
    assertFalse(lockManager.setPin("12"))
    assertTrue(lockManager.setPin("2580"))
    assertTrue(lockManager.hasPin())

    assertTrue(lockManager.verifyPin("2580"))
    assertFalse(lockManager.verifyPin("9999"))

    lockManager.clearPin()
    assertFalse(lockManager.hasPin())
    assertFalse(lockManager.verifyPin("2580"))
  }

  @Test
  fun `app lock enforces brute force cooldown after 5 wrong attempts and upgrades legacy v1 hashes`() {
    var fakeNowMs = 1_000_000L
    val lockManager = AppLockManager(context) { fakeNowMs }
    assertTrue(lockManager.setPin("4321"))

    // 4 wrong attempts do not lock yet
    repeat(4) {
      assertFalse(lockManager.verifyPin("0000"))
      assertEquals(0, lockManager.remainingLockoutSeconds())
    }
    // 5th wrong attempt triggers 30s cooldown
    assertFalse(lockManager.verifyPin("0000"))
    assertEquals(30, lockManager.remainingLockoutSeconds())
    // Even the correct PIN is rejected during active cooldown
    assertFalse(lockManager.verifyPin("4321"))

    // Advance clock past cooldown -> correct PIN unlocks and resets failed counter
    fakeNowMs += 31_000L
    assertEquals(0, lockManager.remainingLockoutSeconds())
    assertTrue(lockManager.verifyPin("4321"))
    assertEquals(0, lockManager.failedAttemptsCount())

    // Simulate legacy v1 salted SHA-256 PIN stored on disk and verify transparent upgrade to v2 PBKDF2
    val prefs = context.getSharedPreferences("sankalan_lock_store", Context.MODE_PRIVATE)
    val legacySalt = PinHasher.generateSalt()
    val legacyHash = PinHasher.hashPin("7890", legacySalt)
    prefs.edit()
      .putString("pin_salt_v1", legacySalt)
      .putString("pin_hash_v1", legacyHash)
      .remove("pin_algo_v2")
      .commit()
    assertTrue(lockManager.verifyPin("7890"))
    val upgradedAlgo = prefs.getString("pin_algo_v2", null)
    assertNotNull(upgradedAlgo)
    assertTrue(upgradedAlgo!!.startsWith("pbkdf2_"))
    assertTrue(lockManager.verifyPin("7890"))
  }
}
