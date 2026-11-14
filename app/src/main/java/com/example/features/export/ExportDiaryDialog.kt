package com.example.features.export

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.i18n.LocalStrings
import com.example.core.model.DiaryWithPages
import com.example.core.model.NumeralStyle
import com.example.core.model.Page
import com.example.core.model.ReadingLayout
import com.example.ui.theme.LocalSankalanPalette
import com.example.ui.theme.SankalanSpacing
import com.example.ui.theme.SankalanTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class PendingExportMode {
  PDF_SINGLE,
  PDF_SPREAD,
  PAGE_PNG,
  PAGE_JPEG,
  PAGES_ZIP,
}

@Composable
fun ExportDiaryDialog(
  diaryWithPages: DiaryWithPages,
  activePage: Page?,
  numeralStyle: NumeralStyle,
  onDismiss: () -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val targetPage =
    remember(diaryWithPages, activePage) {
      activePage ?: diaryWithPages.pages.firstOrNull { !it.isDeleted }
    }
  val safeBaseName =
    remember(diaryWithPages.diary.title) {
      diaryWithPages.diary.title.trim().replace(Regex("[^\\p{L}\\p{N}_-]+"), "_").take(32).ifBlank { "Sankalan" }
    }

  var pendingMode by remember { mutableStateOf<PendingExportMode?>(null) }
  var isExporting by remember { mutableStateOf(false) }
  var statusMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

  fun launchSave(mode: PendingExportMode, launcher: androidx.activity.result.ActivityResultLauncher<String>, ext: String) {
    pendingMode = mode
    statusMessage = null
    launcher.launch("${safeBaseName}.$ext")
  }

  val pdfLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
      val mode = pendingMode
      if (uri != null && (mode == PendingExportMode.PDF_SINGLE || mode == PendingExportMode.PDF_SPREAD)) {
        isExporting = true
        scope.launch {
          val ok =
            withContext(Dispatchers.IO) {
              runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                  val layout = if (mode == PendingExportMode.PDF_SPREAD) ReadingLayout.SPREAD else ReadingLayout.SINGLE
                  DiaryExporter.exportDiaryToPdf(context, diaryWithPages, layout, numeralStyle, out)
                } ?: error("Cannot open output stream")
              }.isSuccess
            }
          isExporting = false
          statusMessage = ok to (if (ok) strings.exportSuccessMsg else strings.exportFailedMsg)
        }
      }
    }

  val pngLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
      val page = targetPage
      if (uri != null && page != null) {
        isExporting = true
        scope.launch {
          val ok =
            withContext(Dispatchers.IO) {
              runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                  DiaryExporter.exportPageToImage(context, page, numeralStyle, Bitmap.CompressFormat.PNG, out)
                } ?: error("Cannot open output stream")
              }.isSuccess
            }
          isExporting = false
          statusMessage = ok to (if (ok) strings.exportSuccessMsg else strings.exportFailedMsg)
        }
      }
    }

  val jpegLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { uri ->
      val page = targetPage
      if (uri != null && page != null) {
        isExporting = true
        scope.launch {
          val ok =
            withContext(Dispatchers.IO) {
              runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                  DiaryExporter.exportPageToImage(context, page, numeralStyle, Bitmap.CompressFormat.JPEG, out)
                } ?: error("Cannot open output stream")
              }.isSuccess
            }
          isExporting = false
          statusMessage = ok to (if (ok) strings.exportSuccessMsg else strings.exportFailedMsg)
        }
      }
    }

  val zipLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
      if (uri != null) {
        isExporting = true
        scope.launch {
          val ok =
            withContext(Dispatchers.IO) {
              runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                  DiaryExporter.exportDiaryImagesZip(context, diaryWithPages, numeralStyle, out)
                } ?: error("Cannot open output stream")
              }.isSuccess
            }
          isExporting = false
          statusMessage = ok to (if (ok) strings.exportSuccessMsg else strings.exportFailedMsg)
        }
      }
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = palette.cardSurface,
    titleContentColor = palette.textPrimary,
    textContentColor = palette.textPrimary,
    title = { Text("${strings.exportDiaryTitle} · ${diaryWithPages.diary.title}") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(SankalanSpacing.sm)) {
        ExportOptionRow(
          icon = Icons.Outlined.PictureAsPdf,
          label = strings.exportPdfSingle,
          enabled = !isExporting,
          testTag = "export_pdf_single_btn",
          onClick = { launchSave(PendingExportMode.PDF_SINGLE, pdfLauncher, "pdf") },
        )
        ExportOptionRow(
          icon = Icons.Outlined.AutoStories,
          label = strings.exportPdfSpread,
          enabled = !isExporting,
          testTag = "export_pdf_spread_btn",
          onClick = { launchSave(PendingExportMode.PDF_SPREAD, pdfLauncher, "pdf") },
        )
        if (targetPage != null) {
          ExportOptionRow(
            icon = Icons.Outlined.Image,
            label = strings.exportPagePng,
            enabled = !isExporting,
            testTag = "export_page_png_btn",
            onClick = { launchSave(PendingExportMode.PAGE_PNG, pngLauncher, "png") },
          )
          ExportOptionRow(
            icon = Icons.Outlined.Image,
            label = strings.exportPageJpeg,
            enabled = !isExporting,
            testTag = "export_page_jpeg_btn",
            onClick = { launchSave(PendingExportMode.PAGE_JPEG, jpegLauncher, "jpg") },
          )
        }
        ExportOptionRow(
          icon = Icons.Outlined.FolderZip,
          label = strings.exportPagesZip,
          enabled = !isExporting,
          testTag = "export_pages_zip_btn",
          onClick = { launchSave(PendingExportMode.PAGES_ZIP, zipLauncher, "zip") },
        )

        if (isExporting) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 6.dp),
          ) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = palette.goldAccent, strokeWidth = 2.dp)
            Text(strings.exportInProgress, style = MaterialTheme.typography.bodySmall, color = palette.goldAccent)
          }
        }

        statusMessage?.let { (ok, msg) ->
          Surface(
            shape = MaterialTheme.shapes.small,
            color = palette.shelfSurface,
            border = BorderStroke(1.dp, if (ok) palette.goldAccent else SankalanTokens.Terracotta),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = if (ok) palette.goldAccent else SankalanTokens.Terracotta,
                modifier = Modifier.size(18.dp),
              )
              Text(msg, style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.closeLabel, color = palette.goldAccent) }
    },
  )
}

@Composable
private fun ExportOptionRow(
  icon: ImageVector,
  label: String,
  enabled: Boolean,
  testTag: String,
  onClick: () -> Unit,
) {
  val palette = LocalSankalanPalette.current
  OutlinedButton(
    onClick = onClick,
    enabled = enabled,
    border = BorderStroke(1.dp, palette.borderGold),
    shape = MaterialTheme.shapes.small,
    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag(testTag),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Start,
    ) {
      Icon(icon, contentDescription = null, tint = palette.goldAccent, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(10.dp))
      Text(label, style = MaterialTheme.typography.labelLarge, color = palette.textPrimary)
    }
  }
}
