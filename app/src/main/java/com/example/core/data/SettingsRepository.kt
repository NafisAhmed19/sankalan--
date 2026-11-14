package com.example.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.model.AppLanguage
import com.example.core.model.AppSettings
import com.example.core.model.AppThemeMode
import com.example.core.model.NumeralStyle
import com.example.core.model.ReadingLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "sankalan_settings")

class SettingsRepository(private val context: Context) {

  private object Keys {
    val LANGUAGE = stringPreferencesKey("language")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val NUMERAL_STYLE = stringPreferencesKey("numeral_style")
    val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
    val TEXT_SCALE = floatPreferencesKey("text_scale")
    val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
    val READING_LAYOUT = stringPreferencesKey("reading_layout")
    val PAGE_TURN_ANIMATION = booleanPreferencesKey("page_turn_animation")
    val PAGE_TURN_SOUND = booleanPreferencesKey("page_turn_sound")
    val LITE_MODE = booleanPreferencesKey("lite_mode")
    val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
    val REMINDER_HOUR = intPreferencesKey("reminder_hour")
    val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
    val LOCK_ENABLED = booleanPreferencesKey("lock_enabled")
    val HAS_CHOSEN_INITIAL_LANGUAGE = booleanPreferencesKey("has_chosen_initial_language")
    val GITHUB_UPDATE_REPO = stringPreferencesKey("github_update_repo")
  }

  val settingsFlow: Flow<AppSettings> =
    context.settingsDataStore.data.map { prefs ->
      AppSettings(
        language = AppLanguage.fromCode(prefs[Keys.LANGUAGE] ?: AppLanguage.BN_BD.code),
        themeMode = AppThemeMode.fromId(prefs[Keys.THEME_MODE] ?: AppThemeMode.DARK.id),
        numeralStyle = NumeralStyle.fromId(prefs[Keys.NUMERAL_STYLE] ?: NumeralStyle.LATIN.id),
        reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
        textScale = (prefs[Keys.TEXT_SCALE] ?: 1.0f).coerceIn(0.85f, 1.4f),
        highContrast = prefs[Keys.HIGH_CONTRAST] ?: false,
        readingLayout = ReadingLayout.fromId(prefs[Keys.READING_LAYOUT] ?: ReadingLayout.SINGLE.id),
        pageTurnAnimation = prefs[Keys.PAGE_TURN_ANIMATION] ?: true,
        pageTurnSound = prefs[Keys.PAGE_TURN_SOUND] ?: true,
        liteMode = prefs[Keys.LITE_MODE] ?: false,
        reminderEnabled = prefs[Keys.REMINDER_ENABLED] ?: false,
        reminderHour = (prefs[Keys.REMINDER_HOUR] ?: 20).coerceIn(0, 23),
        reminderMinute = (prefs[Keys.REMINDER_MINUTE] ?: 0).coerceIn(0, 59),
        lockEnabled = prefs[Keys.LOCK_ENABLED] ?: false,
        hasChosenInitialLanguage = prefs[Keys.HAS_CHOSEN_INITIAL_LANGUAGE] ?: false,
        githubUpdateRepo =
          prefs[Keys.GITHUB_UPDATE_REPO]
            ?.takeUnless { it.isBlank() || it == "nrstech-dev/sankalan-releases" }
            ?: "NafisAhmed19/sankalan--",
      )
    }

  suspend fun setLanguage(language: AppLanguage, markInitialChosen: Boolean = true) {
    context.settingsDataStore.edit { prefs ->
      prefs[Keys.LANGUAGE] = language.code
      if (markInitialChosen) {
        prefs[Keys.HAS_CHOSEN_INITIAL_LANGUAGE] = true
      }
    }
  }

  suspend fun setThemeMode(themeMode: AppThemeMode) {
    context.settingsDataStore.edit { it[Keys.THEME_MODE] = themeMode.id }
  }

  suspend fun setNumeralStyle(style: NumeralStyle) {
    context.settingsDataStore.edit { it[Keys.NUMERAL_STYLE] = style.id }
  }

  suspend fun setReducedMotion(enabled: Boolean) {
    context.settingsDataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
  }

  suspend fun setTextScale(scale: Float) {
    context.settingsDataStore.edit { it[Keys.TEXT_SCALE] = scale.coerceIn(0.85f, 1.4f) }
  }

  suspend fun setHighContrast(enabled: Boolean) {
    context.settingsDataStore.edit { it[Keys.HIGH_CONTRAST] = enabled }
  }

  suspend fun setReadingLayout(layout: ReadingLayout) {
    context.settingsDataStore.edit { it[Keys.READING_LAYOUT] = layout.id }
  }

  suspend fun setPageTurnAnimation(enabled: Boolean) {
    context.settingsDataStore.edit { it[Keys.PAGE_TURN_ANIMATION] = enabled }
  }

  suspend fun setPageTurnSound(enabled: Boolean) {
    context.settingsDataStore.edit { it[Keys.PAGE_TURN_SOUND] = enabled }
  }

  suspend fun setLiteMode(enabled: Boolean) {
    context.settingsDataStore.edit { it[Keys.LITE_MODE] = enabled }
  }

  suspend fun setReminderEnabled(enabled: Boolean) {
    context.settingsDataStore.edit { it[Keys.REMINDER_ENABLED] = enabled }
  }

  suspend fun setReminderTime(hour: Int, minute: Int) {
    context.settingsDataStore.edit { prefs ->
      prefs[Keys.REMINDER_HOUR] = hour.coerceIn(0, 23)
      prefs[Keys.REMINDER_MINUTE] = minute.coerceIn(0, 59)
    }
  }

  suspend fun setLockEnabled(enabled: Boolean) {
    context.settingsDataStore.edit { it[Keys.LOCK_ENABLED] = enabled }
  }

  suspend fun setGithubUpdateRepo(repo: String) {
    context.settingsDataStore.edit { it[Keys.GITHUB_UPDATE_REPO] = repo.trim().ifEmpty { "NafisAhmed19/sankalan--" } }
  }
}
