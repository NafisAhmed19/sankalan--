package com.example.features.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.MainActivity
import com.example.R
import com.example.core.data.SettingsRepository
import com.example.core.i18n.LocalStrings
import com.example.core.model.AppLanguage
import com.example.ui.theme.LocalSankalanPalette
import com.example.ui.theme.SankalanSpacing
import com.example.ui.theme.SankalanTokens
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UpdateReleaseManifest(
  val versionCode: Int,
  val versionName: String,
  val changelogEn: String = "",
  val changelogBn: String = "",
  val downloadUrl: String = "",
)

sealed interface UpdateCheckResult {
  data class UpdateAvailable(val release: UpdateReleaseManifest) : UpdateCheckResult
  data class UpToDate(val currentVersionName: String) : UpdateCheckResult
  data class Failed(val reason: String) : UpdateCheckResult
}

/**
 * Free GitHub Releases Update Checker & Auto-Notifier (Play-Policy compliant: no DCL / no silent APK install).
 * Checks a lightweight JSON manifest (`version.json`) hosted on GitHub Releases and opens the
 * official release download URL in the user's browser if a newer `versionCode` is available.
 */
object AppUpdateChecker {
  private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val adapter = moshi.adapter(UpdateReleaseManifest::class.java)

  private const val DEFAULT_GITHUB_REPO = "NafisAhmed19/sankalan--"
  private val GITHUB_SLUG_REGEX = Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")

  fun isSafeHttpsUrl(rawUrl: String): Boolean {
    val clean = rawUrl.trim()
    if (!clean.startsWith("https://", ignoreCase = true)) return false
    return runCatching {
      val uri = URI(clean)
      uri.scheme.equals("https", ignoreCase = true) &&
        !uri.host.isNullOrBlank() &&
        uri.userInfo == null &&
        !uri.rawAuthority.orEmpty().contains('@')
    }.getOrDefault(false)
  }

  private fun extractGithubRepoSlugOrNull(clean: String): String? {
    if (!isSafeHttpsUrl(clean)) return null
    val uri = runCatching { URI(clean) }.getOrNull() ?: return null
    if (!uri.host.equals("github.com", ignoreCase = true)) return null
    val path =
      (uri.path ?: "")
        .trim('/')
        .removeSuffix(".git")
    val segments = path.split('/').filter { it.isNotBlank() }
    if (segments.size != 2) return null
    val candidate = "${segments[0]}/${segments[1]}"
    return candidate.takeIf { GITHUB_SLUG_REGEX.matches(it) }
  }

  fun resolveManifestUrl(repoOrUrl: String): String {
    val clean = repoOrUrl.trim()
    val pastedGithubSlug = extractGithubRepoSlugOrNull(clean)
    val cleanSlug = clean.trim('/')
    return when {
      pastedGithubSlug != null -> "https://github.com/$pastedGithubSlug/releases/latest/download/version.json"
      isSafeHttpsUrl(clean) -> clean
      GITHUB_SLUG_REGEX.matches(cleanSlug) -> "https://github.com/$cleanSlug/releases/latest/download/version.json"
      else -> "https://github.com/$DEFAULT_GITHUB_REPO/releases/latest/download/version.json"
    }
  }

  /**
   * Permanent fixed share URL that always points to the latest GitHub Release page for friends.
   */
  fun resolveFixedReleaseUrl(repoOrUrl: String): String {
    val clean = repoOrUrl.trim()
    val pastedGithubSlug = extractGithubRepoSlugOrNull(clean)
    val cleanSlug = clean.trim('/')
    return when {
      pastedGithubSlug != null -> "https://github.com/$pastedGithubSlug/releases/latest"
      isSafeHttpsUrl(clean) -> {
        if (clean.contains("/releases/latest/download/")) {
          clean.substringBefore("/releases/latest/download/") + "/releases/latest"
        } else {
          clean
        }
      }
      GITHUB_SLUG_REGEX.matches(cleanSlug) -> "https://github.com/$cleanSlug/releases/latest"
      else -> "https://github.com/$DEFAULT_GITHUB_REPO/releases/latest"
    }
  }

  fun buildSampleManifestJson(
    repoOrUrl: String,
    nextVersionCode: Int,
    nextVersionName: String,
  ): String {
    val releaseBase = resolveFixedReleaseUrl(repoOrUrl)
    return """
      {
        "versionCode": $nextVersionCode,
        "versionName": "$nextVersionName",
        "changelogEn": "New features and improvements in Sankalan-t.",
        "changelogBn": "সংকলন-ৎ এর নতুন ফিচার ও উন্নতি যুক্ত হয়েছে।",
        "downloadUrl": "$releaseBase/download/sankalan-t.apk"
      }
    """.trimIndent()
  }

  fun parseUpdateManifest(
    json: String,
    currentVersionCode: Int,
    currentVersionName: String,
  ): UpdateCheckResult {
    val parsed =
      runCatching { adapter.fromJson(json) }.getOrNull()
        ?: return UpdateCheckResult.Failed("Invalid JSON manifest")
    if (parsed.downloadUrl.isBlank()) {
      return UpdateCheckResult.Failed("Missing downloadUrl")
    }
    if (!isSafeHttpsUrl(parsed.downloadUrl)) {
      return UpdateCheckResult.Failed("Untrusted non-HTTPS downloadUrl")
    }
    return if (parsed.versionCode > currentVersionCode) {
      UpdateCheckResult.UpdateAvailable(parsed)
    } else {
      UpdateCheckResult.UpToDate(currentVersionName)
    }
  }

  suspend fun checkForUpdates(
    repoOrUrl: String,
    currentVersionCode: Int,
    currentVersionName: String,
  ): UpdateCheckResult =
    withContext(Dispatchers.IO) {
      var conn: HttpURLConnection? = null
      try {
        val manifestUrl = resolveManifestUrl(repoOrUrl)
        if (!isSafeHttpsUrl(manifestUrl)) {
          return@withContext UpdateCheckResult.Failed("HTTPS required")
        }
        val url = URL(manifestUrl)
        conn =
          (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7000
            readTimeout = 7000
            useCaches = false
            setRequestProperty("Accept", "application/json")
          }
        val code = conn.responseCode
        if (code !in 200..299) {
          return@withContext UpdateCheckResult.Failed("HTTP $code")
        }
        if (!isSafeHttpsUrl(conn.url.toString())) {
          return@withContext UpdateCheckResult.Failed("HTTPS redirect required")
        }
        val body =
          conn.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
            val buf = CharArray(8192)
            val sb = StringBuilder()
            while (sb.length < 262_144) {
              val n = reader.read(buf)
              if (n <= 0) break
              sb.appendRange(buf, 0, n)
            }
            sb.toString()
          }
        parseUpdateManifest(body, currentVersionCode, currentVersionName)
      } catch (t: Throwable) {
        UpdateCheckResult.Failed(t.message ?: "Network error")
      } finally {
        runCatching { conn?.disconnect() }
      }
    }

  fun openReleasePage(context: Context, url: String) {
    if (!isSafeHttpsUrl(url)) return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url.trim())).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
  }
}

object AppUpdateNotifier {
  private const val CHANNEL_ID = "sankalan_app_updates"
  private const val PREFS_NAME = "sankalan_update_prefs"
  private const val KEY_LAST_NOTIFIED_VERSION = "last_notified_version_code"
  private const val NOTIFICATION_ID = 2026

  fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  private fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      if (manager.getNotificationChannel(CHANNEL_ID) == null) {
        val channel =
          NotificationChannel(
            CHANNEL_ID,
            "Sankalan-t App Updates",
            NotificationManager.IMPORTANCE_HIGH,
          ).apply {
            description = "Automatic alerts when a new version of Sankalan-t is published."
          }
        manager.createNotificationChannel(channel)
      }
    }
  }

  fun showUpdateNotificationIfNeeded(
    context: Context,
    release: UpdateReleaseManifest,
    isBangla: Boolean,
    forceNotify: Boolean = false,
  ) {
    if (!hasNotificationPermission(context)) return
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val lastNotified = prefs.getInt(KEY_LAST_NOTIFIED_VERSION, -1)
    if (!forceNotify && release.versionCode <= lastNotified) return

    ensureChannel(context)
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

    val targetIntent =
      if (AppUpdateChecker.isSafeHttpsUrl(release.downloadUrl)) {
        Intent(Intent.ACTION_VIEW, Uri.parse(release.downloadUrl.trim())).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      } else {
        Intent(context, MainActivity::class.java).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
      }
    val pendingIntent =
      PendingIntent.getActivity(
        context,
        NOTIFICATION_ID,
        targetIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val title =
      if (isBangla) "সংকলন-ৎ নতুন ভার্সন v${release.versionName} এসেছে!"
      else "Sankalan-t v${release.versionName} Update Available"
    val body =
      if (isBangla) {
        release.changelogBn.ifBlank { release.changelogEn }.ifBlank { "নতুন ফিচার পেতে ট্যাপ করে আপডেট করে নিন।" }
      } else {
        release.changelogEn.ifBlank { release.changelogBn }.ifBlank { "Tap to download and install the latest version." }
      }

    val notification =
      androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    runCatching {
      manager.notify(NOTIFICATION_ID, notification)
      prefs.edit().putInt(KEY_LAST_NOTIFIED_VERSION, release.versionCode).apply()
    }
  }
}

object AppUpdateScheduler {
  private const val UPDATE_JOB_ID = 4107

  fun schedulePeriodicCheck(context: Context) {
    runCatching {
      val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
      if (scheduler.allPendingJobs.any { it.id == UPDATE_JOB_ID }) return
      val component = ComponentName(context, AppUpdateJobService::class.java)
      val jobInfo =
        JobInfo.Builder(UPDATE_JOB_ID, component)
          .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
          .setPeriodic(12L * 60L * 60L * 1000L) // Check automatically every 12h when online
          .setPersisted(false)
          .build()
      scheduler.schedule(jobInfo)
    }
  }
}

class AppUpdateJobService : JobService() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var activeJob: Job? = null

  override fun onStartJob(params: JobParameters?): Boolean {
    activeJob =
      scope.launch {
        runCatching {
          val settings = SettingsRepository(applicationContext).settingsFlow.first()
          val res =
            AppUpdateChecker.checkForUpdates(
              repoOrUrl = settings.githubUpdateRepo,
              currentVersionCode = BuildConfig.VERSION_CODE,
              currentVersionName = BuildConfig.VERSION_NAME,
            )
          if (res is UpdateCheckResult.UpdateAvailable) {
            AppUpdateNotifier.showUpdateNotificationIfNeeded(
              context = applicationContext,
              release = res.release,
              isBangla = settings.language == AppLanguage.BN_BD,
            )
          }
        }
        jobFinished(params, false)
      }
    return true
  }

  override fun onStopJob(params: JobParameters?): Boolean {
    activeJob?.cancel()
    return true
  }
}

@Composable
fun UpdateAvailableDialog(
  release: UpdateReleaseManifest,
  language: AppLanguage,
  onDismiss: () -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val context = LocalContext.current
  val notes =
    if (language == AppLanguage.BN_BD) {
      release.changelogBn.ifBlank { release.changelogEn }
    } else {
      release.changelogEn.ifBlank { release.changelogBn }
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = palette.cardSurface,
    titleContentColor = palette.textPrimary,
    textContentColor = palette.textSecondary,
    icon = { Icon(Icons.Outlined.NewReleases, contentDescription = null, tint = palette.goldAccent) },
    title = { Text("${strings.updateAvailableTitle} (v${release.versionName})") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(SankalanSpacing.sm)) {
        if (notes.isNotBlank()) {
          Text(notes, style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          AppUpdateChecker.openReleasePage(context, release.downloadUrl)
          onDismiss()
        },
        colors =
          ButtonDefaults.buttonColors(
            containerColor = palette.goldAccent,
            contentColor = SankalanTokens.Walnut900,
          ),
        modifier = Modifier.testTag("update_download_open_btn"),
      ) {
        Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(strings.updateDownloadButton)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.closeLabel, color = palette.textSecondary) }
    },
  )
}
