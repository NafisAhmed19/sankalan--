package com.example.core.model

enum class AppLanguage(val code: String) {
  BN_BD("bn-BD"),
  EN("en");

  companion object {
    fun fromCode(code: String?): AppLanguage =
      entries.firstOrNull { it.code == code } ?: EN
  }
}

enum class AppThemeMode(val id: String) {
  DARK("dark"),
  LIGHT("light"),
  SYSTEM("system");

  companion object {
    fun fromId(id: String?): AppThemeMode =
      entries.firstOrNull { it.id == id } ?: DARK
  }
}

enum class NumeralStyle(val id: String) {
  LATIN("latin"),
  BENGALI("bengali");

  companion object {
    fun fromId(id: String?): NumeralStyle =
      entries.firstOrNull { it.id == id } ?: LATIN
  }
}

enum class ReadingLayout(val id: String) {
  SINGLE("single"),
  SPREAD("spread");

  companion object {
    fun fromId(id: String?): ReadingLayout =
      entries.firstOrNull { it.id == id } ?: SINGLE
  }
}

/**
 * Application-wide user preferences persisted locally via DataStore.
 * Matches specification §11 (AppSettings).
 */
data class AppSettings(
  val language: AppLanguage = AppLanguage.BN_BD,
  val themeMode: AppThemeMode = AppThemeMode.DARK,
  val numeralStyle: NumeralStyle = NumeralStyle.LATIN,
  val reducedMotion: Boolean = false,
  val textScale: Float = 1.0f,
  val highContrast: Boolean = false,
  val readingLayout: ReadingLayout = ReadingLayout.SINGLE,
  val pageTurnAnimation: Boolean = true,
  val pageTurnSound: Boolean = true,
  val liteMode: Boolean = false,
  val reminderEnabled: Boolean = false,
  val reminderHour: Int = 20,
  val reminderMinute: Int = 0,
  val lockEnabled: Boolean = false,
  val hasChosenInitialLanguage: Boolean = false,
  val githubUpdateRepo: String = "NafisAhmed19/sankalan--",
)

/**
 * Feature flags per D-29 & D-30:
 * Phase 1 unlocks all local features (including PDF export) for free,
 * while Phase 2 community/cloud sync remains gated off.
 */
object PhaseFlags {
  const val PHASE_1_ALL_LOCAL_FEATURES_FREE = true
  const val PHASE_2_COMMUNITY_EXPLORE_ENABLED = false
  const val PHASE_2_CLOUD_SYNC_ENABLED = false
}
