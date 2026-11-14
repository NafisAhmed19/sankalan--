package com.example.features.export

import android.content.Context
import com.example.core.model.DiaryWithPages
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class SankalanBackupManifest(
  val formatVersion: Int = 1,
  val createdAt: Long = System.currentTimeMillis(),
  val appName: String = "Sankalan-t",
  val diaries: List<DiaryWithPages>,
)

/**
 * Step 10 Portable Offline Archive (.sankalan ZIP) combining structured journals JSON
 * and embedded local media/voice/upload files with path remapping across installs/devices.
 */
object BackupArchiveManager {
  private const val ENTRY_MANIFEST = "manifest.json"

  private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val adapter = moshi.adapter(SankalanBackupManifest::class.java)

  fun exportFullBackup(
    diaries: List<DiaryWithPages>,
    outputStream: OutputStream,
  ) {
    val mediaToPack = linkedMapOf<String, String>() // absPath -> zipEntryName
    val remappedDiaries =
      diaries.map { bundle ->
        bundle.copy(
          pages =
            bundle.pages.map { page ->
              page.copy(
                elements =
                  page.elements.map { el ->
                    val ref = el.blobRef
                    if (!ref.isNullOrBlank() && File(ref).exists()) {
                      val zipName =
                        mediaToPack.getOrPut(ref) {
                          val ext = File(ref).extension.ifBlank { "bin" }
                          "media/${UUID.randomUUID()}.$ext"
                        }
                      el.copy(blobRef = zipName)
                    } else {
                      el
                    }
                  }
              )
            }
        )
      }

    val json = adapter.toJson(SankalanBackupManifest(diaries = remappedDiaries))
    ZipOutputStream(outputStream).use { zip ->
      zip.putNextEntry(ZipEntry(ENTRY_MANIFEST))
      zip.write(json.toByteArray(Charsets.UTF_8))
      zip.closeEntry()

      mediaToPack.forEach { (absPath, zipEntryPath) ->
        val src = File(absPath)
        if (src.exists()) {
          zip.putNextEntry(ZipEntry(zipEntryPath))
          FileInputStream(src).use { it.copyTo(zip) }
          zip.closeEntry()
        }
      }
    }
  }

  fun readFullBackup(
    context: Context,
    inputStream: InputStream,
  ): List<DiaryWithPages> {
    val restoredMediaDir = File(context.filesDir, "restored_media").apply { mkdirs() }
    var manifestJson: String? = null
    val extractedMediaPaths = mutableMapOf<String, String>() // zipEntryName -> newAbsPath

    ZipInputStream(inputStream).use { zip ->
      var entry = zip.nextEntry
      while (entry != null) {
        if (!entry.isDirectory) {
          when {
            entry.name == ENTRY_MANIFEST -> {
              manifestJson = zip.readBytes().toString(Charsets.UTF_8)
            }
            entry.name.startsWith("media/") -> {
              val safeFileName = entry.name.removePrefix("media/").replace("/", "_")
              val outFile = File(restoredMediaDir, safeFileName)
              FileOutputStream(outFile).use { zip.copyTo(it) }
              extractedMediaPaths[entry.name] = outFile.absolutePath
            }
          }
        }
        zip.closeEntry()
        entry = zip.nextEntry
      }
    }

    val rawManifest = manifestJson ?: error("Missing manifest.json inside archive")
    val decoded = adapter.fromJson(rawManifest) ?: error("Corrupt manifest.json")
    return decoded.diaries.map { bundle ->
      bundle.copy(
        pages =
          bundle.pages.map { page ->
            page.copy(
              elements =
                page.elements.map { el ->
                  val mapped = el.blobRef?.let { extractedMediaPaths[it] }
                  if (mapped != null) el.copy(blobRef = mapped) else el
                }
            )
          }
      )
    }
  }
}
