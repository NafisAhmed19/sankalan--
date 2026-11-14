package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.data.DiaryRepository
import com.example.core.data.local.SankalanDatabase
import com.example.core.model.CanvasElement
import com.example.core.model.DiaryInvariants
import com.example.core.model.Page
import com.example.core.model.PageBackground
import com.example.features.editor.formatDurationMmSs
import com.example.features.studio.ASSET_PACK_1
import com.example.features.studio.PAGE_TEMPLATES
import com.example.features.studio.PAPER_PRESETS
import com.example.features.studio.StudioCategory
import com.example.features.studio.StudioUserStore
import com.example.features.studio.applyTemplateToPage
import com.example.features.studio.findPaperPreset
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
class StudioAndTemplatesTest {

  private lateinit var context: Context
  private lateinit var database: SankalanDatabase
  private lateinit var repository: DiaryRepository

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context.getSharedPreferences("sankalan_seed_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    context.getSharedPreferences("sankalan_studio_store", Context.MODE_PRIVATE).edit().clear().commit()
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
  fun `paper catalog has 22 unique presets and flags dark archival papers`() {
    assertEquals(22, PAPER_PRESETS.size)
    assertEquals(22, PAPER_PRESETS.map { it.id }.toSet().size)

    val darkPapers = PAPER_PRESETS.filter { it.isDarkPaper }.map { it.id }.toSet()
    assertTrue("midnight_ink" in darkPapers)
    assertTrue("walnut_espresso" in darkPapers)
    assertTrue("obsidian_gold" in darkPapers)

    assertEquals("warm_ivory", findPaperPreset("non_existent_id").id)
    assertEquals("torn", findPaperPreset("tea_stained").defaultEdge)
  }

  @Test
  fun `template catalog has 12 unique templates with valid normalized bounds in both languages`() {
    assertEquals(12, PAGE_TEMPLATES.size)
    assertEquals(12, PAGE_TEMPLATES.map { it.id }.toSet().size)

    PAGE_TEMPLATES.forEach { tpl ->
      listOf(false, true).forEach { bn ->
        val els = tpl.elementsFactory(bn)
        assertTrue("Template ${tpl.id} should produce elements", els.isNotEmpty())
        els.forEach { el ->
          assertTrue("${tpl.id} element ${el.type} width must be positive", el.w > 0f && el.w <= 1f)
          assertTrue("${tpl.id} element ${el.type} height must be positive", el.h > 0f && el.h <= 1f)
        }
      }
    }
  }

  @Test
  fun `asset pack 1 covers washi tape, frames, labels, and stamps cleanly`() {
    assertEquals(ASSET_PACK_1.size, ASSET_PACK_1.map { it.id }.toSet().size)
    val byCat = ASSET_PACK_1.groupBy { it.category }
    assertTrue((byCat[StudioCategory.WASHI_TAPE]?.size ?: 0) >= 4)
    assertTrue((byCat[StudioCategory.FRAMES]?.size ?: 0) >= 3)
    assertTrue((byCat[StudioCategory.LABELS]?.size ?: 0) >= 3)
    assertTrue((byCat[StudioCategory.STAMPS]?.size ?: 0) >= 5)
    ASSET_PACK_1.forEach { asset ->
      assertTrue(asset.defaultW > 0f && asset.defaultH > 0f)
      assertTrue(asset.nameEn.isNotBlank() && asset.nameBn.isNotBlank())
    }
  }

  @Test
  fun `applyTemplateToPage supports both safe overlay and replace without losing data`() {
    val existingNote =
      CanvasElement(
        id = "user-note-1",
        type = "text",
        x = 0.1f,
        y = 0.2f,
        w = 0.8f,
        h = 0.2f,
        text = "My precious memory",
        zIndex = 5,
      )
    val basePage =
      Page(
        id = "p-1",
        diaryId = "d-1",
        order = 0,
        title = "Existing Title",
        elements = listOf(existingNote),
      )
    val tpl = PAGE_TEMPLATES.first { it.id == "tpl_travel" }

    // Safe Overlay preserves user element + shifts template zIndex above 5
    val overlaid = applyTemplateToPage(basePage, tpl, replaceExisting = false, isBangla = true)
    assertTrue(overlaid.elements.any { it.id == "user-note-1" && it.text == "My precious memory" })
    val newTemplateEls = overlaid.elements.filterNot { it.id == "user-note-1" }
    assertTrue(newTemplateEls.isNotEmpty())
    assertTrue(newTemplateEls.all { it.zIndex > 5 })
    assertEquals(tpl.background.presetId, overlaid.background.presetId)

    // Explicit Replace substitutes existing elements cleanly
    val replaced = applyTemplateToPage(basePage, tpl, replaceExisting = true, isBangla = false)
    assertFalse(replaced.elements.any { it.id == "user-note-1" })
    assertEquals(tpl.background.presetId, replaced.background.presetId)
    assertEquals("Existing Title", replaced.title)
  }

  @Test
  fun `studio favorites store toggles and persists across instances and repository saves studio elements`() =
    runBlocking {
      val store = StudioUserStore(context)
      assertTrue(store.loadFavorites().isEmpty())

      store.toggleFavorite("tape_gold_foil")
      store.toggleFavorite("obsidian_gold")
      val reloadedStore = StudioUserStore(context)
      assertEquals(setOf("tape_gold_foil", "obsidian_gold"), reloadedStore.loadFavorites())

      reloadedStore.toggleFavorite("tape_gold_foil")
      assertEquals(setOf("obsidian_gold"), StudioUserStore(context).loadFavorites())

      assertEquals("0:05", formatDurationMmSs(5))
      assertEquals("2:07", formatDurationMmSs(127))

      repository.seedDemoDiaryIfFirstLaunch()
      val bundle = repository.allDiariesFlow.first().first()
      val targetPage = bundle.pages.last()

      val studioElements =
        listOf(
          CanvasElement(id = "s-tape", type = "tape", assetId = "tape_gold_foil", x = 0.2f, y = 0.08f, w = 0.36f, h = 0.06f, zIndex = 1),
          CanvasElement(id = "s-frame", type = "frame", assetId = "frame_double_gold", x = 0.1f, y = 0.2f, w = 0.6f, h = 0.35f, zIndex = 2),
          CanvasElement(id = "s-label", type = "label", assetId = "label_ribbon_gold", text = "স্মৃতিকথা", x = 0.2f, y = 0.58f, w = 0.44f, h = 0.10f, zIndex = 3),
          CanvasElement(id = "s-stamp", type = "sticker", assetId = "stamp_wax_sankalan", text = "ৎ", x = 0.7f, y = 0.75f, w = 0.18f, h = 0.14f, zIndex = 4),
          CanvasElement(id = "s-voice", type = "voice", blobRef = "/tmp/clip.m4a", fontSize = 18f, text = "ভয়েস মেমোরি", x = 0.14f, y = 0.72f, w = 0.70f, h = 0.11f, zIndex = 5),
        )
      val savedBg = PageBackground(presetId = "obsidian_gold", tintHex = "#1D1B1A", edgeStyle = "torn", opacity = 0.92f)
      val saveRes =
        repository.savePageChanges(
          bundle.diary.id,
          targetPage.copy(background = savedBg, elements = studioElements),
        )
      assertTrue(saveRes.isSuccess)

      val reloaded = repository.observeDiary(bundle.diary.id).first()!!
      assertTrue(DiaryInvariants.validateDiaryPages(reloaded.diary, reloaded.pages).isSuccess)
      val persistedPage = reloaded.pages.first { it.id == targetPage.id }
      assertEquals("obsidian_gold", persistedPage.background.presetId)
      assertEquals("torn", persistedPage.background.edgeStyle)
      assertEquals(0.92f, persistedPage.background.opacity, 0.001f)
      assertEquals(5, persistedPage.elements.size)
      assertEquals("voice", persistedPage.elements.last().type)
      assertEquals(18f, persistedPage.elements.last().fontSize ?: 0f, 0.01f)
    }
}
