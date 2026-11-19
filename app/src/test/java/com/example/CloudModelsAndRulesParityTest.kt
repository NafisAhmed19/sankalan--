package com.example

import com.example.core.cloud.FeedbackCategory
import com.example.core.cloud.FeedbackEntry
import com.example.core.cloud.UserProfile
import com.example.core.i18n.StringsBnBd
import com.example.core.i18n.StringsEn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudModelsAndRulesParityTest {

  @Test
  fun `feedback category parses ids and falls back safely`() {
    assertEquals(FeedbackCategory.GENERAL, FeedbackCategory.fromId("general"))
    assertEquals(FeedbackCategory.FEATURE, FeedbackCategory.fromId("feature"))
    assertEquals(FeedbackCategory.BUG, FeedbackCategory.fromId("bug"))
    assertEquals(FeedbackCategory.LOVE, FeedbackCategory.fromId("love"))
    assertEquals(FeedbackCategory.GENERAL, FeedbackCategory.fromId(null))
    assertEquals(FeedbackCategory.GENERAL, FeedbackCategory.fromId("unexpected"))
  }

  @Test
  fun `feedback entry clamps ratings and preserves rule defaults`() {
    val low = FeedbackEntry(rating = 0L)
    val mid = FeedbackEntry(rating = 4L)
    val high = FeedbackEntry(rating = 9L)
    assertEquals(1, low.safeRatingInt)
    assertEquals(4, mid.safeRatingInt)
    assertEquals(5, high.safeRatingInt)
    assertEquals("published", low.status)
    assertEquals("", low.id)
  }

  @Test
  fun `user profile defaults match google auth provider contract`() {
    val profile = UserProfile()
    assertEquals("google", profile.authProvider)
    assertEquals("", profile.userId)
    assertEquals("", profile.email)
    assertEquals("", profile.displayName)
  }

  @Test
  fun `bilingual auth and feedback strings are non-blank in en and bn`() {
    listOf(StringsEn, StringsBnBd).forEach { s ->
      assertTrue(s.authTitle.isNotBlank())
      assertTrue(s.authSignInWithGoogleButton.isNotBlank())
      assertTrue(s.authCreateAccountButton.isNotBlank())
      assertTrue(s.accountCardTitle.isNotBlank())
      assertTrue(s.feedbackSectionTitle.isNotBlank())
      assertTrue(s.feedbackCommentsTitle.isNotBlank())
      assertTrue(s.feedbackScrollHint.isNotBlank())
    }
  }
}
