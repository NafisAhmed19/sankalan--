package com.example.core.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class DiaryWithPageEntities(
  @Embedded val diary: DiaryEntity,
  @Relation(parentColumn = "id", entityColumn = "diaryId")
  val pages: List<PageEntity>,
)

@Dao
interface DiaryDao {
  @Transaction
  @Query("SELECT * FROM diaries ORDER BY updatedAt DESC")
  fun observeAllDiariesWithPages(): Flow<List<DiaryWithPageEntities>>

  @Transaction
  @Query("SELECT * FROM diaries WHERE id = :diaryId")
  fun observeDiaryWithPages(diaryId: String): Flow<DiaryWithPageEntities?>

  @Query("SELECT COUNT(*) FROM diaries")
  suspend fun countDiaries(): Int

  @Transaction
  @Query("SELECT * FROM diaries ORDER BY updatedAt DESC")
  suspend fun getAllDiariesWithPagesOnce(): List<DiaryWithPageEntities>

  @Transaction
  @Query("SELECT * FROM diaries WHERE id = :diaryId")
  suspend fun getDiaryWithPagesOnce(diaryId: String): DiaryWithPageEntities?

  @Query("SELECT * FROM pages WHERE diaryId = :diaryId AND isDeleted = 0 ORDER BY orderIndex ASC")
  suspend fun getActivePagesOnce(diaryId: String): List<PageEntity>

  @Query("SELECT * FROM pages WHERE diaryId = :diaryId AND isDeleted = 1 ORDER BY deletedAt DESC")
  suspend fun getDeletedPagesOnce(diaryId: String): List<PageEntity>

  @Query("SELECT * FROM pages WHERE dateIso IS NOT NULL AND isDeleted = 0 ORDER BY dateIso DESC, updatedAt DESC")
  fun observeAllDatedPages(): Flow<List<PageEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDiary(diary: DiaryEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPages(pages: List<PageEntity>)

  @Update
  suspend fun updateDiary(diary: DiaryEntity)

  @Update
  suspend fun updatePage(page: PageEntity)

  @Update
  suspend fun updatePages(pages: List<PageEntity>)

  @Query("DELETE FROM diaries WHERE id = :diaryId")
  suspend fun deleteDiaryById(diaryId: String)

  @Query("DELETE FROM pages WHERE id = :pageId")
  suspend fun deletePagePermanentlyById(pageId: String)

  @Transaction
  suspend fun createDiaryWithPages(diary: DiaryEntity, pages: List<PageEntity>) {
    insertDiary(diary)
    insertPages(pages)
  }

  @Transaction
  suspend fun saveReorderedPages(diary: DiaryEntity, updatedPages: List<PageEntity>) {
    updateDiary(diary)
    updatePages(updatedPages)
  }

  @Transaction
  suspend fun insertPageAndTouchDiary(diary: DiaryEntity, page: PageEntity) {
    insertPages(listOf(page))
    updateDiary(diary)
  }
}
