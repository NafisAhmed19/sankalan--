package com.example.core.model

enum class CoverStyle(val id: String) {
  LUXURY("luxury"),
  ARTISTIC("artistic"),
  MINIMAL("minimal");

  companion object {
    fun fromId(id: String?): CoverStyle = entries.firstOrNull { it.id == id } ?: LUXURY
  }
}

enum class SpineStyle(val id: String) {
  RIBBED_GOLD("ribbed_gold"),
  SMOOTH_LEATHER("smooth_leather"),
  STITCHED_CLOTH("stitched_cloth");

  companion object {
    fun fromId(id: String?): SpineStyle = entries.firstOrNull { it.id == id } ?: RIBBED_GOLD
  }
}

enum class Visibility(val id: String) {
  PRIVATE("private"),
  UNLISTED("unlisted"),
  PUBLIC("public");

  companion object {
    fun fromId(id: String?): Visibility = entries.firstOrNull { it.id == id } ?: PRIVATE
  }
}

data class DiaryCover(
  val style: CoverStyle = CoverStyle.LUXURY,
  val colorHex: String = "#593B2B",
  val accentHex: String = "#B08A4F",
  val spineStyle: SpineStyle = SpineStyle.RIBBED_GOLD,
  val monogram: String = "ৎ",
)

data class PageBackground(
  val presetId: String = "warm_ivory",
  val tintHex: String = "#E9DDC9",
  val grainIntensity: Float = 0.25f,
  val opacity: Float = 1.0f,
  val edgeStyle: String = "clean_cut",
)

/**
 * Application-owned canonical element schema (Handoff §11 & Decision Record §12.2).
 * Completely independent of any canvas rendering library.
 */
data class CanvasElement(
  val id: String,
  val type: String, // text | stroke | image | sticker | tape | frame | shape | label | voice
  val x: Float = 0.1f,
  val y: Float = 0.1f,
  val w: Float = 0.8f,
  val h: Float = 0.2f,
  val rotation: Float = 0f,
  val opacity: Float = 1f,
  val locked: Boolean = false,
  val hidden: Boolean = false,
  val zIndex: Int = 0,
  val groupId: String? = null,
  val text: String? = null,
  val fontId: String? = null,
  val fontSize: Float? = null,
  val colorHex: String? = null,
  val align: String? = null,
  val bold: Boolean = false,
  val italic: Boolean = false,
  val underline: Boolean = false,
  val assetId: String? = null,
  val blobRef: String? = null,
  val strokePointsJson: String? = null,
)

data class Page(
  val id: String, // Stable UUID; NEVER regenerated on reorder or rename
  val diaryId: String,
  val order: Int, // Unique deterministic order among active pages
  val dateIso: String? = null, // YYYY-MM-DD optional dated page
  val title: String? = null,
  val mood: String? = null,
  val pinned: Boolean = false,
  val background: PageBackground = PageBackground(),
  val elements: List<CanvasElement> = emptyList(),
  val updatedAt: Long = System.currentTimeMillis(),
  val isDeleted: Boolean = false,
  val deletedAt: Long? = null,
) {
  val hasContent: Boolean
    get() = elements.isNotEmpty() || !title.isNullOrBlank()
}

data class Diary(
  val id: String,
  val title: String,
  val description: String? = null,
  val cover: DiaryCover = DiaryCover(),
  val visibility: Visibility = Visibility.PRIVATE,
  val pageIds: List<String>,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val archived: Boolean = false,
  val isDemo: Boolean = false,
)

data class DiaryWithPages(
  val diary: Diary,
  val pages: List<Page>,
  val deletedPages: List<Page> = emptyList(),
)

sealed interface SaveStatus {
  data object Idle : SaveStatus
  data object Saving : SaveStatus
  data object Saved : SaveStatus
  data class Failed(val message: String) : SaveStatus
}

/**
 * Core Domain Invariants (Handoff §11 & Decision Record §12.2):
 * - Every Diary maintains 5..200 active pages.
 * - Page IDs remain stable across reorder/rename/edit.
 * - Page orders are deterministic and unique (0..N-1).
 */
object DiaryInvariants {
  const val MIN_PAGES = 5
  const val MAX_PAGES = 200

  fun canAddPages(currentActiveCount: Int, countToAdd: Int = 1): Boolean =
    countToAdd > 0 && currentActiveCount + countToAdd <= MAX_PAGES

  fun canDeletePages(currentActiveCount: Int, countToDelete: Int = 1): Boolean =
    countToDelete > 0 && currentActiveCount - countToDelete >= MIN_PAGES

  fun validateInitialPageCount(requested: Int): Int =
    requested.coerceIn(MIN_PAGES, MAX_PAGES)

  fun validateDiaryPages(diary: Diary, activePages: List<Page>): Result<Unit> {
    if (activePages.size !in MIN_PAGES..MAX_PAGES) {
      return Result.failure(
        IllegalStateException("Diary ${diary.id} has ${activePages.size} active pages; expected $MIN_PAGES..$MAX_PAGES")
      )
    }
    if (diary.pageIds.size != activePages.size) {
      return Result.failure(
        IllegalStateException("Diary pageIds (${diary.pageIds.size}) mismatch active pages (${activePages.size})")
      )
    }
    val pageMap = activePages.associateBy { it.id }
    if (pageMap.size != activePages.size) {
      return Result.failure(IllegalStateException("Duplicate Page IDs detected in diary ${diary.id}"))
    }
    if (diary.pageIds.any { it !in pageMap }) {
      return Result.failure(IllegalStateException("Orphaned pageId found in diary ${diary.id}"))
    }
    val sortedOrders = activePages.map { it.order }.sorted()
    val expectedOrders = (0 until activePages.size).toList()
    if (sortedOrders != expectedOrders) {
      return Result.failure(IllegalStateException("Non-deterministic or duplicate page orders in diary ${diary.id}"))
    }
    return Result.success(Unit)
  }
}
