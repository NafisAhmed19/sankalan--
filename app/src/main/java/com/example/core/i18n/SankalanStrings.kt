package com.example.core.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.core.model.AppLanguage
import com.example.core.model.NumeralStyle

interface SankalanStrings {
  val brandName: String
  val brandLogoWordmark: String
  val tagline: String
  val welcomeSkip: String
  val welcomeTapHint: String
  val welcomeLanguagePrompt: String
  val languageBanglaLabel: String
  val languageEnglishLabel: String

  // Primary navigation & screens
  val navHome: String
  val navBookshelf: String
  val navCalendar: String
  val navStudio: String
  val navArchive: String
  val navSettings: String

  // Core actions & states (Handoff §9 + Spec §11)
  val myBookshelf: String
  val createJournal: String
  val readingMode: String
  val editPage: String
  val addPage: String
  val pageManager: String
  val save: String
  val saving: String
  val saved: String
  val saveFailed: String
  val retry: String
  val exportPdf: String
  val share: String
  val cancel: String
  val confirm: String
  val delete: String
  val deletePageConfirm: String
  val visibilityPrivate: String
  val visibilityUnlisted: String
  val visibilityPublic: String

  // Welcome & Home
  val replayWelcome: String
  val homeSubtitle: String
  val localFirstBadge: String
  val memoryLaneTitle: String
  val memoryLaneSubtitle: String
  val memoryLaneEmptyHint: String
  val bookshelfEmptyTitle: String
  val bookshelfEmptyBody: String
  val coverStylesPreviewTitle: String
  val coverLuxuryLabel: String
  val coverArtisticLabel: String
  val coverMinimalLabel: String
  val studioPreviewTitle: String
  val studioPreviewBody: String
  val archiveEmptyTitle: String
  val archiveEmptyBody: String
  val phaseMilestoneBadge: String

  // Bookshelf 3D, Search, Actions (Steps 3-4)
  val shelfViewLabel: String
  val gridViewLabel: String
  val searchBooksHint: String
  val filterAll: String
  val sortRecent: String
  val sortTitle: String
  val sortPages: String
  val pagesUnit: String
  val openDiary: String
  val renameDiary: String
  val duplicateDiary: String
  val archiveDiary: String
  val unarchiveDiary: String
  val deleteDiary: String
  val deleteDiaryConfirmTitle: String
  val deleteDiaryConfirmBody: String
  val renameDiaryTitle: String
  val diaryTitleHint: String
  val diaryDescHint: String
  val coverStyleLabel: String
  val coverColorLabel: String
  val spineStyleLabel: String
  val spineRibbedGold: String
  val spineSmoothLeather: String
  val spineStitchedCloth: String
  val initialPagesLabel: String
  val initialPagesRuleHint: String
  val createButtonLabel: String
  val frontCoverTag: String
  val backCoverTag: String
  val demoBadge: String
  val closeLabel: String
  val previewPagesTitle: String
  val nextPageLabel: String
  val prevPageLabel: String
  val coverLabel: String
  val pageNumberPrefix: String
  val editorComingNextHint: String

  // Calendar View (Step 4)
  val calendarTitle: String
  val calendarSubtitle: String
  val calendarNoEntriesForDay: String
  val calendarCreateDatedPage: String
  val calendarSelectDiary: String
  val calendarPinnedBadge: String

  // Memory Lane (Step 5)
  val memoryLanePlay: String
  val memoryLanePause: String
  val memoryLanePrev: String
  val memoryLaneNext: String
  val memoryLaneReadMemory: String
  val memoryLanePinnedLabel: String
  val memoryLaneOnThisDayLabel: String
  val memoryLaneReflectionLabel: String
  val memoryLaneNoMemories: String

  // Page Manager (Step 5)
  val activePagesTab: String
  val recentlyDeletedTab: String
  val pinPageLabel: String
  val unpinPageLabel: String
  val duplicatePageLabel: String
  val resetPageLabel: String
  val resetPageConfirmBody: String
  val moveEarlierLabel: String
  val moveLaterLabel: String
  val restorePageLabel: String
  val deleteForeverLabel: String
  val minPagesReachedNotice: String
  val maxPagesReachedNotice: String
  val recentlyDeletedEmpty: String
  val blankPageLabel: String

  // Reading Mode (Step 6)
  val singlePageLayout: String
  val twoPageSpreadLayout: String
  val toggleFullscreenLabel: String
  val resetZoomLabel: String
  val tapToToggleHudHint: String
  val spreadLabel: String
  val editLocationPreservedHint: String

  // Canvas Editor (Step 7)
  val editorUndo: String
  val editorRedo: String
  val editorDone: String
  val editorToolSelect: String
  val editorToolText: String
  val editorToolDraw: String
  val editorToolPhoto: String
  val editorToolShapes: String
  val editorToolLayers: String
  val editorToolPageInfo: String
  val editorAddTextBox: String
  val editorEditTextTitle: String
  val editorTextPlaceholder: String
  val editorFontLabel: String
  val editorPenInk: String
  val editorPenPencil: String
  val editorPenMarker: String
  val editorPenHighlighter: String
  val editorEraser: String
  val editorStylusOnly: String
  val editorStylusOnlyHint: String
  val editorStrokeSize: String
  val editorPickPhoto: String
  val editorCropAndFrame: String
  val editorFrameClean: String
  val editorFramePolaroid: String
  val editorFrameTornPaper: String
  val editorFrameRounded: String
  val editorFrameOval: String
  val editorCropZoom: String
  val editorLayersEmpty: String
  val editorLayerText: String
  val editorLayerStroke: String
  val editorLayerPhoto: String
  val editorLayerShape: String
  val editorBringForward: String
  val editorSendBackward: String
  val editorLockLayer: String
  val editorHideLayer: String
  val editorGroupSelected: String
  val editorUngroup: String
  val editorOpacity: String
  val editorRotate: String
  val editorMoodLabel: String
  val editorDateLabel: String
  val editorPageTitleHint: String

  // Step 8: Studio, 22 Papers, 12 Templates, Asset Pack 1 & Voice Notes
  val editorToolStudioAssets: String
  val editorToolPaper: String
  val editorToolTemplates: String
  val editorToolVoice: String
  val studioSearchHint: String
  val studioCatAll: String
  val studioCatPapers: String
  val studioCatTemplates: String
  val studioCatTape: String
  val studioCatFrames: String
  val studioCatLabels: String
  val studioCatStamps: String
  val studioCatUploads: String
  val studioCatFavorites: String
  val studioUploadAssetButton: String
  val studioFavoritesEmpty: String
  val studioUploadsEmpty: String
  val studioUseInDiary: String
  val studioPickTargetPage: String
  val paperEdgeStyleLabel: String
  val paperEdgeClean: String
  val paperEdgeDeckle: String
  val paperEdgeTorn: String
  val templateSafeTitle: String
  val templateSafeBody: String
  val templateReplaceOption: String
  val templateOverlayOption: String
  val templateAppliedBanner: String
  val voiceRecordStart: String
  val voiceRecordStop: String
  val voiceRecordingStatus: String
  val voicePermissionNeeded: String
  val voiceGrantPermission: String
  val voicePlayLabel: String
  val voiceStopPlayLabel: String
  val voiceNoteLabel: String

  // Settings -> Appearance & Language
  val appearanceAndLanguage: String
  val settingsSubtitle: String
  val uiLanguageTitle: String
  val uiLanguageDescription: String
  val numeralStyleTitle: String
  val numeralLatinOption: String
  val numeralBengaliOption: String
  val themeTitle: String
  val themeLight: String
  val themeDark: String
  val themeSystem: String
  val themePaletteCaption: String
  val typographyPreviewTitle: String
  val typographyBanglaHeadingLabel: String
  val typographyBanglaHeadingSample: String
  val typographyBanglaBodyLabel: String
  val typographyBanglaBodySample: String
  val typographyEnglishHeadingLabel: String
  val typographyEnglishHeadingSample: String
  val typographyEnglishBodyLabel: String
  val typographyEnglishBodySample: String
  val readingPreferencesTitle: String
  val readingLayoutLabel: String
  val readingLayoutSingle: String
  val readingLayoutSpread: String
  val pageTurnAnimationLabel: String
  val pageTurnSoundLabel: String
  val liteModeLabel: String
  val liteModeDescription: String
  val accessibilityTitle: String
  val reduceMotionLabel: String
  val reduceMotionDescription: String
  val textScaleLabel: String
  val highContrastLabel: String
  val highContrastDescription: String
  val privacyAndStorageTitle: String
  val privacyAndStorageBody: String
  val appVersionLabel: String

  // Step 9: Asset Packs 2 & 3, Moods, Prompts, Global Search, Reminders/.ics, App Lock
  val studioCatFlorals: String
  val studioCatEphemera: String
  val studioCatSeasonal: String
  val insightsTitle: String
  val moodTrackerTitle: String
  val moodTrackerEmpty: String
  val dailyPromptTitle: String
  val dailyPromptNext: String
  val dailyPromptWriteNow: String
  val streakEntriesLabel: String
  val globalSearchTitle: String
  val globalSearchHint: String
  val globalSearchNoResults: String
  val globalSearchDiariesHeader: String
  val globalSearchPagesHeader: String
  val reminderSettingsTitle: String
  val reminderEnableLabel: String
  val reminderEnableDesc: String
  val reminderTimeLabel: String
  val reminderExportIcsLabel: String
  val reminderIcsExportedMsg: String
  val reminderBannerTitle: String
  val reminderBannerAction: String
  val reminderNotificationTest: String
  val appLockTitle: String
  val appLockEnableLabel: String
  val appLockEnableDesc: String
  val appLockSetPinLabel: String
  val appLockChangePinLabel: String
  val appLockEnterPinTitle: String
  val appLockPinHint: String
  val appLockPinMismatch: String
  val appLockPinTooShort: String
  val appLockUnlockButton: String
  val appLockBiometricButton: String
  val appLockBiometricPromptTitle: String
  val appLockBiometricPromptSub: String
  val appLockWrongPin: String

  // Step 10: Native PDF/PNG/JPEG/ZIP Export, .sankalan Backup & GitHub Releases Update Checker
  val exportDiaryTitle: String
  val exportPdfSingle: String
  val exportPdfSpread: String
  val exportPagePng: String
  val exportPageJpeg: String
  val exportPagesZip: String
  val exportSuccessMsg: String
  val exportFailedMsg: String
  val exportInProgress: String
  val backupAndUpdateTitle: String
  val backupExportButton: String
  val backupExportDesc: String
  val backupRestoreButton: String
  val backupRestoreSuccess: String
  val backupRestoreFailed: String
  val updateCheckerTitle: String
  val updateCheckNowButton: String
  val updateCheckingLabel: String
  val updateRepoHint: String
  val updateUpToDateLabel: String
  val updateAvailableTitle: String
  val updateDownloadButton: String
  val updateCheckFailedLabel: String
  val updateCurrentVersionBadge: String
  val updateFriendsShareGuide: String
  val updateFixedLinkLabel: String
  val updateCopyLinkBtn: String
  val updateShareLinkBtn: String
  val updateLinkCopiedMsg: String
  val updateShareInviteBody: String
  val updateEnableNotifBtn: String
  val updateNotifActiveBadge: String
  val updateCopyManifestBtn: String
  val updateManifestCopiedMsg: String

  // UX Polish: Inline on-paper typing, Word bar, Quick-Tool Orb, Multi-shelf & NRST.tech Owner Card
  val homeHeroTagline: String
  val editorInlineTextHint: String
  val editorQuickOrbLabel: String
  val editorQuickOrbPen: String
  val editorQuickOrbPencil: String
  val editorQuickOrbHighlighter: String
  val editorQuickOrbEraser: String
  val editorQuickOrbText: String
  val editorQuickOrbSticker: String
  val editorQuickAddSticker: String
  val editorQuickStickerFromPhoto: String
  val editorQuickOrbSelect: String
  val editorQuickOrbStylusToggle: String
  val editorFontSizeLabel: String
  val editorBoldLabel: String
  val editorItalicLabel: String
  val editorUnderlineLabel: String
  val editorAlignLeft: String
  val editorAlignCenter: String
  val editorAlignRight: String
  val editorRotateHandleHint: String
  val bookshelfEmptySlotCta: String
  val ownerSectionTitle: String
  val ownerCompanyLabel: String
  val ownerCompanyName: String
  val ownerFounderLabel: String
  val ownerFounderName: String
  val ownerEmailLabel: String
  val ownerEmailAddress: String
  val ownerCraftTagline: String

  // Cloud Auth, Account Creation & Settings Feedback Bar
  val authTitle: String
  val authSubtitle: String
  val authSignInWithGoogleButton: String
  val authCreateAccountButton: String
  val authCreateConfirmButton: String
  val authBackToExistingSignIn: String
  val authDisplayNameInputLabel: String
  val authDisplayNameInputHint: String
  val authGmailAutoAccountHint: String
  val authFacebookManualNote: String
  val accountCardTitle: String
  val accountGmailLabel: String
  val accountNameLabel: String
  val accountSaveNameBtn: String
  val accountNameSavedMsg: String
  val accountNameErrorMsg: String
  val accountSwitchBtn: String
  val accountSignOutBtn: String
  val feedbackSectionTitle: String
  val feedbackSectionSubtitle: String
  val feedbackInputHint: String
  val feedbackSubmitBtn: String
  val feedbackSubmittingBtn: String
  val feedbackSuccessMsg: String
  val feedbackErrorMsg: String
  val feedbackCatGeneral: String
  val feedbackCatFeature: String
  val feedbackCatBug: String
  val feedbackCatLove: String
  val feedbackCommentsTitle: String
  val feedbackScrollHint: String
  val feedbackEmptyLabel: String
  val feedbackDeleteBtn: String
  val feedbackYouBadge: String

  // Smart Sticker Upload, Crop, Object Cutout & Die-Cut Border Editor
  val stickerEditorTitle: String
  val stickerEditorSubtitle: String
  val stickerTitleLabel: String
  val stickerTitleHint: String
  val stickerTabCrop: String
  val stickerTabCutout: String
  val stickerTabBorder: String
  val stickerAspectFree: String
  val stickerAspectSquare: String
  val stickerRotate90: String
  val stickerResetCrop: String
  val stickerModeAuto: String
  val stickerModeLasso: String
  val stickerModeOriginal: String
  val stickerBrushKeep: String
  val stickerBrushErase: String
  val stickerUndoStroke: String
  val stickerClearLasso: String
  val stickerSensitivityLabel: String
  val stickerLassoGuideHint: String
  val stickerAutoGuideHint: String
  val stickerBorderToggleLabel: String
  val stickerBorderThicknessLabel: String
  val stickerBorderColorLabel: String
  val stickerBorderGoldTrimLabel: String
  val stickerBorderShadowLabel: String
  val stickerSaveBtn: String
  val stickerSavingBtn: String
  val stickerEditExistingBtn: String
}

object StringsEn : SankalanStrings {
  override val brandName = "Sankalan-t"
  override val brandLogoWordmark = "Sankalan-ৎ"
  override val tagline = "With the Memories of Your Life."
  override val welcomeSkip = "Skip"
  override val welcomeTapHint = "Tap anywhere on the water to enter"
  override val welcomeLanguagePrompt = "Choose your interface language"
  override val languageBanglaLabel = "বাংলা (বাংলাদেশ)"
  override val languageEnglishLabel = "English"

  override val navHome = "Home"
  override val navBookshelf = "Bookshelf"
  override val navCalendar = "Calendar"
  override val navStudio = "Studio"
  override val navArchive = "Archive"
  override val navSettings = "Settings"

  override val myBookshelf = "My Bookshelf"
  override val createJournal = "Create a Journal"
  override val readingMode = "Reading Mode"
  override val editPage = "Edit Page"
  override val addPage = "Add Page"
  override val pageManager = "Page Manager"
  override val save = "Save"
  override val saving = "Saving…"
  override val saved = "Saved"
  override val saveFailed = "Save failed"
  override val retry = "Retry"
  override val exportPdf = "Export PDF"
  override val share = "Share"
  override val cancel = "Cancel"
  override val confirm = "Confirm"
  override val delete = "Delete"
  override val deletePageConfirm = "Delete Page?"
  override val visibilityPrivate = "Private"
  override val visibilityUnlisted = "Unlisted"
  override val visibilityPublic = "Public"

  override val replayWelcome = "Replay Water Intro"
  override val homeSubtitle = "A quiet walnut-and-gold sanctuary for your memories, sketches, and voice."
  override val localFirstBadge = "100% On-Device · Private by Default"
  override val memoryLaneTitle = "Memory Lane"
  override val memoryLaneSubtitle = "Pinned pages, On This Day reflections & timeless spreads"
  override val memoryLaneEmptyHint = "Create or write a diary entry to watch your memories glide across Memory Lane."
  override val bookshelfEmptyTitle = "No journals match this filter"
  override val bookshelfEmptyBody = "Tap Create a Journal to bind a new 5–200 page diary with a Luxury, Artistic, or Minimal cover."
  override val coverStylesPreviewTitle = "Signature Cover Craft"
  override val coverLuxuryLabel = "Luxury"
  override val coverArtisticLabel = "Artistic"
  override val coverMinimalLabel = "Minimal"
  override val studioPreviewTitle = "Templates & Asset Studio"
  override val studioPreviewBody = "22 tactile papers, 12 templates, Asset Packs 1–3 (botanicals, ephemera & seasonal), uploads & favorites."
  override val archiveEmptyTitle = "Archive Shelf is Empty"
  override val archiveEmptyBody = "Archived journals remain safely stored on your device and are automatically excluded from Memory Lane."
  override val phaseMilestoneBadge = "Phase 1 Complete · Steps 1–10 (Offline Sanctuary, Asset Packs 1–3, Moods, Lock, Native PDF Export, Backup & Release Updates)"

  override val shelfViewLabel = "3D Shelf"
  override val gridViewLabel = "Covers"
  override val searchBooksHint = "Search journals (Bangla / English)…"
  override val filterAll = "All"
  override val sortRecent = "Recent"
  override val sortTitle = "Title"
  override val sortPages = "Pages"
  override val pagesUnit = "pages"
  override val openDiary = "Open Diary"
  override val renameDiary = "Rename"
  override val duplicateDiary = "Duplicate"
  override val archiveDiary = "Archive"
  override val unarchiveDiary = "Restore to Shelf"
  override val deleteDiary = "Delete"
  override val deleteDiaryConfirmTitle = "Delete Journal?"
  override val deleteDiaryConfirmBody = "This will permanently remove the journal and all of its pages from this device."
  override val renameDiaryTitle = "Rename Journal"
  override val diaryTitleHint = "Journal title (Bangla or English)"
  override val diaryDescHint = "Short description or subtitle (optional)"
  override val coverStyleLabel = "Cover Style"
  override val coverColorLabel = "Binding Material & Color"
  override val spineStyleLabel = "Spine Craft"
  override val spineRibbedGold = "Ribbed Gold"
  override val spineSmoothLeather = "Smooth Leather"
  override val spineStitchedCloth = "Stitched Cloth"
  override val initialPagesLabel = "Starting Pages"
  override val initialPagesRuleHint = "Each Diary maintains 5–200 active pages."
  override val createButtonLabel = "Bind Journal"
  override val frontCoverTag = "Front Cover"
  override val backCoverTag = "Back Cover"
  override val demoBadge = "Demo"
  override val closeLabel = "Close"
  override val previewPagesTitle = "Journal Preview"
  override val nextPageLabel = "Next"
  override val prevPageLabel = "Previous"
  override val coverLabel = "Cover"
  override val pageNumberPrefix = "Page"
  override val editorComingNextHint = "22 Papers, 12 Templates, Voice Notes & Asset Pack 1 are ready inside the toolbar."

  override val calendarTitle = "Journal Calendar"
  override val calendarSubtitle = "Browse dated diary pages and jump straight into any memory."
  override val calendarNoEntriesForDay = "No dated pages on this date yet."
  override val calendarCreateDatedPage = "Add Dated Page"
  override val calendarSelectDiary = "Select Diary"
  override val calendarPinnedBadge = "Pinned"

  override val memoryLanePlay = "Play"
  override val memoryLanePause = "Pause"
  override val memoryLanePrev = "Previous memory"
  override val memoryLaneNext = "Next memory"
  override val memoryLaneReadMemory = "Open Memory"
  override val memoryLanePinnedLabel = "Pinned Memory"
  override val memoryLaneOnThisDayLabel = "On This Day"
  override val memoryLaneReflectionLabel = "Curated Page"
  override val memoryLaneNoMemories = "Write or pin a page in any non-archived journal to illuminate Memory Lane."

  override val activePagesTab = "Active Pages"
  override val recentlyDeletedTab = "Recently Deleted"
  override val pinPageLabel = "Pin to Memory Lane"
  override val unpinPageLabel = "Unpin"
  override val duplicatePageLabel = "Duplicate Page"
  override val resetPageLabel = "Reset Page"
  override val resetPageConfirmBody = "Clear the text, date, and elements on this page? The page background and order remain."
  override val moveEarlierLabel = "Move Earlier"
  override val moveLaterLabel = "Move Later"
  override val restorePageLabel = "Restore"
  override val deleteForeverLabel = "Delete Forever"
  override val minPagesReachedNotice = "Minimum 5 active pages required per diary."
  override val maxPagesReachedNotice = "Maximum 200 active pages reached."
  override val recentlyDeletedEmpty = "No recently deleted pages in this journal."
  override val blankPageLabel = "Blank Page"

  override val singlePageLayout = "Single"
  override val twoPageSpreadLayout = "Spread"
  override val toggleFullscreenLabel = "Fullscreen"
  override val resetZoomLabel = "Reset Zoom"
  override val tapToToggleHudHint = "Drag corners to curl · Pinch to zoom · Tap page to hide controls"
  override val spreadLabel = "Spread"
  override val editLocationPreservedHint = "Preserved exact page location for Canvas Editor."

  override val editorUndo = "Undo"
  override val editorRedo = "Redo"
  override val editorDone = "Done"
  override val editorToolSelect = "Select"
  override val editorToolText = "Text"
  override val editorToolDraw = "Draw"
  override val editorToolPhoto = "Photo"
  override val editorToolShapes = "Shapes"
  override val editorToolLayers = "Layers"
  override val editorToolPageInfo = "Page Info"
  override val editorAddTextBox = "Add Text"
  override val editorEditTextTitle = "Edit Text & Typography"
  override val editorTextPlaceholder = "Write your memory in বাংলা or English…"
  override val editorFontLabel = "Diary & Handwriting Font"
  override val editorPenInk = "Fountain Pen"
  override val editorPenPencil = "Pencil"
  override val editorPenMarker = "Marker"
  override val editorPenHighlighter = "Highlighter"
  override val editorEraser = "Eraser"
  override val editorStylusOnly = "Palm Rejection (Stylus Ink)"
  override val editorStylusOnlyHint = "Ignore finger touches while drawing with a stylus"
  override val editorStrokeSize = "Stroke Size"
  override val editorPickPhoto = "Choose Photo"
  override val editorCropAndFrame = "Crop & Frame Photo"
  override val editorFrameClean = "Clean Cut"
  override val editorFramePolaroid = "Polaroid"
  override val editorFrameTornPaper = "Torn Paper"
  override val editorFrameRounded = "Soft Rounded"
  override val editorFrameOval = "Oval Cameo"
  override val editorCropZoom = "Crop Focus"
  override val editorLayersEmpty = "No elements on this page yet. Add text, ink strokes, photos, or shapes."
  override val editorLayerText = "Text"
  override val editorLayerStroke = "Ink Stroke"
  override val editorLayerPhoto = "Photo"
  override val editorLayerShape = "Shape"
  override val editorBringForward = "Forward"
  override val editorSendBackward = "Backward"
  override val editorLockLayer = "Lock"
  override val editorHideLayer = "Visibility"
  override val editorGroupSelected = "Group"
  override val editorUngroup = "Ungroup"
  override val editorOpacity = "Opacity"
  override val editorRotate = "Rotate"
  override val editorMoodLabel = "Mood Tag"
  override val editorDateLabel = "Date (YYYY-MM-DD)"
  override val editorPageTitleHint = "Page Heading / Memory Title"

  override val editorToolStudioAssets = "Stickers"
  override val editorToolPaper = "Paper"
  override val editorToolTemplates = "Templates"
  override val editorToolVoice = "Voice"
  override val studioSearchHint = "Search papers, templates, tape, stamps…"
  override val studioCatAll = "All"
  override val studioCatPapers = "Papers (22)"
  override val studioCatTemplates = "Templates (12)"
  override val studioCatTape = "Washi Tape"
  override val studioCatFrames = "Frames"
  override val studioCatLabels = "Labels"
  override val studioCatStamps = "Stamps"
  override val studioCatUploads = "My Uploads"
  override val studioCatFavorites = "Favorites"
  override val studioUploadAssetButton = "Upload Sticker / Photo"
  override val studioFavoritesEmpty = "Tap the star on any paper, template, or asset to pin it to Favorites."
  override val studioUploadsEmpty = "Upload personal stickers or cutouts cleanly using Photo Picker."
  override val studioUseInDiary = "Apply to Diary"
  override val studioPickTargetPage = "Choose Journal & Page"
  override val paperEdgeStyleLabel = "Paper Cut Edge"
  override val paperEdgeClean = "Clean Cut"
  override val paperEdgeDeckle = "Deckle Edge"
  override val paperEdgeTorn = "Torn Parchment"
  override val templateSafeTitle = "Page Has Existing Content"
  override val templateSafeBody = "This page already contains elements. Replace the layout or add the template elements on top?"
  override val templateReplaceOption = "Replace Layout"
  override val templateOverlayOption = "Add on Top"
  override val templateAppliedBanner = "Template applied"
  override val voiceRecordStart = "Record Voice Note"
  override val voiceRecordStop = "Stop & Attach"
  override val voiceRecordingStatus = "Recording voice memory…"
  override val voicePermissionNeeded = "Microphone permission lets you record offline voice notes right on your page."
  override val voiceGrantPermission = "Allow Microphone"
  override val voicePlayLabel = "Play"
  override val voiceStopPlayLabel = "Stop"
  override val voiceNoteLabel = "Voice Memory"

  override val appearanceAndLanguage = "Appearance & Language"
  override val settingsSubtitle = "Customize chrome theme, bilingual labels, typography scale, and reading comfort. Diary page designs remain untouched."
  override val uiLanguageTitle = "Interface Language"
  override val uiLanguageDescription = "Switches all navigation, buttons, dialogs, and labels instantly without altering your journal content."
  override val numeralStyleTitle = "Numeral Display"
  override val numeralLatinOption = "Latin (0–9)"
  override val numeralBengaliOption = "Bengali (০–৯)"
  override val themeTitle = "App Chrome Theme"
  override val themeLight = "Light"
  override val themeDark = "Dark"
  override val themeSystem = "System"
  override val themePaletteCaption = "Classic Dark walnut & antique gold is the signature default."
  override val typographyPreviewTitle = "Typography & Conjunct Preview"
  override val typographyBanglaHeadingLabel = "Bangla Heading · Noto Serif Bengali"
  override val typographyBanglaHeadingSample = "সংকলন-ৎ — জীবন চলার প্রতিটি ছাপের সাথে"
  override val typographyBanglaBodyLabel = "Bangla Body & Conjunct Test · Noto Sans Bengali"
  override val typographyBanglaBodySample = "সংকলন, স্মৃতি, শ্রদ্ধা, প্রিয়, স্বাধীনতা, দৃষ্টান্ত, কৃতজ্ঞতা, বইয়ের পাতা — Mixed English & বাংলা 2026."
  override val typographyEnglishHeadingLabel = "English Heading · Playfair Display"
  override val typographyEnglishHeadingSample = "Every Imprint of Your Life's Journey"
  override val typographyEnglishBodyLabel = "English Body · Inter"
  override val typographyEnglishBodySample = "Warm parchment pages, delicate botanical pressings, and calm evening reflections."
  override val readingPreferencesTitle = "Reading Preferences"
  override val readingLayoutLabel = "Default Opening Layout"
  override val readingLayoutSingle = "Single Page"
  override val readingLayoutSpread = "Two-Page Spread"
  override val pageTurnAnimationLabel = "Realistic Page Curl Animation"
  override val pageTurnSoundLabel = "Page-Turn Sound"
  override val liteModeLabel = "Lite Performance Mode"
  override val liteModeDescription = "Uses a crisp, lightweight page flip on older devices."
  override val accessibilityTitle = "Accessibility"
  override val reduceMotionLabel = "Reduce Motion"
  override val reduceMotionDescription = "Keeps the Welcome water logo static and simplifies transitions."
  override val textScaleLabel = "Interface Text Scale"
  override val highContrastLabel = "High Contrast Chrome"
  override val highContrastDescription = "Boosts text and border contrast across dark and light surfaces."
  override val privacyAndStorageTitle = "Privacy & Local Storage"
  override val privacyAndStorageBody = "All journals, voice clips, and PIN hashes stay strictly offline on your device. No account or hidden telemetry exists."
  override val appVersionLabel = "Sankalan-t · Version 1.0 (NRST.tech)"

  override val studioCatFlorals = "Florals (Pack 2)"
  override val studioCatEphemera = "Ephemera (Pack 3)"
  override val studioCatSeasonal = "Seasonal (Pack 3)"
  override val insightsTitle = "Mood Insights & Daily Prompt"
  override val moodTrackerTitle = "Mood Balance"
  override val moodTrackerEmpty = "Tag moods in Page Info to see your reflection balance here."
  override val dailyPromptTitle = "Today's Literary Prompt"
  override val dailyPromptNext = "Another Prompt"
  override val dailyPromptWriteNow = "Write Now"
  override val streakEntriesLabel = "Logged entries"
  override val globalSearchTitle = "Global Library Search"
  override val globalSearchHint = "Search all journals, titles, moods & entries…"
  override val globalSearchNoResults = "No matching journals or pages found."
  override val globalSearchDiariesHeader = "Journals"
  override val globalSearchPagesHeader = "Diary Pages"
  override val reminderSettingsTitle = "Daily Reminder & Calendar (.ics)"
  override val reminderEnableLabel = "Gentle Writing Reminder"
  override val reminderEnableDesc = "Shows a quiet daily prompt on Home and supports optional notification alerts."
  override val reminderTimeLabel = "Preferred Evening Hour"
  override val reminderExportIcsLabel = "Export Schedule (.ics)"
  override val reminderIcsExportedMsg = "Calendar schedule (.ics) exported."
  override val reminderBannerTitle = "A quiet moment awaits your journal tonight."
  override val reminderBannerAction = "Write a Page"
  override val reminderNotificationTest = "Send Test Alert"
  override val appLockTitle = "Privacy Lock (PIN & Biometric)"
  override val appLockEnableLabel = "Lock App on Launch"
  override val appLockEnableDesc = "Requires your PIN or Android Biometrics before opening journals."
  override val appLockSetPinLabel = "Set 4-Digit PIN"
  override val appLockChangePinLabel = "Change PIN"
  override val appLockEnterPinTitle = "Unlock Sankalan-ৎ"
  override val appLockPinHint = "Enter 4–6 digit PIN"
  override val appLockPinMismatch = "PINs do not match."
  override val appLockPinTooShort = "Use 4 to 6 digits."
  override val appLockUnlockButton = "Unlock"
  override val appLockBiometricButton = "Use Fingerprint / Biometric"
  override val appLockBiometricPromptTitle = "Unlock Sankalan-ৎ"
  override val appLockBiometricPromptSub = "Verify with your device fingerprint or face unlock"
  override val appLockWrongPin = "Incorrect PIN. Please try again."

  override val exportDiaryTitle = "Export & Print Diary"
  override val exportPdfSingle = "Single-Page PDF (Crisp Text)"
  override val exportPdfSpread = "Two-Page Book Spread PDF"
  override val exportPagePng = "First / Active Page as PNG"
  override val exportPageJpeg = "First / Active Page as JPEG"
  override val exportPagesZip = "All Pages as PNG Archive (.zip)"
  override val exportSuccessMsg = "Exported cleanly to your chosen folder."
  override val exportFailedMsg = "Could not complete export. Please try again."
  override val exportInProgress = "Preparing high-resolution pages…"
  override val backupAndUpdateTitle = "Offline Backup (.sankalan) & Updates"
  override val backupExportButton = "Export Full Backup (.sankalan)"
  override val backupExportDesc = "Bundles all journals, pages, photos, custom stickers, and voice notes into one portable file."
  override val backupRestoreButton = "Restore from Backup (.sankalan)"
  override val backupRestoreSuccess = "Backup restored cleanly to your shelf."
  override val backupRestoreFailed = "Invalid or unreadable .sankalan backup archive."
  override val updateCheckerTitle = "Free GitHub Releases Auto-Check"
  override val updateCheckNowButton = "Check for Updates"
  override val updateCheckingLabel = "Checking GitHub Releases…"
  override val updateRepoHint = "GitHub owner/repo or version.json URL"
  override val updateUpToDateLabel = "You are on the latest version."
  override val updateAvailableTitle = "New Version Available"
  override val updateDownloadButton = "Open Update Page"
  override val updateCheckFailedLabel = "Unable to reach release manifest. Check internet or repo URL."
  override val updateCurrentVersionBadge = "Installed"
  override val updateFriendsShareGuide = "Upload a newer APK + version.json on GitHub Releases so friends receive an automatic update alert whenever online."
  override val updateFixedLinkLabel = "Fixed Shareable Download Link"
  override val updateCopyLinkBtn = "Copy Link"
  override val updateShareLinkBtn = "Share App Link"
  override val updateLinkCopiedMsg = "Download link copied!"
  override val updateShareInviteBody = "Download Sankalan-t (সংকলন-ৎ) offline journal & sketchbook from this permanent link:"
  override val updateEnableNotifBtn = "Enable Auto Update Alerts"
  override val updateNotifActiveBadge = "Auto update alerts active when online"
  override val updateCopyManifestBtn = "Copy version.json Template"
  override val updateManifestCopiedMsg = "version.json template copied!"

  override val homeHeroTagline = "With the Memories of Your Life."
  override val editorInlineTextHint = "Tap paper & type directly here…"
  override val editorQuickOrbLabel = "Quick Tools"
  override val editorQuickOrbPen = "Pen / Stylus"
  override val editorQuickOrbPencil = "Pencil"
  override val editorQuickOrbHighlighter = "Highlighter"
  override val editorQuickOrbEraser = "Eraser"
  override val editorQuickOrbText = "Type Text"
  override val editorQuickOrbSticker = "Add Sticker"
  override val editorQuickAddSticker = "+ Sticker"
  override val editorQuickStickerFromPhoto = "Sticker from Photo"
  override val editorQuickOrbSelect = "Move & Spin"
  override val editorQuickOrbStylusToggle = "Stylus Mode"
  override val editorFontSizeLabel = "Size"
  override val editorBoldLabel = "Bold"
  override val editorItalicLabel = "Italic"
  override val editorUnderlineLabel = "Underline"
  override val editorAlignLeft = "Align Left"
  override val editorAlignCenter = "Align Center"
  override val editorAlignRight = "Align Right"
  override val editorRotateHandleHint = "Drag top gold knob or pinch with 2 fingers to rotate & scale"
  override val bookshelfEmptySlotCta = "Add a journal to this shelf"
  override val ownerSectionTitle = "Crafted & Maintained By"
  override val ownerCompanyLabel = "Company"
  override val ownerCompanyName = "NRST.tech"
  override val ownerFounderLabel = "Owner & Lead Developer"
  override val ownerFounderName = "Nafis Ahmed"
  override val ownerEmailLabel = "Contact Email"
  override val ownerEmailAddress = "nafisahmeddn@gmail.com"
  override val ownerCraftTagline = "Built offline-first with quiet craft, warmth, and respect for your private memories."

  override val authTitle = "Sign In or Create Account"
  override val authSubtitle = "Use any Gmail account to keep your profile and community feedback synced securely."
  override val authSignInWithGoogleButton = "Sign in with Google (Gmail)"
  override val authCreateAccountButton = "Create Account with Gmail"
  override val authCreateConfirmButton = "Create Account & Continue"
  override val authBackToExistingSignIn = "Back to Quick Google Sign-In"
  override val authDisplayNameInputLabel = "Your Display Name"
  override val authDisplayNameInputHint = "e.g., Nafis Ahmed"
  override val authGmailAutoAccountHint = "Any Gmail account automatically registers your Sankalan-t profile in Firebase."
  override val authFacebookManualNote = "Facebook Login requires adding your Meta App ID & Secret manually in Firebase Console."
  override val accountCardTitle = "Account & Gmail Profile"
  override val accountGmailLabel = "Signed-in Gmail"
  override val accountNameLabel = "Profile Name"
  override val accountSaveNameBtn = "Update Name"
  override val accountNameSavedMsg = "Profile name updated in cloud."
  override val accountNameErrorMsg = "Could not update profile name."
  override val accountSwitchBtn = "Switch / Create Gmail Account"
  override val accountSignOutBtn = "Sign Out"
  override val feedbackSectionTitle = "Community Feedback Bar"
  override val feedbackSectionSubtitle = "Share ideas or thoughts and scroll below to read what other readers shared."
  override val feedbackInputHint = "Write your feedback or suggestion…"
  override val feedbackSubmitBtn = "Post Feedback"
  override val feedbackSubmittingBtn = "Posting…"
  override val feedbackSuccessMsg = "Thank you! Your feedback is now visible below."
  override val feedbackErrorMsg = "Could not post feedback. Check connection."
  override val feedbackCatGeneral = "General"
  override val feedbackCatFeature = "Idea"
  override val feedbackCatBug = "Issue"
  override val feedbackCatLove = "Praise"
  override val feedbackCommentsTitle = "Recent Reader Comments"
  override val feedbackScrollHint = "Scroll to read more"
  override val feedbackEmptyLabel = "No comments yet — be the first to leave feedback above!"
  override val feedbackDeleteBtn = "Delete"
  override val feedbackYouBadge = "You"

  override val stickerEditorTitle = "Smart Sticker Studio"
  override val stickerEditorSubtitle = "Crop, trace an object roughly with your finger, and let smart cutout + border polish it."
  override val stickerTitleLabel = "Sticker Name"
  override val stickerTitleHint = "e.g., Pressed Rose Sticker"
  override val stickerTabCrop = "1. Crop & Turn"
  override val stickerTabCutout = "2. Object Cutout"
  override val stickerTabBorder = "3. Sticker Border"
  override val stickerAspectFree = "Free"
  override val stickerAspectSquare = "1:1 Square"
  override val stickerRotate90 = "Rotate 90°"
  override val stickerResetCrop = "Reset Crop"
  override val stickerModeAuto = "Auto Smart"
  override val stickerModeLasso = "Finger Lasso"
  override val stickerModeOriginal = "Full Crop"
  override val stickerBrushKeep = "+ Keep"
  override val stickerBrushErase = "− Erase"
  override val stickerUndoStroke = "Undo"
  override val stickerClearLasso = "Clear Outline"
  override val stickerSensitivityLabel = "Smart Edge Tightness"
  override val stickerLassoGuideHint = "Draw roughly around the object with your finger — smart edge-snap refines the contour automatically."
  override val stickerAutoGuideHint = "Automatically isolates the central object. Use + Keep / − Erase strokes to guide tricky corners."
  override val stickerBorderToggleLabel = "Die-Cut Sticker Border"
  override val stickerBorderThicknessLabel = "Border Thickness"
  override val stickerBorderColorLabel = "Border Color"
  override val stickerBorderGoldTrimLabel = "Antique Gold Hairline Trim"
  override val stickerBorderShadowLabel = "Soft Paper Shadow"
  override val stickerSaveBtn = "Save Sticker"
  override val stickerSavingBtn = "Refining…"
  override val stickerEditExistingBtn = "Edit Sticker"
}

object StringsBnBd : SankalanStrings {
  override val brandName = "সংকলন-ৎ"
  override val brandLogoWordmark = "Sankalan-ৎ"
  override val tagline = "আপনার জীবনের স্মৃতির সাথে।"
  override val welcomeSkip = "এড়িয়ে যান"
  override val welcomeTapHint = "প্রবেশ করতে পানির ওপর যেকোনো জায়গায় স্পর্শ করুন"
  override val welcomeLanguagePrompt = "অ্যাপের ভাষা বেছে নিন"
  override val languageBanglaLabel = "বাংলা (বাংলাদেশ)"
  override val languageEnglishLabel = "English"

  override val navHome = "নীড়"
  override val navBookshelf = "বইয়ের তাক"
  override val navCalendar = "ক্যালেন্ডার"
  override val navStudio = "স্টুডিও"
  override val navArchive = "আর্কাইভ"
  override val navSettings = "সেটিংস"

  override val myBookshelf = "আমার বইয়ের তাক"
  override val createJournal = "নতুন ডায়েরি তৈরি করুন"
  override val readingMode = "পড়ার মোড"
  override val editPage = "পৃষ্ঠা সম্পাদনা"
  override val addPage = "পৃষ্ঠা যোগ করুন"
  override val pageManager = "পৃষ্ঠা ব্যবস্থাপনা"
  override val save = "সংরক্ষণ"
  override val saving = "সংরক্ষণ হচ্ছে…"
  override val saved = "সংরক্ষিত হয়েছে"
  override val saveFailed = "সংরক্ষণ ব্যর্থ হয়েছে"
  override val retry = "আবার চেষ্টা করুন"
  override val exportPdf = "PDF হিসেবে রপ্তানি"
  override val share = "শেয়ার করুন"
  override val cancel = "বাতিল"
  override val confirm = "নিশ্চিত করুন"
  override val delete = "মুছে ফেলুন"
  override val deletePageConfirm = "পৃষ্ঠা মুছে ফেলবেন?"
  override val visibilityPrivate = "ব্যক্তিগত"
  override val visibilityUnlisted = "লিংক থাকলে দেখা যাবে"
  override val visibilityPublic = "সবার জন্য উন্মুক্ত"

  override val replayWelcome = "ওয়াটার ইন্ট্রো আবার দেখুন"
  override val homeSubtitle = "আপনার স্মৃতি, আঁকাআঁকি ও কণ্ঠস্বরের জন্য ওয়ালনাট ও সোনালি ভিন্টেজ লাইব্রেরি।"
  override val localFirstBadge = "১০০% নিজস্ব ডিভাইসে · ডিফল্টভাবে ব্যক্তিগত"
  override val memoryLaneTitle = "মেমোরি লেন"
  override val memoryLaneSubtitle = "পিন করা পাতা, অতীতের আজকের দিন এবং প্রিয় স্মৃতির ভ্রমণ"
  override val memoryLaneEmptyHint = "কোনো ডায়েরিতে লেখা বা পিন করা পাতা যুক্ত হলে এখানে মেমোরি লেনে ভেসে উঠবে।"
  override val bookshelfEmptyTitle = "এই মুহূর্তে কোনো ডায়েরি পাওয়া যায়নি"
  override val bookshelfEmptyBody = "Luxury, Artistic বা Minimal মলাটে ৫–২০০ পাতার নতুন ডায়েরি বাঁধাই করতে নিচের বাটনে ট্যাপ করুন।"
  override val coverStylesPreviewTitle = "তিনটি সিগনেচার কভার স্টাইল"
  override val coverLuxuryLabel = "লাক্সারি"
  override val coverArtisticLabel = "আর্টিস্টিক"
  override val coverMinimalLabel = "মিনিমাল"
  override val studioPreviewTitle = "টেমপ্লেট ও অ্যাসেট স্টুডিও"
  override val studioPreviewBody = "২২ ধরনের কাগজ, ১২টি এডিটেবল টেমপ্লেট, অ্যাসেট প্যাক ১–৩ (ফুল, ইফেমেরা, ঋতুভিত্তিক স্টিকার), আপলোড ও প্রিয় তালিকা।"
  override val archiveEmptyTitle = "আর্কাইভ তাক ফাঁকা রয়েছে"
  override val archiveEmptyBody = "আর্কাইভ করা ডায়েরি নিরাপদে ডিভাইসে সংরক্ষিত থাকে এবং মেমোরি লেন থেকে স্বয়ংক্রিয়ভাবে বাদ থাকে।"
  override val phaseMilestoneBadge = "ফেজ ১ সম্পূর্ণ · ধাপ ১–১০ (সম্পূর্ণ অফলাইন লাইব্রেরি, অ্যাসেট প্যাক ১–৩, মুড ট্র্যাকার, লক, নেটিভ PDF রপ্তানি, ব্যাকআপ ও আপডেট)"

  override val shelfViewLabel = "3D তাক"
  override val gridViewLabel = "মলাট ভিউ"
  override val searchBooksHint = "ডায়েরি খুঁজুন (বাংলা / English)…"
  override val filterAll = "সবগুলো"
  override val sortRecent = "সাম্প্রতিক"
  override val sortTitle = "শিরোনাম"
  override val sortPages = "পৃষ্ঠা"
  override val pagesUnit = "পৃষ্ঠা"
  override val openDiary = "ডায়েরি খুলুন"
  override val renameDiary = "নাম বদলান"
  override val duplicateDiary = "অনুলিপি করুন"
  override val archiveDiary = "আর্কাইভ করুন"
  override val unarchiveDiary = "তাকে ফিরিয়ে আনুন"
  override val deleteDiary = "মুছে ফেলুন"
  override val deleteDiaryConfirmTitle = "ডায়েরি মুছে ফেলবেন?"
  override val deleteDiaryConfirmBody = "এতে ডায়েরিটি এবং এর সব পৃষ্ঠা এই ডিভাইস থেকে স্থায়ীভাবে মুছে যাবে।"
  override val renameDiaryTitle = "ডায়েরির নাম পরিবর্তন"
  override val diaryTitleHint = "ডায়েরির শিরোনাম (বাংলা বা ইংরেজি)"
  override val diaryDescHint = "সংক্ষিপ্ত বিবরণ বা সাবটাইটেল (ঐচ্ছিক)"
  override val coverStyleLabel = "মলাটের ধরন"
  override val coverColorLabel = "বাঁধাইয়ের উপাদান ও রঙ"
  override val spineStyleLabel = "স্পাইনের কারুকাজ"
  override val spineRibbedGold = "সোনালি রিব"
  override val spineSmoothLeather = "মসৃণ লেদার"
  override val spineStitchedCloth = "সেলাই করা কাপড়"
  override val initialPagesLabel = "শুরুর পৃষ্ঠা সংখ্যা"
  override val initialPagesRuleHint = "প্রতিটি ডায়েরিতে কমপক্ষে ৫ ও সর্বোচ্চ ২০০ পৃষ্ঠা থাকে।"
  override val createButtonLabel = "ডায়েরি বাঁধাই করুন"
  override val frontCoverTag = "সামনের মলাট"
  override val backCoverTag = "পিছনের মলাট"
  override val demoBadge = "ডেমো"
  override val closeLabel = "বন্ধ করুন"
  override val previewPagesTitle = "ডায়েরির ভেতর"
  override val nextPageLabel = "পরের পাতা"
  override val prevPageLabel = "আগের পাতা"
  override val coverLabel = "মলাট"
  override val pageNumberPrefix = "পৃষ্ঠা"
  override val editorComingNextHint = "২২ ধরনের কাগজ, ১২টি টেমপ্লেট, ভয়েস নোট ও প্রথম অ্যাসেট প্যাক এখন এডিটরে যুক্ত হয়েছে।"

  override val calendarTitle = "ডায়েরি ক্যালেন্ডার"
  override val calendarSubtitle = "তারিখ অনুযায়ী লেখা পৃষ্ঠাগুলো দেখুন এবং সরাসরি নতুন এন্ট্রি যোগ করুন।"
  override val calendarNoEntriesForDay = "এই তারিখে এখনো কোনো পৃষ্ঠা নেই।"
  override val calendarCreateDatedPage = "তারিখযুক্ত পাতা যোগ করুন"
  override val calendarSelectDiary = "ডায়েরি বেছে নিন"
  override val calendarPinnedBadge = "পিন করা"

  override val memoryLanePlay = "চালু করুন"
  override val memoryLanePause = "থামান"
  override val memoryLanePrev = "আগের স্মৃতি"
  override val memoryLaneNext = "পরের স্মৃতি"
  override val memoryLaneReadMemory = "স্মৃতিটি পড়ুন"
  override val memoryLanePinnedLabel = "পিন করা স্মৃতি"
  override val memoryLaneOnThisDayLabel = "অতীতের আজকের দিন"
  override val memoryLaneReflectionLabel = "নির্বাচিত পাতা"
  override val memoryLaneNoMemories = "মেমোরি লেন আলোকিত করতে যেকোনো সক্রিয় ডায়েরিতে পাতা লিখুন বা পিন করুন।"

  override val activePagesTab = "সক্রিয় পাতা"
  override val recentlyDeletedTab = "রিসেন্টলি ডিলিটেড"
  override val pinPageLabel = "মেমোরি লেনে পিন করুন"
  override val unpinPageLabel = "পিন সরান"
  override val duplicatePageLabel = "পাতার অনুলিপি"
  override val resetPageLabel = "পাতা রিসেট করুন"
  override val resetPageConfirmBody = "এই পাতার লেখা, তারিখ ও উপাদান মুছে পরিষ্কার করবেন? পাতার কাগজ ও অবস্থান ঠিক থাকবে।"
  override val moveEarlierLabel = "আগে নিন"
  override val moveLaterLabel = "পরে নিন"
  override val restorePageLabel = "ফিরিয়ে আনুন"
  override val deleteForeverLabel = "স্থায়ীভাবে মুছুন"
  override val minPagesReachedNotice = "প্রতিটি ডায়েরিতে কমপক্ষে ৫টি সক্রিয় পৃষ্ঠা থাকা বাধ্যতামূলক।"
  override val maxPagesReachedNotice = "সর্বোচ্চ ২০০ পৃষ্ঠার সীমা পূর্ণ হয়েছে।"
  override val recentlyDeletedEmpty = "এই ডায়েরিতে সম্প্রতি মুছে ফেলা কোনো পাতা নেই।"
  override val blankPageLabel = "ফাঁকা পাতা"

  override val singlePageLayout = "একক পাতা"
  override val twoPageSpreadLayout = "দুই পাতার স্প্রেড"
  override val toggleFullscreenLabel = "ফুলস্ক্রিন"
  override val resetZoomLabel = "জুম রিসেট"
  override val tapToToggleHudHint = "আঙুলে টেনে পাতা ওল্টান · পিঞ্চ করে জুম · নিয়ন্ত্রণ লুকাতে ট্যাপ করুন"
  override val spreadLabel = "স্প্রেড"
  override val editLocationPreservedHint = "ক্যানভাস এডিটরের জন্য বর্তমান পাতার অবস্থান সংরক্ষিত হয়েছে।"

  override val editorUndo = "আনডু"
  override val editorRedo = "রিডু"
  override val editorDone = "সম্পন্ন"
  override val editorToolSelect = "সিলেক্ট"
  override val editorToolText = "লেখা"
  override val editorToolDraw = "আঁকা"
  override val editorToolPhoto = "ছবি"
  override val editorToolShapes = "আকৃতি"
  override val editorToolLayers = "লেয়ার"
  override val editorToolPageInfo = "পাতার তথ্য"
  override val editorAddTextBox = "নতুন লেখা যোগ"
  override val editorEditTextTitle = "লেখা ও ফন্ট সাজান"
  override val editorTextPlaceholder = "বাংলা বা ইংরেজিতে আপনার মনের কথা লিখুন…"
  override val editorFontLabel = "বাংলা ও ইংরেজি হস্তাক্ষর ফন্ট"
  override val editorPenInk = "কলম (Pen)"
  override val editorPenPencil = "পেন্সিল"
  override val editorPenMarker = "মার্কার"
  override val editorPenHighlighter = "হাইলাইটার"
  override val editorEraser = "ইরেজার"
  override val editorStylusOnly = "পাম রিজেকশন (শুধু স্টাইলাস)"
  override val editorStylusOnlyHint = "আঁকার সময় হাতের তালু বা আঙুলের ছোঁয়া উপেক্ষা করবে"
  override val editorStrokeSize = "দাগের পুরুত্ব"
  override val editorPickPhoto = "গ্যালারি থেকে ছবি বাছুন"
  override val editorCropAndFrame = "ছবি ক্রপ ও ফ্রেম করুন"
  override val editorFrameClean = "সাধারণ কাট"
  override val editorFramePolaroid = "পোলারয়েড"
  override val editorFrameTornPaper = "ছেঁড়া কাগজ"
  override val editorFrameRounded = "নরম কোণা"
  override val editorFrameOval = "ডিম্বাকৃতি ফ্রেম"
  override val editorCropZoom = "ক্রপ ফোকাস"
  override val editorLayersEmpty = "এই পাতায় এখনো কোনো উপাদান নেই। লেখা, আঁকা, ছবি বা আকৃতি যোগ করুন।"
  override val editorLayerText = "লেখা"
  override val editorLayerStroke = "কলমের দাগ"
  override val editorLayerPhoto = "ছবি"
  override val editorLayerShape = "আকৃতি"
  override val editorBringForward = "উপরে আনুন"
  override val editorSendBackward = "নিচে নিন"
  override val editorLockLayer = "লক"
  override val editorHideLayer = "দৃশ্যমান"
  override val editorGroupSelected = "গ্রুপ করুন"
  override val editorUngroup = "আনগ্রুপ"
  override val editorOpacity = "স্বচ্ছতা (Opacity)"
  override val editorRotate = "ঘোরান"
  override val editorMoodLabel = "অনুভূতি (Mood)"
  override val editorDateLabel = "তারিখ (YYYY-MM-DD)"
  override val editorPageTitleHint = "পাতার শিরোনাম"

  override val editorToolStudioAssets = "স্টিকার ও টেপ"
  override val editorToolPaper = "কাগজ"
  override val editorToolTemplates = "টেমপ্লেট"
  override val editorToolVoice = "ভয়েস নোট"
  override val studioSearchHint = "কাগজ, টেমপ্লেট, টেপ বা স্ট্যাম্প খুঁজুন…"
  override val studioCatAll = "সবগুলো"
  override val studioCatPapers = "কাগজ (২২)"
  override val studioCatTemplates = "টেমপ্লেট (১২)"
  override val studioCatTape = "ওয়াশি টেপ"
  override val studioCatFrames = "ফ্রেম"
  override val studioCatLabels = "লেবেল"
  override val studioCatStamps = "স্ট্যাম্প"
  override val studioCatUploads = "আমার আপলোড"
  override val studioCatFavorites = "প্রিয় তালিকা"
  override val studioUploadAssetButton = "নিজস্ব ছবি/স্টিকার আপলোড"
  override val studioFavoritesEmpty = "যেকোনো কাগজ, টেমপ্লেট বা স্টিকারে তারকা চিহ্নে ট্যাপ করলে এখানে জমা হবে।"
  override val studioUploadsEmpty = "Photo Picker দিয়ে নিজের পছন্দের স্টিকার বা ছবি আপলোড করুন।"
  override val studioUseInDiary = "ডায়েরিতে বসান"
  override val studioPickTargetPage = "ডায়েরি ও পাতা নির্বাচন করুন"
  override val paperEdgeStyleLabel = "কাগজের কিনারা"
  override val paperEdgeClean = "সাধারণ কাট"
  override val paperEdgeDeckle = "ডেকল এজ"
  override val paperEdgeTorn = "ছেঁড়া কাগজ"
  override val templateSafeTitle = "পাতায় আগে থেকেই কাজ রয়েছে"
  override val templateSafeBody = "এই পাতায় আগে থেকে লেখা বা উপাদান আছে। বর্তমান লেআউট বদলাবেন নাকি এর উপরে যোগ করবেন?"
  override val templateReplaceOption = "নতুন লেআউটে বদলান"
  override val templateOverlayOption = "উপরে যোগ করুন"
  override val templateAppliedBanner = "টেমপ্লেট বসানো হয়েছে"
  override val voiceRecordStart = "ভয়েস রেকর্ড করুন"
  override val voiceRecordStop = "রেকর্ড থামিয়ে যোগ করুন"
  override val voiceRecordingStatus = "কণ্ঠস্বর রেকর্ড হচ্ছে…"
  override val voicePermissionNeeded = "পাতার ভেতর অফলাইন ভয়েস মেমোরি যোগ করতে মাইক্রোফোন অনুমতি প্রয়োজন।"
  override val voiceGrantPermission = "মাইক্রোফোন অনুমতি দিন"
  override val voicePlayLabel = "শুনুন"
  override val voiceStopPlayLabel = "থামান"
  override val voiceNoteLabel = "ভয়েস মেমোরি"

  override val appearanceAndLanguage = "চেহারা ও ভাষা"
  override val settingsSubtitle = "অ্যাপের থিম, ভাষা, টাইপোগ্রাফি এবং পড়ার অভিজ্ঞতা সাজিয়ে নিন। এতে ডায়েরির মূল পাতার ডিজাইন বদলাবে না।"
  override val uiLanguageTitle = "অ্যাপের ভাষা"
  override val uiLanguageDescription = "নেভিগেশন, বাটন, ডায়ালগ ও সমস্ত লেবেল একসাথে বদলাবে; ডায়েরির ভেতরের লেখা অপরিবর্তিত থাকবে।"
  override val numeralStyleTitle = "সংখ্যার ধরন"
  override val numeralLatinOption = "ইংরেজি অঙ্ক (0–9)"
  override val numeralBengaliOption = "বাংলা অঙ্ক (০–৯)"
  override val themeTitle = "অ্যাপের থিম"
  override val themeLight = "লাইট মোড"
  override val themeDark = "ডার্ক মোড"
  override val themeSystem = "ডিভাইসের সেটিং অনুসরণ"
  override val themePaletteCaption = "ভিন্টেজ ডার্ক ওয়ালনাট ও অ্যান্টিক গোল্ড হলো মূল ডিফল্ট থিম।"
  override val typographyPreviewTitle = "বাংলা ও ইংরেজি ফন্ট প্রিভিউ"
  override val typographyBanglaHeadingLabel = "বাংলা শিরোনাম · Noto Serif Bengali"
  override val typographyBanglaHeadingSample = "সংকলন-ৎ — আপনার জীবনের স্মৃতির সাথে"
  override val typographyBanglaBodyLabel = "বাংলা অনুচ্ছেদ ও যুক্তাক্ষর পরীক্ষা · Noto Sans Bengali"
  override val typographyBanglaBodySample = "সংকলন, স্মৃতি, শ্রদ্ধা, প্রিয়, স্বাধীনতা, দৃষ্টান্ত, কৃতজ্ঞতা, বইয়ের পাতা — Mixed English & বাংলা 2026।"
  override val typographyEnglishHeadingLabel = "ইংরেজি শিরোনাম · Playfair Display"
  override val typographyEnglishHeadingSample = "Every Imprint of Your Life's Journey"
  override val typographyEnglishBodyLabel = "ইংরেজি অনুচ্ছেদ · Inter"
  override val typographyEnglishBodySample = "Warm parchment pages, delicate botanical pressings, and calm evening reflections."
  override val readingPreferencesTitle = "পড়ার পছন্দসমূহ"
  override val readingLayoutLabel = "ডিফল্ট পড়ার লেআউট"
  override val readingLayoutSingle = "একক পৃষ্ঠা"
  override val readingLayoutSpread = "দুই পৃষ্ঠার স্প্রেড"
  override val pageTurnAnimationLabel = "বাস্তবসম্মত পেজ-কার্ল অ্যানিমেশন"
  override val pageTurnSoundLabel = "পৃষ্ঠা উল্টানোর শব্দ"
  override val liteModeLabel = "লাইট পারফরম্যান্স মোড"
  override val liteModeDescription = "পুরোনো ডিভাইসে হালকা ও দ্রুত পেজ-ফ্লিপ ব্যবহার করে।"
  override val accessibilityTitle = "অ্যাক্সেসিবিলিটি"
  override val reduceMotionLabel = "গতি কমান (Reduce Motion)"
  override val reduceMotionDescription = "ওয়েলকাম স্ক্রিনের ওয়াটার লোগো স্থির রাখে এবং অ্যানিমেশন সহজ করে।"
  override val textScaleLabel = "UI লেখার আকার"
  override val highContrastLabel = "উচ্চ কনট্রাস্ট মোড"
  override val highContrastDescription = "ডার্ক ও লাইট উভয় মোডে লেখা ও বর্ডারের স্পষ্টতা বাড়ায়।"
  override val privacyAndStorageTitle = "গোপনীয়তা ও লোকাল স্টোরেজ"
  override val privacyAndStorageBody = "আপনার সমস্ত ডায়েরি, ভয়েস নোট ও পিন কোড সম্পূর্ণ অফলাইনে নিজস্ব ডিভাইসে সুরক্ষিত থাকে।"
  override val appVersionLabel = "সংকলন-ৎ · সংস্করণ ১.০ (NRST.tech)"

  override val studioCatFlorals = "ফুল ও পাতা (প্যাক ২)"
  override val studioCatEphemera = "ইফেমেরা ও কাগজ (প্যাক ৩)"
  override val studioCatSeasonal = "বাংলা ও ঋতু (প্যাক ৩)"
  override val insightsTitle = "মনের অনুভূতি ও আজকের প্রম্পট"
  override val moodTrackerTitle = "মুড পরিসংখ্যান"
  override val moodTrackerEmpty = "পাতার তথ্য থেকে 'অনুভূতি (Mood)' যোগ করলে এখানে সারাংশ দেখাবে।"
  override val dailyPromptTitle = "আজকের লেখার অনুপ্রেরণা"
  override val dailyPromptNext = "আরেকটি উক্তি"
  override val dailyPromptWriteNow = "এখনই লিখুন"
  override val streakEntriesLabel = "মোট লেখা পাতা"
  override val globalSearchTitle = "পুরো লাইব্রেরিতে খুঁজুন"
  override val globalSearchHint = "সব ডায়েরি, শিরোনাম, অনুভূতি বা লেখা খুঁজুন…"
  override val globalSearchNoResults = "এই শব্দে কোনো ডায়েরি বা পৃষ্ঠা পাওয়া যায়নি।"
  override val globalSearchDiariesHeader = "ডায়েরিসমূহ"
  override val globalSearchPagesHeader = "ডায়েরির পাতা"
  override val reminderSettingsTitle = "লেখার রিমাইন্ডার ও ক্যালেন্ডার (.ics)"
  override val reminderEnableLabel = "দৈনিক লেখার মৃদু রিমাইন্ডার"
  override val reminderEnableDesc = "নীড় পাতায় লেখার মনে করিয়ে দেবে এবং চাইলে নোটিফিকেশন দেখাবে।"
  override val reminderTimeLabel = "পছন্দের সময় (রাত/বিকেল)"
  override val reminderExportIcsLabel = "ক্যালেন্ডারে রপ্তানি (.ics)"
  override val reminderIcsExportedMsg = "ক্যালেন্ডার শিডিউল (.ics) সংরক্ষিত হয়েছে।"
  override val reminderBannerTitle = "আজকের মনের কথা ডায়েরিতে লিখে রাখার শান্ত সময়।"
  override val reminderBannerAction = "পাতা লিখুন"
  override val reminderNotificationTest = "টেস্ট নোটিফিকেশন পাঠান"
  override val appLockTitle = "অ্যাপ লক ও গোপনীয়তা (PIN ও ফিঙ্গারপ্রিন্ট)"
  override val appLockEnableLabel = "অ্যাপ চালুর সময় লক করুন"
  override val appLockEnableDesc = "ডায়েরি খোলার আগে আপনার পিন অথবা ডিভাইসের বায়োমেট্রিক চাইবে।"
  override val appLockSetPinLabel = "৪ ডিজিটের পিন সেট করুন"
  override val appLockChangePinLabel = "পিন পরিবর্তন করুন"
  override val appLockEnterPinTitle = "সংকলন-ৎ আনলক করুন"
  override val appLockPinHint = "৪–৬ সংখ্যার পিন দিন"
  override val appLockPinMismatch = "দুইবারের পিন মিলছে না।"
  override val appLockPinTooShort = "কমপক্ষে ৪ থেকে ৬টি সংখ্যা দিন।"
  override val appLockUnlockButton = "খুলুন"
  override val appLockBiometricButton = "ফিঙ্গারপ্রিন্ট / বায়োমেট্রিক ব্যবহার করুন"
  override val appLockBiometricPromptTitle = "সংকলন-ৎ আনলক করুন"
  override val appLockBiometricPromptSub = "ডায়েরি খুলতে ফিঙ্গারপ্রিন্ট বা ফেস আনলক দিন"
  override val appLockWrongPin = "পিন ভুল হয়েছে, আবার চেষ্টা করুন।"

  override val exportDiaryTitle = "PDF ও ছবি হিসেবে রপ্তানি"
  override val exportPdfSingle = "একক পৃষ্ঠা PDF (স্পষ্ট বাংলা ও ইংরেজি)"
  override val exportPdfSpread = "দুই পৃষ্ঠার বই স্প্রেড PDF"
  override val exportPagePng = "বর্তমান পাতা PNG ছবি"
  override val exportPageJpeg = "বর্তমান পাতা JPEG ছবি"
  override val exportPagesZip = "সবগুলো পাতা PNG জিপ (.zip)"
  override val exportSuccessMsg = "নির্বাচিত ফোল্ডারে সফলভাবে সংরক্ষিত হয়েছে।"
  override val exportFailedMsg = "রপ্তানি সম্পন্ন করা যায়নি। আবার চেষ্টা করুন।"
  override val exportInProgress = "উচ্চ-মানের পৃষ্ঠা তৈরি হচ্ছে…"
  override val backupAndUpdateTitle = "সম্পূর্ণ ব্যাকআপ (.sankalan) ও আপডেট"
  override val backupExportButton = "সম্পূর্ণ ব্যাকআপ নিন (.sankalan)"
  override val backupExportDesc = "সব ডায়েরি, পাতা, ছবি, স্টিকার ও ভয়েস নোট একসাথে একটি পোর্টেবল ফাইলে সংরক্ষণ করে।"
  override val backupRestoreButton = "ব্যাকআপ থেকে ফিরিয়ে আনুন (.sankalan)"
  override val backupRestoreSuccess = "ব্যাকআপ থেকে সব ডায়েরি বইয়ের তাকে ফিরে এসেছে।"
  override val backupRestoreFailed = ".sankalan ব্যাকআপ ফাইলটি পড়া যায়নি।"
  override val updateCheckerTitle = "ফ্রি GitHub Releases আপডেট চেকার"
  override val updateCheckNowButton = "নতুন আপডেট দেখুন"
  override val updateCheckingLabel = "নতুন সংস্করণ খোঁজা হচ্ছে…"
  override val updateRepoHint = "GitHub owner/repo অথবা version.json লিংক"
  override val updateUpToDateLabel = "আপনি সর্বশেষ সংস্করণ ব্যবহার করছেন।"
  override val updateAvailableTitle = "নতুন ভার্সন পাওয়া গেছে!"
  override val updateDownloadButton = "আপডেট পেজ খুলুন"
  override val updateCheckFailedLabel = "আপডেট তথ্য পাওয়া যায়নি। ইন্টারনেট বা রিপোজিটরি লিংক দেখুন।"
  override val updateCurrentVersionBadge = "বর্তমান ভার্সন"
  override val updateFriendsShareGuide = "GitHub Releases-এ নতুন APK ও version.json আপলোড করলেই বন্ধুরা ইন্টারনেটে যুক্ত অবস্থায় অটোমেটিক আপডেট নোটিফিকেশন পাবেন।"
  override val updateFixedLinkLabel = "বন্ধুদের দেওয়ার স্থায়ী (ফিক্সড) লিংক"
  override val updateCopyLinkBtn = "লিংক কপি করুন"
  override val updateShareLinkBtn = "অ্যাপ লিংক শেয়ার করুন"
  override val updateLinkCopiedMsg = "ডাউনলোড লিংক কপি হয়েছে!"
  override val updateShareInviteBody = "সংকলন-ৎ (Sankalan-t) অফলাইন বাংলা ডায়েরি অ্যাপটি এই স্থায়ী লিংক থেকে ডাউনলোড করুন:"
  override val updateEnableNotifBtn = "অটো-আপডেট নোটিফিকেশন চালু করুন"
  override val updateNotifActiveBadge = "ইন্টারনেট থাকলে অটো-আপডেট নোটিফিকেশন চালু আছে"
  override val updateCopyManifestBtn = "version.json নমুনা কপি করুন"
  override val updateManifestCopiedMsg = "version.json নমুনা কপি হয়েছে!"

  override val homeHeroTagline = "আপনার জীবনের স্মৃতির সাথে"
  override val editorInlineTextHint = "কাগজের উপরেই সরাসরি লিখুন…"
  override val editorQuickOrbLabel = "কুইক টুলস"
  override val editorQuickOrbPen = "কলম / স্টাইলাস"
  override val editorQuickOrbPencil = "পেন্সিল"
  override val editorQuickOrbHighlighter = "হাইলাইটার"
  override val editorQuickOrbEraser = "মুছনী (ইরেজার)"
  override val editorQuickOrbText = "কাগজে টাইপ"
  override val editorQuickOrbSticker = "স্টিকার যোগ"
  override val editorQuickAddSticker = "+ স্টিকার"
  override val editorQuickStickerFromPhoto = "ছবি থেকে স্টিকার"
  override val editorQuickOrbSelect = "সরান ও ঘোরান"
  override val editorQuickOrbStylusToggle = "স্টাইলাস পাম-লক"
  override val editorFontSizeLabel = "আকার"
  override val editorBoldLabel = "বোল্ড"
  override val editorItalicLabel = "ইটালিক"
  override val editorUnderlineLabel = "আন্ডারলাইন"
  override val editorAlignLeft = "বাম সারিবদ্ধ"
  override val editorAlignCenter = "মাঝ বরাবর"
  override val editorAlignRight = "ডান সারিবদ্ধ"
  override val editorRotateHandleHint = "উপরের সোনালি বিন্দু ঘোরান অথবা দুই আঙুলে ঘুরিয়ে ছোট-বড় করুন"
  override val bookshelfEmptySlotCta = "এই তাকে নতুন ডায়েরি রাখুন"
  override val ownerSectionTitle = "নির্মাতা ও মালিকানা পরিচিতি"
  override val ownerCompanyLabel = "প্রতিষ্ঠান"
  override val ownerCompanyName = "NRST.tech"
  override val ownerFounderLabel = "স্বত্বাধিকারী ও নির্মাতা"
  override val ownerFounderName = "Nafis Ahmed (নাফিস আহমেদ)"
  override val ownerEmailLabel = "যোগাযোগ ইমেইল"
  override val ownerEmailAddress = "nafisahmeddn@gmail.com"
  override val ownerCraftTagline = "আপনার ব্যক্তিগত স্মৃতি ও অনুভূতির সম্পূর্ণ গোপনীয়তা নিশ্চিত করে অফলাইন-ফার্স্ট ভালোবাসায় নির্মিত।"

  override val authTitle = "জিমেইল লগইন ও অ্যাকাউন্ট তৈরি"
  override val authSubtitle = "অ্যাপে প্রবেশ করতে এবং আপনার প্রোফাইল ও ফিডব্যাক সংরক্ষণ করতে যেকোনো Gmail দিয়ে যুক্ত হোন।"
  override val authSignInWithGoogleButton = "Google (Gmail) দিয়ে লগইন করুন"
  override val authCreateAccountButton = "নতুন অ্যাকাউন্ট তৈরি করুন (Gmail)"
  override val authCreateConfirmButton = "অ্যাকাউন্ট তৈরি করে প্রবেশ করুন"
  override val authBackToExistingSignIn = "সরাসরি জিমেইল লগইনে ফিরুন"
  override val authDisplayNameInputLabel = "আপনার প্রোফাইল নাম"
  override val authDisplayNameInputHint = "যেমন: নাফিস আহমেদ"
  override val authGmailAutoAccountHint = "যেকোনো Gmail দিয়ে প্রথমবার ঢুকলেই অটোমেটিক নতুন অ্যাকাউন্ট ও প্রোফাইল তৈরি হয়ে যাবে।"
  override val authFacebookManualNote = "নোট: Facebook লগইন চালু করতে চাইলে পরবর্তীতে Firebase Console-এ Meta App ID ও Secret যোগ করতে হবে।"
  override val accountCardTitle = "অ্যাকাউন্ট ও জিমেইল প্রোফাইল"
  override val accountGmailLabel = "লগইন করা জিমেইল"
  override val accountNameLabel = "প্রোফাইল নাম"
  override val accountSaveNameBtn = "নাম সংরক্ষণ"
  override val accountNameSavedMsg = "প্রোফাইল নাম সফলভাবে আপডেট হয়েছে।"
  override val accountNameErrorMsg = "প্রোফাইল নাম আপডেট করা যায়নি।"
  override val accountSwitchBtn = "অন্য Gmail / নতুন অ্যাকাউন্ট খুলুন"
  override val accountSignOutBtn = "লগআউট"
  override val feedbackSectionTitle = "ফিডব্যাক বার ও পাঠকদের মতামত"
  override val feedbackSectionSubtitle = "আপনার মতামত লিখুন এবং নিচে স্ক্রল করে অন্যান্য ব্যবহারকারীদের কমেন্ট দেখুন।"
  override val feedbackInputHint = "আপনার ফিডব্যাক বা পরামর্শ এখানে লিখুন…"
  override val feedbackSubmitBtn = "পাঠান"
  override val feedbackSubmittingBtn = "পাঠানো হচ্ছে…"
  override val feedbackSuccessMsg = "ধন্যবাদ! আপনার ফিডব্যাক নিচে যুক্ত হয়েছে।"
  override val feedbackErrorMsg = "ফিডব্যাক পাঠানো যায়নি। ইন্টারনেট সংযোগ দেখুন।"
  override val feedbackCatGeneral = "সাধারণ"
  override val feedbackCatFeature = "নতুন ফিচার"
  override val feedbackCatBug = "সমস্যা"
  override val feedbackCatLove = "ভালোবাসা"
  override val feedbackCommentsTitle = "ব্যবহারকারীদের কমেন্টসমূহ"
  override val feedbackScrollHint = "স্ক্রল করে আরও দেখুন"
  override val feedbackEmptyLabel = "এখনো কোনো কমেন্ট নেই — উপরের বারে প্রথম ফিডব্যাক দিন!"
  override val feedbackDeleteBtn = "মুছুন"
  override val feedbackYouBadge = "আপনি"

  override val stickerEditorTitle = "স্মার্ট স্টিকার স্টুডিও"
  override val stickerEditorSubtitle = "ছবি ক্রপ করুন, হাত দিয়ে মোটামুটি অবজেক্ট ঘিরে দেখান — সফটওয়্যার সুন্দর মতো কেটে স্টিকার বর্ডার যোগ করবে।"
  override val stickerTitleLabel = "স্টিকারের নাম"
  override val stickerTitleHint = "যেমন: গোলাপ ফুলের স্টিকার"
  override val stickerTabCrop = "১. ক্রপ ও ঘোরান"
  override val stickerTabCutout = "২. অবজেক্ট কাটআউট"
  override val stickerTabBorder = "৩. স্টিকার বর্ডার"
  override val stickerAspectFree = "মুক্ত ক্রপ"
  override val stickerAspectSquare = "১:১ বর্গ"
  override val stickerRotate90 = "৯০° ঘোরান"
  override val stickerResetCrop = "রিসেট ক্রপ"
  override val stickerModeAuto = "অটো স্মার্ট"
  override val stickerModeLasso = "হাত দিয়ে ঘিরুন"
  override val stickerModeOriginal = "সম্পূর্ণ ছবি"
  override val stickerBrushKeep = "+ রাখুন"
  override val stickerBrushErase = "− মুছুন"
  override val stickerUndoStroke = "আনডু"
  override val stickerClearLasso = "দাগ মুছুন"
  override val stickerSensitivityLabel = "স্মার্ট এজ সংবেদনশীলতা"
  override val stickerLassoGuideHint = "আঙুল দিয়ে অবজেক্টের চারপাশে মোটামুটি ঘিরে দিন — ভিতরের স্মার্ট সফটওয়্যার আসল অবজেক্ট বরাবর নিখুঁতভাবে কেটে নেবে।"
  override val stickerAutoGuideHint = "মাঝের মূল অবজেক্টটি স্বয়ংক্রিয়ভাবে আলাদা করা হচ্ছে। প্রয়োজনে + রাখুন বা − মুছুন দিয়ে নির্দেশনা দিন।"
  override val stickerBorderToggleLabel = "স্টিকার ডাই-কাট বর্ডার"
  override val stickerBorderThicknessLabel = "বর্ডারের পুরুত্ব"
  override val stickerBorderColorLabel = "বর্ডারের রঙ"
  override val stickerBorderGoldTrimLabel = "সোনালি হেয়ারলাইন বর্ডার"
  override val stickerBorderShadowLabel = "হালকা কাগজের ছায়া"
  override val stickerSaveBtn = "স্টিকার সেভ করুন"
  override val stickerSavingBtn = "প্রস্তুত হচ্ছে…"
  override val stickerEditExistingBtn = "এডিট করুন"
}

val LocalStrings = staticCompositionLocalOf<SankalanStrings> { StringsBnBd }

fun stringsFor(language: AppLanguage): SankalanStrings =
  when (language) {
    AppLanguage.BN_BD -> StringsBnBd
    AppLanguage.EN -> StringsEn
  }

private val BENGALI_DIGITS = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

fun formatDigits(input: String, style: NumeralStyle): String {
  if (style == NumeralStyle.LATIN) return input
  val out = StringBuilder(input.length)
  for (ch in input) {
    if (ch in '0'..'9') {
      out.append(BENGALI_DIGITS[ch - '0'])
    } else {
      out.append(ch)
    }
  }
  return out.toString()
}

fun formatNumber(value: Int, style: NumeralStyle): String =
  formatDigits(value.toString(), style)

fun formatPercent(scale: Float, style: NumeralStyle): String =
  formatDigits("${(scale * 100).toInt()}%", style)
