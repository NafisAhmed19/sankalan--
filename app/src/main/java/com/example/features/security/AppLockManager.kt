package com.example.features.security

import android.content.Context
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.LocalStrings
import com.example.core.i18n.formatDigits
import com.example.core.model.NumeralStyle
import com.example.ui.theme.LocalSankalanPalette
import com.example.ui.theme.NotoSerifBengaliFontFamily
import com.example.ui.theme.SankalanSpacing
import com.example.ui.theme.SankalanTokens
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.delay

object PinHasher {
  const val ALGO_SHA256_V1 = "sha256_v1"
  const val ALGO_PBKDF2_SHA256_V2 = "pbkdf2_sha256_v2"
  const val ALGO_PBKDF2_SHA1_V2 = "pbkdf2_sha1_v2"
  private const val PBKDF2_ITERATIONS = 60_000
  private const val PBKDF2_KEY_BITS = 256

  fun generateSalt(): String {
    val bytes = ByteArray(16)
    SecureRandom().nextBytes(bytes)
    return bytes.joinToString("") { "%02x".format(it) }
  }

  /** Legacy single-round SHA-256 kept only for verifying & upgrading pre-existing v1 PINs. */
  fun hashPin(pin: String, salt: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest("$salt:$pin".toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }

  fun hashPinStrong(pin: String, saltHex: String): Pair<String, String> {
    val saltBytes = saltHex.toByteArray(Charsets.UTF_8)
    val preferredAlgo =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) "PBKDF2WithHmacSHA256" else "PBKDF2WithHmacSHA1"
    val algoTag =
      if (preferredAlgo == "PBKDF2WithHmacSHA256") ALGO_PBKDF2_SHA256_V2 else ALGO_PBKDF2_SHA1_V2
    val derived =
      runCatching {
        val spec = PBEKeySpec(pin.toCharArray(), saltBytes, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS)
        try {
          SecretKeyFactory.getInstance(preferredAlgo).generateSecret(spec).encoded
        } finally {
          spec.clearPassword()
        }
      }.getOrElse {
        // Guaranteed fallback on older JVMs lacking PBKDF2WithHmacSHA256: try HmacSHA1
        val spec = PBEKeySpec(pin.toCharArray(), saltBytes, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS)
        try {
          SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
        } finally {
          spec.clearPassword()
        }
      }
    val resolvedTag =
      if (algoTag == ALGO_PBKDF2_SHA256_V2 &&
        runCatching { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256") }.isSuccess
      ) {
        ALGO_PBKDF2_SHA256_V2
      } else {
        ALGO_PBKDF2_SHA1_V2
      }
    return resolvedTag to derived.joinToString("") { "%02x".format(it) }
  }

  fun verifyWithAlgo(pin: String, salt: String, expectedHash: String, algo: String): Boolean {
    val actual =
      when (algo) {
        ALGO_PBKDF2_SHA256_V2 -> derivePbkdf2(pin, salt, "PBKDF2WithHmacSHA256")
        ALGO_PBKDF2_SHA1_V2 -> derivePbkdf2(pin, salt, "PBKDF2WithHmacSHA1")
        else -> hashPin(pin, salt)
      } ?: return false
    return MessageDigest.isEqual(
      actual.toByteArray(Charsets.UTF_8),
      expectedHash.toByteArray(Charsets.UTF_8),
    )
  }

  private fun derivePbkdf2(pin: String, salt: String, jcaName: String): String? =
    runCatching {
      val spec = PBEKeySpec(pin.toCharArray(), salt.toByteArray(Charsets.UTF_8), PBKDF2_ITERATIONS, PBKDF2_KEY_BITS)
      try {
        val bytes = SecretKeyFactory.getInstance(jcaName).generateSecret(spec).encoded
        bytes.joinToString("") { "%02x".format(it) }
      } finally {
        spec.clearPassword()
      }
    }.getOrNull()

  fun isValidPinFormat(pin: String): Boolean =
    pin.length in 4..6 && pin.all { it in '0'..'9' }
}

class AppLockManager(
  private val context: Context,
  private val clockMs: () -> Long = { System.currentTimeMillis() },
) {
  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun hasPin(): Boolean =
    !prefs.getString(KEY_HASH, null).isNullOrBlank() &&
      !prefs.getString(KEY_SALT, null).isNullOrBlank()

  fun setPin(rawPin: String): Boolean {
    if (!PinHasher.isValidPinFormat(rawPin)) return false
    val salt = PinHasher.generateSalt()
    val (algo, hash) = PinHasher.hashPinStrong(rawPin, salt)
    prefs.edit()
      .putString(KEY_SALT, salt)
      .putString(KEY_HASH, hash)
      .putString(KEY_ALGO, algo)
      .putInt(KEY_FAILED_ATTEMPTS, 0)
      .putLong(KEY_LOCKOUT_UNTIL_MS, 0L)
      .apply()
    return true
  }

  fun remainingLockoutSeconds(): Int {
    val until = prefs.getLong(KEY_LOCKOUT_UNTIL_MS, 0L)
    val diff = until - clockMs()
    return if (diff > 0L) ((diff + 999L) / 1000L).toInt() else 0
  }

  fun failedAttemptsCount(): Int = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)

  fun verifyPin(rawPin: String): Boolean {
    if (remainingLockoutSeconds() > 0) return false
    val salt = prefs.getString(KEY_SALT, null) ?: return false
    val expected = prefs.getString(KEY_HASH, null) ?: return false
    val algo = prefs.getString(KEY_ALGO, PinHasher.ALGO_SHA256_V1) ?: PinHasher.ALGO_SHA256_V1
    val matched = PinHasher.verifyWithAlgo(rawPin, salt, expected, algo)
    if (matched) {
      resetLockout()
      if (algo == PinHasher.ALGO_SHA256_V1) {
        // Transparently upgrade legacy single-round SHA-256 PIN to PBKDF2-HMAC upon verified unlock
        val newSalt = PinHasher.generateSalt()
        val (newAlgo, newHash) = PinHasher.hashPinStrong(rawPin, newSalt)
        prefs.edit()
          .putString(KEY_SALT, newSalt)
          .putString(KEY_HASH, newHash)
          .putString(KEY_ALGO, newAlgo)
          .apply()
      }
      return true
    }

    val nextAttempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
    val cooldownMs =
      when {
        nextAttempts >= 10 -> 120_000L
        nextAttempts >= 8 -> 60_000L
        nextAttempts >= 5 -> 30_000L
        else -> 0L
      }
    prefs.edit()
      .putInt(KEY_FAILED_ATTEMPTS, nextAttempts)
      .putLong(KEY_LOCKOUT_UNTIL_MS, if (cooldownMs > 0L) clockMs() + cooldownMs else 0L)
      .apply()
    return false
  }

  fun resetLockout() {
    prefs.edit()
      .putInt(KEY_FAILED_ATTEMPTS, 0)
      .putLong(KEY_LOCKOUT_UNTIL_MS, 0L)
      .apply()
  }

  fun clearPin() {
    prefs.edit()
      .remove(KEY_SALT)
      .remove(KEY_HASH)
      .remove(KEY_ALGO)
      .remove(KEY_FAILED_ATTEMPTS)
      .remove(KEY_LOCKOUT_UNTIL_MS)
      .apply()
  }

  fun canUsePlatformBiometrics(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

  fun launchBiometricUnlock(
    title: String,
    subtitle: String,
    cancelLabel: String,
    onSuccess: () -> Unit,
    onFallbackOrError: () -> Unit,
  ) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
      onFallbackOrError()
      return
    }
    val cancellation = CancellationSignal()
    val executor = context.mainExecutor
    try {
      val prompt =
        BiometricPrompt.Builder(context)
          .setTitle(title)
          .setSubtitle(subtitle)
          .setNegativeButton(cancelLabel, executor) { _, _ -> onFallbackOrError() }
          .build()
      prompt.authenticate(
        cancellation,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
          override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
            resetLockout()
            onSuccess()
          }

          override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            onFallbackOrError()
          }
        },
      )
    } catch (_: Throwable) {
      onFallbackOrError()
    }
  }

  companion object {
    private const val PREFS_NAME = "sankalan_lock_store"
    private const val KEY_SALT = "pin_salt_v1"
    private const val KEY_HASH = "pin_hash_v1"
    private const val KEY_ALGO = "pin_algo_v2"
    private const val KEY_FAILED_ATTEMPTS = "pin_failed_attempts"
    private const val KEY_LOCKOUT_UNTIL_MS = "pin_lockout_until_ms"
  }
}

@Composable
fun AppLockGateOverlay(
  numeralStyle: NumeralStyle,
  onUnlocked: () -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val context = LocalContext.current
  val lockManager = remember(context) { AppLockManager(context) }

  var enteredPin by remember { mutableStateOf("") }
  var showError by remember { mutableStateOf(false) }
  var lockoutSeconds by remember { mutableIntStateOf(lockManager.remainingLockoutSeconds()) }

  LaunchedEffect(lockoutSeconds) {
    if (lockoutSeconds > 0) {
      delay(1_000L)
      lockoutSeconds = lockManager.remainingLockoutSeconds()
    }
  }

  fun submitPin(candidate: String) {
    val currentLockout = lockManager.remainingLockoutSeconds()
    if (currentLockout > 0) {
      lockoutSeconds = currentLockout
      enteredPin = ""
      showError = true
      return
    }
    if (lockManager.verifyPin(candidate)) {
      enteredPin = ""
      showError = false
      lockoutSeconds = 0
      onUnlocked()
    } else {
      enteredPin = ""
      showError = true
      lockoutSeconds = lockManager.remainingLockoutSeconds()
    }
  }

  Surface(
    color = SankalanTokens.Walnut900,
    contentColor = SankalanTokens.Cream,
    modifier = Modifier.fillMaxSize().testTag("app_lock_overlay"),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(SankalanSpacing.xl),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Surface(
        shape = CircleShape,
        color = SankalanTokens.AntiqueGold.copy(alpha = 0.16f),
        border = BorderStroke(1.5.dp, SankalanTokens.AntiqueGold),
        modifier = Modifier.size(64.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(
            text = "ৎ",
            fontFamily = NotoSerifBengaliFontFamily,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = SankalanTokens.AntiqueGold,
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = strings.appLockEnterPinTitle,
        style = MaterialTheme.typography.headlineSmall,
        color = SankalanTokens.Cream,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(6.dp))
      val subtitleText =
        when {
          lockoutSeconds > 0 ->
            "${strings.appLockWrongPin} (${formatDigits(lockoutSeconds.toString(), numeralStyle)}s)"
          showError -> strings.appLockWrongPin
          else -> strings.appLockPinHint
        }
      Text(
        text = subtitleText,
        style = MaterialTheme.typography.bodyMedium,
        color = if (showError || lockoutSeconds > 0) SankalanTokens.Terracotta else SankalanTokens.Cream.copy(alpha = 0.76f),
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(18.dp))

      // Masked Pin Dots
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for (i in 0 until 6) {
          val filled = i < enteredPin.length
          Box(
            modifier =
              Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(if (filled) SankalanTokens.AntiqueGold else SankalanTokens.Walnut700)
          )
        }
      }

      Spacer(modifier = Modifier.height(22.dp))

      // Tactile Walnut & Gold Keypad
      val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
      Column(
        modifier = Modifier.widthIn(max = 280.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        rows.forEach { rowDigits ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            rowDigits.forEach { digit ->
              val isKeypadEnabled = lockoutSeconds <= 0
              Surface(
                shape = CircleShape,
                color = SankalanTokens.Walnut800,
                border = BorderStroke(1.dp, SankalanTokens.AntiqueGold.copy(alpha = 0.45f)),
                modifier =
                  Modifier
                    .weight(1f)
                    .height(58.dp)
                    .alpha(if (isKeypadEnabled) 1f else 0.45f)
                    .clip(CircleShape)
                    .clickable(enabled = isKeypadEnabled, role = Role.Button) {
                      showError = false
                      if (enteredPin.length < 6) {
                        enteredPin += digit
                      }
                    }
                    .testTag("lock_digit_$digit"),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = formatDigits(digit, numeralStyle),
                    style = MaterialTheme.typography.titleLarge,
                    color = SankalanTokens.Cream,
                  )
                }
              }
            }
          }
        }

        // Bottom row: Backspace, 0, Unlock
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Surface(
            shape = CircleShape,
            color = SankalanTokens.Walnut800,
            border = BorderStroke(1.dp, palette.borderSubtle),
            modifier =
              Modifier
                .weight(1f)
                .height(58.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button) {
                  if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                },
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = null, tint = SankalanTokens.Cream.copy(alpha = 0.76f))
            }
          }

          Surface(
            shape = CircleShape,
            color = SankalanTokens.Walnut800,
            border = BorderStroke(1.dp, SankalanTokens.AntiqueGold.copy(alpha = 0.45f)),
            modifier =
              Modifier
                .weight(1f)
                .height(58.dp)
                .alpha(if (lockoutSeconds <= 0) 1f else 0.45f)
                .clip(CircleShape)
                .clickable(enabled = lockoutSeconds <= 0, role = Role.Button) {
                  showError = false
                  if (enteredPin.length < 6) enteredPin += "0"
                }
                .testTag("lock_digit_0"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = formatDigits("0", numeralStyle),
                style = MaterialTheme.typography.titleLarge,
                color = SankalanTokens.Cream,
              )
            }
          }

          Button(
            onClick = { submitPin(enteredPin) },
            enabled = lockoutSeconds <= 0 && enteredPin.length >= 4,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = SankalanTokens.AntiqueGold,
                contentColor = SankalanTokens.Walnut900,
              ),
            shape = CircleShape,
            modifier = Modifier.weight(1f).height(58.dp).testTag("lock_unlock_btn"),
          ) {
            Icon(Icons.Outlined.Lock, contentDescription = strings.appLockUnlockButton)
          }
        }
      }

      if (lockManager.canUsePlatformBiometrics()) {
        Spacer(modifier = Modifier.height(18.dp))
        OutlinedButton(
          onClick = {
            lockManager.launchBiometricUnlock(
              title = strings.appLockBiometricPromptTitle,
              subtitle = strings.appLockBiometricPromptSub,
              cancelLabel = strings.cancel,
              onSuccess = onUnlocked,
              onFallbackOrError = {},
            )
          },
          border = BorderStroke(1.dp, SankalanTokens.AntiqueGold),
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("lock_biometric_btn"),
        ) {
          Icon(Icons.Outlined.Fingerprint, contentDescription = null, tint = SankalanTokens.AntiqueGold)
          Spacer(modifier = Modifier.width(6.dp))
          Text(strings.appLockBiometricButton, color = SankalanTokens.AntiqueGold)
        }
      }
    }
  }
}

@Composable
fun SetAppPinDialog(
  onDismiss: () -> Unit,
  onPinSaved: (String) -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  var pin by remember { mutableStateOf("") }
  var confirmPin by remember { mutableStateOf("") }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = palette.cardSurface,
    titleContentColor = palette.textPrimary,
    textContentColor = palette.textPrimary,
    title = { Text(strings.appLockSetPinLabel) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(SankalanSpacing.sm)) {
        OutlinedTextField(
          value = pin,
          onValueChange = { raw ->
            pin = raw.filter { it in '0'..'9' }.take(6)
            errorMsg = null
          },
          label = { Text(strings.appLockPinHint) },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          modifier = Modifier.fillMaxWidth().testTag("set_pin_input"),
        )
        OutlinedTextField(
          value = confirmPin,
          onValueChange = { raw ->
            confirmPin = raw.filter { it in '0'..'9' }.take(6)
            errorMsg = null
          },
          label = { Text(strings.confirm) },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          modifier = Modifier.fillMaxWidth().testTag("set_pin_confirm_input"),
        )
        if (errorMsg != null) {
          Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, color = SankalanTokens.Terracotta)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          when {
            !PinHasher.isValidPinFormat(pin) -> errorMsg = strings.appLockPinTooShort
            pin != confirmPin -> errorMsg = strings.appLockPinMismatch
            else -> onPinSaved(pin)
          }
        },
        colors =
          ButtonDefaults.buttonColors(
            containerColor = palette.goldAccent,
            contentColor = SankalanTokens.Walnut900,
          ),
        modifier = Modifier.testTag("set_pin_save_btn"),
      ) {
        Text(strings.save)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.cancel, color = palette.textSecondary) }
    },
  )
}
