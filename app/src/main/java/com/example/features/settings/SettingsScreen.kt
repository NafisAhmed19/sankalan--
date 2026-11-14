package com.example.features.settings

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.R
import com.example.core.i18n.LocalStrings
import com.example.core.i18n.formatDigits
import com.example.core.i18n.formatNumber
import com.example.core.i18n.formatPercent
import com.example.core.model.AppLanguage
import com.example.core.model.AppSettings
import com.example.core.model.AppThemeMode
import com.example.core.model.DiaryWithPages
import com.example.core.model.NumeralStyle
import com.example.core.model.Page
import com.example.core.model.ReadingLayout
import com.example.features.calendar.CalendarIcsExporter
import com.example.features.calendar.ReminderNotifier
import com.example.features.export.BackupArchiveManager
import com.example.features.security.AppLockManager
import com.example.features.security.SetAppPinDialog
import com.example.features.update.AppUpdateChecker
import com.example.features.update.AppUpdateNotifier
import com.example.features.update.UpdateAvailableDialog
import com.example.features.update.UpdateCheckResult
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.LocalSankalanPalette
import com.example.ui.theme.NotoSansBengaliFontFamily
import com.example.ui.theme.NotoSerifBengaliFontFamily
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.SankalanSpacing
import com.example.ui.theme.SankalanTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings -> Appearance & Language screen (Specification §11).
 * Stacks cards vertically on Compact screens and uses a 2-column layout on Medium/Expanded screens.
 */
@Composable
fun SettingsScreen(
  settings: AppSettings,
  activeDiaries: List<DiaryWithPages> = emptyList(),
  datedPages: List<Page> = emptyList(),
  onLanguageChange: (AppLanguage) -> Unit,
  onNumeralStyleChange: (NumeralStyle) -> Unit,
  onThemeModeChange: (AppThemeMode) -> Unit,
  onReadingLayoutChange: (ReadingLayout) -> Unit,
  onPageTurnAnimationChange: (Boolean) -> Unit,
  onPageTurnSoundChange: (Boolean) -> Unit,
  onLiteModeChange: (Boolean) -> Unit,
  onReducedMotionChange: (Boolean) -> Unit,
  onTextScaleChange: (Float) -> Unit,
  onHighContrastChange: (Boolean) -> Unit,
  onReminderEnabledChange: (Boolean) -> Unit = {},
  onReminderTimeChange: (Int, Int) -> Unit = { _, _ -> },
  onLockEnabledChange: (Boolean) -> Unit = {},
  onGithubUpdateRepoChange: (String) -> Unit = {},
  onExportBackupSuspend: suspend () -> List<DiaryWithPages> = { activeDiaries },
  onRestoreBackupSuspend: suspend (List<DiaryWithPages>) -> Result<Int> = { Result.success(0) },
  onReplayWelcome: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current

  BoxWithConstraints(
    modifier = modifier.fillMaxSize().background(palette.background),
    contentAlignment = Alignment.TopCenter,
  ) {
    val twoColumns = maxWidth >= 700.dp

    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 1080.dp),
      contentPadding = PaddingValues(horizontal = SankalanSpacing.lg, vertical = SankalanSpacing.xl),
      verticalArrangement = Arrangement.spacedBy(SankalanSpacing.lg),
    ) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(SankalanSpacing.xs)) {
          Text(
            text = strings.appearanceAndLanguage,
            style = MaterialTheme.typography.headlineMedium,
            color = palette.textPrimary,
          )
          Text(
            text = strings.settingsSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
          )
        }
      }

      if (twoColumns) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.lg),
            verticalAlignment = Alignment.Top,
          ) {
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(SankalanSpacing.lg),
            ) {
              LanguageAndNumeralCard(
                settings = settings,
                onLanguageChange = onLanguageChange,
                onNumeralStyleChange = onNumeralStyleChange,
              )
              ThemeAppearanceCard(
                settings = settings,
                onThemeModeChange = onThemeModeChange,
              )
              ReadingPreferencesCard(
                settings = settings,
                onReadingLayoutChange = onReadingLayoutChange,
                onPageTurnAnimationChange = onPageTurnAnimationChange,
                onPageTurnSoundChange = onPageTurnSoundChange,
                onLiteModeChange = onLiteModeChange,
              )
              RemindersAndIcsCard(
                settings = settings,
                activeDiaries = activeDiaries,
                datedPages = datedPages,
                onReminderEnabledChange = onReminderEnabledChange,
                onReminderTimeChange = onReminderTimeChange,
              )
            }
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(SankalanSpacing.lg),
            ) {
              TypographyPreviewCard(textScale = settings.textScale)
              AccessibilityCard(
                settings = settings,
                onReducedMotionChange = onReducedMotionChange,
                onTextScaleChange = onTextScaleChange,
                onHighContrastChange = onHighContrastChange,
              )
              AppLockSecurityCard(
                settings = settings,
                onLockEnabledChange = onLockEnabledChange,
              )
              BackupAndUpdatesCard(
                settings = settings,
                onGithubUpdateRepoChange = onGithubUpdateRepoChange,
                onExportBackupSuspend = onExportBackupSuspend,
                onRestoreBackupSuspend = onRestoreBackupSuspend,
              )
              PrivacyAndIntroCard(onReplayWelcome = onReplayWelcome)
            }
          }
        }
      } else {
        item {
          LanguageAndNumeralCard(
            settings = settings,
            onLanguageChange = onLanguageChange,
            onNumeralStyleChange = onNumeralStyleChange,
          )
        }
        item {
          ThemeAppearanceCard(
            settings = settings,
            onThemeModeChange = onThemeModeChange,
          )
        }
        item { TypographyPreviewCard(textScale = settings.textScale) }
        item {
          ReadingPreferencesCard(
            settings = settings,
            onReadingLayoutChange = onReadingLayoutChange,
            onPageTurnAnimationChange = onPageTurnAnimationChange,
            onPageTurnSoundChange = onPageTurnSoundChange,
            onLiteModeChange = onLiteModeChange,
          )
        }
        item {
          RemindersAndIcsCard(
            settings = settings,
            activeDiaries = activeDiaries,
            datedPages = datedPages,
            onReminderEnabledChange = onReminderEnabledChange,
            onReminderTimeChange = onReminderTimeChange,
          )
        }
        item {
          AppLockSecurityCard(
            settings = settings,
            onLockEnabledChange = onLockEnabledChange,
          )
        }
        item {
          BackupAndUpdatesCard(
            settings = settings,
            onGithubUpdateRepoChange = onGithubUpdateRepoChange,
            onExportBackupSuspend = onExportBackupSuspend,
            onRestoreBackupSuspend = onRestoreBackupSuspend,
          )
        }
        item {
          AccessibilityCard(
            settings = settings,
            onReducedMotionChange = onReducedMotionChange,
            onTextScaleChange = onTextScaleChange,
            onHighContrastChange = onHighContrastChange,
          )
        }
        item { PrivacyAndIntroCard(onReplayWelcome = onReplayWelcome) }
      }

      item {
        OwnerAndCompanyCard()
      }

      item {
        Text(
          text = strings.appVersionLabel,
          style = MaterialTheme.typography.labelSmall,
          color = palette.textSecondary,
          modifier = Modifier.padding(top = SankalanSpacing.xs, bottom = SankalanSpacing.lg),
        )
      }
    }
  }
}

@Composable
private fun OwnerAndCompanyCard() {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val context = LocalContext.current

  Surface(
    shape = MaterialTheme.shapes.medium,
    color = if (palette.isDark) SankalanTokens.Walnut900 else palette.cardSurface,
    border = BorderStroke(1.2.dp, palette.borderGold),
    tonalElevation = 2.dp,
    modifier = Modifier.fillMaxWidth().testTag("settings_owner_card"),
  ) {
    Column(
      modifier = Modifier.padding(SankalanSpacing.lg),
      verticalArrangement = Arrangement.spacedBy(SankalanSpacing.md),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.md),
      ) {
        // Official NRST.tech Company Logo Badge
        Image(
          painter = painterResource(id = R.drawable.nrst_tech_logo),
          contentDescription = strings.ownerCompanyName,
          contentScale = ContentScale.Crop,
          modifier =
            Modifier
              .width(64.dp)
              .height(45.dp)
              .clip(RoundedCornerShape(10.dp))
              .border(1.2.dp, SankalanTokens.AntiqueGold, RoundedCornerShape(10.dp))
              .testTag("settings_nrst_logo"),
        )

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = strings.ownerSectionTitle,
            style = MaterialTheme.typography.labelMedium,
            color = palette.goldAccent,
          )
          Text(
            text = strings.ownerCompanyName,
            fontFamily = PlayfairDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
            color = palette.textPrimary,
          )
          Text(
            text = strings.ownerCraftTagline,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
          )
        }
      }

      HorizontalDivider(color = palette.borderSubtle)

      // Owner Name Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
      ) {
        Icon(
          imageVector = Icons.Outlined.PersonOutline,
          contentDescription = null,
          tint = palette.goldAccent,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = "${strings.ownerFounderLabel}:",
          style = MaterialTheme.typography.labelLarge,
          color = palette.textSecondary,
        )
        Text(
          text = strings.ownerFounderName,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.SemiBold,
          color = palette.textPrimary,
          modifier = Modifier.weight(1f),
        )
      }

      // Tappable Support / Founder Email Button (opens default mail client via ACTION_SENDTO)
      Surface(
        shape = MaterialTheme.shapes.small,
        color = palette.shelfSurface,
        border = BorderStroke(1.dp, palette.borderGold),
        modifier =
          Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button) {
              runCatching {
                val intent =
                  Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:${strings.ownerEmailAddress}")
                    putExtra(Intent.EXTRA_SUBJECT, "Sankalan-t / সংকলন-ৎ")
                  }
                context.startActivity(intent)
              }
            }
            .testTag("settings_owner_email_btn"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.Email,
            contentDescription = strings.ownerEmailLabel,
            tint = palette.goldAccent,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = "${strings.ownerEmailLabel}:",
            style = MaterialTheme.typography.labelLarge,
            color = palette.textSecondary,
          )
          Text(
            text = strings.ownerEmailAddress,
            style = MaterialTheme.typography.labelLarge,
            color = palette.goldHighlight,
            textDecoration = TextDecoration.Underline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }
  }
}

@Composable
private fun LanguageAndNumeralCard(
  settings: AppSettings,
  onLanguageChange: (AppLanguage) -> Unit,
  onNumeralStyleChange: (NumeralStyle) -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current

  SettingsCard(
    icon = Icons.Outlined.Translate,
    title = strings.uiLanguageTitle,
    subtitle = strings.uiLanguageDescription,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
    ) {
      SelectableChip(
        label = strings.languageBanglaLabel,
        selected = settings.language == AppLanguage.BN_BD,
        onClick = { onLanguageChange(AppLanguage.BN_BD) },
        modifier = Modifier.weight(1f).testTag("settings_lang_bn"),
      )
      SelectableChip(
        label = strings.languageEnglishLabel,
        selected = settings.language == AppLanguage.EN,
        onClick = { onLanguageChange(AppLanguage.EN) },
        modifier = Modifier.weight(1f).testTag("settings_lang_en"),
      )
    }

    HorizontalDivider(color = palette.borderSubtle)

    Text(
      text = strings.numeralStyleTitle,
      style = MaterialTheme.typography.labelLarge,
      color = palette.textPrimary,
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
    ) {
      SelectableChip(
        label = strings.numeralLatinOption,
        selected = settings.numeralStyle == NumeralStyle.LATIN,
        onClick = { onNumeralStyleChange(NumeralStyle.LATIN) },
        modifier = Modifier.weight(1f).testTag("settings_numeral_latin"),
      )
      SelectableChip(
        label = strings.numeralBengaliOption,
        selected = settings.numeralStyle == NumeralStyle.BENGALI,
        onClick = { onNumeralStyleChange(NumeralStyle.BENGALI) },
        modifier = Modifier.weight(1f).testTag("settings_numeral_bn"),
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeAppearanceCard(
  settings: AppSettings,
  onThemeModeChange: (AppThemeMode) -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current

  SettingsCard(
    icon = Icons.Outlined.Palette,
    title = strings.themeTitle,
    subtitle = strings.themePaletteCaption,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
    ) {
      SelectableChip(
        label = strings.themeDark,
        selected = settings.themeMode == AppThemeMode.DARK,
        onClick = { onThemeModeChange(AppThemeMode.DARK) },
        modifier = Modifier.weight(1f).testTag("settings_theme_dark"),
      )
      SelectableChip(
        label = strings.themeLight,
        selected = settings.themeMode == AppThemeMode.LIGHT,
        onClick = { onThemeModeChange(AppThemeMode.LIGHT) },
        modifier = Modifier.weight(1f).testTag("settings_theme_light"),
      )
      SelectableChip(
        label = strings.themeSystem,
        selected = settings.themeMode == AppThemeMode.SYSTEM,
        onClick = { onThemeModeChange(AppThemeMode.SYSTEM) },
        modifier = Modifier.weight(1f).testTag("settings_theme_system"),
      )
    }

    // Visual token swatch bar from §5.1
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
      verticalArrangement = Arrangement.spacedBy(SankalanSpacing.xs),
    ) {
      val swatches =
        listOf(
          SankalanTokens.Walnut900,
          SankalanTokens.Walnut800,
          SankalanTokens.Leather,
          SankalanTokens.AntiqueGold,
          SankalanTokens.Cream,
          SankalanTokens.Paper,
          SankalanTokens.Sage,
          SankalanTokens.Terracotta,
        )
      swatches.forEach { swatchColor ->
        Box(
          modifier =
            Modifier
              .size(22.dp)
              .clip(CircleShape)
              .background(swatchColor)
              .border(1.dp, palette.borderGold, CircleShape)
        )
      }
    }
  }
}

@Composable
private fun TypographyPreviewCard(textScale: Float) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val s = textScale.coerceIn(0.85f, 1.4f)

  SettingsCard(
    icon = Icons.Outlined.TextFields,
    title = strings.typographyPreviewTitle,
    subtitle = null,
  ) {
    Surface(
      shape = MaterialTheme.shapes.small,
      color = palette.paperBase,
      border = BorderStroke(1.dp, palette.paperShadow),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(SankalanSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SankalanSpacing.md),
      ) {
        PreviewBlock(
          label = strings.typographyBanglaHeadingLabel,
          sample = strings.typographyBanglaHeadingSample,
          fontFamily = NotoSerifBengaliFontFamily,
          fontSizeSp = 19f * s,
          lineHeightSp = 28f * s,
          fontWeight = FontWeight.SemiBold,
        )
        HorizontalDivider(color = palette.paperShadow)
        PreviewBlock(
          label = strings.typographyBanglaBodyLabel,
          sample = strings.typographyBanglaBodySample,
          fontFamily = NotoSansBengaliFontFamily,
          fontSizeSp = 14.5f * s,
          lineHeightSp = 25.5f * s,
          fontWeight = FontWeight.Normal,
        )
        HorizontalDivider(color = palette.paperShadow)
        PreviewBlock(
          label = strings.typographyEnglishHeadingLabel,
          sample = strings.typographyEnglishHeadingSample,
          fontFamily = PlayfairDisplayFontFamily,
          fontSizeSp = 19f * s,
          lineHeightSp = 25f * s,
          fontWeight = FontWeight.SemiBold,
        )
        HorizontalDivider(color = palette.paperShadow)
        PreviewBlock(
          label = strings.typographyEnglishBodyLabel,
          sample = strings.typographyEnglishBodySample,
          fontFamily = InterFontFamily,
          fontSizeSp = 14f * s,
          lineHeightSp = 21.5f * s,
          fontWeight = FontWeight.Normal,
        )
      }
    }
  }
}

@Composable
private fun PreviewBlock(
  label: String,
  sample: String,
  fontFamily: androidx.compose.ui.text.font.FontFamily,
  fontSizeSp: Float,
  lineHeightSp: Float,
  fontWeight: FontWeight,
) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = SankalanTokens.Muted,
    )
    Text(
      text = sample,
      fontFamily = fontFamily,
      fontWeight = fontWeight,
      fontSize = fontSizeSp.sp,
      lineHeight = lineHeightSp.sp,
      color = SankalanTokens.Ink,
    )
  }
}

@Composable
private fun ReadingPreferencesCard(
  settings: AppSettings,
  onReadingLayoutChange: (ReadingLayout) -> Unit,
  onPageTurnAnimationChange: (Boolean) -> Unit,
  onPageTurnSoundChange: (Boolean) -> Unit,
  onLiteModeChange: (Boolean) -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current

  SettingsCard(
    icon = Icons.Outlined.AutoStories,
    title = strings.readingPreferencesTitle,
    subtitle = null,
  ) {
    Text(
      text = strings.readingLayoutLabel,
      style = MaterialTheme.typography.labelLarge,
      color = palette.textPrimary,
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
    ) {
      SelectableChip(
        label = strings.readingLayoutSingle,
        selected = settings.readingLayout == ReadingLayout.SINGLE,
        onClick = { onReadingLayoutChange(ReadingLayout.SINGLE) },
        modifier = Modifier.weight(1f).testTag("settings_layout_single"),
      )
      SelectableChip(
        label = strings.readingLayoutSpread,
        selected = settings.readingLayout == ReadingLayout.SPREAD,
        onClick = { onReadingLayoutChange(ReadingLayout.SPREAD) },
        modifier = Modifier.weight(1f).testTag("settings_layout_spread"),
      )
    }

    SettingsToggleRow(
      title = strings.pageTurnAnimationLabel,
      description = null,
      checked = settings.pageTurnAnimation,
      onCheckedChange = onPageTurnAnimationChange,
      testTag = "settings_page_turn_anim",
    )
    SettingsToggleRow(
      title = strings.pageTurnSoundLabel,
      description = null,
      checked = settings.pageTurnSound,
      onCheckedChange = onPageTurnSoundChange,
      testTag = "settings_page_turn_sound",
    )
    SettingsToggleRow(
      title = strings.liteModeLabel,
      description = strings.liteModeDescription,
      checked = settings.liteMode,
      onCheckedChange = onLiteModeChange,
      testTag = "settings_lite_mode",
    )
  }
}

@Composable
private fun AccessibilityCard(
  settings: AppSettings,
  onReducedMotionChange: (Boolean) -> Unit,
  onTextScaleChange: (Float) -> Unit,
  onHighContrastChange: (Boolean) -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current

  SettingsCard(
    icon = Icons.Outlined.AccessibilityNew,
    title = strings.accessibilityTitle,
    subtitle = null,
  ) {
    SettingsToggleRow(
      title = strings.reduceMotionLabel,
      description = strings.reduceMotionDescription,
      checked = settings.reducedMotion,
      onCheckedChange = onReducedMotionChange,
      testTag = "settings_reduce_motion",
    )

    Column(verticalArrangement = Arrangement.spacedBy(SankalanSpacing.xs)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = strings.textScaleLabel,
          style = MaterialTheme.typography.bodyLarge,
          color = palette.textPrimary,
        )
        Text(
          text = formatPercent(settings.textScale, settings.numeralStyle),
          style = MaterialTheme.typography.labelLarge,
          color = palette.goldAccent,
        )
      }
      Slider(
        value = settings.textScale,
        onValueChange = onTextScaleChange,
        valueRange = 0.85f..1.4f,
        steps = 10,
        colors =
          SliderDefaults.colors(
            thumbColor = palette.goldAccent,
            activeTrackColor = palette.goldAccent,
            inactiveTrackColor = palette.borderSubtle,
          ),
        modifier = Modifier.testTag("settings_text_scale_slider"),
      )
    }

    SettingsToggleRow(
      title = strings.highContrastLabel,
      description = strings.highContrastDescription,
      checked = settings.highContrast,
      onCheckedChange = onHighContrastChange,
      testTag = "settings_high_contrast",
    )
  }
}

@Composable
private fun PrivacyAndIntroCard(onReplayWelcome: () -> Unit) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current

  SettingsCard(
    icon = Icons.Outlined.Lock,
    title = strings.privacyAndStorageTitle,
    subtitle = strings.privacyAndStorageBody,
  ) {
    OutlinedButton(
      onClick = onReplayWelcome,
      border = BorderStroke(1.dp, palette.borderGold),
      colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.goldAccent),
      shape = MaterialTheme.shapes.small,
      modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("replay_welcome_button"),
    ) {
      Icon(
        imageVector = Icons.Outlined.WaterDrop,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.size(SankalanSpacing.sm))
      Text(text = strings.replayWelcome, style = MaterialTheme.typography.labelLarge)
    }
  }
}

@Composable
private fun RemindersAndIcsCard(
  settings: AppSettings,
  activeDiaries: List<DiaryWithPages>,
  datedPages: List<Page>,
  onReminderEnabledChange: (Boolean) -> Unit,
  onReminderTimeChange: (Int, Int) -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val context = LocalContext.current
  var icsExported by remember { mutableStateOf(false) }

  val notifPermLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        ReminderNotifier.showGentleReminderNotification(context, settings.language == AppLanguage.BN_BD)
      }
    }

  val icsLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/calendar")) { uri ->
      if (uri != null) {
        val ics =
          CalendarIcsExporter.buildIcs(
            diaries = activeDiaries,
            datedPages = datedPages,
            reminderEnabled = settings.reminderEnabled,
            reminderHour = settings.reminderHour,
            reminderMinute = settings.reminderMinute,
            includeBanglaTitle = settings.language == AppLanguage.BN_BD,
          )
        runCatching {
          context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(ics.toByteArray(Charsets.UTF_8))
          }
          icsExported = true
        }
      }
    }

  SettingsCard(
    icon = Icons.Outlined.NotificationsActive,
    title = strings.reminderSettingsTitle,
    subtitle = null,
  ) {
    SettingsToggleRow(
      title = strings.reminderEnableLabel,
      description = strings.reminderEnableDesc,
      checked = settings.reminderEnabled,
      onCheckedChange = onReminderEnabledChange,
      testTag = "settings_reminder_toggle",
    )

    if (settings.reminderEnabled) {
      Text(strings.reminderTimeLabel, style = MaterialTheme.typography.labelLarge, color = palette.textPrimary)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.xs),
      ) {
        listOf(19, 20, 21, 22).forEach { hr ->
          SelectableChip(
            label = formatDigits("$hr:00", settings.numeralStyle),
            selected = settings.reminderHour == hr,
            onClick = { onReminderTimeChange(hr, 0) },
            modifier = Modifier.weight(1f),
          )
        }
      }

      OutlinedButton(
        onClick = {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
          ) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          } else {
            ReminderNotifier.showGentleReminderNotification(context, settings.language == AppLanguage.BN_BD)
          }
        },
        border = BorderStroke(1.dp, palette.borderSubtle),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("settings_test_reminder_notif_btn"),
      ) {
        Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = palette.goldAccent, modifier = Modifier.size(17.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(strings.reminderNotificationTest, color = palette.textPrimary)
      }
    }

    OutlinedButton(
      onClick = { icsLauncher.launch("sankalan_journal_schedule.ics") },
      border = BorderStroke(1.dp, palette.borderGold),
      shape = MaterialTheme.shapes.small,
      modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("settings_export_ics_btn"),
    ) {
      Icon(Icons.Outlined.EventAvailable, contentDescription = null, tint = palette.goldAccent, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text(strings.reminderExportIcsLabel, color = palette.textPrimary)
    }

    if (icsExported) {
      Text(strings.reminderIcsExportedMsg, style = MaterialTheme.typography.labelSmall, color = palette.goldAccent)
    }
  }
}

@Composable
private fun AppLockSecurityCard(
  settings: AppSettings,
  onLockEnabledChange: (Boolean) -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val context = LocalContext.current
  val lockManager = remember(context) { AppLockManager(context) }
  var hasPin by remember { mutableStateOf(lockManager.hasPin()) }
  var showSetPinDialog by remember { mutableStateOf(false) }

  SettingsCard(
    icon = Icons.Outlined.Fingerprint,
    title = strings.appLockTitle,
    subtitle = strings.appLockEnableDesc,
  ) {
    SettingsToggleRow(
      title = strings.appLockEnableLabel,
      description = null,
      checked = settings.lockEnabled && hasPin,
      onCheckedChange = { enabled ->
        if (enabled && !hasPin) {
          showSetPinDialog = true
        } else {
          onLockEnabledChange(enabled)
        }
      },
      testTag = "settings_app_lock_toggle",
    )

    OutlinedButton(
      onClick = { showSetPinDialog = true },
      border = BorderStroke(1.dp, palette.borderGold),
      shape = MaterialTheme.shapes.small,
      modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("settings_set_pin_btn"),
    ) {
      Icon(Icons.Outlined.Key, contentDescription = null, tint = palette.goldAccent, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text(if (hasPin) strings.appLockChangePinLabel else strings.appLockSetPinLabel, color = palette.textPrimary)
    }
  }

  if (showSetPinDialog) {
    SetAppPinDialog(
      onDismiss = { showSetPinDialog = false },
      onPinSaved = { rawPin ->
        if (lockManager.setPin(rawPin)) {
          hasPin = true
          onLockEnabledChange(true)
          showSetPinDialog = false
        }
      },
    )
  }
}

@Composable
private fun BackupAndUpdatesCard(
  settings: AppSettings,
  onGithubUpdateRepoChange: (String) -> Unit,
  onExportBackupSuspend: suspend () -> List<DiaryWithPages>,
  onRestoreBackupSuspend: suspend (List<DiaryWithPages>) -> Result<Int>,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var backupFeedback by remember { mutableStateOf<String?>(null) }
  var repoInput by remember(settings.githubUpdateRepo) { mutableStateOf(settings.githubUpdateRepo) }
  var isCheckingUpdate by remember { mutableStateOf(false) }
  var updateStatusText by remember { mutableStateOf<String?>(null) }
  var pendingUpdateDialog by remember { mutableStateOf<com.example.features.update.UpdateReleaseManifest?>(null) }
  var hasUpdateNotifPerm by remember {
    mutableStateOf(AppUpdateNotifier.hasNotificationPermission(context))
  }
  val fixedShareLink = remember(repoInput) { AppUpdateChecker.resolveFixedReleaseUrl(repoInput) }

  val updateNotifPermLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasUpdateNotifPerm = granted
    }

  val exportBackupLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
      if (uri != null) {
        scope.launch {
          val ok =
            withContext(Dispatchers.IO) {
              runCatching {
                val diaries = onExportBackupSuspend()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                  BackupArchiveManager.exportFullBackup(diaries, out)
                } ?: error("Cannot open stream")
              }.isSuccess
            }
          backupFeedback = if (ok) strings.exportSuccessMsg else strings.exportFailedMsg
        }
      }
    }

  val restoreBackupLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        scope.launch {
          val restoredList =
            withContext(Dispatchers.IO) {
              runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                  BackupArchiveManager.readFullBackup(context, input)
                }
              }.getOrNull()
            }
          if (restoredList != null && restoredList.isNotEmpty()) {
            val res = onRestoreBackupSuspend(restoredList)
            backupFeedback = if (res.isSuccess) strings.backupRestoreSuccess else strings.backupRestoreFailed
          } else {
            backupFeedback = strings.backupRestoreFailed
          }
        }
      }
    }

  SettingsCard(
    icon = Icons.Outlined.Backup,
    title = strings.backupAndUpdateTitle,
    subtitle = strings.backupExportDesc,
  ) {
    Button(
      onClick = { exportBackupLauncher.launch("sankalan_backup.sankalan") },
      colors =
        ButtonDefaults.buttonColors(
          containerColor = palette.goldAccent,
          contentColor = SankalanTokens.Walnut900,
        ),
      shape = MaterialTheme.shapes.small,
      modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("settings_export_backup_btn"),
    ) {
      Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text(strings.backupExportButton, style = MaterialTheme.typography.labelLarge)
    }

    OutlinedButton(
      onClick = { restoreBackupLauncher.launch(arrayOf("*/*")) },
      border = BorderStroke(1.dp, palette.borderGold),
      shape = MaterialTheme.shapes.small,
      modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("settings_restore_backup_btn"),
    ) {
      Icon(Icons.Outlined.Restore, contentDescription = null, tint = palette.goldAccent, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text(strings.backupRestoreButton, color = palette.textPrimary)
    }

    if (backupFeedback != null) {
      Text(backupFeedback!!, style = MaterialTheme.typography.labelSmall, color = palette.goldAccent)
    }

    HorizontalDivider(color = palette.borderSubtle)

    Text(
      text = "${strings.updateCheckerTitle} (${strings.updateCurrentVersionBadge}: v${BuildConfig.VERSION_NAME})",
      style = MaterialTheme.typography.titleSmall,
      color = palette.textPrimary,
    )
    Text(
      text = strings.updateFriendsShareGuide,
      style = MaterialTheme.typography.bodySmall,
      color = palette.textSecondary,
    )

    OutlinedTextField(
      value = repoInput,
      onValueChange = {
        repoInput = it
        onGithubUpdateRepoChange(it)
      },
      label = { Text(strings.updateRepoHint) },
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("settings_update_repo_input"),
    )

    // Permanent Fixed Shareable Link box + Copy & Share App Link buttons
    Surface(
      shape = MaterialTheme.shapes.small,
      color = palette.shelfSurface,
      border = BorderStroke(1.dp, palette.borderSubtle),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(SankalanSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = strings.updateFixedLinkLabel,
          style = MaterialTheme.typography.labelSmall,
          color = palette.goldAccent,
        )
        Text(
          text = fixedShareLink,
          style = MaterialTheme.typography.bodySmall,
          color = palette.textPrimary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
        ) {
          OutlinedButton(
            onClick = {
              val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
              cm?.setPrimaryClip(ClipData.newPlainText("Sankalan-t Link", fixedShareLink))
              updateStatusText = strings.updateLinkCopiedMsg
            },
            border = BorderStroke(1.dp, palette.borderGold),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("settings_copy_share_link_btn"),
          ) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = palette.goldAccent, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(strings.updateCopyLinkBtn, color = palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          Button(
            onClick = {
              runCatching {
                val shareIntent =
                  Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "সংকলন-ৎ | Sankalan-t")
                    putExtra(Intent.EXTRA_TEXT, "${strings.updateShareInviteBody}\n$fixedShareLink")
                  }
                context.startActivity(Intent.createChooser(shareIntent, strings.updateShareLinkBtn))
              }
            },
            colors =
              ButtonDefaults.buttonColors(
                containerColor = palette.goldAccent,
                contentColor = SankalanTokens.Walnut900,
              ),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("settings_share_app_link_btn"),
          ) {
            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(strings.updateShareLinkBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }
      }
    }

    // Auto-update system notification status / 1-tap permission prompt (Android 13+)
    if (!hasUpdateNotifPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      OutlinedButton(
        onClick = { updateNotifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
        border = BorderStroke(1.dp, palette.borderGold),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("settings_enable_update_notif_btn"),
      ) {
        Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = palette.goldAccent, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(strings.updateEnableNotifBtn, color = palette.textPrimary)
      }
    } else {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(Icons.Outlined.CheckCircleOutline, contentDescription = null, tint = palette.goldAccent, modifier = Modifier.size(16.dp))
        Text(strings.updateNotifActiveBadge, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
    ) {
      OutlinedButton(
        onClick = {
          isCheckingUpdate = true
          updateStatusText = strings.updateCheckingLabel
          scope.launch {
            val res =
              AppUpdateChecker.checkForUpdates(
                repoOrUrl = repoInput,
                currentVersionCode = BuildConfig.VERSION_CODE,
                currentVersionName = BuildConfig.VERSION_NAME,
              )
            isCheckingUpdate = false
            when (res) {
              is UpdateCheckResult.UpdateAvailable -> {
                pendingUpdateDialog = res.release
                AppUpdateNotifier.showUpdateNotificationIfNeeded(
                  context = context,
                  release = res.release,
                  isBangla = settings.language == AppLanguage.BN_BD,
                  forceNotify = true,
                )
                updateStatusText = "${strings.updateAvailableTitle}: v${res.release.versionName}"
              }
              is UpdateCheckResult.UpToDate -> updateStatusText = strings.updateUpToDateLabel
              is UpdateCheckResult.Failed -> updateStatusText = strings.updateCheckFailedLabel
            }
          }
        },
        enabled = !isCheckingUpdate,
        border = BorderStroke(1.dp, palette.borderGold),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("settings_check_updates_btn"),
      ) {
        Icon(Icons.Outlined.SystemUpdate, contentDescription = null, tint = palette.goldAccent, modifier = Modifier.size(17.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(
          text = if (isCheckingUpdate) strings.updateCheckingLabel else strings.updateCheckNowButton,
          color = palette.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      OutlinedButton(
        onClick = {
          val sample =
            AppUpdateChecker.buildSampleManifestJson(
              repoOrUrl = repoInput,
              nextVersionCode = BuildConfig.VERSION_CODE + 1,
              nextVersionName = "${BuildConfig.VERSION_CODE + 1}.0",
            )
          val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
          cm?.setPrimaryClip(ClipData.newPlainText("version.json", sample))
          updateStatusText = strings.updateManifestCopiedMsg
        },
        border = BorderStroke(1.dp, palette.borderSubtle),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("settings_copy_manifest_btn"),
      ) {
        Icon(Icons.Outlined.ContentCopy, contentDescription = strings.updateCopyManifestBtn, tint = palette.goldAccent, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("version.json", style = MaterialTheme.typography.labelMedium, color = palette.textPrimary)
      }
    }

    if (updateStatusText != null) {
      Text(updateStatusText!!, style = MaterialTheme.typography.labelSmall, color = palette.goldAccent)
    }
  }

  if (pendingUpdateDialog != null) {
    UpdateAvailableDialog(
      release = pendingUpdateDialog!!,
      language = settings.language,
      onDismiss = { pendingUpdateDialog = null },
    )
  }
}

@Composable
private fun SettingsCard(
  icon: ImageVector,
  title: String,
  subtitle: String?,
  content: @Composable ColumnScope.() -> Unit,
) {
  val palette = LocalSankalanPalette.current
  Surface(
    shape = MaterialTheme.shapes.medium,
    color = palette.cardSurface,
    border = BorderStroke(1.dp, palette.borderSubtle),
    tonalElevation = 1.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(SankalanSpacing.lg),
      verticalArrangement = Arrangement.spacedBy(SankalanSpacing.md),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = palette.goldAccent,
          modifier = Modifier.size(22.dp),
        )
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = palette.textPrimary,
        )
      }
      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = palette.textSecondary,
        )
      }
      content()
    }
  }
}

@Composable
private fun SelectableChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalSankalanPalette.current
  Surface(
    shape = MaterialTheme.shapes.small,
    color = if (selected) palette.goldAccent else palette.cardElevated.copy(alpha = 0.55f),
    border = BorderStroke(1.dp, if (selected) palette.goldAccent else palette.borderSubtle),
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .clip(MaterialTheme.shapes.small)
        .clickable(role = Role.RadioButton, onClick = onClick),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.padding(horizontal = SankalanSpacing.md, vertical = SankalanSpacing.sm),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) SankalanTokens.Walnut900 else palette.textPrimary,
        maxLines = 2,
      )
    }
  }
}

@Composable
private fun SettingsToggleRow(
  title: String,
  description: String?,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
) {
  val palette = LocalSankalanPalette.current
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .minimumInteractiveComponentSize()
        .clickable(role = Role.Switch) { onCheckedChange(!checked) }
        .padding(vertical = SankalanSpacing.xs),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(
      modifier = Modifier.weight(1f).padding(end = SankalanSpacing.md),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        color = palette.textPrimary,
      )
      if (!description.isNullOrBlank()) {
        Text(
          text = description,
          style = MaterialTheme.typography.bodyMedium,
          color = palette.textSecondary,
        )
      }
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors =
        SwitchDefaults.colors(
          checkedThumbColor = SankalanTokens.Walnut900,
          checkedTrackColor = palette.goldAccent,
          uncheckedThumbColor = palette.textSecondary,
          uncheckedTrackColor = palette.cardElevated,
          uncheckedBorderColor = palette.borderSubtle,
        ),
      modifier = Modifier.testTag(testTag),
    )
  }
}
