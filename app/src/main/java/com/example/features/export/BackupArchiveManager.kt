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

  private const val MAX_MANIFEST_BYTES = 16L * 1024L * 1024L // 16 MB
  private const val MAX_MEDIA_ENTRY_BYTES = 50L * 1024L * 1024L // 50 MB
  private const val MAX_TOTAL_RESTORED_BYTES = 256L * 1024L * 1024L // 256 MB
  private const val MAX_ARCHIVE_ENTRIES = 5_000

  private fun readBoundedBytes(input: InputStream, maxBytes: Long): ByteArray {
    val buffer = ByteArray(8192)
    val out = java.io.ByteArrayOutputStream()
    var total = 0L
    while (true) {
      val read = input.read(buffer)
      if (read <= 0) break
      total += read
      if (total > maxBytes) error("Archive entry exceeds safe size limit")
      out.write(buffer, 0, read)
    }
    return out.toByteArray()
  }

  private fun copyBounded(
    input: InputStream,
    output: OutputStream,
    maxEntryBytes: Long,
    remainingArchiveBudget: Long,
  ): Long {
    val buffer = ByteArray(8192)
    var total = 0L
    while (true) {
      val read = input.read(buffer)
      if (read <= 0) break
      total += read
      if (total > maxEntryBytes || total > remainingArchiveBudget) {
        error("Media entry exceeds safe size limit")
      }
      output.write(buffer, 0, read)
    }
    return total
  }

  fun readFullBackup(
    context: Context,
    inputStream: InputStream,
  ): List<DiaryWithPages> {
    val restoredMediaDir = File(context.filesDir, "restored_media").apply { mkdirs() }
    val canonicalBase = restoredMediaDir.canonicalPath + File.separator
    var manifestJson: String? = null
    val extractedMediaPaths = mutableMapOf<String, String>() // zipEntryName -> newAbsPath
    var totalBytesRestored = 0L
    var entryCount = 0

    ZipInputStream(inputStream).use { zip ->
      var entry = zip.nextEntry
      while (entry != null) {
        entryCount += 1
        if (entryCount > MAX_ARCHIVE_ENTRIES) error("Archive exceeds safe entry limit")
        if (!entry.isDirectory) {
          when {
            entry.name == ENTRY_MANIFEST -> {
              val manifestBytes = readBoundedBytes(zip, MAX_MANIFEST_BYTES)
              totalBytesRestored += manifestBytes.size
              if (totalBytesRestored > MAX_TOTAL_RESTORED_BYTES) error("Archive exceeds safe size limit")
              manifestJson = manifestBytes.toString(Charsets.UTF_8)
            }
            entry.name.startsWith("media/") -> {
              val rawName = entry.name.removePrefix("media/")
              val sanitized =
                rawName
                  .replace("\\", "/")
                  .substringAfterLast('/')
                  .replace(Regex("[^A-Za-z0-9._-]"), "_")
              if (sanitized.isNotBlank() && sanitized != "." && sanitized != "..") {
                val outFile = File(restoredMediaDir, sanitized)
                if (outFile.canonicalPath.startsWith(canonicalBase)) {
                  val remainingBudget = MAX_TOTAL_RESTORED_BYTES - totalBytesRestored
                  val written =
                    FileOutputStream(outFile).use {
                      copyBounded(zip, it, MAX_MEDIA_ENTRY_BYTES, remainingBudget)
                    }
                  totalBytesRestored += written
                  extractedMediaPaths[entry.name] = outFile.absolutePath
                }
              }
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
                  // Only keep blobRef if it was safely extracted from this archive's media/ directory.
                  val mapped = el.blobRef?.let { extractedMediaPaths[it] }
                  el.copy(blobRef = mapped)
                }
            )
          }
      )
    }
  }
}
