package com.example.features.calendar

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.i18n.LocalStrings
import com.example.core.i18n.formatDigits
import com.example.core.i18n.formatNumber
import com.example.core.model.AppLanguage
import com.example.core.model.DiaryInvariants
import com.example.core.model.DiaryWithPages
import com.example.core.model.NumeralStyle
import com.example.core.model.Page
import com.example.ui.components.SankalanAtmosphericBackdrop
import com.example.ui.theme.LocalSankalanPalette
import com.example.ui.theme.SankalanSpacing
import com.example.ui.theme.SankalanTokens
import java.util.Calendar
import java.util.Locale

private val MONTH_NAMES_EN =
  listOf(
    "January",
    "February",
    "March",
    "April",
    "May",
    "June",
    "July",
    "August",
    "September",
    "October",
    "November",
    "December",
  )

private val MONTH_NAMES_BN =
  listOf(
    "জানুয়ারি",
    "ফেব্রুয়ারি",
    "মার্চ",
    "এপ্রিল",
    "মে",
    "জুন",
    "জুলাই",
    "আগস্ট",
    "সেপ্টেম্বর",
    "অক্টোবর",
    "নভেম্বর",
    "ডিসেম্বর",
  )

private val WEEKDAYS_EN = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
private val WEEKDAYS_BN = listOf("রবি", "সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র", "শনি")

fun formatIsoDate(year: Int, monthZeroBased: Int, dayOfMonth: Int): String =
  String.format(Locale.US, "%04d-%02d-%02d", year, monthZeroBased + 1, dayOfMonth)

@Composable
fun CalendarScreen(
  language: AppLanguage,
  numeralStyle: NumeralStyle,
  reminderEnabled: Boolean,
  reminderHour: Int,
  reminderMinute: Int,
  activeDiaries: List<DiaryWithPages>,
  datedPages: List<Page>,
  onOpenDiaryAtPage: (diaryWithPages: DiaryWithPages, pageIndex: Int) -> Unit,
  onAddDatedPage: (diaryId: String, dateIso: String, title: String?, bodyText: String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val context = LocalContext.current

  val initialCal = remember { Calendar.getInstance() }
  var displayYear by rememberSaveable { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
  var displayMonth by rememberSaveable { mutableIntStateOf(initialCal.get(Calendar.MONTH)) }
  var selectedDateIso by rememberSaveable {
    mutableStateOf(
      formatIsoDate(
        initialCal.get(Calendar.YEAR),
        initialCal.get(Calendar.MONTH),
        initialCal.get(Calendar.DAY_OF_MONTH),
      )
    )
  }
  var showAddDialog by remember { mutableStateOf(false) }
  var icsSavedBanner by remember { mutableStateOf(false) }

  val exportIcsLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/calendar")) { uri ->
      if (uri != null) {
        val ics =
          CalendarIcsExporter.buildIcs(
            diaries = activeDiaries,
            datedPages = datedPages,
            reminderEnabled = reminderEnabled,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute,
            includeBanglaTitle = language == AppLanguage.BN_BD,
          )
        runCatching {
          context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(ics.toByteArray(Charsets.UTF_8))
          }
          icsSavedBanner = true
        }
      }
    }

  val diaryLookup = remember(activeDiaries) { activeDiaries.associateBy { it.diary.id } }
  val activeDatedPages =
    remember(datedPages, diaryLookup) {
      datedPages.filter { it.diaryId in diaryLookup && !it.dateIso.isNullOrBlank() }
    }
  val pagesByDate = remember(activeDatedPages) { activeDatedPages.groupBy { it.dateIso.orEmpty() } }
  val selectedDayPages = pagesByDate[selectedDateIso].orEmpty()

  val monthTitle =
    if (language == AppLanguage.BN_BD) {
      "${MONTH_NAMES_BN[displayMonth]} ${formatNumber(displayYear, numeralStyle)}"
    } else {
      "${MONTH_NAMES_EN[displayMonth]} ${formatNumber(displayYear, numeralStyle)}"
    }
  val weekdays = if (language == AppLanguage.BN_BD) WEEKDAYS_BN else WEEKDAYS_EN

  val monthCells =
    remember(displayYear, displayMonth) {
      val cal = Calendar.getInstance().apply {
        clear()
        set(Calendar.YEAR, displayYear)
        set(Calendar.MONTH, displayMonth)
        set(Calendar.DAY_OF_MONTH, 1)
      }
      val leadingBlanks = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
      val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
      val list = MutableList<Int?>(leadingBlanks) { null }
      for (day in 1..daysInMonth) list.add(day)
      while (list.size % 7 != 0) list.add(null)
      list.chunked(7)
    }

  SankalanAtmosphericBackdrop(modifier = modifier) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
      val isCompactHeader = maxWidth < 560.dp

      LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 920.dp),
        contentPadding = PaddingValues(horizontal = SankalanSpacing.lg, vertical = SankalanSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(SankalanSpacing.lg),
      ) {
        // Header + Add Dated Page Button (responsive so title never squeezes vertically)
        item {
          Column(verticalArrangement = Arrangement.spacedBy(SankalanSpacing.sm)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = SankalanSpacing.sm)) {
                Text(
                  text = strings.calendarTitle,
                  style = MaterialTheme.typography.headlineMedium,
                  color = palette.textPrimary,
                )
                Text(
                  text = strings.calendarSubtitle,
                  style = MaterialTheme.typography.bodyMedium,
                  color = palette.textSecondary,
                )
              }

              if (!isCompactHeader && activeDiaries.isNotEmpty()) {
                Button(
                  onClick = { showAddDialog = true },
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = palette.goldAccent,
                      contentColor = SankalanTokens.Walnut900,
                    ),
                  shape = MaterialTheme.shapes.small,
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("calendar_add_entry_button"),
                ) {
                  Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(text = strings.calendarCreateDatedPage, style = MaterialTheme.typography.labelLarge)
                }
              }
            }

            if (isCompactHeader && activeDiaries.isNotEmpty()) {
              Button(
                onClick = { showAddDialog = true },
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = palette.goldAccent,
                    contentColor = SankalanTokens.Walnut900,
                  ),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("calendar_add_entry_button"),
              ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = strings.calendarCreateDatedPage,
                  style = MaterialTheme.typography.labelLarge,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }

            OutlinedButton(
              onClick = { exportIcsLauncher.launch("sankalan_journal_schedule.ics") },
              border = BorderStroke(1.dp, palette.borderGold),
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("calendar_export_ics_btn"),
            ) {
              Icon(Icons.Outlined.EventAvailable, contentDescription = null, tint = palette.goldAccent, modifier = Modifier.size(17.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(strings.reminderExportIcsLabel, style = MaterialTheme.typography.labelMedium, color = palette.textPrimary)
            }

            if (icsSavedBanner) {
              Text(
                text = strings.reminderIcsExportedMsg,
                style = MaterialTheme.typography.labelSmall,
                color = palette.goldAccent,
              )
            }
          }
        }

        // Monthly Calendar Card
        item {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = palette.cardSurface,
            border = BorderStroke(1.dp, palette.borderGold),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(SankalanSpacing.md),
              verticalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
            ) {
              // Month Header Controls
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                IconButton(
                  onClick = {
                    if (displayMonth == 0) {
                      displayMonth = 11
                      displayYear--
                    } else {
                      displayMonth--
                    }
                  },
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("calendar_prev_month"),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = strings.prevPageLabel,
                    tint = palette.goldAccent,
                  )
                }

                Text(
                  text = monthTitle,
                  style = MaterialTheme.typography.titleLarge,
                  color = palette.textPrimary,
                )

                IconButton(
                  onClick = {
                    if (displayMonth == 11) {
                      displayMonth = 0
                      displayYear++
                    } else {
                      displayMonth++
                    }
                  },
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("calendar_next_month"),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = strings.nextPageLabel,
                    tint = palette.goldAccent,
                  )
                }
              }

              // Weekday Labels
              Row(modifier = Modifier.fillMaxWidth()) {
                weekdays.forEach { dayName ->
                  Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.goldAccent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                  )
                }
              }

              // Month Grid Cells
              monthCells.forEach { week ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  week.forEach { dayNum ->
                    if (dayNum == null) {
                      Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                      val cellIso = formatIsoDate(displayYear, displayMonth, dayNum)
                      val isSelected = cellIso == selectedDateIso
                      val hasEntries = !pagesByDate[cellIso].isNullOrEmpty()

                      Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) palette.goldAccent else palette.shelfSurface.copy(alpha = 0.65f),
                        border =
                          if (hasEntries && !isSelected) BorderStroke(1.dp, palette.borderGold)
                          else null,
                        modifier =
                          Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(role = Role.Button) { selectedDateIso = cellIso },
                      ) {
                        Column(
                          modifier = Modifier.fillMaxSize().padding(4.dp),
                          horizontalAlignment = Alignment.CenterHorizontally,
                          verticalArrangement = Arrangement.Center,
                        ) {
                          Text(
                            text = formatNumber(dayNum, numeralStyle),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected || hasEntries) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) SankalanTokens.Walnut900 else palette.textPrimary,
                          )
                          if (hasEntries) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                              modifier =
                                Modifier
                                  .size(6.dp)
                                  .clip(CircleShape)
                                  .background(if (isSelected) SankalanTokens.Walnut900 else palette.goldAccent)
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // Selected Day Entries Header
        item {
          Text(
            text = formatDigits(selectedDateIso, numeralStyle),
            style = MaterialTheme.typography.titleMedium,
            color = palette.goldAccent,
          )
        }

        if (selectedDayPages.isEmpty()) {
          item {
            Surface(
              shape = MaterialTheme.shapes.medium,
              color = palette.shelfSurface,
              border = BorderStroke(1.dp, palette.borderSubtle),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = strings.calendarNoEntriesForDay,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
                modifier = Modifier.padding(SankalanSpacing.lg),
              )
            }
          }
        } else {
          items(selectedDayPages, key = { it.id }) { page ->
            val diaryBundle = diaryLookup[page.diaryId] ?: return@items
            val pageIndex = diaryBundle.pages.indexOfFirst { it.id == page.id }.coerceAtLeast(0)
            val excerpt =
              page.elements.firstOrNull { !it.hidden && !it.text.isNullOrBlank() }?.text.orEmpty()

            Surface(
              shape = MaterialTheme.shapes.medium,
              color = palette.cardSurface,
              border = BorderStroke(1.dp, palette.borderGold),
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clip(MaterialTheme.shapes.medium)
                  .clickable(role = Role.Button) { onOpenDiaryAtPage(diaryBundle, pageIndex) },
            ) {
              Row(
                modifier = Modifier.padding(SankalanSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.md),
              ) {
                Icon(
                  imageVector = Icons.Outlined.AutoStories,
                  contentDescription = null,
                  tint = palette.goldAccent,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Text(
                      text = diaryBundle.diary.title,
                      style = MaterialTheme.typography.labelSmall,
                      color = palette.goldAccent,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    if (page.pinned) {
                      Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = strings.calendarPinnedBadge,
                        tint = palette.terracotta,
                        modifier = Modifier.size(13.dp),
                      )
                    }
                  }
                  Text(
                    text = page.title ?: "${strings.pageNumberPrefix} ${formatNumber(pageIndex + 1, numeralStyle)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  if (excerpt.isNotBlank()) {
                    Text(
                      text = excerpt,
                      style = MaterialTheme.typography.bodyMedium,
                      color = palette.textSecondary,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (showAddDialog && activeDiaries.isNotEmpty()) {
    AddDatedPageDialog(
      dateIso = selectedDateIso,
      diaries = activeDiaries,
      numeralStyle = numeralStyle,
      onDismiss = { showAddDialog = false },
      onConfirm = { diaryId, title, body ->
        onAddDatedPage(diaryId, selectedDateIso, title, body)
        showAddDialog = false
      },
    )
  }
}

@Composable
private fun AddDatedPageDialog(
  dateIso: String,
  diaries: List<DiaryWithPages>,
  numeralStyle: NumeralStyle,
  onDismiss: () -> Unit,
  onConfirm: (diaryId: String, title: String?, body: String?) -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val eligibleDiaries = diaries.filter { DiaryInvariants.canAddPages(it.pages.size) }
  var selectedDiaryId by remember { mutableStateOf(eligibleDiaries.firstOrNull()?.diary?.id.orEmpty()) }
  var title by remember { mutableStateOf("") }
  var body by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = palette.cardSurface,
    titleContentColor = palette.textPrimary,
    textContentColor = palette.textPrimary,
    title = { Text("${strings.calendarCreateDatedPage} · ${formatDigits(dateIso, numeralStyle)}") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(SankalanSpacing.sm)) {
        Text(text = strings.calendarSelectDiary, style = MaterialTheme.typography.labelLarge)
        eligibleDiaries.take(4).forEach { bundle ->
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .clickable(role = Role.RadioButton) { selectedDiaryId = bundle.diary.id },
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(
              selected = selectedDiaryId == bundle.diary.id,
              onClick = { selectedDiaryId = bundle.diary.id },
              colors = RadioButtonDefaults.colors(selectedColor = palette.goldAccent),
            )
            Text(
              text = "${bundle.diary.title} (${formatNumber(bundle.pages.size, numeralStyle)}/${formatNumber(DiaryInvariants.MAX_PAGES, numeralStyle)})",
              style = MaterialTheme.typography.bodyMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text(strings.diaryTitleHint) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = body,
          onValueChange = { body = it },
          label = { Text(strings.editPage) },
          minLines = 3,
          maxLines = 5,
          modifier = Modifier.fillMaxWidth().testTag("calendar_entry_body_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm(
            selectedDiaryId,
            title.trim().takeIf { it.isNotEmpty() },
            body.trim().takeIf { it.isNotEmpty() },
          )
        },
        enabled = selectedDiaryId.isNotEmpty() && (title.isNotBlank() || body.isNotBlank()),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = palette.goldAccent,
            contentColor = SankalanTokens.Walnut900,
          ),
        modifier = Modifier.testTag("calendar_entry_submit"),
      ) {
        Text(strings.save)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(strings.cancel, color = palette.textSecondary)
      }
    },
  )
}
