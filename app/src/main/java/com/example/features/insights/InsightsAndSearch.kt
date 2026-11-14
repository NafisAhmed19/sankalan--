package com.example.features.insights

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.data.DiaryRepository
import com.example.core.i18n.LocalStrings
import com.example.core.i18n.formatDigits
import com.example.core.i18n.formatNumber
import com.example.core.model.AppLanguage
import com.example.core.model.DiaryWithPages
import com.example.core.model.NumeralStyle
import com.example.core.model.Page
import com.example.ui.theme.LocalSankalanPalette
import com.example.ui.theme.NotoSerifBengaliFontFamily
import com.example.ui.theme.SankalanSpacing
import com.example.ui.theme.SankalanTokens
import java.util.Calendar

data class MoodCountStat(
  val moodKey: String,
  val count: Int,
  val fraction: Float,
)

data class MoodInsightsSummary(
  val totalLoggedPages: Int,
  val activeDaysCount: Int,
  val moodBreakdown: List<MoodCountStat>,
)

object MoodTrackerAggregator {
  fun summarize(diaries: List<DiaryWithPages>): MoodInsightsSummary {
    val activePages = diaries.flatMap { it.pages }.filter { !it.isDeleted }
    val loggedPages =
      activePages.filter { page ->
        !page.title.isNullOrBlank() ||
          !page.mood.isNullOrBlank() ||
          page.elements.any { !it.hidden && (!it.text.isNullOrBlank() || !it.blobRef.isNullOrBlank() || it.type == "stroke") }
      }
    val uniqueDates = activePages.mapNotNull { it.dateIso?.takeIf(String::isNotBlank) }.toSet()
    val moodCounts = mutableMapOf<String, Int>()
    activePages.forEach { page ->
      val m = page.mood?.trim()?.takeIf { it.isNotEmpty() }
      if (m != null) {
        moodCounts[m] = (moodCounts[m] ?: 0) + 1
      }
    }
    val totalMoodTags = moodCounts.values.sum().coerceAtLeast(1)
    val sorted =
      moodCounts.entries
        .sortedByDescending { it.value }
        .map { (key, count) ->
          MoodCountStat(
            moodKey = key,
            count = count,
            fraction = count.toFloat() / totalMoodTags.toFloat(),
          )
        }
    return MoodInsightsSummary(
      totalLoggedPages = loggedPages.size,
      activeDaysCount = uniqueDates.size,
      moodBreakdown = sorted,
    )
  }

  fun localizedMoodLabel(rawMood: String, language: AppLanguage): String {
    if (language != AppLanguage.BN_BD) return rawMood
    return when (rawMood.lowercase()) {
      "peaceful" -> "শান্ত (Peaceful)"
      "joyful" -> "আনন্দিত (Joyful)"
      "reflective" -> "ভাবুক (Reflective)"
      "grateful" -> "কৃতজ্ঞ (Grateful)"
      "focused" -> "মনোযোগী (Focused)"
      "nostalgic" -> "স্মৃতিকাতর (Nostalgic)"
      else -> rawMood
    }
  }
}

data class DailyPrompt(
  val id: String,
  val quoteEn: String,
  val quoteBn: String,
  val promptEn: String,
  val promptBn: String,
)

object DailyPromptCatalog {
  val prompts: List<DailyPrompt> =
    listOf(
      DailyPrompt(
        id = "p_01",
        quoteEn = "“Keep what is true and tender inside pages only you unlock.”",
        quoteBn = "“যা কিছু সুন্দর, সত্য ও কোমল, তা স্মৃতির পাতায় যত্ন করে রাখো।”",
        promptEn = "What small ordinary moment today felt unexpectedly peaceful?",
        promptBn = "আজকের সাধারণ কোনো মুহূর্ত কি অজান্তেই মনে প্রশান্তি এনে দিয়েছে?",
      ),
      DailyPrompt(
        id = "p_02",
        quoteEn = "“Every ordinary afternoon carries a story worth keeping.”",
        quoteBn = "“প্রতিটি বিকেলের আলোতেই একটি গল্প লুকিয়ে থাকে।”",
        promptEn = "Describe the sounds, light, or cup of tea around you right now.",
        promptBn = "এই মুহূর্তে তোমার চারপাশের আলো, শব্দ কিংবা এক কাপ চায়ের গল্প লেখো।",
      ),
      DailyPrompt(
        id = "p_03",
        quoteEn = "“Gratitude turns fleeting hours into heirloom keepsakes.”",
        quoteBn = "“কৃতজ্ঞতা ক্ষণস্থায়ী সময়কেও অমূল্য স্মারকে পরিণত করে।”",
        promptEn = "List three quiet blessings or kind gestures from this week.",
        promptBn = "এই সপ্তাহের তিনটি ছোট প্রাপ্তি বা প্রিয় মানুষের ভালোবাসার কথা লেখো।",
      ),
      DailyPrompt(
        id = "p_04",
        quoteEn = "“Letters to your future self are bridges across seasons.”",
        quoteBn = "“নিজের কাছে লেখা চিঠি সময়ের দুই প্রান্তকে জুড়ে দেয়।”",
        promptEn = "If you read today's page five years from now, what should you remember?",
        promptBn = "পাঁচ বছর পর আজকের পাতাটি খুললে কোন কথাটি তুমি অবশ্যই মনে রাখতে চাইবে?",
      ),
      DailyPrompt(
        id = "p_05",
        quoteEn = "“A single line written honestly outweighs a hundred unread pages.”",
        quoteBn = "“সততার সাথে লেখা একটি পঙ্‌ক্তিও অসংখ্য পাতার চেয়ে মূল্যবান।”",
        promptEn = "What has been waiting on your mind that you haven't said aloud yet?",
        promptBn = "মনের ভেতর জমে থাকা এমন কী কথা আছে যা এখনো কাউকে বলা হয়নি?",
      ),
      DailyPrompt(
        id = "p_06",
        quoteEn = "“Books and memories both live longer when revisited slowly.”",
        quoteBn = "“বই আর স্মৃতি—দুটিই ধীরে পড়লে আরও দীর্ঘায়ু হয়।”",
        promptEn = "Write down a line from a book, poem, or song that stayed with you lately.",
        promptBn = "সম্প্রতি পড়া কোনো বই, কবিতা বা গানের একটি প্রিয় লাইন এখানে টুকে রাখো।",
      ),
      DailyPrompt(
        id = "p_07",
        quoteEn = "“Seasons shift like parchment pages turned at dusk.”",
        quoteBn = "“গোধূলিতে উল্টানো ডায়েরির পাতার মতোই ঋতুগুলো বদলে যায়।”",
        promptEn = "What memory from an earlier season in your life feels vivid tonight?",
        promptBn = "অতীতের কোন বর্ষা, শরৎ বা শীতের বিকেল আজ হঠাৎ মনে পড়ে গেল?",
      ),
    )

  fun forToday(offset: Int = 0): DailyPrompt {
    val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    val index = Math.floorMod(dayOfYear + offset, prompts.size)
    return prompts[index]
  }
}

sealed interface GlobalSearchHit {
  data class DiaryHit(val bundle: DiaryWithPages) : GlobalSearchHit
  data class PageHit(val bundle: DiaryWithPages, val page: Page, val pageIndex: Int, val snippet: String) : GlobalSearchHit
}

object GlobalLibrarySearch {
  fun search(diaries: List<DiaryWithPages>, rawQuery: String): List<GlobalSearchHit> {
    val norm = DiaryRepository.normalizeForSearch(rawQuery)
    if (norm.isEmpty()) return emptyList()
    val results = mutableListOf<GlobalSearchHit>()
    diaries.forEach { bundle ->
      val titleMatch = DiaryRepository.normalizeForSearch(bundle.diary.title).contains(norm)
      val descMatch = DiaryRepository.normalizeForSearch(bundle.diary.description.orEmpty()).contains(norm)
      if (titleMatch || descMatch) {
        results.add(GlobalSearchHit.DiaryHit(bundle))
      }
      bundle.pages.filter { !it.isDeleted }.sortedBy { it.order }.forEachIndexed { idx, page ->
        val pTitle = page.title.orEmpty()
        val pDate = page.dateIso.orEmpty()
        val pMood = page.mood.orEmpty()
        val textContent =
          page.elements
            .filter { !it.hidden && !it.text.isNullOrBlank() }
            .joinToString(" ") { it.text.orEmpty() }
        val haystack = "$pTitle $pDate $pMood $textContent"
        if (DiaryRepository.normalizeForSearch(haystack).contains(norm)) {
          val snippet = textContent.trim().ifEmpty { pTitle.ifEmpty { pDate } }
          results.add(GlobalSearchHit.PageHit(bundle = bundle, page = page, pageIndex = idx, snippet = snippet))
        }
      }
    }
    return results
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoodAndPromptDashboardCard(
  language: AppLanguage,
  numeralStyle: NumeralStyle,
  activeDiaries: List<DiaryWithPages>,
  onWritePromptEntry: (promptTitle: String, promptQuestion: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  var promptOffset by rememberSaveable { mutableIntStateOf(0) }
  val prompt = remember(promptOffset) { DailyPromptCatalog.forToday(promptOffset) }
  val summary = remember(activeDiaries) { MoodTrackerAggregator.summarize(activeDiaries) }
  val isBn = language == AppLanguage.BN_BD

  Surface(
    shape = MaterialTheme.shapes.medium,
    color = palette.cardSurface,
    border = BorderStroke(1.dp, palette.borderGold),
    modifier = modifier.fillMaxWidth().testTag("home_insights_card"),
  ) {
    Column(
      modifier = Modifier.padding(SankalanSpacing.lg),
      verticalArrangement = Arrangement.spacedBy(SankalanSpacing.md),
    ) {
      // Daily Prompt Header + Cycle
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = palette.goldAccent, modifier = Modifier.size(18.dp))
          Text(
            text = strings.dailyPromptTitle,
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        OutlinedButton(
          onClick = { promptOffset++ },
          border = BorderStroke(1.dp, palette.borderSubtle),
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("daily_prompt_next_btn"),
        ) {
          Icon(Icons.Outlined.Refresh, contentDescription = null, tint = palette.goldAccent, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(strings.dailyPromptNext, style = MaterialTheme.typography.labelSmall, color = palette.textPrimary)
        }
      }

      Text(
        text = if (isBn) prompt.quoteBn else prompt.quoteEn,
        fontFamily = if (isBn) NotoSerifBengaliFontFamily else MaterialTheme.typography.bodyLarge.fontFamily,
        fontStyle = FontStyle.Italic,
        style = MaterialTheme.typography.bodyMedium,
        color = palette.goldAccent,
      )

      Text(
        text = if (isBn) prompt.promptBn else prompt.promptEn,
        style = MaterialTheme.typography.bodyLarge,
        color = palette.textPrimary,
      )

      if (activeDiaries.isNotEmpty()) {
        Button(
          onClick = {
            val q = if (isBn) prompt.promptBn else prompt.promptEn
            val t = if (isBn) strings.dailyPromptTitle else strings.dailyPromptTitle
            onWritePromptEntry(t, q)
          },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = palette.goldAccent,
              contentColor = SankalanTokens.Walnut900,
            ),
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("daily_prompt_write_btn"),
        ) {
          Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(strings.dailyPromptWriteNow, style = MaterialTheme.typography.labelLarge)
        }
      }

      HorizontalDivider(color = palette.borderSubtle)

      // Mood Tracker Summary
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(Icons.Outlined.Mood, contentDescription = null, tint = palette.goldAccent, modifier = Modifier.size(18.dp))
          Text(text = strings.moodTrackerTitle, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
        }
        Text(
          text = "${strings.streakEntriesLabel}: ${formatNumber(summary.totalLoggedPages, numeralStyle)}",
          style = MaterialTheme.typography.labelMedium,
          color = palette.goldAccent,
        )
      }

      if (summary.moodBreakdown.isEmpty()) {
        Text(
          text = strings.moodTrackerEmpty,
          style = MaterialTheme.typography.bodySmall,
          color = palette.textSecondary,
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          summary.moodBreakdown.take(5).forEachIndexed { idx, stat ->
            val barColor =
              when (idx % 4) {
                0 -> palette.goldAccent
                1 -> SankalanTokens.Sage
                2 -> SankalanTokens.Terracotta
                else -> SankalanTokens.Leather
              }
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = MoodTrackerAggregator.localizedMoodLabel(stat.moodKey, language),
                style = MaterialTheme.typography.labelSmall,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(126.dp),
              )
              Box(
                modifier =
                  Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(palette.shelfSurface),
              ) {
                Box(
                  modifier =
                    Modifier
                      .fillMaxHeight()
                      .fillMaxWidth(stat.fraction.coerceIn(0.08f, 1f))
                      .clip(RoundedCornerShape(6.dp))
                      .background(barColor),
                )
              }
              Text(
                text = formatNumber(stat.count, numeralStyle),
                style = MaterialTheme.typography.labelSmall,
                color = palette.goldAccent,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun GlobalSearchDialog(
  diaries: List<DiaryWithPages>,
  numeralStyle: NumeralStyle,
  onDismiss: () -> Unit,
  onSelectDiary: (DiaryWithPages) -> Unit,
  onSelectPage: (DiaryWithPages, Int) -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  var query by rememberSaveable { mutableStateOf("") }
  val hits = remember(diaries, query) { GlobalLibrarySearch.search(diaries, query) }
  val diaryHits = hits.filterIsInstance<GlobalSearchHit.DiaryHit>()
  val pageHits = hits.filterIsInstance<GlobalSearchHit.PageHit>()

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = MaterialTheme.shapes.large,
      color = palette.cardSurface,
      border = BorderStroke(1.dp, palette.borderGold),
      modifier = Modifier.fillMaxWidth(0.94f).widthIn(max = 640.dp).heightIn(max = 620.dp),
    ) {
      Column(
        modifier = Modifier.padding(SankalanSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SankalanSpacing.md),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(strings.globalSearchTitle, style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
          IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
            Icon(Icons.Outlined.Close, contentDescription = strings.closeLabel, tint = palette.textSecondary)
          }
        }

        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          placeholder = { Text(strings.globalSearchHint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
          leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = palette.goldAccent) },
          trailingIcon = {
            if (query.isNotEmpty()) {
              IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, contentDescription = null) }
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
          modifier = Modifier.fillMaxWidth().testTag("global_search_input"),
        )

        if (query.isNotBlank() && hits.isEmpty()) {
          Text(
            text = strings.globalSearchNoResults,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
            modifier = Modifier.padding(vertical = SankalanSpacing.md),
          )
        } else {
          LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
          ) {
            if (diaryHits.isNotEmpty()) {
              item {
                Text(strings.globalSearchDiariesHeader, style = MaterialTheme.typography.labelLarge, color = palette.goldAccent)
              }
              items(diaryHits, key = { "d_${it.bundle.diary.id}" }) { hit ->
                Surface(
                  shape = MaterialTheme.shapes.small,
                  color = palette.shelfSurface,
                  border = BorderStroke(1.dp, palette.borderSubtle),
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .clip(MaterialTheme.shapes.small)
                      .clickable(role = Role.Button) {
                        onSelectDiary(hit.bundle)
                        onDismiss()
                      },
                ) {
                  Row(
                    modifier = Modifier.padding(SankalanSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Icon(Icons.Outlined.AutoStories, contentDescription = null, tint = palette.goldAccent)
                    Column(modifier = Modifier.weight(1f)) {
                      Text(hit.bundle.diary.title, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
                      if (!hit.bundle.diary.description.isNullOrBlank()) {
                        Text(hit.bundle.diary.description, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                      }
                    }
                    Text("${formatNumber(hit.bundle.pages.size, numeralStyle)} ${strings.pagesUnit}", style = MaterialTheme.typography.labelSmall, color = palette.goldAccent)
                  }
                }
              }
            }

            if (pageHits.isNotEmpty()) {
              item {
                Text(strings.globalSearchPagesHeader, style = MaterialTheme.typography.labelLarge, color = palette.goldAccent)
              }
              items(pageHits, key = { "p_${it.page.id}" }) { hit ->
                Surface(
                  shape = MaterialTheme.shapes.small,
                  color = palette.shelfSurface,
                  border = BorderStroke(1.dp, palette.borderGold.copy(alpha = 0.6f)),
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .clip(MaterialTheme.shapes.small)
                      .clickable(role = Role.Button) {
                        onSelectPage(hit.bundle, hit.pageIndex)
                        onDismiss()
                      },
                ) {
                  Row(
                    modifier = Modifier.padding(SankalanSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Icon(Icons.Outlined.Description, contentDescription = null, tint = palette.goldAccent)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                      Text(
                        text = "${hit.bundle.diary.title} · ${strings.pageNumberPrefix} ${formatNumber(hit.pageIndex + 1, numeralStyle)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.goldAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      if (!hit.page.title.isNullOrBlank()) {
                        Text(hit.page.title, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                      }
                      if (hit.snippet.isNotBlank()) {
                        Text(hit.snippet, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                      }
                      if (!hit.page.dateIso.isNullOrBlank()) {
                        Text(formatDigits(hit.page.dateIso, numeralStyle), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
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
  }
}
