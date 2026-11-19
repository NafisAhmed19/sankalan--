package com.example.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.SwitchAccount
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.cloud.FeedbackCategory
import com.example.core.cloud.FeedbackEntry
import com.example.core.cloud.UserProfile
import com.example.core.i18n.LocalStrings
import com.example.core.i18n.SankalanStrings
import com.example.features.auth.CloudUiState
import com.example.ui.theme.LocalSankalanPalette
import com.example.ui.theme.SankalanSpacing
import com.example.ui.theme.SankalanTokens
import java.text.DateFormat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountProfileCard(
  currentUserEmail: String,
  userProfileState: CloudUiState<UserProfile?>,
  isSavingProfile: Boolean,
  onUpdateDisplayName: (String) -> Unit,
  onSwitchAccount: () -> Unit,
  onSignOut: () -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val profile = (userProfileState as? CloudUiState.Success)?.data
  val resolvedEmail = profile?.email?.takeIf { it.isNotBlank() } ?: currentUserEmail
  val resolvedName = profile?.displayName?.takeIf { it.isNotBlank() } ?: ""

  var nameDraft by remember(resolvedName) { mutableStateOf(resolvedName) }

  LaunchedEffect(resolvedName) {
    if (nameDraft.isBlank() && resolvedName.isNotBlank()) {
      nameDraft = resolvedName
    }
  }

  Surface(
    shape = MaterialTheme.shapes.medium,
    color = palette.cardSurface,
    border = BorderStroke(1.dp, palette.borderGold),
    tonalElevation = 1.dp,
    modifier = Modifier.fillMaxWidth().testTag("settings_account_card"),
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
          imageVector = Icons.Outlined.AccountCircle,
          contentDescription = null,
          tint = palette.goldAccent,
          modifier = Modifier.size(22.dp),
        )
        Text(
          text = strings.accountCardTitle,
          style = MaterialTheme.typography.titleMedium,
          color = palette.textPrimary,
        )
      }

      if (resolvedEmail.isNotBlank()) {
        Surface(
          shape = MaterialTheme.shapes.small,
          color = palette.shelfSurface,
          border = BorderStroke(1.dp, palette.borderSubtle),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = SankalanSpacing.md, vertical = SankalanSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.AlternateEmail,
              contentDescription = null,
              tint = palette.goldAccent,
              modifier = Modifier.size(16.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = strings.accountGmailLabel,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
              )
              Text(
                text = resolvedEmail,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }

      OutlinedTextField(
        value = nameDraft,
        onValueChange = { nameDraft = it.take(60) },
        label = { Text(strings.accountNameLabel) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("settings_account_name_input"),
      )

      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(SankalanSpacing.xs),
      ) {
        Button(
          onClick = { onUpdateDisplayName(nameDraft.trim()) },
          enabled = !isSavingProfile && nameDraft.trim().isNotEmpty(),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = palette.goldAccent,
              contentColor = SankalanTokens.Walnut900,
            ),
          modifier = Modifier.minimumInteractiveComponentSize().testTag("settings_save_name_btn"),
        ) {
          if (isSavingProfile) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              color = SankalanTokens.Walnut900,
              strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.width(6.dp))
          }
          Text(strings.accountSaveNameBtn)
        }

        OutlinedButton(
          onClick = onSwitchAccount,
          border = BorderStroke(1.dp, palette.borderGold),
          modifier = Modifier.minimumInteractiveComponentSize().testTag("settings_switch_account_btn"),
        ) {
          Icon(
            imageVector = Icons.Outlined.SwitchAccount,
            contentDescription = null,
            tint = palette.goldAccent,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(strings.accountSwitchBtn, color = palette.textPrimary)
        }

        OutlinedButton(
          onClick = onSignOut,
          border = BorderStroke(1.dp, SankalanTokens.Terracotta.copy(alpha = 0.7f)),
          modifier = Modifier.minimumInteractiveComponentSize().testTag("settings_signout_btn"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.Logout,
            contentDescription = null,
            tint = SankalanTokens.Terracotta,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(strings.accountSignOutBtn, color = SankalanTokens.Terracotta)
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackCommunityCard(
  currentUserId: String,
  currentAuthorName: String,
  feedbackListState: CloudUiState<List<FeedbackEntry>>,
  isSubmittingFeedback: Boolean,
  statusBanner: Pair<Boolean, String>?,
  onSubmitFeedback: (authorName: String, message: String, category: FeedbackCategory, rating: Int, onDone: () -> Unit) -> Unit,
  onDeleteFeedback: (String) -> Unit,
  onDismissBanner: () -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current

  var feedbackDraft by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf(FeedbackCategory.GENERAL) }
  var selectedRating by remember { mutableIntStateOf(5) }
  val dateFormatter = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }

  Surface(
    shape = MaterialTheme.shapes.medium,
    color = palette.cardSurface,
    border = BorderStroke(1.dp, palette.borderSubtle),
    tonalElevation = 1.dp,
    modifier = Modifier.fillMaxWidth().testTag("settings_feedback_card"),
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
          imageVector = Icons.Outlined.Forum,
          contentDescription = null,
          tint = palette.goldAccent,
          modifier = Modifier.size(22.dp),
        )
        Text(
          text = strings.feedbackSectionTitle,
          style = MaterialTheme.typography.titleMedium,
          color = palette.textPrimary,
        )
      }

      Text(
        text = strings.feedbackSectionSubtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = palette.textSecondary,
      )

      if (statusBanner != null) {
        val (isSuccess, msg) = statusBanner
        val bannerColor = if (isSuccess) palette.goldAccent else SankalanTokens.Terracotta
        Surface(
          shape = MaterialTheme.shapes.small,
          color = bannerColor.copy(alpha = 0.14f),
          border = BorderStroke(1.dp, bannerColor.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth().testTag("feedback_status_banner"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = msg,
              style = MaterialTheme.typography.bodySmall,
              color = bannerColor,
              modifier = Modifier.weight(1f),
            )
            IconButton(
              onClick = onDismissBanner,
              modifier = Modifier.size(32.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = strings.closeLabel,
                tint = bannerColor,
                modifier = Modifier.size(16.dp),
              )
            }
          }
        }
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        FeedbackCategory.entries.forEach { category ->
          val selected = category == selectedCategory
          FilterChip(
            selected = selected,
            onClick = { selectedCategory = category },
            label = { Text(categoryLabel(category, strings)) },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = palette.goldAccent,
                selectedLabelColor = SankalanTokens.Walnut900,
              ),
            modifier = Modifier.testTag("feedback_cat_${category.id}"),
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        (1..5).forEach { star ->
          IconButton(
            onClick = { selectedRating = star },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("feedback_star_$star"),
          ) {
            Icon(
              imageVector = if (star <= selectedRating) Icons.Filled.Star else Icons.Outlined.StarBorder,
              contentDescription = "$star / 5",
              tint = if (star <= selectedRating) palette.goldAccent else palette.textSecondary,
              modifier = Modifier.size(22.dp),
            )
          }
        }
      }

      OutlinedTextField(
        value = feedbackDraft,
        onValueChange = { feedbackDraft = it.take(500) },
        placeholder = { Text(strings.feedbackInputHint) },
        minLines = 2,
        maxLines = 3,
        supportingText = {
          Text(
            text = "${feedbackDraft.length}/500",
            style = MaterialTheme.typography.labelSmall,
          )
        },
        modifier = Modifier.fillMaxWidth().testTag("feedback_input"),
      )

      Button(
        onClick = {
          val msg = feedbackDraft.trim()
          if (msg.isNotEmpty()) {
            onSubmitFeedback(currentAuthorName, msg, selectedCategory, selectedRating) {
              feedbackDraft = ""
            }
          }
        },
        enabled = !isSubmittingFeedback && feedbackDraft.trim().isNotEmpty(),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = palette.goldAccent,
            contentColor = SankalanTokens.Walnut900,
          ),
        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("feedback_submit_btn"),
      ) {
        if (isSubmittingFeedback) {
          CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = SankalanTokens.Walnut900,
            strokeWidth = 2.dp,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(strings.feedbackSubmittingBtn)
        } else {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.Send,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(strings.feedbackSubmitBtn)
        }
      }

      HorizontalDivider(color = palette.borderSubtle)

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = strings.feedbackCommentsTitle,
          style = MaterialTheme.typography.labelLarge,
          color = palette.textPrimary,
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.UnfoldMore,
            contentDescription = null,
            tint = palette.goldAccent,
            modifier = Modifier.size(15.dp),
          )
          Text(
            text = strings.feedbackScrollHint,
            style = MaterialTheme.typography.labelSmall,
            color = palette.goldAccent,
          )
        }
      }

      // Bounded 2-comment-tall scrollable viewport directly underneath the feedback bar
      Surface(
        shape = MaterialTheme.shapes.small,
        color = palette.shelfSurface,
        border = BorderStroke(1.dp, palette.borderSubtle),
        modifier = Modifier.fillMaxWidth().testTag("feedback_comments_viewport"),
      ) {
        when (feedbackListState) {
          is CloudUiState.Loading -> {
            Box(
              modifier = Modifier.fillMaxWidth().height(120.dp),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator(
                color = palette.goldAccent,
                modifier = Modifier.size(28.dp),
              )
            }
          }
          is CloudUiState.Error -> {
            Text(
              text = feedbackListState.message,
              style = MaterialTheme.typography.bodySmall,
              color = SankalanTokens.Terracotta,
              modifier = Modifier.padding(SankalanSpacing.md),
            )
          }
          is CloudUiState.Success -> {
            val entries = feedbackListState.data
            if (entries.isEmpty()) {
              Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = 104.dp).padding(SankalanSpacing.md),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = strings.feedbackEmptyLabel,
                  style = MaterialTheme.typography.bodySmall,
                  color = palette.textSecondary,
                )
              }
            } else {
              Column(
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .height(216.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(SankalanSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
              ) {
                entries.forEach { entry ->
                  val isMine = entry.userId == currentUserId
                  val dateLabel = entry.createdAt?.toDate()?.let { dateFormatter.format(it) }.orEmpty()
                  Surface(
                    shape = MaterialTheme.shapes.small,
                    color = palette.cardElevated.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, if (isMine) palette.borderGold else palette.borderSubtle),
                    modifier = Modifier.fillMaxWidth().testTag("feedback_comment_item_${entry.id}"),
                  ) {
                    Column(
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                      verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                      ) {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(6.dp),
                          modifier = Modifier.weight(1f),
                        ) {
                          Text(
                            text = entry.authorName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                          )
                          if (isMine) {
                            Surface(
                              shape = MaterialTheme.shapes.extraSmall,
                              color = palette.goldAccent.copy(alpha = 0.2f),
                            ) {
                              Text(
                                text = strings.feedbackYouBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.goldAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                              )
                            }
                          }
                        }
                        if (isMine && entry.id.isNotBlank()) {
                          IconButton(
                            onClick = { onDeleteFeedback(entry.id) },
                            modifier = Modifier.size(32.dp).testTag("feedback_delete_${entry.id}"),
                          ) {
                            Icon(
                              imageVector = Icons.Outlined.DeleteOutline,
                              contentDescription = strings.feedbackDeleteBtn,
                              tint = SankalanTokens.Terracotta,
                              modifier = Modifier.size(16.dp),
                            )
                          }
                        }
                      }

                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                      ) {
                        Text(
                          text = categoryLabel(entry.safeCategory, strings),
                          style = MaterialTheme.typography.labelSmall,
                          color = palette.goldAccent,
                        )
                        Text(
                          text = "★ ${entry.safeRatingInt}/5",
                          style = MaterialTheme.typography.labelSmall,
                          color = palette.goldAccent,
                        )
                        if (dateLabel.isNotBlank()) {
                          Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                          )
                        }
                      }

                      Text(
                        text = entry.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textPrimary,
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
}

private fun categoryLabel(category: FeedbackCategory, strings: SankalanStrings): String =
  when (category) {
    FeedbackCategory.GENERAL -> strings.feedbackCatGeneral
    FeedbackCategory.FEATURE -> strings.feedbackCatFeature
    FeedbackCategory.BUG -> strings.feedbackCatBug
    FeedbackCategory.LOVE -> strings.feedbackCatLove
  }
