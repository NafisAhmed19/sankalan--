package com.example.features.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.core.i18n.LocalStrings
import com.example.core.model.AppLanguage
import com.example.ui.theme.LocalSankalanPalette
import com.example.ui.theme.NotoSerifBengaliFontFamily
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.SankalanSpacing
import com.example.ui.theme.SankalanTokens
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth ->
    trySend(auth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

@Suppress("DiscouragedApi")
private fun resolveWebClientId(context: Context): String? {
  val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
  if (resId == 0) return null
  return runCatching { context.getString(resId) }.getOrNull()?.takeIf { it.isNotBlank() }
}

private fun isFirebaseInitialized(context: Context): Boolean =
  runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false)

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope,
) {
  if (!isFirebaseInitialized(context)) {
    onUnauthenticated()
    return
  }
  if (Firebase.auth.currentUser != null) {
    onAuthSuccess()
    return
  }
  val clientId =
    resolveWebClientId(context)
      ?: run {
        onUnauthenticated()
        return
      }
  val googleIdOption =
    GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()
  val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
  scope.launch {
    try {
      withTimeout(6_000L) {
        val cred = credentialManager.getCredential(context, request).credential
        if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
          Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
          onAuthSuccess()
        } else {
          onUnauthenticated()
        }
      }
    } catch (_: Exception) {
      onUnauthenticated()
    }
  }
}

private tailrec fun Context.findActivity(): Activity? =
  when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }

private fun isTechnicalCancellation(e: GetCredentialCancellationException): Boolean {
  val detail = "${e.errorMessage ?: ""} ${e.message ?: ""}".lowercase()
  if (detail.isBlank()) return false
  val technicalKeywords =
    listOf(
      "28444",
      "developer",
      "reauth",
      "blocked",
      "cooldown",
      "sha",
      "config",
      "403",
      "access_denied",
      "failed",
      "error",
      "[16]",
      "[10]",
    )
  return technicalKeywords.any { detail.contains(it) }
}

private fun formatAuthError(e: Throwable): String {
  val rawCodeAndDetail =
    when (e) {
      is FirebaseAuthException ->
        "[FirebaseAuth/${e.errorCode}] ${e.localizedMessage ?: e.message ?: "Authentication failed"}"
      is GoogleIdTokenParsingException ->
        "[GoogleIdTokenParsing] ${e.localizedMessage ?: e.message ?: "Invalid Google ID token"}"
      is GetCredentialException -> {
        val detail = e.errorMessage?.toString()?.takeIf { it.isNotBlank() }
          ?: e.localizedMessage
          ?: e.message
          ?: "Credential request failed"
        "[${e::class.simpleName ?: "CredentialError"} | ${e.type}] $detail"
      }
      else -> "[${e::class.simpleName ?: "AuthError"}] ${e.localizedMessage ?: e.message ?: "Sign in failed"}"
    }

  val lower = rawCodeAndDetail.lowercase()
  val hint =
    when {
      lower.contains("28444") ||
        lower.contains("developer") ||
        lower.contains("10:") ||
        lower.contains("sha") ->
        "\n\nসম্ভাব্য কারণ: Firebase Console-এ এই অ্যাপের SHA-1 / SHA-256 যুক্ত নেই অথবা OAuth Consent Screen এখনো Testing মোডে আছে (Test user হিসেবে আপনার Gmail যোগ করুন)।"
      lower.contains("operation_not_allowed") || lower.contains("operation-not-allowed") ->
        "\n\nসম্ভাব্য কারণ: Firebase Console -> Authentication -> Sign-in method এ Google প্রোভাইডারটি Enable করা নেই।"
      lower.contains("nocredential") || lower.contains("no credentials") ->
        "\n\nসম্ভাব্য কারণ: ফোনে কোনো সক্রিয় Google/Gmail অ্যাকাউন্ট পাওয়া যায়নি বা Google Play Services আপডেট প্রয়োজন।"
      lower.contains("blocked") || lower.contains("cooldown") ->
        "\n\nসম্ভাব্য কারণ: পরপর কয়েকবার বাতিল হওয়ায় Google Sign-In সাময়িকভাবে বিরতিতে আছে। কিছুক্ষণ পর আবার চেষ্টা করুন।"
      lower.contains("network") ->
        "\n\nসম্ভাব্য কারণ: Google/Firebase সার্ভারে ইন্টারনেট সংযোগ পাওয়া যায়নি।"
      else -> ""
    }

  return rawCodeAndDetail + hint
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {},
) {
  if (!isFirebaseInitialized(context)) {
    onAuthError(
      "Firebase config (google-services.json) missing in this APK build. Add GOOGLE_SERVICES_JSON secret on GitHub and rebuild."
    )
    return
  }
  val clientId =
    resolveWebClientId(context)
      ?: run {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
      }
  val activityContext = context.findActivity() ?: context
  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val primaryRequest = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

  scope.launch {
    try {
      val response =
        try {
          withTimeout(45_000L) { credentialManager.getCredential(activityContext, primaryRequest) }
        } catch (primaryError: GetCredentialException) {
          val shouldTryGoogleIdFallback =
            primaryError is NoCredentialException ||
              primaryError is GetCredentialUnsupportedException ||
              primaryError is GetCredentialProviderConfigurationException
          if (!shouldTryGoogleIdFallback) throw primaryError
          Log.w(
            "Auth",
            "Primary GetSignInWithGoogleOption failed (${primaryError::class.simpleName}: ${primaryError.errorMessage}); trying GetGoogleIdOption fallback",
            primaryError,
          )
          val fallbackOption =
            GetGoogleIdOption.Builder()
              .setFilterByAuthorizedAccounts(false)
              .setServerClientId(clientId)
              .setAutoSelectEnabled(false)
              .build()
          val fallbackRequest =
            GetCredentialRequest.Builder().addCredentialOption(fallbackOption).build()
          withTimeout(45_000L) { credentialManager.getCredential(activityContext, fallbackRequest) }
        }

      val cred = response.credential
      if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
        withTimeout(20_000L) {
          Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        }
        onAuthSuccess()
      } else {
        onAuthError("Unexpected credential type: ${cred.type}")
      }
    } catch (e: TimeoutCancellationException) {
      Log.e("Auth", "Google Sign-In timed out waiting for Play Services or Firebase", e)
      onAuthError(
        "[Timeout] Google Play Services বা Firebase থেকে নির্ধারিত সময়ে উত্তর পাওয়া যায়নি। আপনার ইন্টারনেট সংযোগ, Google Play Services আপডেট ও ফোনে সক্রিয় Gmail অ্যাকাউন্ট আছে কি না দেখে আবার চেষ্টা করুন।"
      )
    } catch (e: GetCredentialCancellationException) {
      Log.w(
        "Auth",
        "Google Sign-In flow cancelled or dismissed (${e.type}): ${e.errorMessage ?: e.message}",
        e,
      )
      if (isTechnicalCancellation(e)) {
        onAuthError(formatAuthError(e))
      } else {
        onAuthCancelled()
      }
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      onAuthError(formatAuthError(e))
    }
  }
}

fun signOutFromCloud(
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onSignOutComplete: () -> Unit = {},
) {
  scope.launch {
    withContext(NonCancellable) {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w("Auth", "Failed to clear credential state: ${e.message}")
      } finally {
        runCatching { Firebase.auth.signOut() }
        onSignOutComplete()
      }
    }
  }
}

@Composable
fun AuthScreen(
  currentLanguage: AppLanguage,
  onSelectLanguage: (AppLanguage) -> Unit,
  onPendingDisplayNameChosen: (String?) -> Unit,
  onAuthSuccess: () -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val context = LocalContext.current
  val activityContext = remember(context) { context.findActivity() ?: context }
  val scope = rememberCoroutineScope()
  val credentialManager = remember(activityContext) { CredentialManager.create(activityContext) }

  val firebaseReady = remember(context) { isFirebaseInitialized(context) }
  var isLoading by remember { mutableStateOf(false) }
  var isCreatingAccountMode by remember { mutableStateOf(false) }
  var customAccountName by remember { mutableStateOf("") }
  var errorMsg by remember(firebaseReady, currentLanguage) {
    mutableStateOf<String?>(
      if (firebaseReady) null
      else if (currentLanguage == AppLanguage.BN_BD) {
        "এই APK-তে Firebase কনফিগারেশন (google-services.json) যুক্ত করা হয়নি। GitHub-এ GOOGLE_SERVICES_JSON সিক্রেট বা app/google-services.json যোগ করে নতুন করে APK বিল্ড করুন।"
      } else {
        "Firebase configuration (google-services.json) is missing in this APK build. Add GOOGLE_SERVICES_JSON secret on GitHub and rebuild."
      }
    )
  }

  LaunchedEffect(firebaseReady) {
    if (firebaseReady) {
      attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = onAuthSuccess,
        onUnauthenticated = {},
        scope = scope,
      )
    }
  }

  fun launchGoogleAuth(customName: String?) {
    errorMsg = null
    isLoading = true
    onPendingDisplayNameChosen(customName?.trim()?.takeIf { it.isNotEmpty() })
    onGoogleSignInClicked(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = {
        isLoading = false
        onAuthSuccess()
      },
      onAuthError = { msg ->
        isLoading = false
        errorMsg = msg
      },
      scope = scope,
      onAuthCancelled = {
        isLoading = false
      },
    )
  }

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(SankalanTokens.Walnut900)
        .testTag("auth_screen"),
    contentAlignment = Alignment.Center,
  ) {
    Image(
      painter = painterResource(id = R.drawable.img_walnut_study_dark),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize().alpha(0.76f),
    )

    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = SankalanSpacing.lg, vertical = SankalanSpacing.xl),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Column(
        modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SankalanSpacing.md),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
        ) {
          Icon(
            imageVector = Icons.Outlined.Translate,
            contentDescription = null,
            tint = SankalanTokens.AntiqueGold,
            modifier = Modifier.size(16.dp),
          )
          FilterChip(
            selected = currentLanguage == AppLanguage.BN_BD,
            onClick = { onSelectLanguage(AppLanguage.BN_BD) },
            label = { Text(strings.languageBanglaLabel) },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = SankalanTokens.AntiqueGold,
                selectedLabelColor = SankalanTokens.Walnut900,
                labelColor = SankalanTokens.Cream,
              ),
            modifier = Modifier.testTag("auth_lang_bn"),
          )
          FilterChip(
            selected = currentLanguage == AppLanguage.EN,
            onClick = { onSelectLanguage(AppLanguage.EN) },
            label = { Text(strings.languageEnglishLabel) },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = SankalanTokens.AntiqueGold,
                selectedLabelColor = SankalanTokens.Walnut900,
                labelColor = SankalanTokens.Cream,
              ),
            modifier = Modifier.testTag("auth_lang_en"),
          )
        }

        Surface(
          shape = CircleShape,
          color = SankalanTokens.AntiqueGold.copy(alpha = 0.16f),
          border = BorderStroke(1.5.dp, SankalanTokens.AntiqueGold),
          modifier = Modifier.size(72.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = "ৎ",
              fontFamily = NotoSerifBengaliFontFamily,
              fontSize = 34.sp,
              fontWeight = FontWeight.Bold,
              color = SankalanTokens.AntiqueGold,
            )
          }
        }

        Text(
          text = strings.brandName,
          fontFamily = NotoSerifBengaliFontFamily,
          fontSize = 32.sp,
          fontWeight = FontWeight.Bold,
          color = SankalanTokens.Cream,
          textAlign = TextAlign.Center,
        )
        Text(
          text = strings.brandLogoWordmark,
          fontFamily = PlayfairDisplayFontFamily,
          fontSize = 18.sp,
          color = SankalanTokens.AntiqueGold,
          textAlign = TextAlign.Center,
        )

        Surface(
          shape = MaterialTheme.shapes.large,
          color = palette.cardSurface,
          border = BorderStroke(1.2.dp, palette.borderGold),
          tonalElevation = 4.dp,
          modifier = Modifier.fillMaxWidth(),
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
              )
              Text(
                text = strings.authTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary,
              )
            }

            Text(
              text = strings.authSubtitle,
              style = MaterialTheme.typography.bodyMedium,
              color = palette.textSecondary,
            )

            if (isCreatingAccountMode) {
              OutlinedTextField(
                value = customAccountName,
                onValueChange = { customAccountName = it.take(60) },
                label = { Text(strings.authDisplayNameInputLabel) },
                placeholder = { Text(strings.authDisplayNameInputHint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("auth_create_name_input"),
              )
            }

            Button(
              onClick = {
                launchGoogleAuth(if (isCreatingAccountMode) customAccountName else null)
              },
              enabled = !isLoading,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = palette.goldAccent,
                  contentColor = SankalanTokens.Walnut900,
                ),
              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("auth_google_signin_btn"),
            ) {
              if (isLoading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  color = SankalanTokens.Walnut900,
                  strokeWidth = 2.dp,
                )
              } else {
                Icon(
                  imageVector = Icons.AutoMirrored.Outlined.Login,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text =
                    if (isCreatingAccountMode) strings.authCreateConfirmButton
                    else strings.authSignInWithGoogleButton,
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }

            OutlinedButton(
              onClick = { isCreatingAccountMode = !isCreatingAccountMode },
              enabled = !isLoading,
              border = BorderStroke(1.dp, palette.borderGold),
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("auth_create_account_toggle_btn"),
            ) {
              Icon(
                imageVector = Icons.Outlined.PersonAdd,
                contentDescription = null,
                tint = palette.goldAccent,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text =
                  if (isCreatingAccountMode) strings.authBackToExistingSignIn
                  else strings.authCreateAccountButton,
                color = palette.textPrimary,
              )
            }

            if (errorMsg != null) {
              Surface(
                shape = MaterialTheme.shapes.small,
                color = SankalanTokens.Terracotta.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, SankalanTokens.Terracotta.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().testTag("auth_error_banner"),
              ) {
                SelectionContainer {
                  Text(
                    text = errorMsg!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = SankalanTokens.Terracotta,
                    modifier = Modifier.padding(SankalanSpacing.sm),
                  )
                }
              }
            }

            Row(
              verticalAlignment = Alignment.Top,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.padding(top = 2.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.VerifiedUser,
                contentDescription = null,
                tint = palette.goldAccent,
                modifier = Modifier.size(16.dp).padding(top = 2.dp),
              )
              Text(
                text = strings.authGmailAutoAccountHint,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
              )
            }

            Row(
              verticalAlignment = Alignment.Top,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(15.dp).padding(top = 2.dp),
              )
              Text(
                text = strings.authFacebookManualNote,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
              )
            }
          }
        }
      }
    }
  }
}
