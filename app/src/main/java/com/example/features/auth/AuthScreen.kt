package com.example.features.auth

import android.app.Activity
import android.content.Context
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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope,
) {
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
      val cred = credentialManager.getCredential(context, request).credential
      if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
        Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (_: Exception) {
      onUnauthenticated()
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {},
) {
  val clientId =
    resolveWebClientId(context)
      ?: run {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
      }
  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
  scope.launch {
    try {
      val activityContext = (context as? Activity) ?: context
      val cred = credentialManager.getCredential(activityContext, request).credential
      if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
        Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        onAuthSuccess()
      } else {
        onAuthError("Unexpected credential type")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
      onAuthCancelled()
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      onAuthError(e.localizedMessage ?: "Sign in failed")
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
        Firebase.auth.signOut()
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
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isLoading by remember { mutableStateOf(false) }
  var isCreatingAccountMode by remember { mutableStateOf(false) }
  var customAccountName by remember { mutableStateOf("") }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = scope,
    )
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
                Text(
                  text = errorMsg!!,
                  style = MaterialTheme.typography.bodySmall,
                  color = SankalanTokens.Terracotta,
                  modifier = Modifier.padding(SankalanSpacing.sm),
                )
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
