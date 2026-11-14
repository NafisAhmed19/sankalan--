package com.example

import com.example.core.i18n.SankalanStrings
import com.example.core.i18n.StringsBnBd
import com.example.core.i18n.StringsEn
import com.example.core.i18n.formatDigits
import com.example.core.i18n.formatNumber
import com.example.core.i18n.formatPercent
import com.example.core.i18n.stringsFor
import com.example.core.model.AppLanguage
import com.example.core.model.AppSettings
import com.example.core.model.AppThemeMode
import com.example.core.model.NumeralStyle
import com.example.core.model.PhaseFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class I18nAndSettingsTest {

  @Test
  fun allEnglishAndBanglaStringsAreNonBlank() {
    val getters = SankalanStrings::class.java.methods.filter { it.name.startsWith("get") && it.parameterCount == 0 && it.returnType == String::class.java }
    assertTrue("Expected string getters in SankalanStrings", getters.isNotEmpty())
    for (getter in getters) {
      val enVal = getter.invoke(StringsEn) as String
      val bnVal = getter.invoke(StringsBnBd) as String
      assertTrue("English key ${getter.name} is blank", enVal.isNotBlank())
      assertTrue("Bangla key ${getter.name} is blank", bnVal.isNotBlank())
    }
  }

  @Test
  fun brandSpellingAndApprovedTaglinesMatchDecisionRecord() {
    assertEquals("Sankalan-t", StringsEn.brandName)
    assertEquals("সংকলন-ৎ", StringsBnBd.brandName)
    assertEquals("Sankalan-ৎ", StringsEn.brandLogoWordmark)
    assertEquals("Sankalan-ৎ", StringsBnBd.brandLogoWordmark)
    assertEquals("With Every Imprint of Your Life's Journey.", StringsEn.tagline)
    assertEquals("জীবন চলার প্রতিটি ছাপের সাথে।", StringsBnBd.tagline)
    assertEquals("আপনার জীবনের ছাপের সাথে", StringsBnBd.homeHeroTagline)
    assertEquals("NRST.tech", StringsEn.ownerCompanyName)
    assertEquals("NRST.tech", StringsBnBd.ownerCompanyName)
    assertEquals("Nafis Ahmed", StringsEn.ownerFounderName)
    assertTrue(StringsBnBd.ownerFounderName.contains("Nafis Ahmed"))
    assertEquals("nafisahmeddn@gmail.com", StringsEn.ownerEmailAddress)
    assertEquals("nafisahmeddn@gmail.com", StringsBnBd.ownerEmailAddress)
    assertEquals(StringsEn, stringsFor(AppLanguage.EN))
    assertEquals(StringsBnBd, stringsFor(AppLanguage.BN_BD))
  }

  @Test
  fun defaultSettingsFollowDecisionRecord() {
    val defaults = AppSettings()
    assertEquals(AppThemeMode.DARK, defaults.themeMode) // D-06 Classic Dark default
    assertEquals(NumeralStyle.LATIN, defaults.numeralStyle) // §11 default 0-9
    assertTrue(PhaseFlags.PHASE_1_ALL_LOCAL_FEATURES_FREE) // D-29
    assertFalse(PhaseFlags.PHASE_2_COMMUNITY_EXPLORE_ENABLED) // §4 hidden until Phase 2
  }

  @Test
  fun numeralFormattingConvertsDigitsAccurately() {
    assertEquals("200", formatNumber(200, NumeralStyle.LATIN))
    assertEquals("২০০", formatNumber(200, NumeralStyle.BENGALI))
    assertEquals("100%", formatPercent(1.0f, NumeralStyle.LATIN))
    assertEquals("১০০%", formatPercent(1.0f, NumeralStyle.BENGALI))
    assertEquals("৫–২০০", formatDigits("5–200", NumeralStyle.BENGALI))
  }
}
