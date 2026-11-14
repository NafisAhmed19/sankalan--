package com.example.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.BuildConfig
import com.example.core.i18n.LocalStrings
import com.example.core.i18n.SankalanStrings
import com.example.core.i18n.stringsFor
import com.example.core.model.DiaryWithPages
import com.example.core.model.Page
import com.example.features.bookshelf.ArchiveScreen
import com.example.features.bookshelf.BookCoverInspectionDialog
import com.example.features.bookshelf.BookshelfScreen
import com.example.features.bookshelf.CreateJournalSheet
import com.example.features.bookshelf.DeleteDiaryConfirmDialog
import com.example.features.bookshelf.RenameDiaryDialog
import com.example.features.calendar.CalendarScreen
import com.example.features.calendar.formatIsoDate
import com.example.features.editor.CanvasEditorScreen
import com.example.features.export.ExportDiaryDialog
import com.example.features.insights.GlobalSearchDialog
import com.example.features.pagemanager.PageManagerSheet
import com.example.features.reading.ReadingModeScreen
import com.example.features.security.AppLockGateOverlay
import com.example.features.security.AppLockManager
import com.example.features.settings.SettingsScreen
import com.example.features.shell.HomeScreen
import com.example.features.studio.StudioScreen
import com.example.features.update.AppUpdateChecker
import com.example.features.update.AppUpdateNotifier
import com.example.features.update.AppUpdateScheduler
import com.example.features.update.UpdateAvailableDialog
import com.example.features.update.UpdateCheckResult
import com.example.features.update.UpdateReleaseManifest
import com.example.features.welcome.WelcomeScreen
import com.example.ui.theme.LocalSankalanPalette
import com.example.ui.theme.SankalanTheme
import com.example.ui.theme.SankalanTokens
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SankalanRoutes {
  const val HOME = "home"
  const val BOOKSHELF = "bookshelf"
  const val CALENDAR = "calendar"
  const val STUDIO = "studio"
  const val ARCHIVE = "archive"
  const val SETTINGS = "settings"
}

private data class NavTabItem(
  val route: String,
  val label: (SankalanStrings) -> String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

private val PhoneBottomTabs =
  listOf(
    NavTabItem(
      route = SankalanRoutes.HOME,
      label = { it.navHome },
      selectedIcon = Icons.Filled.Home,
      unselectedIcon = Icons.Outlined.Home,
      testTag = "nav_home",
    ),
    NavTabItem(
      route = SankalanRoutes.BOOKSHELF,
      label = { it.navBookshelf },
      selectedIcon = Icons.Filled.AutoStories,
      unselectedIcon = Icons.Outlined.AutoStories,
      testTag = "nav_bookshelf",
    ),
    NavTabItem(
      route = SankalanRoutes.CALENDAR,
      label = { it.navCalendar },
      selectedIcon = Icons.Filled.CalendarMonth,
      unselectedIcon = Icons.Outlined.CalendarMonth,
      testTag = "nav_calendar",
    ),
    NavTabItem(
      route = SankalanRoutes.STUDIO,
      label = { it.navStudio },
      selectedIcon = Icons.Filled.Brush,
      unselectedIcon = Icons.Outlined.Brush,
      testTag = "nav_studio",
    ),
    NavTabItem(
      route = SankalanRoutes.SETTINGS,
      label = { it.navSettings },
      selectedIcon = Icons.Filled.Settings,
      unselectedIcon = Icons.Outlined.Settings,
      testTag = "nav_settings",
    ),
  )

private val RailTabs =
  listOf(
    PhoneBottomTabs[0],
    PhoneBottomTabs[1],
    PhoneBottomTabs[2],
    PhoneBottomTabs[3],
    NavTabItem(
      route = SankalanRoutes.ARCHIVE,
      label = { it.navArchive },
      selectedIcon = Icons.Filled.Archive,
      unselectedIcon = Icons.Outlined.Archive,
      testTag = "nav_archive",
    ),
    PhoneBottomTabs[4],
  )

@Composable
fun SankalanApp(viewModel: MainViewModel = viewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val settings = uiState.settings
  val strings = stringsFor(settings.language)

  val context = LocalContext.current
  val lockManager = remember(context) { AppLockManager(context) }

  var showCreateJournal by remember { mutableStateOf(false) }
  var showGlobalSearch by remember { mutableStateOf(false) }
  var inspectedBookId by remember { mutableStateOf<String?>(null) }
  var readingBookId by remember { mutableStateOf<String?>(null) }
  var readingInitialPageId by remember { mutableStateOf<String?>(null) }
  var pageManagerBookId by remember { mutableStateOf<String?>(null) }
  var editingTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
  var renameTarget by remember { mutableStateOf<DiaryWithPages?>(null) }
  var deleteTarget by remember { mutableStateOf<DiaryWithPages?>(null) }
  var exportTarget by remember { mutableStateOf<Pair<DiaryWithPages, Page?>?>(null) }
  var isSessionUnlocked by rememberSaveable { mutableStateOf(false) }
  var previousLockState by remember { mutableStateOf<Boolean?>(null) }
  var pendingStartupUpdate by remember { mutableStateOf<UpdateReleaseManifest?>(null) }
  var hasCheckedStartupUpdate by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(uiState.isLoaded, settings.lockEnabled) {
    if (uiState.isLoaded) {
      if (previousLockState == false && settings.lockEnabled) {
        // User just enabled App Lock in Settings: keep current active session unlocked.
        isSessionUnlocked = true
      }
      previousLockState = settings.lockEnabled
    }
  }

  val updateScope = rememberCoroutineScope()

  LaunchedEffect(uiState.isLoaded) {
    if (uiState.isLoaded) {
      AppUpdateScheduler.schedulePeriodicCheck(context)
    }
  }

  // Run automatic update check at launch AND automatically when internet connects later
  DisposableEffect(uiState.isLoaded, settings.githubUpdateRepo) {
    if (!uiState.isLoaded) {
      return@DisposableEffect onDispose {}
    }

    fun runAutoUpdateCheck() {
      updateScope.launch(Dispatchers.Main) {
        if (hasCheckedStartupUpdate) return@launch
        val res =
          AppUpdateChecker.checkForUpdates(
            repoOrUrl = settings.githubUpdateRepo,
            currentVersionCode = BuildConfig.VERSION_CODE,
            currentVersionName = BuildConfig.VERSION_NAME,
          )
        if (res is UpdateCheckResult.UpdateAvailable) {
          hasCheckedStartupUpdate = true
          pendingStartupUpdate = res.release
          AppUpdateNotifier.showUpdateNotificationIfNeeded(
            context = context,
            release = res.release,
            isBangla = settings.language == com.example.core.model.AppLanguage.BN_BD,
          )
        } else if (res is UpdateCheckResult.UpToDate) {
          hasCheckedStartupUpdate = true
        }
      }
    }

    runAutoUpdateCheck()

    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val callback =
      object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
          runAutoUpdateCheck()
        }
      }
    val registered =
      runCatching {
        val req =
          NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm?.registerNetworkCallback(req, callback)
      }.isSuccess

    onDispose {
      if (registered) {
        runCatching { cm?.unregisterNetworkCallback(callback) }
      }
    }
  }

  val allBooks = remember(uiState.activeDiaries, uiState.archivedDiaries) {
    uiState.activeDiaries + uiState.archivedDiaries
  }
  val inspectedBook = remember(inspectedBookId, allBooks) {
    inspectedBookId?.let { id -> allBooks.firstOrNull { it.diary.id == id } }
  }
  val readingBook = remember(readingBookId, allBooks) {
    readingBookId?.let { id -> allBooks.firstOrNull { it.diary.id == id } }
  }
  val pageManagerBook = remember(pageManagerBookId, allBooks) {
    pageManagerBookId?.let { id -> allBooks.firstOrNull { it.diary.id == id } }
  }

  SankalanTheme(
    themeMode = settings.themeMode,
    highContrast = settings.highContrast,
    language = settings.language,
    textScale = settings.textScale,
  ) {
    CompositionLocalProvider(LocalStrings provides strings) {
      val palette = LocalSankalanPalette.current
      val navController = rememberNavController()
      val backStackEntry by navController.currentBackStackEntryAsState()
      val currentRoute = backStackEntry?.destination?.route ?: SankalanRoutes.HOME

      fun navigateTab(route: String) {
        navController.navigate(route) {
          popUpTo(navController.graph.findStartDestination().id) { saveState = true }
          launchSingleTop = true
          restoreState = true
        }
      }

      if (!uiState.isLoaded) {
        Box(modifier = Modifier.fillMaxSize().background(SankalanTokens.Walnut900))
        return@CompositionLocalProvider
      }

      Box(modifier = Modifier.fillMaxSize().background(palette.background)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
          val useRail = maxWidth >= 700.dp

          Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = palette.background,
            bottomBar = {
              if (!useRail) {
                NavigationBar(
                  containerColor = palette.shelfSurface,
                  contentColor = palette.textPrimary,
                ) {
                  PhoneBottomTabs.forEach { tab ->
                    val selected = currentRoute == tab.route
                    NavigationBarItem(
                      selected = selected,
                      onClick = { navigateTab(tab.route) },
                      icon = {
                        Icon(
                          imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                          contentDescription = tab.label(strings),
                        )
                      },
                      label = {
                        Text(
                          text = tab.label(strings),
                          style = MaterialTheme.typography.labelSmall,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                      },
                      colors =
                        NavigationBarItemDefaults.colors(
                          selectedIconColor = SankalanTokens.Walnut900,
                          selectedTextColor = palette.goldAccent,
                          indicatorColor = palette.goldAccent,
                          unselectedIconColor = palette.textSecondary,
                          unselectedTextColor = palette.textSecondary,
                        ),
                      modifier = Modifier.testTag(tab.testTag),
                    )
                  }
                }
              }
            },
          ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
              if (useRail) {
                NavigationRail(
                  containerColor = palette.shelfSurface,
                  contentColor = palette.textPrimary,
                  modifier = Modifier.fillMaxHeight(),
                ) {
                  RailTabs.forEach { tab ->
                    val selected = currentRoute == tab.route
                    NavigationRailItem(
                      selected = selected,
                      onClick = { navigateTab(tab.route) },
                      icon = {
                        Icon(
                          imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                          contentDescription = tab.label(strings),
                        )
                      },
                      label = {
                        Text(
                          text = tab.label(strings),
                          style = MaterialTheme.typography.labelSmall,
                          maxLines = 1,
                        )
                      },
                      colors =
                        NavigationRailItemDefaults.colors(
                          selectedIconColor = SankalanTokens.Walnut900,
                          selectedTextColor = palette.goldAccent,
                          indicatorColor = palette.goldAccent,
                          unselectedIconColor = palette.textSecondary,
                          unselectedTextColor = palette.textSecondary,
                        ),
                      modifier = Modifier.testTag(tab.testTag),
                    )
                  }
                }
              }

              NavHost(
                navController = navController,
                startDestination = SankalanRoutes.HOME,
                modifier = Modifier.weight(1f).fillMaxHeight(),
              ) {
                composable(SankalanRoutes.HOME) {
                  HomeScreen(
                    currentLanguage = settings.language,
                    numeralStyle = settings.numeralStyle,
                    reduceMotion = settings.reducedMotion,
                    liteMode = settings.liteMode,
                    reminderEnabled = settings.reminderEnabled,
                    activeDiaries = uiState.activeDiaries,
                    archivedCount = uiState.archivedDiaries.size,
                    saveStatus = uiState.saveStatus,
                    onRetrySave = { viewModel.retryLastSave() },
                    onToggleLanguage = { viewModel.toggleLanguage() },
                    onReplayWelcome = { viewModel.replayWelcome() },
                    onOpenSettings = { navigateTab(SankalanRoutes.SETTINGS) },
                    onOpenBookshelf = { navigateTab(SankalanRoutes.BOOKSHELF) },
                    onOpenArchive = { navigateTab(SankalanRoutes.ARCHIVE) },
                    onOpenGlobalSearch = { showGlobalSearch = true },
                    onOpenCreateJournal = { showCreateJournal = true },
                    onWritePromptEntry = { promptTitle, promptQuestion ->
                      val targetDiary = uiState.activeDiaries.firstOrNull()
                      if (targetDiary != null) {
                        val cal = Calendar.getInstance()
                        val todayIso =
                          formatIsoDate(
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH),
                          )
                        viewModel.createPromptPage(
                          diaryId = targetDiary.diary.id,
                          dateIso = todayIso,
                          title = promptTitle,
                          bodyText = promptQuestion,
                        ) { newPageId ->
                          editingTarget = targetDiary.diary.id to newPageId
                        }
                      } else {
                        showCreateJournal = true
                      }
                    },
                    onInspectBook = { book -> inspectedBookId = book.diary.id },
                    onOpenMemory = { diaryBundle, pageIndex ->
                      readingInitialPageId = diaryBundle.pages.getOrNull(pageIndex)?.id
                      readingBookId = diaryBundle.diary.id
                    },
                  )
                }
                composable(SankalanRoutes.BOOKSHELF) {
                  BookshelfScreen(
                    diaries = uiState.activeDiaries,
                    archivedCount = uiState.archivedDiaries.size,
                    numeralStyle = settings.numeralStyle,
                    saveStatus = uiState.saveStatus,
                    onRetrySave = { viewModel.retryLastSave() },
                    onOpenCreateJournal = { showCreateJournal = true },
                    onOpenArchive = { navigateTab(SankalanRoutes.ARCHIVE) },
                    onInspectBook = { book -> inspectedBookId = book.diary.id },
                  )
                }
                composable(SankalanRoutes.CALENDAR) {
                  CalendarScreen(
                    language = settings.language,
                    numeralStyle = settings.numeralStyle,
                    reminderEnabled = settings.reminderEnabled,
                    reminderHour = settings.reminderHour,
                    reminderMinute = settings.reminderMinute,
                    activeDiaries = uiState.activeDiaries,
                    datedPages = uiState.datedPages,
                    onOpenDiaryAtPage = { diaryBundle, pageIndex ->
                      readingInitialPageId = diaryBundle.pages.getOrNull(pageIndex)?.id
                      readingBookId = diaryBundle.diary.id
                    },
                    onAddDatedPage = { diaryId, dateIso, title, body ->
                      viewModel.addDatedPage(diaryId, dateIso, title, body)
                    },
                  )
                }
                composable(SankalanRoutes.STUDIO) {
                  StudioScreen(
                    language = settings.language,
                    numeralStyle = settings.numeralStyle,
                    activeDiaries = uiState.activeDiaries,
                    onApplyToDiaryPage = { diaryId, updatedPage ->
                      viewModel.savePageChanges(diaryId, updatedPage)
                      editingTarget = diaryId to updatedPage.id
                    },
                  )
                }
                composable(SankalanRoutes.ARCHIVE) {
                  ArchiveScreen(
                    archivedDiaries = uiState.archivedDiaries,
                    numeralStyle = settings.numeralStyle,
                    onInspectBook = { book -> inspectedBookId = book.diary.id },
                  )
                }
                composable(SankalanRoutes.SETTINGS) {
                  SettingsScreen(
                    settings = settings,
                    activeDiaries = allBooks,
                    datedPages = uiState.datedPages,
                    onLanguageChange = { viewModel.setLanguage(it) },
                    onNumeralStyleChange = { viewModel.setNumeralStyle(it) },
                    onThemeModeChange = { viewModel.setThemeMode(it) },
                    onReadingLayoutChange = { viewModel.setReadingLayout(it) },
                    onPageTurnAnimationChange = { viewModel.setPageTurnAnimation(it) },
                    onPageTurnSoundChange = { viewModel.setPageTurnSound(it) },
                    onLiteModeChange = { viewModel.setLiteMode(it) },
                    onReducedMotionChange = { viewModel.setReducedMotion(it) },
                    onTextScaleChange = { viewModel.setTextScale(it) },
                    onHighContrastChange = { viewModel.setHighContrast(it) },
                    onReminderEnabledChange = { viewModel.setReminderEnabled(it) },
                    onReminderTimeChange = { h, m -> viewModel.setReminderTime(h, m) },
                    onLockEnabledChange = { viewModel.setLockEnabled(it) },
                    onGithubUpdateRepoChange = { viewModel.setGithubUpdateRepo(it) },
                    onExportBackupSuspend = { viewModel.getAllDiariesOnce() },
                    onRestoreBackupSuspend = { list -> viewModel.restoreBackupDiaries(list) },
                    onReplayWelcome = { viewModel.replayWelcome() },
                  )
                }
              }
            }
          }
        }

        // Modals & Interactive Book Overlay Flows
        if (showCreateJournal) {
          CreateJournalSheet(
            numeralStyle = settings.numeralStyle,
            onDismiss = { showCreateJournal = false },
            onCreate = { title, desc, cover, pages ->
              viewModel.createDiary(title, desc, cover, pages)
              showCreateJournal = false
            },
          )
        }

        if (inspectedBook != null) {
          BookCoverInspectionDialog(
            diaryWithPages = inspectedBook,
            numeralStyle = settings.numeralStyle,
            onDismiss = { inspectedBookId = null },
            onOpenDiary = {
              readingInitialPageId = null
              readingBookId = inspectedBook.diary.id
              inspectedBookId = null
            },
            onManagePages = {
              pageManagerBookId = inspectedBook.diary.id
              inspectedBookId = null
            },
            onExport = {
              exportTarget = inspectedBook to null
              inspectedBookId = null
            },
            onRename = { renameTarget = inspectedBook },
            onDuplicate = {
              viewModel.duplicateDiary(inspectedBook.diary.id)
              inspectedBookId = null
            },
            onToggleArchive = {
              viewModel.setDiaryArchived(inspectedBook.diary.id, !inspectedBook.diary.archived)
              inspectedBookId = null
            },
            onDelete = { deleteTarget = inspectedBook },
          )
        }

        if (renameTarget != null) {
          val target = renameTarget!!
          RenameDiaryDialog(
            initialTitle = target.diary.title,
            initialDescription = target.diary.description,
            onDismiss = { renameTarget = null },
            onConfirm = { newTitle, newDesc ->
              viewModel.renameDiary(target.diary.id, newTitle, newDesc)
              renameTarget = null
            },
          )
        }

        if (deleteTarget != null) {
          val target = deleteTarget!!
          DeleteDiaryConfirmDialog(
            diaryTitle = target.diary.title,
            onDismiss = { deleteTarget = null },
            onConfirmDelete = {
              viewModel.deleteDiary(target.diary.id)
              deleteTarget = null
              inspectedBookId = null
              if (readingBookId == target.diary.id) readingBookId = null
              if (pageManagerBookId == target.diary.id) pageManagerBookId = null
            },
          )
        }

        if (readingBook != null) {
          ReadingModeScreen(
            diaryWithPages = readingBook,
            settings = settings,
            initialPageId = readingInitialPageId,
            onDismiss = { readingBookId = null },
            onOpenPageManager = { currentPageId ->
              readingInitialPageId = currentPageId
              pageManagerBookId = readingBook.diary.id
            },
            onRequestEditPage = { dId, pId ->
              readingInitialPageId = pId
              editingTarget = dId to pId
            },
            onRequestExport = { activePageId ->
              val activePage = activePageId?.let { id -> readingBook.pages.firstOrNull { it.id == id } }
              exportTarget = readingBook to activePage
            },
          )
        }

        if (editingTarget != null) {
          val (editDiaryId, editPageId) = editingTarget!!
          val editBundle = allBooks.firstOrNull { it.diary.id == editDiaryId }
          val editPageIdx = editBundle?.pages?.indexOfFirst { it.id == editPageId } ?: -1
          if (editBundle != null && editPageIdx >= 0) {
            CanvasEditorScreen(
              diary = editBundle.diary,
              initialPage = editBundle.pages[editPageIdx],
              pageNumber = editPageIdx + 1,
              language = settings.language,
              numeralStyle = settings.numeralStyle,
              saveStatus = uiState.saveStatus,
              onRetrySave = { viewModel.retryLastSave() },
              onPersistPage = { updatedPage ->
                viewModel.savePageChanges(editDiaryId, updatedPage)
              },
              onDismiss = { editingTarget = null },
            )
          }
        }

        if (pageManagerBook != null) {
          PageManagerSheet(
            diaryWithPages = pageManagerBook,
            numeralStyle = settings.numeralStyle,
            onDismiss = { pageManagerBookId = null },
            onOpenPageInReader = { pageId ->
              readingInitialPageId = pageId
              readingBookId = pageManagerBook.diary.id
              pageManagerBookId = null
            },
            onAddPage = { dId -> viewModel.addBlankPage(dId) },
            onDuplicatePage = { dId, pId -> viewModel.duplicatePage(dId, pId) },
            onReorderPages = { dId, orderedIds -> viewModel.reorderPages(dId, orderedIds) },
            onTogglePinPage = { dId, pId -> viewModel.togglePinPage(dId, pId) },
            onResetPage = { dId, pId -> viewModel.resetPage(dId, pId) },
            onSoftDeletePage = { dId, pId -> viewModel.softDeletePage(dId, pId) },
            onRestoreDeletedPage = { dId, pId -> viewModel.restoreDeletedPage(dId, pId) },
            onPermanentlyDeletePage = { pId -> viewModel.permanentlyDeletePage(pId) },
          )
        }

        if (showGlobalSearch) {
          GlobalSearchDialog(
            diaries = allBooks,
            numeralStyle = settings.numeralStyle,
            onDismiss = { showGlobalSearch = false },
            onSelectDiary = { bundle -> inspectedBookId = bundle.diary.id },
            onSelectPage = { bundle, pageIdx ->
              readingInitialPageId = bundle.pages.getOrNull(pageIdx)?.id
              readingBookId = bundle.diary.id
            },
          )
        }

        if (exportTarget != null) {
          val (targetDiary, targetPage) = exportTarget!!
          ExportDiaryDialog(
            diaryWithPages = targetDiary,
            activePage = targetPage,
            numeralStyle = settings.numeralStyle,
            onDismiss = { exportTarget = null },
          )
        }

        if (pendingStartupUpdate != null &&
          !uiState.showWelcome &&
          (!settings.lockEnabled || !lockManager.hasPin() || isSessionUnlocked)
        ) {
          UpdateAvailableDialog(
            release = pendingStartupUpdate!!,
            language = settings.language,
            onDismiss = { pendingStartupUpdate = null },
          )
        }

        if (settings.lockEnabled && lockManager.hasPin() && !isSessionUnlocked && !uiState.showWelcome) {
          AppLockGateOverlay(
            numeralStyle = settings.numeralStyle,
            onUnlocked = { isSessionUnlocked = true },
          )
        }

        // Launch & Replay Water-Logo Welcome Overlay
        AnimatedVisibility(
          visible = uiState.showWelcome,
          enter = fadeIn(tween(if (settings.reducedMotion) 0 else 250)),
          exit = fadeOut(tween(if (settings.reducedMotion) 0 else 380)),
        ) {
          WelcomeScreen(
            currentLanguage = settings.language,
            showLanguagePrompt = !settings.hasChosenInitialLanguage,
            reducedMotionSetting = settings.reducedMotion,
            onSelectLanguage = { viewModel.setLanguage(it, markInitialChosen = true) },
            onEnterHome = { viewModel.dismissWelcome() },
          )
        }
      }
    }
  }
}
