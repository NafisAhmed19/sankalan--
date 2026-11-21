package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.core.model.CanvasElement
import com.example.core.model.Diary
import com.example.core.model.DiaryCover
import com.example.core.model.DiaryWithPages
import com.example.core.model.NumeralStyle
import com.example.core.model.Page
import com.example.core.model.PageBackground
import com.example.core.model.ReadingLayout
import com.example.features.export.BackupArchiveManager
import com.example.features.export.DiaryExporter
import com.example.features.update.AppUpdateChecker
import com.example.features.update.UpdateCheckResult
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExportBackupAndUpdateTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
  }

  private fun sampleDiaryWithBanglaConjuncts(mediaFile: File? = null): DiaryWithPages {
    val page1 =
      Page(
        id = "exp-page-1",
        diaryId = "exp-diary-1",
        order = 0,
        dateIso = "2026-11-13",
        title = "সংকলন ও স্মৃতিকথা",
        mood = "Peaceful",
        background = PageBackground(presetId = "warm_ivory", tintHex = "#F6EFE2"),
        elements =
          buildList {
            add(
              CanvasElement(
                id = "el-text-bn",
                type = "text",
                x = 0.08f,
                y = 0.14f,
                w = 0.84f,
                h = 0.40f,
                text = "সংকলন, স্মৃতি, শ্রদ্ধা, প্রিয়, স্বাধীনতা, দৃষ্টান্ত, কৃতজ্ঞতা, বইয়ের পাতা",
                fontId = "noto_serif_bn",
                fontSize = 16f,
                colorHex = "#302C29",
              )
            )
            add(
              CanvasElement(
                id = "el-stamp",
                type = "sticker",
                assetId = "floral_shapla_lotus",
                x = 0.65f,
                y = 0.65f,
                w = 0.22f,
                h = 0.18f,
              )
            )
            if (mediaFile != null) {
              add(
                CanvasElement(
                  id = "el-voice",
                  type = "voice",
                  blobRef = mediaFile.absolutePath,
                  text = "Voice 0:08",
                  x = 0.10f,
                  y = 0.72f,
                  w = 0.50f,
                  h = 0.08f,
                )
              )
            }
          },
      )
    val fillerPages =
      (1..4).map { idx ->
        Page(
          id = "exp-page-${idx + 1}",
          diaryId = "exp-diary-1",
          order = idx,
          background = PageBackground(presetId = if (idx % 2 == 0) "ruled" else "dot_grid"),
          elements =
            listOf(
              CanvasElement(
                id = "el-p-$idx",
                type = "label",
                assetId = "label_ribbon_gold",
                text = "Page ${idx + 1}",
                x = 0.15f,
                y = 0.20f,
                w = 0.70f,
                h = 0.10f,
              )
            ),
        )
      }
    return DiaryWithPages(
      diary =
        Diary(
          id = "exp-diary-1",
          title = "স্মৃতির খাতা · Export Test",
          description = "বাংলা যুক্তবর্ণ ও দুই-পাতার স্প্রেড পরীক্ষা",
          cover = DiaryCover(monogram = "ৎ"),
          pageIds = listOf(page1.id) + fillerPages.map { it.id },
        ),
      pages = listOf(page1) + fillerPages,
    )
  }

  @Test
  fun `native pdf and bitmap exporter render Bangla conjuncts cleanly across single, spread, png, and zip`() {
    val diary = sampleDiaryWithBanglaConjuncts()

    // 1. Bitmap rendering check (Bangla conjuncts rendered onto cream parchment)
    val bmp = DiaryExporter.exportPageToBitmap(context, diary.pages.first(), NumeralStyle.BENGALI, 720, 1000)
    assertEquals(720, bmp.width)
    assertEquals(1000, bmp.height)
    bmp.recycle()

    // 2. Single-Page PNG stream
    val pngOut = ByteArrayOutputStream()
    DiaryExporter.exportPageToImage(context, diary.pages.first(), NumeralStyle.BENGALI, Bitmap.CompressFormat.PNG, pngOut)
    val pngBytes = pngOut.toByteArray()
    assertTrue("PNG bytes should be non-empty", pngBytes.size > 1000)
    // PNG magic header
    assertEquals(0x89.toByte(), pngBytes[0])
    assertEquals('P'.code.toByte(), pngBytes[1])

    // 3. Single-Page Native PdfDocument (1 cover + 5 pages = 6 PDF pages)
    val singlePdfOut = ByteArrayOutputStream()
    DiaryExporter.exportDiaryToPdf(context, diary, ReadingLayout.SINGLE, NumeralStyle.BENGALI, singlePdfOut)
    val singlePdfBytes = singlePdfOut.toByteArray()
    val singlePdfHeader = String(singlePdfBytes, Charsets.ISO_8859_1)
    assertTrue("Single PDF must start with %PDF-", singlePdfHeader.startsWith("%PDF-"))
    assertTrue("Single PDF must contain 6 pages or EOF marker", singlePdfBytes.size > 2000)

    // 4. Two-Page Book Spread Native PdfDocument (1 cover + 3 spread sheets = 4 PDF sheets)
    val spreadPdfOut = ByteArrayOutputStream()
    DiaryExporter.exportDiaryToPdf(context, diary, ReadingLayout.SPREAD, NumeralStyle.LATIN, spreadPdfOut)
    val spreadPdfBytes = spreadPdfOut.toByteArray()
    val spreadPdfHeader = String(spreadPdfBytes, Charsets.ISO_8859_1)
    assertTrue("Spread PDF must start with %PDF-", spreadPdfHeader.startsWith("%PDF-"))
    assertTrue("Spread PDF must be substantial", spreadPdfBytes.size > 2000)

    // 5. All-Pages ZIP pack
    val zipOut = ByteArrayOutputStream()
    DiaryExporter.exportDiaryImagesZip(context, diary, NumeralStyle.BENGALI, zipOut)
    val zipEntries = mutableListOf<String>()
    ZipInputStream(ByteArrayInputStream(zipOut.toByteArray())).use { zip ->
      var entry = zip.nextEntry
      while (entry != null) {
        zipEntries.add(entry.name)
        zip.closeEntry()
        entry = zip.nextEntry
      }
    }
    assertEquals(5, zipEntries.size)
    assertEquals("page_001.png", zipEntries.first())
    assertEquals("page_005.png", zipEntries.last())
  }

  @Test
  fun `backup archive exports manifest and remaps local media cleanly on restore`() {
    val srcMedia = File(context.cacheDir, "sample_voice_clip.m4a").apply {
      writeBytes("FAKE_M4A_AUDIO_BYTES_12345".toByteArray())
    }
    val diary = sampleDiaryWithBanglaConjuncts(mediaFile = srcMedia)

    val archiveOut = ByteArrayOutputStream()
    BackupArchiveManager.exportFullBackup(listOf(diary), archiveOut)
    val zipBytes = archiveOut.toByteArray()
    assertTrue(zipBytes.size > 200)

    // Remove original file to simulate moving backup to a brand new device
    srcMedia.delete()
    assertFalse(srcMedia.exists())

    val restored = BackupArchiveManager.readFullBackup(context, ByteArrayInputStream(zipBytes))
    assertEquals(1, restored.size)
    val restoredDiary = restored.first()
    assertEquals(diary.diary.title, restoredDiary.diary.title)
    assertEquals(5, restoredDiary.pages.size)

    val restoredVoice = restoredDiary.pages.first().elements.firstOrNull { it.type == "voice" }
    assertTrue("Voice element should exist after restore", restoredVoice != null)
    val restoredPath = restoredVoice!!.blobRef!!
    assertTrue("Restored media path should live in filesDir/restored_media", restoredPath.contains("restored_media"))
    assertTrue("Restored media file must exist on disk", File(restoredPath).exists())
    assertEquals("FAKE_M4A_AUDIO_BYTES_12345", File(restoredPath).readText())
  }

  @Test
  fun `github releases update checker resolves urls and parses update manifests cleanly`() {
    assertEquals(
      "https://github.com/octo/sankalan/releases/latest/download/version.json",
      AppUpdateChecker.resolveManifestUrl("octo/sankalan"),
    )
    assertEquals(
      "https://example.com/custom/version.json",
      AppUpdateChecker.resolveManifestUrl("https://example.com/custom/version.json"),
    )

    val newerJson =
      """
      {
        "versionCode": 3,
        "versionName": "1.2.0",
        "changelogEn": "Added seasonal Bangla frames & fast PDF export.",
        "changelogBn": "নতুন বাংলা ফ্রেম ও দ্রুত পিডিএফ এক্সপোর্ট যুক্ত হয়েছে।",
        "downloadUrl": "https://github.com/octo/sankalan/releases/tag/v1.2.0"
      }
      """.trimIndent()
    val resNew = AppUpdateChecker.parseUpdateManifest(newerJson, currentVersionCode = 1, currentVersionName = "1.0")
    assertTrue(resNew is UpdateCheckResult.UpdateAvailable)
    val avail = resNew as UpdateCheckResult.UpdateAvailable
    assertEquals(3, avail.release.versionCode)
    assertEquals("1.2.0", avail.release.versionName)

    val sameRes = AppUpdateChecker.parseUpdateManifest(newerJson, currentVersionCode = 3, currentVersionName = "1.2.0")
    assertTrue(sameRes is UpdateCheckResult.UpToDate)

    assertEquals(
      "https://github.com/octo/sankalan/releases/latest",
      AppUpdateChecker.resolveFixedReleaseUrl("octo/sankalan"),
    )
    assertEquals(
      "https://github.com/octo/sankalan/releases/latest",
      AppUpdateChecker.resolveFixedReleaseUrl("https://github.com/octo/sankalan/releases/latest/download/version.json"),
    )
    val template = AppUpdateChecker.buildSampleManifestJson("octo/sankalan", 2, "1.1")
    assertTrue(template.contains("\"versionCode\": 2"))
    assertTrue(template.contains("https://github.com/octo/sankalan/releases/latest/download/sankalan-t.apk"))

    assertEquals(
      "https://github.com/NafisAhmed19/sankalan--/releases/latest/download/version.json",
      AppUpdateChecker.resolveManifestUrl("https://github.com/NafisAhmed19/sankalan--"),
    )
    assertEquals(
      "https://github.com/NafisAhmed19/sankalan--/releases/latest",
      AppUpdateChecker.resolveFixedReleaseUrl("NafisAhmed19/sankalan--"),
    )
    assertEquals(
      "https://github.com/NafisAhmed19/sankalan--/releases/latest",
      AppUpdateChecker.resolveFixedReleaseUrl(""),
    )

    val badRes = AppUpdateChecker.parseUpdateManifest("{broken json", currentVersionCode = 1, currentVersionName = "1.0")
    assertTrue(badRes is UpdateCheckResult.Failed)

    // Non-HTTPS downloadUrl and HTTP repo links must be rejected cleanly
    val httpDownloadJson =
      """
      {
        "versionCode": 9,
        "versionName": "2.0",
        "downloadUrl": "http://insecure.example.com/sankalan-t.apk"
      }
      """.trimIndent()
    assertTrue(AppUpdateChecker.parseUpdateManifest(httpDownloadJson, 1, "1.0") is UpdateCheckResult.Failed)
    assertEquals(
      "https://github.com/NafisAhmed19/sankalan--/releases/latest/download/version.json",
      AppUpdateChecker.resolveManifestUrl("http://github.com/evil/repo"),
    )
  }

  @Test
  fun `backup restore prevents zip slip path traversal outside restored_media`() {
    val archiveOut = ByteArrayOutputStream()
    ZipOutputStream(archiveOut).use { zip ->
      zip.putNextEntry(ZipEntry("manifest.json"))
      zip.write(
        """
        {
          "formatVersion": 1,
          "createdAt": 1700000000000,
          "appName": "Sankalan-t",
          "diaries": []
        }
        """.trimIndent().toByteArray(Charsets.UTF_8)
      )
      zip.closeEntry()

      zip.putNextEntry(ZipEntry("media/../../escaped_payload.txt"))
      zip.write("MALICIOUS".toByteArray(Charsets.UTF_8))
      zip.closeEntry()
    }

    val restored = BackupArchiveManager.readFullBackup(context, ByteArrayInputStream(archiveOut.toByteArray()))
    assertTrue(restored.isEmpty())
    assertFalse(File(context.filesDir, "escaped_payload.txt").exists())
    assertFalse(File(context.filesDir.parentFile, "escaped_payload.txt").exists())
  }

  @Test
  fun `update checker blocks authority spoofing URLs and backup restore strips unmapped external blobRefs`() {
    assertFalse(AppUpdateChecker.isSafeHttpsUrl("https://github.com@evil.example/apk"))
    assertFalse(AppUpdateChecker.isSafeHttpsUrl("https://user:pass@github.com/octo/repo"))
    assertTrue(AppUpdateChecker.isSafeHttpsUrl("https://github.com/NafisAhmed19/sankalan--"))

    val spoofManifest =
      """
      {
        "versionCode": 99,
        "versionName": "9.9.9",
        "downloadUrl": "https://github.com@evil.example/malware.apk"
      }
      """.trimIndent()
    assertTrue(AppUpdateChecker.parseUpdateManifest(spoofManifest, 1, "1.0") is UpdateCheckResult.Failed)

    // Backup manifest attempting to inject a local shared_prefs path via blobRef must be cleared
    val craftedZip = ByteArrayOutputStream()
    ZipOutputStream(craftedZip).use { zip ->
      zip.putNextEntry(ZipEntry("manifest.json"))
      zip.write(
        """
        {
          "formatVersion": 1,
          "createdAt": 1700000000000,
          "appName": "Sankalan-t",
          "diaries": [
            {
              "diary": {"id": "d-inj", "title": "Injected", "cover": {"presetId":"classic","colorHex":"#593B2B","accentHex":"#B08A4F","monogram":"ৎ"}, "pageIds": ["p-inj"]},
              "pages": [
                {
                  "id": "p-inj",
                  "diaryId": "d-inj",
                  "order": 0,
                  "background": {"presetId":"warm_ivory","tintHex":"#F6EFE2"},
                  "elements": [
                    {"id": "img-inj", "type": "image", "blobRef": "/data/data/com.example/shared_prefs/sankalan_lock_store.xml"}
                  ]
                }
              ]
            }
          ]
        }
        """.trimIndent().toByteArray(Charsets.UTF_8)
      )
      zip.closeEntry()
    }
    val restored = BackupArchiveManager.readFullBackup(context, ByteArrayInputStream(craftedZip.toByteArray()))
    val injectedEl = restored.single().pages.single().elements.single()
    assertEquals(null, injectedEl.blobRef)
  }
}
