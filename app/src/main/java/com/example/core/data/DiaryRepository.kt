package com.example.core.data

import android.content.Context
import com.example.core.data.local.DiaryDao
import com.example.core.data.local.DiaryEntity
import com.example.core.data.local.DiaryWithPageEntities
import com.example.core.data.local.PageEntity
import com.example.core.data.local.SankalanDatabase
import com.example.core.model.CanvasElement
import com.example.core.model.CoverStyle
import com.example.core.model.Diary
import com.example.core.model.DiaryCover
import com.example.core.model.DiaryInvariants
import com.example.core.model.DiaryWithPages
import com.example.core.model.Page
import com.example.core.model.PageBackground
import com.example.core.model.SaveStatus
import com.example.core.model.SpineStyle
import com.example.core.model.Visibility
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.text.Normalizer
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class DiaryRepository(
  private val context: Context,
  private val dao: DiaryDao = SankalanDatabase.getInstance(context).diaryDao(),
) {
  private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val elementsType =
    Types.newParameterizedType(List::class.java, CanvasElement::class.java)
  private val elementsAdapter = moshi.adapter<List<CanvasElement>>(elementsType)

  private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
  val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

  private var lastPendingOperation: (suspend () -> Unit)? = null

  val allDiariesFlow: Flow<List<DiaryWithPages>> =
    dao.observeAllDiariesWithPages().map { list -> list.map { it.toDomain() } }

  val allDatedPagesFlow: Flow<List<Page>> =
    dao.observeAllDatedPages().map { list -> list.map { it.toDomain() } }

  fun observeDiary(diaryId: String): Flow<DiaryWithPages?> =
    dao.observeDiaryWithPages(diaryId).map { it?.toDomain() }

  suspend fun seedDemoDiaryIfFirstLaunch() {
    val prefs = context.getSharedPreferences(SEED_PREFS, Context.MODE_PRIVATE)
    if (prefs.getBoolean(KEY_DEMO_SEEDED, false)) return
    if (dao.countDiaries() > 0) {
      prefs.edit().putBoolean(KEY_DEMO_SEEDED, true).apply()
      return
    }

    val now = System.currentTimeMillis()
    val diaryId = "demo-diary-autumn-01"
    val pageSpecs =
      listOf(
        DemoPageSeed(
          id = "demo-page-01",
          dateIso = "2026-11-13",
          title = "প্রথম পাতা — শুরু",
          mood = "Peaceful",
          pinned = true,
          presetId = "warm_ivory",
          body =
            "আজ থেকে আমার নতুন সংকলন শুরু হলো। জীবন চলার প্রতিটি ছাপের সাথে নিজের ছোট ছোট অনুভূতিগুলো এখানে যত্ন করে রেখে দেবো।\n\nA quiet walnut-and-gold sanctuary to gather memories, sketches, and reflections.",
        ),
        DemoPageSeed(
          id = "demo-page-02",
          dateIso = "2026-11-10",
          title = "হেমন্তের বিকেল ও চা",
          mood = "Reflective",
          pinned = false,
          presetId = "antique_parchment",
          body =
            "জানালার বাইরে মৃদু বাতাস আর টেবিলের ওপর খোলা ডায়েরি। কিছু কথা মুখে বলা যায় না, কেবল কাগজেই জমে থাকে।",
        ),
        DemoPageSeed(
          id = "demo-page-03",
          dateIso = "2026-11-05",
          title = "সমুদ্রতীরে গোধূলি · Sunset Notes",
          mood = "Joyful",
          pinned = true,
          presetId = "cream_cotton",
          body =
            "ঢেউয়ের শব্দ আর দিগন্তে কমলা আলো। Every frame feels timeless when captured beside quiet words.",
        ),
        DemoPageSeed(
          id = "demo-page-04",
          dateIso = "2026-10-28",
          title = "পড়ার খাতা · Reading Log",
          mood = "Focused",
          pinned = false,
          presetId = "ruled",
          body =
            "প্রিয় উক্তি: \"যা কিছু সুন্দর ও সত্য, তা স্মৃতির পাতায় বেঁচে থাকে।\"\n\nBook notes & margin thoughts collected from late-night reading.",
        ),
        DemoPageSeed(
          id = "demo-page-05",
          dateIso = null,
          title = null,
          mood = null,
          pinned = false,
          presetId = "dot_grid",
          body = null,
        ),
        DemoPageSeed(
          id = "demo-page-06",
          dateIso = "2026-10-15",
          title = "কৃতজ্ঞতা · Gratitude",
          mood = "Grateful",
          pinned = false,
          presetId = "warm_ivory",
          body =
            "১. সুস্থ ও শান্ত সকাল\n২. প্রিয়জনদের ভালোবাসা\n৩. সংকলন, স্মৃতি, শ্রদ্ধা, প্রিয়, স্বাধীনতা, দৃষ্টান্ত, কৃতজ্ঞতা, বইয়ের পাতা — Mixed বাংলা & English 2026.",
        ),
      )

    val pages =
      pageSpecs.mapIndexed { idx, spec ->
        val elements =
          if (spec.body != null) {
            listOf(
              CanvasElement(
                id = "${spec.id}-text-1",
                type = "text",
                x = 0.08f,
                y = 0.14f,
                w = 0.84f,
                h = 0.72f,
                text = spec.body,
                colorHex = "#302C29",
                fontSize = 15f,
              )
            )
          } else {
            emptyList()
          }
        PageEntity(
          id = spec.id,
          diaryId = diaryId,
          orderIndex = idx,
          dateIso = spec.dateIso,
          title = spec.title,
          mood = spec.mood,
          pinned = spec.pinned,
          bgPresetId = spec.presetId,
          bgTintHex = "#E9DDC9",
          bgGrainIntensity = 0.25f,
          bgOpacity = 1.0f,
          bgEdgeStyle = "clean_cut",
          elementsJson = serializeElements(elements),
          updatedAt = now - (pageSpecs.size - idx) * 86_400_000L,
          isDeleted = false,
          deletedAt = null,
        )
      }

    val diary =
      DiaryEntity(
        id = diaryId,
        title = "স্মৃতির খাতা · Autumn Journal",
        description = "শরতের বিকেল, ভ্রমণ ও কবিতার খসড়া",
        coverStyle = CoverStyle.LUXURY.id,
        coverColorHex = "#593B2B",
        coverAccentHex = "#B08A4F",
        spineStyle = SpineStyle.RIBBED_GOLD.id,
        monogram = "ৎ",
        visibility = Visibility.PRIVATE.id,
        pageOrderCsv = pages.joinToString(",") { it.id },
        createdAt = now - 30L * 86_400_000L,
        updatedAt = now,
        archived = false,
        isDemo = true,
      )

    dao.createDiaryWithPages(diary, pages)
    prefs.edit().putBoolean(KEY_DEMO_SEEDED, true).apply()
  }

  suspend fun createDiary(
    title: String,
    description: String?,
    cover: DiaryCover,
    initialPageCount: Int,
  ): Result<String> {
    val cleanTitle = title.trim()
    if (cleanTitle.isEmpty()) {
      return Result.failure(IllegalArgumentException("Diary title cannot be empty"))
    }
    val safePageCount = DiaryInvariants.validateInitialPageCount(initialPageCount)
    val now = System.currentTimeMillis()
    val diaryId = UUID.randomUUID().toString()

    val pages =
      (0 until safePageCount).map { index ->
        PageEntity(
          id = UUID.randomUUID().toString(),
          diaryId = diaryId,
          orderIndex = index,
          dateIso = null,
          title = null,
          mood = null,
          pinned = false,
          bgPresetId = "warm_ivory",
          bgTintHex = "#E9DDC9",
          bgGrainIntensity = 0.25f,
          bgOpacity = 1.0f,
          bgEdgeStyle = "clean_cut",
          elementsJson = "[]",
          updatedAt = now,
          isDeleted = false,
          deletedAt = null,
        )
      }

    val diary =
      DiaryEntity(
        id = diaryId,
        title = cleanTitle,
        description = description?.trim()?.takeIf { it.isNotEmpty() },
        coverStyle = cover.style.id,
        coverColorHex = cover.colorHex,
        coverAccentHex = cover.accentHex,
        spineStyle = cover.spineStyle.id,
        monogram = cover.monogram.ifBlank { "ৎ" },
        visibility = Visibility.PRIVATE.id,
        pageOrderCsv = pages.joinToString(",") { it.id },
        createdAt = now,
        updatedAt = now,
        archived = false,
        isDemo = false,
      )

    return runTrackedWrite {
      dao.createDiaryWithPages(diary, pages)
      diaryId
    }
  }

  suspend fun renameDiary(diaryId: String, newTitle: String, newDescription: String?): Result<Unit> {
    val cleanTitle = newTitle.trim()
    if (cleanTitle.isEmpty()) {
      return Result.failure(IllegalArgumentException("Diary title cannot be empty"))
    }
    return runTrackedWrite {
      val existing = dao.getDiaryWithPagesOnce(diaryId)?.diary ?: error("Diary not found")
      dao.updateDiary(
        existing.copy(
          title = cleanTitle,
          description = newDescription?.trim()?.takeIf { it.isNotEmpty() },
          updatedAt = System.currentTimeMillis(),
        )
      )
    }
  }

  suspend fun duplicateDiary(diaryId: String): Result<String> {
    return runTrackedWrite {
      val existing = dao.getDiaryWithPagesOnce(diaryId) ?: error("Diary not found")
      val activePages = existing.pages.filter { !it.isDeleted }.sortedBy { it.orderIndex }
      val newDiaryId = UUID.randomUUID().toString()
      val now = System.currentTimeMillis()
      val newPages =
        activePages.mapIndexed { idx, p ->
          p.copy(
            id = UUID.randomUUID().toString(),
            diaryId = newDiaryId,
            orderIndex = idx,
            updatedAt = now,
          )
        }
      val newDiary =
        existing.diary.copy(
          id = newDiaryId,
          title = "${existing.diary.title} (Copy)",
          pageOrderCsv = newPages.joinToString(",") { it.id },
          createdAt = now,
          updatedAt = now,
          isDemo = false,
          archived = false,
        )
      dao.createDiaryWithPages(newDiary, newPages)
      newDiaryId
    }
  }

  suspend fun setArchived(diaryId: String, archived: Boolean): Result<Unit> {
    return runTrackedWrite {
      val existing = dao.getDiaryWithPagesOnce(diaryId)?.diary ?: error("Diary not found")
      dao.updateDiary(existing.copy(archived = archived, updatedAt = System.currentTimeMillis()))
    }
  }

  suspend fun deleteDiary(diaryId: String): Result<Unit> {
    return runTrackedWrite { dao.deleteDiaryById(diaryId) }
  }

  suspend fun addDatedPage(
    diaryId: String,
    dateIso: String,
    title: String?,
    bodyText: String?,
  ): Result<String> {
    return runTrackedWrite {
      val existing = dao.getDiaryWithPagesOnce(diaryId) ?: error("Diary not found")
      val activePages = existing.pages.filter { !it.isDeleted }.sortedBy { it.orderIndex }
      if (!DiaryInvariants.canAddPages(activePages.size, 1)) {
        error("Maximum 200 pages reached for this diary")
      }
      val now = System.currentTimeMillis()
      val newPageId = UUID.randomUUID().toString()
      val elements =
        if (!bodyText.isNullOrBlank()) {
          listOf(
            CanvasElement(
              id = "$newPageId-text-1",
              type = "text",
              x = 0.08f,
              y = 0.14f,
              w = 0.84f,
              h = 0.72f,
              text = bodyText.trim(),
              colorHex = "#302C29",
              fontSize = 15f,
            )
          )
        } else {
          emptyList()
        }
      val newPage =
        PageEntity(
          id = newPageId,
          diaryId = diaryId,
          orderIndex = activePages.size,
          dateIso = dateIso,
          title = title?.trim()?.takeIf { it.isNotEmpty() },
          mood = null,
          pinned = false,
          bgPresetId = "warm_ivory",
          bgTintHex = "#E9DDC9",
          bgGrainIntensity = 0.25f,
          bgOpacity = 1.0f,
          bgEdgeStyle = "clean_cut",
          elementsJson = serializeElements(elements),
          updatedAt = now,
          isDeleted = false,
          deletedAt = null,
        )
      val newCsv = (activePages.map { it.id } + newPageId).joinToString(",")
      dao.insertPageAndTouchDiary(
        diary = existing.diary.copy(pageOrderCsv = newCsv, updatedAt = now),
        page = newPage,
      )
      newPageId
    }
  }

  suspend fun addBlankPage(diaryId: String, insertAfterPageId: String? = null): Result<String> {
    return runTrackedWrite {
      val existing = dao.getDiaryWithPagesOnce(diaryId) ?: error("Diary not found")
      val activePages = existing.pages.filter { !it.isDeleted }.sortedBy { it.orderIndex }
      if (!DiaryInvariants.canAddPages(activePages.size, 1)) {
        error("Maximum ${DiaryInvariants.MAX_PAGES} active pages reached")
      }
      val now = System.currentTimeMillis()
      val newPageId = UUID.randomUUID().toString()
      val insertAt =
        if (insertAfterPageId != null) {
          val idx = activePages.indexOfFirst { it.id == insertAfterPageId }
          if (idx >= 0) idx + 1 else activePages.size
        } else {
          activePages.size
        }

      val newPage =
        PageEntity(
          id = newPageId,
          diaryId = diaryId,
          orderIndex = insertAt,
          dateIso = null,
          title = null,
          mood = null,
          pinned = false,
          bgPresetId = "warm_ivory",
          bgTintHex = "#E9DDC9",
          bgGrainIntensity = 0.25f,
          bgOpacity = 1.0f,
          bgEdgeStyle = "clean_cut",
          elementsJson = "[]",
          updatedAt = now,
          isDeleted = false,
          deletedAt = null,
        )

      val mutable = activePages.toMutableList()
      mutable.add(insertAt, newPage)
      val reindexed = mutable.mapIndexed { idx, p -> p.copy(orderIndex = idx) }
      val newCsv = reindexed.joinToString(",") { it.id }
      dao.insertPages(reindexed)
      dao.updateDiary(existing.diary.copy(pageOrderCsv = newCsv, updatedAt = now))
      newPageId
    }
  }

  suspend fun duplicatePage(diaryId: String, pageId: String): Result<String> {
    return runTrackedWrite {
      val existing = dao.getDiaryWithPagesOnce(diaryId) ?: error("Diary not found")
      val activePages = existing.pages.filter { !it.isDeleted }.sortedBy { it.orderIndex }
      if (!DiaryInvariants.canAddPages(activePages.size, 1)) {
        error("Maximum ${DiaryInvariants.MAX_PAGES} active pages reached")
      }
      val sourceIndex = activePages.indexOfFirst { it.id == pageId }
      if (sourceIndex < 0) error("Source page not found")
      val source = activePages[sourceIndex]
      val now = System.currentTimeMillis()
      val newPageId = UUID.randomUUID().toString()
      val cloned =
        source.copy(
          id = newPageId,
          title = source.title?.let { "$it (Copy)" },
          pinned = false,
          updatedAt = now,
        )
      val mutable = activePages.toMutableList()
      mutable.add(sourceIndex + 1, cloned)
      val reindexed = mutable.mapIndexed { idx, p -> p.copy(orderIndex = idx) }
      val newCsv = reindexed.joinToString(",") { it.id }
      dao.insertPages(reindexed)
      dao.updateDiary(existing.diary.copy(pageOrderCsv = newCsv, updatedAt = now))
      newPageId
    }
  }

  suspend fun reorderPages(diaryId: String, orderedPageIds: List<String>): Result<Unit> {
    return runTrackedWrite {
      val existing = dao.getDiaryWithPagesOnce(diaryId) ?: error("Diary not found")
      val activePages = existing.pages.filter { !it.isDeleted }
      val pageMap = activePages.associateBy { it.id }
      if (orderedPageIds.size != activePages.size || orderedPageIds.any { it !in pageMap }) {
        error("Reorder list must match exact active page IDs")
      }
      val now = System.currentTimeMillis()
      // Page.id never changes; only orderIndex and pageOrderCsv update deterministically 0..N-1
      val updatedPages =
        orderedPageIds.mapIndexed { idx, id ->
          pageMap.getValue(id).copy(orderIndex = idx)
        }
      val newCsv = orderedPageIds.joinToString(",")
      dao.saveReorderedPages(
        diary = existing.diary.copy(pageOrderCsv = newCsv, updatedAt = now),
        updatedPages = updatedPages,
      )
    }
  }

  suspend fun togglePinPage(diaryId: String, pageId: String): Result<Boolean> {
    return runTrackedWrite {
      val existing = dao.getDiaryWithPagesOnce(diaryId) ?: error("Diary not found")
      val page = existing.pages.firstOrNull { it.id == pageId && !it.isDeleted } ?: error("Page not found")
      val nextPinned = !page.pinned
      val now = System.currentTimeMillis()
      dao.updatePage(page.copy(pinned = nextPinned, updatedAt = now))
      dao.updateDiary(existing.diary.copy(updatedAt = now))
      nextPinned
    }
  }

  suspend fun resetPage(diaryId: String, pageId: String): Result<Unit> {
    return runTrackedWrite {
      val existing = dao.getDiaryWithPagesOnce(diaryId) ?: error("Diary not found")
      val page = existing.pages.firstOrNull { it.id == pageId && !it.isDeleted } ?: error("Page not found")
      val now = System.currentTimeMillis()
      dao.updatePage(
        page.copy(
          title = null,
          dateIso = null,
          mood = null,
          pinned = false,
          elementsJson = "[]",
          updatedAt = now,
        )
      )
      dao.updateDiary(existing.diary.copy(updatedAt = now))
    }
  }

  suspend fun softDeletePage(diaryId: String, pageId: String): Result<Unit> {
    return runTrackedWrite {
      val existing = dao.getDiaryWithPagesOnce(diaryId) ?: error("Diary not found")
      val activePages = existing.pages.filter { !it.isDeleted }.sortedBy { it.orderIndex }
      if (!DiaryInvariants.canDeletePages(activePages.size, 1)) {
        error("Minimum ${DiaryInvariants.MIN_PAGES} active pages required per diary")
      }
      val target = activePages.firstOrNull { it.id == pageId } ?: error("Page not found")
      val now = System.currentTimeMillis()
      val remaining =
        activePages.filter { it.id != pageId }.mapIndexed { idx, p -> p.copy(orderIndex = idx) }
      val deleted = target.copy(isDeleted = true, deletedAt = now, pinned = false)
      dao.saveReorderedPages(
        diary =
          existing.diary.copy(
            pageOrderCsv = remaining.joinToString(",") { it.id },
            updatedAt = now,
          ),
        updatedPages = remaining + deleted,
      )
    }
  }

  suspend fun restoreDeletedPage(diaryId: String, pageId: String): Result<Unit> {
    return runTrackedWrite {
      val existing = dao.getDiaryWithPagesOnce(diaryId) ?: error("Diary not found")
      val activePages = existing.pages.filter { !it.isDeleted }.sortedBy { it.orderIndex }
      if (!DiaryInvariants.canAddPages(activePages.size, 1)) {
        error("Maximum ${DiaryInvariants.MAX_PAGES} active pages reached")
      }
      val target = existing.pages.firstOrNull { it.id == pageId && it.isDeleted } ?: error("Deleted page not found")
      val now = System.currentTimeMillis()
      val restored =
        target.copy(
          isDeleted = false,
          deletedAt = null,
          orderIndex = activePages.size,
          updatedAt = now,
        )
      val allActive = (activePages + restored).mapIndexed { idx, p -> p.copy(orderIndex = idx) }
      dao.saveReorderedPages(
        diary =
          existing.diary.copy(
            pageOrderCsv = allActive.joinToString(",") { it.id },
            updatedAt = now,
          ),
        updatedPages = allActive,
      )
    }
  }

  suspend fun permanentlyDeletePage(pageId: String): Result<Unit> {
    return runTrackedWrite { dao.deletePagePermanentlyById(pageId) }
  }

  suspend fun savePageChanges(diaryId: String, updatedPage: Page): Result<Unit> {
    return runTrackedWrite {
      val existing = dao.getDiaryWithPagesOnce(diaryId) ?: error("Diary not found")
      val currentEntity =
        existing.pages.firstOrNull { it.id == updatedPage.id && !it.isDeleted }
          ?: error("Page not found")
      val now = System.currentTimeMillis()
      dao.updatePage(
        currentEntity.copy(
          title = updatedPage.title?.trim()?.takeIf { it.isNotEmpty() },
          dateIso = updatedPage.dateIso?.trim()?.takeIf { it.isNotEmpty() },
          mood = updatedPage.mood?.trim()?.takeIf { it.isNotEmpty() },
          pinned = updatedPage.pinned,
          bgPresetId = updatedPage.background.presetId,
          bgTintHex = updatedPage.background.tintHex,
          bgGrainIntensity = updatedPage.background.grainIntensity,
          bgOpacity = updatedPage.background.opacity,
          bgEdgeStyle = updatedPage.background.edgeStyle,
          elementsJson = serializeElements(updatedPage.elements),
          updatedAt = now,
        )
      )
      dao.updateDiary(existing.diary.copy(updatedAt = now))
    }
  }

  suspend fun getAllDiariesOnce(): List<DiaryWithPages> =
    dao.getAllDiariesWithPagesOnce().map { it.toDomain() }

  suspend fun restoreDiariesFromBackup(restored: List<DiaryWithPages>): Result<Int> {
    return runTrackedWrite {
      var importedCount = 0
      val now = System.currentTimeMillis()
      restored.forEach { bundle ->
        val rawPages = bundle.pages.filter { !it.isDeleted }.sortedBy { it.order }
        val targetPages = rawPages.take(DiaryInvariants.MAX_PAGES).toMutableList()
        while (targetPages.size < DiaryInvariants.MIN_PAGES) {
          targetPages.add(
            Page(
              id = UUID.randomUUID().toString(),
              diaryId = bundle.diary.id,
              order = targetPages.size,
              background = PageBackground(presetId = "warm_ivory", tintHex = "#E9DDC9"),
              updatedAt = now,
            )
          )
        }
        val diaryId = bundle.diary.id.ifBlank { UUID.randomUUID().toString() }
        val pageEntities =
          targetPages.mapIndexed { idx, p ->
            PageEntity(
              id = p.id.ifBlank { UUID.randomUUID().toString() },
              diaryId = diaryId,
              orderIndex = idx,
              dateIso = p.dateIso,
              title = p.title,
              mood = p.mood,
              pinned = p.pinned,
              bgPresetId = p.background.presetId,
              bgTintHex = p.background.tintHex,
              bgGrainIntensity = p.background.grainIntensity,
              bgOpacity = p.background.opacity,
              bgEdgeStyle = p.background.edgeStyle,
              elementsJson = serializeElements(p.elements),
              updatedAt = p.updatedAt,
              isDeleted = false,
              deletedAt = null,
            )
          }
        val diaryEntity =
          DiaryEntity(
            id = diaryId,
            title = bundle.diary.title.ifBlank { "Restored Diary" },
            description = bundle.diary.description,
            coverStyle = bundle.diary.cover.style.id,
            coverColorHex = bundle.diary.cover.colorHex,
            coverAccentHex = bundle.diary.cover.accentHex,
            spineStyle = bundle.diary.cover.spineStyle.id,
            monogram = bundle.diary.cover.monogram.ifBlank { "ৎ" },
            visibility = bundle.diary.visibility.id,
            pageOrderCsv = pageEntities.joinToString(",") { it.id },
            createdAt = bundle.diary.createdAt,
            updatedAt = now,
            archived = bundle.diary.archived,
            isDemo = bundle.diary.isDemo,
          )
        dao.createDiaryWithPages(diaryEntity, pageEntities)
        importedCount++
      }
      importedCount
    }
  }

  suspend fun retryLastSave() {
    val op = lastPendingOperation ?: return
    runTrackedWrite(op)
  }

  private suspend fun <T> runTrackedWrite(block: suspend () -> T): Result<T> {
    _saveStatus.value = SaveStatus.Saving
    lastPendingOperation = { block() }
    return try {
      val result = block()
      _saveStatus.value = SaveStatus.Saved
      Result.success(result)
    } catch (t: Throwable) {
      _saveStatus.value = SaveStatus.Failed(t.message ?: "Save failed")
      Result.failure(t)
    }
  }

  private fun serializeElements(elements: List<CanvasElement>): String =
    elementsAdapter.toJson(elements)

  private fun deserializeElements(json: String): List<CanvasElement> =
    runCatching { elementsAdapter.fromJson(json) }.getOrNull().orEmpty()

  private fun DiaryWithPageEntities.toDomain(): DiaryWithPages {
    val activePages =
      pages.filter { !it.isDeleted }.sortedBy { it.orderIndex }.map { it.toDomain() }
    val deletedPagesList =
      pages.filter { it.isDeleted }.sortedByDescending { it.deletedAt ?: 0L }.map { it.toDomain() }
    val orderedIds =
      if (diary.pageOrderCsv.isNotBlank()) {
        val idSet = activePages.map { it.id }.toSet()
        val csvIds = diary.pageOrderCsv.split(",").filter { it in idSet }
        if (csvIds.size == activePages.size) csvIds else activePages.map { it.id }
      } else {
        activePages.map { it.id }
      }
    return DiaryWithPages(
      diary =
        Diary(
          id = diary.id,
          title = diary.title,
          description = diary.description,
          cover =
            DiaryCover(
              style = CoverStyle.fromId(diary.coverStyle),
              colorHex = diary.coverColorHex,
              accentHex = diary.coverAccentHex,
              spineStyle = SpineStyle.fromId(diary.spineStyle),
              monogram = diary.monogram,
            ),
          visibility = Visibility.fromId(diary.visibility),
          pageIds = orderedIds,
          createdAt = diary.createdAt,
          updatedAt = diary.updatedAt,
          archived = diary.archived,
          isDemo = diary.isDemo,
        ),
      pages = activePages,
      deletedPages = deletedPagesList,
    )
  }

  private fun PageEntity.toDomain(): Page =
    Page(
      id = id,
      diaryId = diaryId,
      order = orderIndex,
      dateIso = dateIso,
      title = title,
      mood = mood,
      pinned = pinned,
      background =
        PageBackground(
          presetId = bgPresetId,
          tintHex = bgTintHex,
          grainIntensity = bgGrainIntensity,
          opacity = bgOpacity,
          edgeStyle = bgEdgeStyle,
        ),
      elements = deserializeElements(elementsJson),
      updatedAt = updatedAt,
      isDeleted = isDeleted,
      deletedAt = deletedAt,
    )

  private data class DemoPageSeed(
    val id: String,
    val dateIso: String?,
    val title: String?,
    val mood: String?,
    val pinned: Boolean,
    val presetId: String,
    val body: String?,
  )

  companion object {
    private const val SEED_PREFS = "sankalan_seed_prefs"
    private const val KEY_DEMO_SEEDED = "demo_seeded_v1"

    fun normalizeForSearch(text: String): String =
      Normalizer.normalize(text.trim(), Normalizer.Form.NFC).lowercase()
  }
}
