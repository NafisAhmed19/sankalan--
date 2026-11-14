package com.example.features.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.LocalStrings
import com.example.core.i18n.formatNumber
import com.example.core.model.AppLanguage
import com.example.core.model.DiaryWithPages
import com.example.core.model.NumeralStyle
import com.example.core.model.SaveStatus
import com.example.features.bookshelf.WalnutShelfCabinetRow
import com.example.features.insights.MoodAndPromptDashboardCard
import com.example.features.memorylane.MemoryLaneStage
import com.example.ui.components.SankalanAtmosphericBackdrop
import com.example.ui.components.SaveStatusPill
import com.example.ui.theme.LocalSankalanPalette
import com.example.ui.theme.NotoSerifBengaliFontFamily
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.SankalanSpacing
import com.example.ui.theme.SankalanTokens

/**
 * Home Screen implementing approved composition D-07:
 * Top to bottom: (1) Brand & Water-Logo replay bar, (2) Memory Lane stage, (3) 3D Walnut Bookshelf.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
  currentLanguage: AppLanguage,
  numeralStyle: NumeralStyle,
  reduceMotion: Boolean,
  liteMode: Boolean,
  reminderEnabled: Boolean,
  activeDiaries: List<DiaryWithPages>,
  archivedCount: Int,
  saveStatus: SaveStatus,
  onRetrySave: () -> Unit,
  onToggleLanguage: () -> Unit,
  onReplayWelcome: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenBookshelf: () -> Unit,
  onOpenArchive: () -> Unit,
  onOpenGlobalSearch: () -> Unit,
  onOpenCreateJournal: () -> Unit,
  onWritePromptEntry: (title: String, promptQuestion: String) -> Unit,
  onInspectBook: (DiaryWithPages) -> Unit,
  onOpenMemory: (DiaryWithPages, Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  var selectedShelfBookId by rememberSaveable { mutableStateOf<String?>(null) }

  SankalanAtmosphericBackdrop(modifier = modifier) {
    BoxWithConstraints(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.TopCenter,
    ) {
      val isWide = maxWidth >= 700.dp
      val homeShelfBooks = remember(activeDiaries, isWide) { activeDiaries.take(if (isWide) 8 else 5) }

      LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 1080.dp),
        contentPadding = PaddingValues(horizontal = SankalanSpacing.lg, vertical = SankalanSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(SankalanSpacing.xl),
      ) {
        // 1. Literary Walnut Brand Header (responsive stacked on phone so wordmark & badges never squeeze)
        item {
          Column(verticalArrangement = Arrangement.spacedBy(SankalanSpacing.sm)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              FlowRow(
                modifier = Modifier.weight(1f).padding(end = SankalanSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(SankalanSpacing.xs),
              ) {
                Surface(
                  shape = RoundedCornerShape(20.dp),
                  color = palette.shelfSurface,
                  border = BorderStroke(1.dp, palette.borderGold),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Lock,
                      contentDescription = null,
                      tint = palette.goldAccent,
                      modifier = Modifier.size(13.dp),
                    )
                    Text(
                      text = strings.localFirstBadge,
                      style = MaterialTheme.typography.labelSmall,
                      color = palette.textPrimary,
                      maxLines = 1,
                    )
                  }
                }
                SaveStatusPill(status = saveStatus, onRetry = onRetrySave)
              }

              Row(
                horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Surface(
                  shape = MaterialTheme.shapes.small,
                  color = palette.cardSurface,
                  border = BorderStroke(1.dp, palette.borderGold),
                  modifier =
                    Modifier
                      .minimumInteractiveComponentSize()
                      .clip(MaterialTheme.shapes.small)
                      .clickable(role = Role.Button, onClick = onToggleLanguage)
                      .testTag("home_language_toggle"),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Translate,
                      contentDescription = strings.uiLanguageTitle,
                      tint = palette.goldAccent,
                      modifier = Modifier.size(17.dp),
                    )
                    Text(
                      text = if (currentLanguage == AppLanguage.BN_BD) "EN" else "বাং",
                      style = MaterialTheme.typography.labelLarge,
                      color = palette.textPrimary,
                    )
                  }
                }

                IconButton(
                  onClick = onOpenGlobalSearch,
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("home_global_search_btn"),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = strings.globalSearchTitle,
                    tint = palette.goldAccent,
                  )
                }

                IconButton(
                  onClick = onReplayWelcome,
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("home_replay_welcome"),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.WaterDrop,
                    contentDescription = strings.replayWelcome,
                    tint = palette.goldAccent,
                  )
                }

                IconButton(
                  onClick = onOpenSettings,
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("home_open_settings"),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = strings.navSettings,
                    tint = palette.textPrimary,
                  )
                }
              }
            }

            // Centered Illuminated Hero Backdrop Card with radial golden spotlight & Alpona corner arcs
            Surface(
              shape = RoundedCornerShape(22.dp),
              color = if (palette.isDark) SankalanTokens.Walnut900 else Color(0xFF2D1E17),
              border = BorderStroke(1.5.dp, SankalanTokens.AntiqueGold.copy(alpha = 0.72f)),
              shadowElevation = 8.dp,
              modifier = Modifier.fillMaxWidth().testTag("home_hero_header"),
            ) {
              Box(modifier = Modifier.fillMaxWidth()) {
                Canvas(modifier = Modifier.matchParentSize()) {
                  // Deep warm walnut backdrop + centered golden radial glow
                  drawRect(
                    brush =
                      Brush.radialGradient(
                        colors =
                          listOf(
                            SankalanTokens.AntiqueGold.copy(alpha = 0.28f),
                            SankalanTokens.Walnut700.copy(alpha = 0.35f),
                            Color.Transparent,
                          ),
                        center = Offset(size.width * 0.5f, size.height * 0.44f),
                        radius = size.width * 0.68f,
                      )
                  )
                  // Inner double-hairline antique gold filigree border
                  val inset = 8.dp.toPx()
                  drawRoundRect(
                    color = SankalanTokens.AntiqueGold.copy(alpha = 0.42f),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx()),
                  )
                  // Subtle Bengali Alpona corner arcs
                  val arcR = 26.dp.toPx()
                  listOf(
                    Offset(inset, inset) to 0f,
                    Offset(size.width - inset - arcR * 2, inset) to 90f,
                    Offset(size.width - inset - arcR * 2, size.height - inset - arcR * 2) to 180f,
                    Offset(inset, size.height - inset - arcR * 2) to 270f,
                  ).forEach { (topLeft, startAngle) ->
                    drawArc(
                      color = SankalanTokens.GoldHighlight.copy(alpha = 0.45f),
                      startAngle = startAngle,
                      sweepAngle = 90f,
                      useCenter = false,
                      topLeft = topLeft,
                      size = Size(arcR * 2, arcR * 2),
                      style = Stroke(width = 1.2.dp.toPx()),
                    )
                  }
                }

                Column(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = SankalanSpacing.xl, vertical = SankalanSpacing.xl),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Text(
                    text = "— ৎ —",
                    fontFamily = NotoSerifBengaliFontFamily,
                    fontSize = 14.sp,
                    color = SankalanTokens.GoldHighlight.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                  )

                  if (currentLanguage == AppLanguage.BN_BD) {
                    Text(
                      text = strings.brandName,
                      fontFamily = NotoSerifBengaliFontFamily,
                      fontWeight = FontWeight.Bold,
                      style =
                        TextStyle(
                          fontSize = 33.sp,
                          lineHeight = 40.sp,
                          color = SankalanTokens.GoldHighlight,
                          textAlign = TextAlign.Center,
                          shadow =
                            Shadow(
                              color = SankalanTokens.AntiqueGold.copy(alpha = 0.72f),
                              offset = Offset(0f, 0f),
                              blurRadius = 26f,
                            ),
                        ),
                    )
                    Text(
                      text = strings.brandLogoWordmark,
                      fontFamily = PlayfairDisplayFontFamily,
                      fontWeight = FontWeight.Medium,
                      fontSize = 15.sp,
                      letterSpacing = 1.6.sp,
                      color = SankalanTokens.Cream.copy(alpha = 0.86f),
                      textAlign = TextAlign.Center,
                    )
                  } else {
                    Text(
                      text = strings.brandLogoWordmark,
                      fontFamily = PlayfairDisplayFontFamily,
                      fontWeight = FontWeight.Bold,
                      style =
                        TextStyle(
                          fontSize = 32.sp,
                          lineHeight = 38.sp,
                          color = SankalanTokens.GoldHighlight,
                          textAlign = TextAlign.Center,
                          shadow =
                            Shadow(
                              color = SankalanTokens.AntiqueGold.copy(alpha = 0.72f),
                              offset = Offset(0f, 0f),
                              blurRadius = 26f,
                            ),
                        ),
                    )
                    Text(
                      text = "সংকলন-ৎ",
                      fontFamily = NotoSerifBengaliFontFamily,
                      fontSize = 16.sp,
                      color = SankalanTokens.Cream.copy(alpha = 0.86f),
                      textAlign = TextAlign.Center,
                    )
                  }

                  Text(
                    text = strings.homeHeroTagline,
                    fontFamily = if (currentLanguage == AppLanguage.BN_BD) NotoSerifBengaliFontFamily else PlayfairDisplayFontFamily,
                    fontWeight = FontWeight.Medium,
                    style =
                      TextStyle(
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = SankalanTokens.Cream,
                        textAlign = TextAlign.Center,
                        shadow =
                          Shadow(
                            color = SankalanTokens.AntiqueGold.copy(alpha = 0.45f),
                            offset = Offset(0f, 1f),
                            blurRadius = 10f,
                          ),
                      ),
                  )

                  Text(
                    text = strings.homeSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SankalanTokens.Cream.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center,
                  )
                }
              }
            }
          }
        }

        // 2. Animated Memory Lane Stage (between Welcome header and Bookshelf per D-07)
        item {
          MemoryLaneStage(
            activeDiaries = activeDiaries,
            numeralStyle = numeralStyle,
            reduceMotion = reduceMotion,
            liteMode = liteMode,
            onOpenMemory = onOpenMemory,
          )
        }

        // 3. Real 3D Walnut Bookshelf directly on Home (D-07 + User request)
        item {
          Column(verticalArrangement = Arrangement.spacedBy(SankalanSpacing.md)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                modifier = Modifier.weight(1f).padding(end = SankalanSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Book,
                  contentDescription = null,
                  tint = palette.goldAccent,
                )
                Text(
                  text = strings.myBookshelf,
                  style = MaterialTheme.typography.titleLarge,
                  color = palette.textPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              Row(
                horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                if (archivedCount > 0) {
                  OutlinedButton(
                    onClick = onOpenArchive,
                    border = BorderStroke(1.dp, palette.borderSubtle),
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("home_open_archive"),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Archive,
                      contentDescription = strings.navArchive,
                      tint = palette.goldAccent,
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = formatNumber(archivedCount, numeralStyle),
                      style = MaterialTheme.typography.labelMedium,
                      color = palette.textPrimary,
                    )
                  }
                }

                TextButton(
                  onClick = onOpenBookshelf,
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("home_open_full_bookshelf"),
                ) {
                  Text(text = strings.shelfViewLabel, color = palette.goldAccent, maxLines = 1)
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = palette.goldAccent,
                    modifier = Modifier.size(16.dp),
                  )
                }
              }
            }

            if (homeShelfBooks.isNotEmpty()) {
              WalnutShelfCabinetRow(
                books = homeShelfBooks,
                selectedBookId = selectedShelfBookId,
                numeralStyle = numeralStyle,
                onSelectBook = { book ->
                  selectedShelfBookId = book.diary.id
                  onInspectBook(book)
                },
                modifier = Modifier.testTag("home_walnut_shelf_row"),
              )
            } else {
              Surface(
                shape = MaterialTheme.shapes.medium,
                color = palette.cardSurface,
                border = BorderStroke(1.dp, palette.borderSubtle),
                modifier = Modifier.fillMaxWidth().testTag("home_bookshelf_card"),
              ) {
                Column(
                  modifier = Modifier.padding(SankalanSpacing.lg),
                  verticalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
                ) {
                  Text(
                    text = strings.bookshelfEmptyTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                  )
                  Text(
                    text = strings.bookshelfEmptyBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                  )
                }
              }
            }

            Button(
              onClick = onOpenCreateJournal,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = palette.goldAccent,
                  contentColor = SankalanTokens.Walnut900,
                ),
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("home_create_journal_button"),
            ) {
              Icon(imageVector = Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = strings.createJournal, style = MaterialTheme.typography.labelLarge)
            }

            HorizontalDivider(color = palette.borderSubtle)

            Text(
              text = strings.coverStylesPreviewTitle,
              style = MaterialTheme.typography.labelLarge,
              color = palette.goldAccent,
            )

            if (isWide) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.md),
              ) {
                CoverStyleTile(
                  label = strings.coverLuxuryLabel,
                  coverColor = SankalanTokens.Leather,
                  accentColor = SankalanTokens.AntiqueGold,
                  modifier = Modifier.weight(1f),
                )
                CoverStyleTile(
                  label = strings.coverArtisticLabel,
                  coverColor = SankalanTokens.Sage,
                  accentColor = SankalanTokens.Cream,
                  modifier = Modifier.weight(1f),
                )
                CoverStyleTile(
                  label = strings.coverMinimalLabel,
                  coverColor = SankalanTokens.PaperShadow,
                  accentColor = SankalanTokens.Ink,
                  modifier = Modifier.weight(1f),
                )
              }
            } else {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
              ) {
                CoverStyleTile(
                  label = strings.coverLuxuryLabel,
                  coverColor = SankalanTokens.Leather,
                  accentColor = SankalanTokens.AntiqueGold,
                  modifier = Modifier.weight(1f),
                )
                CoverStyleTile(
                  label = strings.coverArtisticLabel,
                  coverColor = SankalanTokens.Sage,
                  accentColor = SankalanTokens.Cream,
                  modifier = Modifier.weight(1f),
                )
                CoverStyleTile(
                  label = strings.coverMinimalLabel,
                  coverColor = SankalanTokens.PaperShadow,
                  accentColor = SankalanTokens.Ink,
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }
        }

        // 4. Mood Balance & Daily Literary Prompt Card (Step 9)
        item {
          MoodAndPromptDashboardCard(
            language = currentLanguage,
            numeralStyle = numeralStyle,
            activeDiaries = activeDiaries,
            onWritePromptEntry = onWritePromptEntry,
          )
        }

        if (reminderEnabled && activeDiaries.isNotEmpty()) {
          item {
            Surface(
              shape = MaterialTheme.shapes.small,
              color = palette.shelfSurface,
              border = BorderStroke(1.dp, palette.borderGold),
              modifier = Modifier.fillMaxWidth().testTag("home_reminder_banner"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = SankalanSpacing.md, vertical = SankalanSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
              ) {
                Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = palette.goldAccent)
                Text(
                  text = strings.reminderBannerTitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = palette.textPrimary,
                  modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onWritePromptEntry(strings.reminderBannerAction, strings.reminderBannerTitle) }) {
                  Text(strings.reminderBannerAction, color = palette.goldAccent)
                }
              }
            }
          }
        }

        item {
          Text(
            text = strings.phaseMilestoneBadge,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary,
            modifier = Modifier.padding(bottom = SankalanSpacing.lg),
          )
        }
      }
    }
  }
}

@Composable
private fun CoverStyleTile(
  label: String,
  coverColor: Color,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  val palette = LocalSankalanPalette.current
  Surface(
    shape = MaterialTheme.shapes.small,
    color = palette.shelfSurface,
    border = BorderStroke(1.dp, palette.borderSubtle),
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier.padding(SankalanSpacing.sm),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
    ) {
      Box(
        modifier =
          Modifier
            .width(28.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp, topStart = 2.dp, bottomStart = 2.dp))
            .background(coverColor),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "ৎ",
          fontFamily = NotoSerifBengaliFontFamily,
          color = accentColor,
          style = MaterialTheme.typography.labelLarge,
        )
      }
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = palette.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
