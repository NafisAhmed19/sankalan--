package com.example.features.bookshelf

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.data.DiaryRepository
import com.example.core.i18n.LocalStrings
import com.example.core.i18n.formatNumber
import com.example.core.model.CoverStyle
import com.example.core.model.DiaryWithPages
import com.example.core.model.NumeralStyle
import com.example.core.model.SaveStatus
import com.example.ui.components.SankalanAtmosphericBackdrop
import com.example.ui.components.SaveStatusPill
import com.example.ui.theme.LocalSankalanPalette
import com.example.ui.theme.SankalanSpacing
import com.example.ui.theme.SankalanTokens

enum class BookshelfSortMode {
  RECENT,
  TITLE,
  PAGES,
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookshelfScreen(
  diaries: List<DiaryWithPages>,
  archivedCount: Int,
  numeralStyle: NumeralStyle,
  saveStatus: SaveStatus,
  onRetrySave: () -> Unit,
  onOpenCreateJournal: () -> Unit,
  onOpenArchive: () -> Unit,
  onInspectBook: (DiaryWithPages) -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current

  var searchQuery by rememberSaveable { mutableStateOf("") }
  var isShelfView by rememberSaveable { mutableStateOf(true) }
  var selectedCoverFilter by rememberSaveable { mutableStateOf<CoverStyle?>(null) }
  var sortMode by rememberSaveable { mutableStateOf(BookshelfSortMode.RECENT) }
  var selectedBookId by rememberSaveable { mutableStateOf<String?>(null) }

  val filteredBooks =
    remember(diaries, searchQuery, selectedCoverFilter, sortMode) {
      val normQuery = DiaryRepository.normalizeForSearch(searchQuery)
      diaries
        .filter { item ->
          val matchesStyle = selectedCoverFilter == null || item.diary.cover.style == selectedCoverFilter
          val matchesQuery =
            if (normQuery.isEmpty()) true
            else {
              val normTitle = DiaryRepository.normalizeForSearch(item.diary.title)
              val normDesc = DiaryRepository.normalizeForSearch(item.diary.description.orEmpty())
              normTitle.contains(normQuery) || normDesc.contains(normQuery)
            }
          matchesStyle && matchesQuery
        }
        .let { list ->
          when (sortMode) {
            BookshelfSortMode.RECENT -> list.sortedByDescending { it.diary.updatedAt }
            BookshelfSortMode.TITLE -> list.sortedBy { it.diary.title.lowercase() }
            BookshelfSortMode.PAGES -> list.sortedByDescending { it.pages.size }
          }
        }
    }

  SankalanAtmosphericBackdrop(modifier = modifier) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
      val isCompactHeader = maxWidth < 600.dp
      val shelfChunkSize = if (maxWidth >= 700.dp) 6 else 4
      val gridColumns = if (maxWidth >= 700.dp) 3 else 2

      LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 1080.dp),
        contentPadding = PaddingValues(horizontal = SankalanSpacing.lg, vertical = SankalanSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(SankalanSpacing.md),
      ) {
        // Header + Action Row (title always gets generous horizontal space so it never wraps vertically)
        item {
          Column(verticalArrangement = Arrangement.spacedBy(SankalanSpacing.sm)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = SankalanSpacing.sm)) {
                Text(
                  text = strings.myBookshelf,
                  style = MaterialTheme.typography.headlineMedium,
                  color = palette.textPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "${formatNumber(diaries.size, numeralStyle)} · ${strings.visibilityPrivate}",
                  style = MaterialTheme.typography.labelMedium,
                  color = palette.goldAccent,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              IconButton(
                onClick = onOpenArchive,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("bookshelf_open_archive"),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Archive,
                  contentDescription = "${strings.navArchive} (${formatNumber(archivedCount, numeralStyle)})",
                  tint = palette.goldAccent,
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              SaveStatusPill(status = saveStatus, onRetry = onRetrySave)
              Spacer(modifier = Modifier.weight(1f))
              Button(
                onClick = onOpenCreateJournal,
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = palette.goldAccent,
                    contentColor = SankalanTokens.Walnut900,
                  ),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("bookshelf_create_button"),
              ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = strings.createJournal,
                  style = MaterialTheme.typography.labelLarge,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }

        // Search Bar + 3D Shelf / Covers Grid Toggle
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = {
                Text(
                  text = strings.searchBooksHint,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              },
              leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = palette.goldAccent) },
              trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                  IconButton(onClick = { searchQuery = "" }) {
                    Icon(Icons.Outlined.Close, contentDescription = strings.closeLabel)
                  }
                }
              },
              singleLine = true,
              colors =
                OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = palette.goldAccent,
                  unfocusedBorderColor = palette.borderSubtle,
                  focusedTextColor = palette.textPrimary,
                  unfocusedTextColor = palette.textPrimary,
                ),
              modifier = Modifier.weight(1f).testTag("bookshelf_search_input"),
            )

            Surface(
              shape = MaterialTheme.shapes.small,
              color = palette.shelfSurface,
              border = BorderStroke(1.dp, palette.borderGold),
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = { isShelfView = true },
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("view_toggle_shelf"),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.ViewWeek,
                    contentDescription = strings.shelfViewLabel,
                    tint = if (isShelfView) palette.goldAccent else palette.textSecondary,
                  )
                }
                IconButton(
                  onClick = { isShelfView = false },
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("view_toggle_grid"),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.GridView,
                    contentDescription = strings.gridViewLabel,
                    tint = if (!isShelfView) palette.goldAccent else palette.textSecondary,
                  )
                }
              }
            }
          }
        }

        // Cover Style Filter & Sort Chips (FlowRow avoids edge truncation)
        item {
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(SankalanSpacing.xs),
          ) {
            val chipColors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = palette.goldAccent,
                selectedLabelColor = SankalanTokens.Walnut900,
                containerColor = palette.shelfSurface,
                labelColor = palette.textPrimary,
              )
            FilterChip(
              selected = selectedCoverFilter == null,
              onClick = { selectedCoverFilter = null },
              label = { Text(strings.filterAll, maxLines = 1) },
              colors = chipColors,
            )
            FilterChip(
              selected = selectedCoverFilter == CoverStyle.LUXURY,
              onClick = { selectedCoverFilter = CoverStyle.LUXURY },
              label = { Text(strings.coverLuxuryLabel, maxLines = 1) },
              colors = chipColors,
            )
            FilterChip(
              selected = selectedCoverFilter == CoverStyle.ARTISTIC,
              onClick = { selectedCoverFilter = CoverStyle.ARTISTIC },
              label = { Text(strings.coverArtisticLabel, maxLines = 1) },
              colors = chipColors,
            )
            FilterChip(
              selected = selectedCoverFilter == CoverStyle.MINIMAL,
              onClick = { selectedCoverFilter = CoverStyle.MINIMAL },
              label = { Text(strings.coverMinimalLabel, maxLines = 1) },
              colors = chipColors,
            )

            listOf(
              BookshelfSortMode.RECENT to strings.sortRecent,
              BookshelfSortMode.TITLE to strings.sortTitle,
              BookshelfSortMode.PAGES to strings.sortPages,
            ).forEach { (mode, label) ->
              FilterChip(
                selected = sortMode == mode,
                onClick = { sortMode = mode },
                label = { Text(label, maxLines = 1) },
                colors = chipColors,
              )
            }
          }
        }

        // Empty filter state or Multi-Tier 3D Bookshelf (3-4 visible shelves) / Grid
        val hasActiveFilter = searchQuery.isNotBlank() || selectedCoverFilter != null
        if (filteredBooks.isEmpty() && (hasActiveFilter || !isShelfView)) {
          item {
            Surface(
              shape = MaterialTheme.shapes.medium,
              color = palette.cardSurface,
              border = BorderStroke(1.dp, palette.borderGold),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(SankalanSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SankalanSpacing.md),
              ) {
                Icon(
                  imageVector = Icons.Outlined.AutoStories,
                  contentDescription = null,
                  tint = palette.goldAccent,
                  modifier = Modifier.size(42.dp),
                )
                Text(
                  text = strings.bookshelfEmptyTitle,
                  style = MaterialTheme.typography.titleMedium,
                  color = palette.textPrimary,
                  textAlign = TextAlign.Center,
                )
                Text(
                  text = strings.bookshelfEmptyBody,
                  style = MaterialTheme.typography.bodyMedium,
                  color = palette.textSecondary,
                  textAlign = TextAlign.Center,
                )
              }
            }
          }
        } else if (isShelfView) {
          val shelfRows = filteredBooks.chunked(shelfChunkSize)
          val minVisibleShelves = if (isCompactHeader) 4 else 3
          val totalShelves = maxOf(shelfRows.size, minVisibleShelves)
          items(totalShelves, key = { idx -> shelfRows.getOrNull(idx)?.firstOrNull()?.diary?.id ?: "empty_shelf_$idx" }) { idx ->
            val rowBooks = shelfRows.getOrNull(idx).orEmpty()
            WalnutShelfCabinetRow(
              books = rowBooks,
              selectedBookId = selectedBookId,
              numeralStyle = numeralStyle,
              onSelectBook = { book ->
                selectedBookId = book.diary.id
                onInspectBook(book)
              },
              emptyShelfLabel = strings.bookshelfEmptySlotCta,
              onEmptyShelfClick = onOpenCreateJournal,
              modifier = Modifier.testTag("bookshelf_walnut_row_$idx"),
            )
          }
        } else {
          val gridRows = filteredBooks.chunked(gridColumns)
          items(gridRows, key = { row -> row.first().diary.id }) { rowBooks ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.md),
            ) {
              rowBooks.forEach { book ->
                BookGridCard(
                  diaryWithPages = book,
                  numeralStyle = numeralStyle,
                  onClick = { onInspectBook(book) },
                  modifier = Modifier.weight(1f),
                )
              }
              repeat(gridColumns - rowBooks.size) {
                Spacer(modifier = Modifier.weight(1f))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun ArchiveScreen(
  archivedDiaries: List<DiaryWithPages>,
  numeralStyle: NumeralStyle,
  onInspectBook: (DiaryWithPages) -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  var selectedBookId by rememberSaveable { mutableStateOf<String?>(null) }

  SankalanAtmosphericBackdrop(modifier = modifier) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
      val shelfChunkSize = if (maxWidth >= 700.dp) 6 else 4

      LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 1080.dp),
        contentPadding = PaddingValues(horizontal = SankalanSpacing.lg, vertical = SankalanSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(SankalanSpacing.lg),
      ) {
        item {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = strings.navArchive,
              style = MaterialTheme.typography.headlineMedium,
              color = palette.textPrimary,
            )
            Text(
              text = strings.archiveEmptyBody,
              style = MaterialTheme.typography.bodyMedium,
              color = palette.textSecondary,
            )
          }
        }

        if (archivedDiaries.isEmpty()) {
          item {
            Surface(
              shape = MaterialTheme.shapes.medium,
              color = palette.cardSurface,
              border = BorderStroke(1.dp, palette.borderGold),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(SankalanSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SankalanSpacing.md),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Archive,
                  contentDescription = null,
                  tint = palette.goldAccent,
                  modifier = Modifier.size(44.dp),
                )
                Text(
                  text = strings.archiveEmptyTitle,
                  style = MaterialTheme.typography.titleMedium,
                  color = palette.textPrimary,
                  textAlign = TextAlign.Center,
                )
                Text(
                  text = strings.archiveEmptyBody,
                  style = MaterialTheme.typography.bodyMedium,
                  color = palette.textSecondary,
                  textAlign = TextAlign.Center,
                )
              }
            }
          }
        } else {
          val rows = archivedDiaries.chunked(shelfChunkSize)
          items(rows, key = { it.first().diary.id }) { rowBooks ->
            WalnutShelfCabinetRow(
              books = rowBooks,
              selectedBookId = selectedBookId,
              numeralStyle = numeralStyle,
              onSelectBook = { book ->
                selectedBookId = book.diary.id
                onInspectBook(book)
              },
            )
          }
        }
      }
    }
  }
}

@Composable
fun RenameDiaryDialog(
  initialTitle: String,
  initialDescription: String?,
  onDismiss: () -> Unit,
  onConfirm: (newTitle: String, newDescription: String?) -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  var title by remember { mutableStateOf(initialTitle) }
  var desc by remember { mutableStateOf(initialDescription.orEmpty()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = palette.cardSurface,
    titleContentColor = palette.textPrimary,
    textContentColor = palette.textPrimary,
    title = { Text(strings.renameDiaryTitle) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(SankalanSpacing.sm)) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text(strings.diaryTitleHint) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("rename_diary_title_input"),
        )
        OutlinedTextField(
          value = desc,
          onValueChange = { desc = it },
          label = { Text(strings.diaryDescHint) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(title.trim(), desc.trim().takeIf { it.isNotEmpty() }) },
        enabled = title.trim().isNotEmpty(),
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
      TextButton(onClick = onDismiss) {
        Text(strings.cancel, color = palette.textSecondary)
      }
    },
  )
}

@Composable
fun DeleteDiaryConfirmDialog(
  diaryTitle: String,
  onDismiss: () -> Unit,
  onConfirmDelete: () -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = palette.cardSurface,
    titleContentColor = palette.textPrimary,
    textContentColor = palette.textSecondary,
    title = { Text("${strings.deleteDiaryConfirmTitle} ($diaryTitle)") },
    text = { Text(strings.deleteDiaryConfirmBody) },
    confirmButton = {
      Button(
        onClick = onConfirmDelete,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = if (palette.isDark) SankalanTokens.ErrorDark else SankalanTokens.ErrorLight,
            contentColor = SankalanTokens.Cream,
          ),
        modifier = Modifier.testTag("confirm_delete_diary_button"),
      ) {
        Text(strings.delete)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(strings.cancel, color = palette.textSecondary)
      }
    },
  )
}
