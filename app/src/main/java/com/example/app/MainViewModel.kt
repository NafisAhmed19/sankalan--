package com.example.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.data.DiaryRepository
import com.example.core.data.SettingsRepository
import com.example.core.model.AppLanguage
import com.example.core.model.AppSettings
import com.example.core.model.AppThemeMode
import com.example.core.model.DiaryCover
import com.example.core.model.DiaryWithPages
import com.example.core.model.NumeralStyle
import com.example.core.model.Page
import com.example.core.model.ReadingLayout
import com.example.core.model.SaveStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
  val settings: AppSettings = AppSettings(),
  val isLoaded: Boolean = false,
  val showWelcome: Boolean = true,
  val activeDiaries: List<DiaryWithPages> = emptyList(),
  val archivedDiaries: List<DiaryWithPages> = emptyList(),
  val datedPages: List<Page> = emptyList(),
  val saveStatus: SaveStatus = SaveStatus.Idle,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
  private val settingsRepository = SettingsRepository(application.applicationContext)
  private val diaryRepository = DiaryRepository(application.applicationContext)
  private val showWelcomeFlow = MutableStateFlow(true)

  init {
    viewModelScope.launch { diaryRepository.seedDemoDiaryIfFirstLaunch() }
  }

  val uiState: StateFlow<MainUiState> =
    combine(
      settingsRepository.settingsFlow,
      showWelcomeFlow,
      diaryRepository.allDiariesFlow,
      diaryRepository.allDatedPagesFlow,
      diaryRepository.saveStatus,
    ) { settings, showWelcome, allDiaries, datedPages, saveStatus ->
      MainUiState(
        settings = settings,
        isLoaded = true,
        showWelcome = showWelcome,
        activeDiaries = allDiaries.filter { !it.diary.archived },
        archivedDiaries = allDiaries.filter { it.diary.archived },
        datedPages = datedPages,
        saveStatus = saveStatus,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = MainUiState(),
    )

  fun dismissWelcome() {
    showWelcomeFlow.value = false
  }

  fun replayWelcome() {
    showWelcomeFlow.value = true
  }

  fun setLanguage(language: AppLanguage, markInitialChosen: Boolean = true) {
    viewModelScope.launch { settingsRepository.setLanguage(language, markInitialChosen) }
  }

  fun toggleLanguage() {
    val next =
      if (uiState.value.settings.language == AppLanguage.BN_BD) AppLanguage.EN
      else AppLanguage.BN_BD
    setLanguage(next)
  }

  fun setThemeMode(themeMode: AppThemeMode) {
    viewModelScope.launch { settingsRepository.setThemeMode(themeMode) }
  }

  fun setNumeralStyle(style: NumeralStyle) {
    viewModelScope.launch { settingsRepository.setNumeralStyle(style) }
  }

  fun setReadingLayout(layout: ReadingLayout) {
    viewModelScope.launch { settingsRepository.setReadingLayout(layout) }
  }

  fun setPageTurnAnimation(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setPageTurnAnimation(enabled) }
  }

  fun setPageTurnSound(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setPageTurnSound(enabled) }
  }

  fun setLiteMode(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setLiteMode(enabled) }
  }

  fun setReducedMotion(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setReducedMotion(enabled) }
  }

  fun setTextScale(scale: Float) {
    viewModelScope.launch { settingsRepository.setTextScale(scale) }
  }

  fun setHighContrast(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setHighContrast(enabled) }
  }

  fun setReminderEnabled(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setReminderEnabled(enabled) }
  }

  fun setReminderTime(hour: Int, minute: Int) {
    viewModelScope.launch { settingsRepository.setReminderTime(hour, minute) }
  }

  fun setLockEnabled(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setLockEnabled(enabled) }
  }

  fun setGithubUpdateRepo(repo: String) {
    viewModelScope.launch { settingsRepository.setGithubUpdateRepo(repo) }
  }

  suspend fun getAllDiariesOnce(): List<DiaryWithPages> =
    diaryRepository.getAllDiariesOnce()

  suspend fun restoreBackupDiaries(restored: List<DiaryWithPages>): Result<Int> =
    diaryRepository.restoreDiariesFromBackup(restored)

  fun createDiary(
    title: String,
    description: String?,
    cover: DiaryCover,
    initialPageCount: Int,
  ) {
    viewModelScope.launch {
      diaryRepository.createDiary(
        title = title,
        description = description,
        cover = cover,
        initialPageCount = initialPageCount,
      )
    }
  }

  fun renameDiary(diaryId: String, newTitle: String, newDescription: String?) {
    viewModelScope.launch { diaryRepository.renameDiary(diaryId, newTitle, newDescription) }
  }

  fun duplicateDiary(diaryId: String) {
    viewModelScope.launch { diaryRepository.duplicateDiary(diaryId) }
  }

  fun setDiaryArchived(diaryId: String, archived: Boolean) {
    viewModelScope.launch { diaryRepository.setArchived(diaryId, archived) }
  }

  fun deleteDiary(diaryId: String) {
    viewModelScope.launch { diaryRepository.deleteDiary(diaryId) }
  }

  fun addDatedPage(diaryId: String, dateIso: String, title: String?, bodyText: String?) {
    viewModelScope.launch { diaryRepository.addDatedPage(diaryId, dateIso, title, bodyText) }
  }

  fun createPromptPage(
    diaryId: String,
    dateIso: String,
    title: String?,
    bodyText: String?,
    onCreated: (String) -> Unit,
  ) {
    viewModelScope.launch {
      diaryRepository.addDatedPage(diaryId, dateIso, title, bodyText).onSuccess { newPageId ->
        onCreated(newPageId)
      }
    }
  }

  fun addBlankPage(diaryId: String, insertAfterPageId: String? = null) {
    viewModelScope.launch { diaryRepository.addBlankPage(diaryId, insertAfterPageId) }
  }

  fun duplicatePage(diaryId: String, pageId: String) {
    viewModelScope.launch { diaryRepository.duplicatePage(diaryId, pageId) }
  }

  fun reorderPages(diaryId: String, orderedPageIds: List<String>) {
    viewModelScope.launch { diaryRepository.reorderPages(diaryId, orderedPageIds) }
  }

  fun togglePinPage(diaryId: String, pageId: String) {
    viewModelScope.launch { diaryRepository.togglePinPage(diaryId, pageId) }
  }

  fun resetPage(diaryId: String, pageId: String) {
    viewModelScope.launch { diaryRepository.resetPage(diaryId, pageId) }
  }

  fun softDeletePage(diaryId: String, pageId: String) {
    viewModelScope.launch { diaryRepository.softDeletePage(diaryId, pageId) }
  }

  fun restoreDeletedPage(diaryId: String, pageId: String) {
    viewModelScope.launch { diaryRepository.restoreDeletedPage(diaryId, pageId) }
  }

  fun permanentlyDeletePage(pageId: String) {
    viewModelScope.launch { diaryRepository.permanentlyDeletePage(pageId) }
  }

  fun savePageChanges(diaryId: String, updatedPage: Page) {
    viewModelScope.launch { diaryRepository.savePageChanges(diaryId, updatedPage) }
  }

  fun retryLastSave() {
    viewModelScope.launch { diaryRepository.retryLastSave() }
  }
}
