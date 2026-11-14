package com.example.features.pagemanager

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.i18n.LocalStrings
import com.example.core.i18n.formatDigits
import com.example.core.i18n.formatNumber
import com.example.core.model.DiaryInvariants
import com.example.core.model.DiaryWithPages
import com.example.core.model.NumeralStyle
import com.example.core.model.Page
import com.example.features.bookshelf.parseHexColor
import com.example.ui.theme.LocalSankalanPalette
import com.example.ui.theme.NotoSerifBengaliFontFamily
import com.example.ui.theme.SankalanSpacing
import com.example.ui.theme.SankalanTokens

@Composable
fun PageManagerSheet(
  diaryWithPages: DiaryWithPages,
  numeralStyle: NumeralStyle,
  onDismiss: () -> Unit,
  onOpenPageInReader: (pageId: String) -> Unit,
  onAddPage: (diaryId: String) -> Unit,
  onDuplicatePage: (diaryId: String, pageId: String) -> Unit,
  onReorderPages: (diaryId: String, orderedPageIds: List<String>) -> Unit,
  onTogglePinPage: (diaryId: String, pageId: String) -> Unit,
  onResetPage: (diaryId: String, pageId: String) -> Unit,
  onSoftDeletePage: (diaryId: String, pageId: String) -> Unit,
  onRestoreDeletedPage: (diaryId: String, pageId: String) -> Unit,
  onPermanentlyDeletePage: (pageId: String) -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val diary = diaryWithPages.diary
  val activePages = remember(diaryWithPages.pages) { diaryWithPages.pages.sortedBy { it.order } }
  val deletedPages = remember(diaryWithPages.deletedPages) { diaryWithPages.deletedPages }

  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Active, 1 = Recently Deleted
  var resetTarget by remember { mutableStateOf<Page?>(null) }
  var deleteTarget by remember { mutableStateOf<Page?>(null) }

  val canAdd = DiaryInvariants.canAddPages(activePages.size)
  val canDelete = DiaryInvariants.canDeletePages(activePages.size)

  BackHandler(onBack = onDismiss)

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      color = palette.background,
      contentColor = palette.textPrimary,
      modifier = Modifier.fillMaxSize(),
    ) {
      BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val gridColumns = if (maxWidth >= 700.dp) 3 else 2

        Column(
          modifier =
            Modifier
              .fillMaxSize()
              .widthIn(max = 980.dp)
              .padding(horizontal = SankalanSpacing.lg, vertical = SankalanSpacing.md),
          verticalArrangement = Arrangement.spacedBy(SankalanSpacing.md),
        ) {
          // Header
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = SankalanSpacing.sm)) {
              Text(
                text = strings.pageManager,
                style = MaterialTheme.typography.headlineSmall,
                color = palette.textPrimary,
              )
              Text(
                text =
                  "${diary.title} · ${formatNumber(activePages.size, numeralStyle)}/${formatNumber(DiaryInvariants.MAX_PAGES, numeralStyle)} ${strings.pagesUnit}",
                style = MaterialTheme.typography.labelMedium,
                color = palette.goldAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            Row(
              horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.xs),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Button(
                onClick = { onAddPage(diary.id) },
                enabled = canAdd,
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = palette.goldAccent,
                    contentColor = SankalanTokens.Walnut900,
                  ),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("page_manager_add_page"),
              ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = strings.addPage, style = MaterialTheme.typography.labelLarge)
              }

              IconButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("page_manager_close"),
              ) {
                Icon(Icons.Outlined.Close, contentDescription = strings.closeLabel, tint = palette.textPrimary)
              }
            }
          }

          // Active Pages vs Recently Deleted Tabs
          TabRow(
            selectedTabIndex = selectedTab,
            containerColor = palette.shelfSurface,
            contentColor = palette.goldAccent,
            indicator = { tabPositions ->
              TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = palette.goldAccent,
              )
            },
            modifier = Modifier.clip(MaterialTheme.shapes.small),
          ) {
            Tab(
              selected = selectedTab == 0,
              onClick = { selectedTab = 0 },
              text = {
                Text("${strings.activePagesTab} (${formatNumber(activePages.size, numeralStyle)})")
              },
              modifier = Modifier.testTag("page_manager_tab_active"),
            )
            Tab(
              selected = selectedTab == 1,
              onClick = { selectedTab = 1 },
              text = {
                Text("${strings.recentlyDeletedTab} (${formatNumber(deletedPages.size, numeralStyle)})")
              },
              modifier = Modifier.testTag("page_manager_tab_deleted"),
            )
          }

          // Boundary Notice (when at 5 min or 200 max)
          if (!canDelete || !canAdd) {
            Surface(
              shape = MaterialTheme.shapes.small,
              color = palette.shelfSurface,
              border = BorderStroke(1.dp, palette.borderGold),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = if (!canAdd) strings.maxPagesReachedNotice else strings.minPagesReachedNotice,
                style = MaterialTheme.typography.labelMedium,
                color = palette.goldAccent,
                modifier = Modifier.padding(horizontal = SankalanSpacing.md, vertical = SankalanSpacing.sm),
              )
            }
          }

          if (selectedTab == 0) {
            LazyVerticalGrid(
              columns = GridCells.Fixed(gridColumns),
              contentPadding = PaddingValues(bottom = SankalanSpacing.xl),
              horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.md),
              verticalArrangement = Arrangement.spacedBy(SankalanSpacing.md),
              modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
              itemsIndexed(activePages, key = { _, page -> page.id }) { index, page ->
                PageThumbnailCard(
                  page = page,
                  pageIndex = index,
                  totalActive = activePages.size,
                  canAdd = canAdd,
                  canDelete = canDelete,
                  numeralStyle = numeralStyle,
                  onOpen = { onOpenPageInReader(page.id) },
                  onMoveEarlier = {
                    if (index > 0) {
                      val ids = activePages.map { it.id }.toMutableList()
                      val moved = ids.removeAt(index)
                      ids.add(index - 1, moved)
                      onReorderPages(diary.id, ids)
                    }
                  },
                  onMoveLater = {
                    if (index < activePages.lastIndex) {
                      val ids = activePages.map { it.id }.toMutableList()
                      val moved = ids.removeAt(index)
                      ids.add(index + 1, moved)
                      onReorderPages(diary.id, ids)
                    }
                  },
                  onTogglePin = { onTogglePinPage(diary.id, page.id) },
                  onDuplicate = { onDuplicatePage(diary.id, page.id) },
                  onReset = { resetTarget = page },
                  onDelete = { deleteTarget = page },
                )
              }
            }
          } else {
            if (deletedPages.isEmpty()) {
              Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                  text = strings.recentlyDeletedEmpty,
                  style = MaterialTheme.typography.bodyMedium,
                  color = palette.textSecondary,
                  textAlign = TextAlign.Center,
                )
              }
            } else {
              LazyColumn(
                contentPadding = PaddingValues(bottom = SankalanSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(SankalanSpacing.md),
                modifier = Modifier.weight(1f).fillMaxWidth(),
              ) {
                items(deletedPages, key = { it.id }) { page ->
                  DeletedPageRowCard(
                    page = page,
                    canRestore = canAdd,
                    numeralStyle = numeralStyle,
                    onRestore = { onRestoreDeletedPage(diary.id, page.id) },
                    onDeleteForever = { onPermanentlyDeletePage(page.id) },
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (resetTarget != null) {
    val target = resetTarget!!
    AlertDialog(
      onDismissRequest = { resetTarget = null },
      containerColor = palette.cardSurface,
      titleContentColor = palette.textPrimary,
      textContentColor = palette.textSecondary,
      title = { Text(strings.resetPageLabel) },
      text = { Text(strings.resetPageConfirmBody) },
      confirmButton = {
        Button(
          onClick = {
            onResetPage(diary.id, target.id)
            resetTarget = null
          },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = palette.goldAccent,
              contentColor = SankalanTokens.Walnut900,
            ),
        ) {
          Text(strings.confirm)
        }
      },
      dismissButton = {
        TextButton(onClick = { resetTarget = null }) {
          Text(strings.cancel, color = palette.textSecondary)
        }
      },
    )
  }

  if (deleteTarget != null) {
    val target = deleteTarget!!
    AlertDialog(
      onDismissRequest = { deleteTarget = null },
      containerColor = palette.cardSurface,
      titleContentColor = palette.textPrimary,
      textContentColor = palette.textSecondary,
      title = { Text(strings.deletePageConfirm) },
      text = {
        Text(
          target.title ?: "${strings.pageNumberPrefix} ${formatNumber(target.order + 1, numeralStyle)}"
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onSoftDeletePage(diary.id, target.id)
            deleteTarget = null
          },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = if (palette.isDark) SankalanTokens.ErrorDark else SankalanTokens.ErrorLight,
              contentColor = SankalanTokens.Cream,
            ),
          modifier = Modifier.testTag("confirm_soft_delete_page"),
        ) {
          Text(strings.delete)
        }
      },
      dismissButton = {
        TextButton(onClick = { deleteTarget = null }) {
          Text(strings.cancel, color = palette.textSecondary)
        }
      },
    )
  }
}

@Composable
private fun PageThumbnailCard(
  page: Page,
  pageIndex: Int,
  totalActive: Int,
  canAdd: Boolean,
  canDelete: Boolean,
  numeralStyle: NumeralStyle,
  onOpen: () -> Unit,
  onMoveEarlier: () -> Unit,
  onMoveLater: () -> Unit,
  onTogglePin: () -> Unit,
  onDuplicate: () -> Unit,
  onReset: () -> Unit,
  onDelete: () -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val paperBg = parseHexColor(page.background.tintHex, SankalanTokens.Paper)
  val excerpt = page.elements.firstOrNull { !it.hidden && !it.text.isNullOrBlank() }?.text.orEmpty()

  Surface(
    shape = MaterialTheme.shapes.medium,
    color = palette.cardSurface,
    border = BorderStroke(1.dp, if (page.pinned) palette.goldAccent else palette.borderSubtle),
    modifier = Modifier.fillMaxWidth().testTag("page_thumb_${page.id}"),
  ) {
    Column(
      modifier = Modifier.padding(SankalanSpacing.sm),
      verticalArrangement = Arrangement.spacedBy(SankalanSpacing.xs),
    ) {
      // Miniature Paper Sheet Preview (tap to open in Reading Mode)
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = paperBg,
        border = BorderStroke(1.dp, SankalanTokens.AntiqueGold.copy(alpha = 0.45f)),
        modifier =
          Modifier
            .fillMaxWidth()
            .height(124.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onOpen),
      ) {
        Box(modifier = Modifier.fillMaxSize()) {
          // Subtle ruled/grid line hint inside thumbnail
          Canvas(modifier = Modifier.fillMaxSize()) {
            var y = 26.dp.toPx()
            while (y < size.height - 8.dp.toPx()) {
              drawLine(
                color = SankalanTokens.Ink.copy(alpha = 0.08f),
                start = Offset(10.dp.toPx(), y),
                end = Offset(size.width - 10.dp.toPx(), y),
                strokeWidth = 0.8.dp.toPx(),
              )
              y += 14.dp.toPx()
            }
          }

          Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = SankalanTokens.Walnut900.copy(alpha = 0.82f),
              ) {
                Text(
                  text = formatNumber(pageIndex + 1, numeralStyle),
                  style = MaterialTheme.typography.labelSmall,
                  color = SankalanTokens.GoldHighlight,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
              if (!page.dateIso.isNullOrBlank()) {
                Text(
                  text = formatDigits(page.dateIso, numeralStyle),
                  style = MaterialTheme.typography.labelSmall,
                  color = SankalanTokens.Muted,
                  maxLines = 1,
                )
              }
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
              Text(
                text = page.title ?: if (excerpt.isBlank()) strings.blankPageLabel else "",
                fontFamily = NotoSerifBengaliFontFamily,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelMedium,
                color = SankalanTokens.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (excerpt.isNotBlank()) {
                Text(
                  text = excerpt,
                  style = MaterialTheme.typography.bodySmall,
                  color = SankalanTokens.Ink.copy(alpha = 0.78f),
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }

      // Reorder (< >) + Pin + Duplicate + Reset + Soft-Delete Toolbar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onMoveEarlier,
            enabled = pageIndex > 0,
            modifier = Modifier.size(36.dp).testTag("page_move_earlier_${page.id}"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
              contentDescription = strings.moveEarlierLabel,
              tint = if (pageIndex > 0) palette.goldAccent else palette.textSecondary.copy(alpha = 0.35f),
            )
          }
          IconButton(
            onClick = onMoveLater,
            enabled = pageIndex < totalActive - 1,
            modifier = Modifier.size(36.dp).testTag("page_move_later_${page.id}"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
              contentDescription = strings.moveLaterLabel,
              tint =
                if (pageIndex < totalActive - 1) palette.goldAccent
                else palette.textSecondary.copy(alpha = 0.35f),
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onTogglePin,
            modifier = Modifier.size(36.dp).testTag("page_pin_${page.id}"),
          ) {
            Icon(
              imageVector = if (page.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
              contentDescription = if (page.pinned) strings.unpinPageLabel else strings.pinPageLabel,
              tint = if (page.pinned) palette.goldAccent else palette.textSecondary,
              modifier = Modifier.size(16.dp),
            )
          }
          IconButton(
            onClick = onDuplicate,
            enabled = canAdd,
            modifier = Modifier.size(36.dp).testTag("page_duplicate_${page.id}"),
          ) {
            Icon(
              imageVector = Icons.Outlined.ContentCopy,
              contentDescription = strings.duplicatePageLabel,
              tint = if (canAdd) palette.textPrimary else palette.textSecondary.copy(alpha = 0.35f),
              modifier = Modifier.size(16.dp),
            )
          }
          IconButton(
            onClick = onReset,
            modifier = Modifier.size(36.dp).testTag("page_reset_${page.id}"),
          ) {
            Icon(
              imageVector = Icons.Outlined.RestartAlt,
              contentDescription = strings.resetPageLabel,
              tint = palette.textPrimary,
              modifier = Modifier.size(16.dp),
            )
          }
          IconButton(
            onClick = onDelete,
            enabled = canDelete,
            modifier = Modifier.size(36.dp).testTag("page_delete_${page.id}"),
          ) {
            Icon(
              imageVector = Icons.Outlined.DeleteOutline,
              contentDescription = strings.delete,
              tint =
                if (!canDelete) palette.textSecondary.copy(alpha = 0.3f)
                else if (palette.isDark) SankalanTokens.ErrorDark
                else SankalanTokens.ErrorLight,
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DeletedPageRowCard(
  page: Page,
  canRestore: Boolean,
  numeralStyle: NumeralStyle,
  onRestore: () -> Unit,
  onDeleteForever: () -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val excerpt = page.elements.firstOrNull { !it.hidden && !it.text.isNullOrBlank() }?.text.orEmpty()

  Surface(
    shape = MaterialTheme.shapes.medium,
    color = palette.cardSurface,
    border = BorderStroke(1.dp, palette.borderSubtle),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(SankalanSpacing.md),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f).padding(end = SankalanSpacing.sm)) {
        Text(
          text = page.title ?: "${strings.pageNumberPrefix} (${strings.blankPageLabel})",
          style = MaterialTheme.typography.titleMedium,
          color = palette.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (!page.dateIso.isNullOrBlank()) {
          Text(
            text = formatDigits(page.dateIso, numeralStyle),
            style = MaterialTheme.typography.labelSmall,
            color = palette.goldAccent,
          )
        }
        if (excerpt.isNotBlank()) {
          Text(
            text = excerpt,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.xs)) {
        OutlinedButton(
          onClick = onRestore,
          enabled = canRestore,
          border = BorderStroke(1.dp, palette.borderGold),
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("page_restore_${page.id}"),
        ) {
          Icon(Icons.Outlined.RestoreFromTrash, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = strings.restorePageLabel, style = MaterialTheme.typography.labelMedium)
        }

        IconButton(
          onClick = onDeleteForever,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("page_purge_${page.id}"),
        ) {
          Icon(
            imageVector = Icons.Outlined.DeleteForever,
            contentDescription = strings.deleteForeverLabel,
            tint = if (palette.isDark) SankalanTokens.ErrorDark else SankalanTokens.ErrorLight,
          )
        }
      }
    }
  }
}
