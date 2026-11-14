package com.example.features.welcome

import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.i18n.LocalStrings
import com.example.core.model.AppLanguage
import com.example.ui.theme.NotoSerifBengaliFontFamily
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.SankalanTokens
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Step 2: Glass / Water Typography Welcome Screen (Specification §5.4 & D-08, D-09).
 * - Shown at launch (~2.5-3.5s), immediately skippable.
 * - Gentle vertical float, broad slow water shimmer, flipped reflection beneath baseline.
 * - Water-drop ripple spreading from user's tap point before entering Home.
 * - Strictly honours reducedMotion with a static, crisp high-contrast presentation.
 */
@Composable
fun WelcomeScreen(
  currentLanguage: AppLanguage,
  showLanguagePrompt: Boolean,
  reducedMotionSetting: Boolean,
  onSelectLanguage: (AppLanguage) -> Unit,
  onEnterHome: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalStrings.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val systemAnimatorDisabled = remember(context) {
    runCatching {
      Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
      ) == 0f
    }.getOrDefault(false)
  }
  val reduceMotion = reducedMotionSetting || systemAnimatorDisabled

  var bgRevealed by remember { mutableStateOf(reduceMotion) }
  var wordmarkRevealed by remember { mutableStateOf(reduceMotion) }
  var taglineRevealed by remember { mutableStateOf(reduceMotion) }
  var interactionPausedAutoAdvance by remember { mutableStateOf(false) }
  var isExiting by remember { mutableStateOf(false) }

  var tapPoint by remember { mutableStateOf<Offset?>(null) }
  val tapRippleProgress = remember { Animatable(0f) }

  // Staggered intro sequence matching §5.4 timing (bg 600-900ms, wordmark 700-1100ms, tagline 400-700ms)
  LaunchedEffect(reduceMotion) {
    if (reduceMotion) {
      bgRevealed = true
      wordmarkRevealed = true
      taglineRevealed = true
    } else {
      bgRevealed = true
      delay(280)
      wordmarkRevealed = true
      delay(650)
      taglineRevealed = true
    }
  }

  // Auto-advance after ~3.2s unless user paused by interacting with language choice or skipped
  LaunchedEffect(interactionPausedAutoAdvance, isExiting, showLanguagePrompt) {
    if (isExiting || interactionPausedAutoAdvance) return@LaunchedEffect
    val autoWaitMs = if (showLanguagePrompt) 4500L else 3100L
    delay(autoWaitMs)
    if (!isExiting && !interactionPausedAutoAdvance) {
      isExiting = true
      onEnterHome()
    }
  }

  val bgAlpha by animateFloatAsState(
    targetValue = if (bgRevealed) 1f else 0f,
    animationSpec = tween(durationMillis = if (reduceMotion) 0 else 750),
    label = "welcomeBgAlpha",
  )

  val wordmarkAlpha by animateFloatAsState(
    targetValue = if (wordmarkRevealed) 1f else 0f,
    animationSpec = tween(durationMillis = if (reduceMotion) 0 else 850, easing = FastOutSlowInEasing),
    label = "wordmarkAlpha",
  )

  val infiniteTransition = rememberInfiniteTransition(label = "waterMotion")
  val waterPhase by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = (2f * PI).toFloat(),
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 4800, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "waterPhase",
    )

  val shimmerProgress by
    infiniteTransition.animateFloat(
      initialValue = -0.35f,
      targetValue = 1.35f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 5600, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "shimmerProgress",
    )

  val floatOffsetPx = if (reduceMotion) 0f else (sin(waterPhase.toDouble()) * 5.5).toFloat()

  fun triggerTapEnter(offset: Offset) {
    if (isExiting) return
    isExiting = true
    if (reduceMotion) {
      onEnterHome()
      return
    }
    tapPoint = offset
    scope.launch {
      tapRippleProgress.snapTo(0f)
      tapRippleProgress.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
      )
      onEnterHome()
    }
  }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(SankalanTokens.Walnut900)
        .pointerInput(reduceMotion) {
          detectTapGestures { offset -> triggerTapEnter(offset) }
        }
        .testTag("welcome_screen")
  ) {
    // Soft atmospheric library & reflective water backdrop
    Image(
      painter = painterResource(id = R.drawable.img_welcome_backdrop),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier =
        Modifier
          .fillMaxSize()
          .alpha(bgAlpha * 0.62f),
    )

    // Deep espresso vignette so typography remains crisp and legible on all phone widths
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors =
                listOf(
                  SankalanTokens.Walnut900.copy(alpha = 0.72f),
                  SankalanTokens.Walnut900.copy(alpha = 0.45f),
                  SankalanTokens.Walnut900.copy(alpha = 0.88f),
                )
            )
          )
    )

    // Subtle slow water surface ripples behind the glass plaque
    if (!reduceMotion) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height * 0.46f)
        val waveRadiusBase = size.minDimension * 0.34f
        val pulse = (sin(waterPhase.toDouble()) * 0.5 + 0.5).toFloat()
        for (i in 0..2) {
          val radius = waveRadiusBase + i * 56f + pulse * 18f
          drawCircle(
            color = SankalanTokens.AntiqueGold.copy(alpha = 0.09f - i * 0.025f),
            radius = radius,
            center = center,
            style = Stroke(width = 1.5.dp.toPx()),
          )
        }
      }
    }

    // Top-right instant Skip action (48dp+ touch target)
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 20.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.End,
    ) {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = SankalanTokens.Walnut800.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, SankalanTokens.AntiqueGold.copy(alpha = 0.45f)),
        modifier =
          Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(24.dp))
            .clickable(role = Role.Button) {
              if (!isExiting) {
                isExiting = true
                onEnterHome()
              }
            }
            .testTag("welcome_skip_button"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = strings.welcomeSkip,
            style = MaterialTheme.typography.labelLarge,
            color = SankalanTokens.Cream,
          )
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = strings.welcomeSkip,
            tint = SankalanTokens.GoldHighlight,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }

    // Centerpiece: Glass Wordmark + Water Reflection + Tagline
    Column(
      modifier =
        Modifier
          .align(Alignment.Center)
          .fillMaxWidth()
          .widthIn(max = 520.dp)
          .padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Glass / Water Wordmark container
      Box(
        modifier =
          Modifier
            .offset { IntOffset(0, floatOffsetPx.roundToInt()) }
            .graphicsLayer { alpha = wordmarkAlpha }
            .clip(RoundedCornerShape(20.dp))
            .background(
              Brush.linearGradient(
                colors =
                  listOf(
                    SankalanTokens.Walnut800.copy(alpha = if (reduceMotion) 0.88f else 0.56f),
                    SankalanTokens.Walnut700.copy(alpha = if (reduceMotion) 0.84f else 0.36f),
                  )
              )
            )
            .border(
              width = 1.dp,
              brush =
                Brush.linearGradient(
                  colors =
                    listOf(
                      SankalanTokens.GoldHighlight.copy(alpha = 0.52f),
                      SankalanTokens.AntiqueGold.copy(alpha = 0.18f),
                      SankalanTokens.Cream.copy(alpha = 0.32f),
                    )
                ),
              shape = RoundedCornerShape(20.dp),
            )
            .drawWithContent {
              drawContent()
              if (!reduceMotion) {
                val bandWidth = size.width * 0.42f
                val startX = shimmerProgress * size.width - bandWidth * 0.5f
                drawRect(
                  brush =
                    Brush.horizontalGradient(
                      colors =
                        listOf(
                          Color.Transparent,
                          SankalanTokens.GoldHighlight.copy(alpha = 0.12f),
                          SankalanTokens.Cream.copy(alpha = 0.10f),
                          Color.Transparent,
                        ),
                      startX = startX,
                      endX = startX + bandWidth,
                    )
                )
              }
            }
            .padding(horizontal = 28.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = strings.brandLogoWordmark,
            fontFamily = PlayfairDisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 34.sp,
            lineHeight = 42.sp,
            color = SankalanTokens.Cream,
            textAlign = TextAlign.Center,
            maxLines = 1,
          )
          if (currentLanguage == AppLanguage.BN_BD) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = strings.brandName,
              fontFamily = NotoSerifBengaliFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = 18.sp,
              lineHeight = 26.sp,
              color = SankalanTokens.GoldHighlight,
              textAlign = TextAlign.Center,
            )
          }
        }
      }

      // Subtle vertically flipped water reflection beneath the glass wordmark (never overlaps tagline)
      if (!reduceMotion) {
        val reflectionModifier =
          Modifier
            .padding(top = 6.dp)
            .graphicsLayer {
              alpha = wordmarkAlpha * 0.28f
              scaleY = -0.78f
              compositingStrategy = CompositingStrategy.Offscreen
            }
            .let { mod ->
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mod.blur(2.dp)
              } else {
                mod
              }
            }
            .drawWithContent {
              drawContent()
              drawRect(
                brush =
                  Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                  ),
                blendMode = BlendMode.DstIn,
              )
            }

        Box(modifier = reflectionModifier, contentAlignment = Alignment.Center) {
          Text(
            text = strings.brandLogoWordmark,
            fontFamily = PlayfairDisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            color = SankalanTokens.AntiqueGold.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            maxLines = 1,
          )
        }
      }

      Spacer(modifier = Modifier.height(if (reduceMotion) 24.dp else 14.dp))

      // Tagline reveal (sharper and calmer than the reflection)
      AnimatedVisibility(
        visible = taglineRevealed,
        enter =
          if (reduceMotion) fadeIn(tween(0))
          else fadeIn(tween(550)) + slideInVertically(tween(550)) { it / 3 },
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(horizontal = 12.dp),
        ) {
          Text(
            text = strings.tagline,
            style = MaterialTheme.typography.titleMedium,
            color = SankalanTokens.Cream.copy(alpha = 0.94f),
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = strings.welcomeTapHint,
            style = MaterialTheme.typography.labelMedium,
            color = SankalanTokens.GoldHighlight.copy(alpha = 0.82f),
            textAlign = TextAlign.Center,
          )
        }
      }
    }

    // Bottom onboarding language selector (suggested, never forced — §11)
    AnimatedVisibility(
      visible = taglineRevealed,
      enter = fadeIn(tween(if (reduceMotion) 0 else 450)),
      modifier =
        Modifier
          .align(Alignment.BottomCenter)
          .navigationBarsPadding()
          .padding(horizontal = 20.dp, vertical = 22.dp),
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (showLanguagePrompt) {
          Text(
            text = strings.welcomeLanguagePrompt,
            style = MaterialTheme.typography.labelSmall,
            color = SankalanTokens.MutedOnDark,
          )
        }
        Surface(
          shape = RoundedCornerShape(28.dp),
          color = SankalanTokens.Walnut800.copy(alpha = 0.84f),
          border = BorderStroke(1.dp, SankalanTokens.AntiqueGold.copy(alpha = 0.35f)),
        ) {
          Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            LanguagePillOption(
              label = strings.languageBanglaLabel,
              selected = currentLanguage == AppLanguage.BN_BD,
              onClick = {
                interactionPausedAutoAdvance = true
                onSelectLanguage(AppLanguage.BN_BD)
              },
              testTag = "welcome_lang_bn",
            )
            LanguagePillOption(
              label = strings.languageEnglishLabel,
              selected = currentLanguage == AppLanguage.EN,
              onClick = {
                interactionPausedAutoAdvance = true
                onSelectLanguage(AppLanguage.EN)
              },
              testTag = "welcome_lang_en",
            )
          }
        }
      }
    }

    // Interactive Water-Drop Ripple spreading from exact tap Offset
    val currentTap = tapPoint
    if (currentTap != null && !reduceMotion && tapRippleProgress.value > 0f) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val p = tapRippleProgress.value
        val maxRadius = size.maxDimension * 0.72f
        val r1 = p * maxRadius
        val r2 = (p - 0.16f).coerceAtLeast(0f) * maxRadius
        drawCircle(
          color = SankalanTokens.GoldHighlight.copy(alpha = (1f - p) * 0.55f),
          radius = r1,
          center = currentTap,
          style = Stroke(width = (3.dp.toPx() * (1f - p * 0.5f))),
        )
        if (r2 > 0f) {
          drawCircle(
            color = SankalanTokens.Cream.copy(alpha = (1f - p) * 0.38f),
            radius = r2,
            center = currentTap,
            style = Stroke(width = 1.5.dp.toPx()),
          )
        }
      }
    }
  }
}

@Composable
private fun LanguagePillOption(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  testTag: String,
) {
  Surface(
    shape = RoundedCornerShape(24.dp),
    color = if (selected) SankalanTokens.AntiqueGold else Color.Transparent,
    modifier =
      Modifier
        .minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(24.dp))
        .clickable(role = Role.RadioButton, onClick = onClick)
        .semantics { contentDescription = label }
        .testTag(testTag),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = if (selected) SankalanTokens.Walnut900 else SankalanTokens.Cream,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
    )
  }
}
