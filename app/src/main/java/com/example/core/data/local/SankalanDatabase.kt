package com.example.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [DiaryEntity::class, PageEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class SankalanDatabase : RoomDatabase() {
  abstract fun diaryDao(): DiaryDao

  companion object {
    @Volatile private var INSTANCE: SankalanDatabase? = null

    fun getInstance(context: Context): SankalanDatabase {
      return INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
              context.applicationContext,
              SankalanDatabase::class.java,
              "sankalan_local.db",
            )
              .build()
              .also { INSTANCE = it }
        }
    }
  }
}
