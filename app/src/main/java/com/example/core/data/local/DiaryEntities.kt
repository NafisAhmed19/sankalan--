package com.example.core.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "diaries")
data class DiaryEntity(
  @PrimaryKey val id: String,
  val title: String,
  val description: String?,
  val coverStyle: String,
  val coverColorHex: String,
  val coverAccentHex: String,
  val spineStyle: String,
  val monogram: String,
  val visibility: String,
  val pageOrderCsv: String,
  val createdAt: Long,
  val updatedAt: Long,
  val archived: Boolean,
  val isDemo: Boolean,
)

@Entity(
  tableName = "pages",
  foreignKeys =
    [
      ForeignKey(
        entity = DiaryEntity::class,
        parentColumns = ["id"],
        childColumns = ["diaryId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index("diaryId"), Index("dateIso")],
)
data class PageEntity(
  @PrimaryKey val id: String,
  val diaryId: String,
  val orderIndex: Int,
  val dateIso: String?,
  val title: String?,
  val mood: String?,
  val pinned: Boolean,
  val bgPresetId: String,
  val bgTintHex: String,
  val bgGrainIntensity: Float,
  val bgOpacity: Float,
  val bgEdgeStyle: String,
  val elementsJson: String,
  val updatedAt: Long,
  val isDeleted: Boolean,
  val deletedAt: Long?,
)
