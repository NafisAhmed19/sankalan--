package com.example.features.editor

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.util.UUID

/**
 * Offline Voice Note recorder & player storing local .m4a audio clips in filesDir/voice_notes.
 */
class VoiceNoteManager(private val context: Context) {
  private var recorder: MediaRecorder? = null
  private var currentRecordingFile: File? = null
  private var recordStartMs: Long = 0L

  private var player: MediaPlayer? = null
  var activePlayingElementId: String? = null
    private set

  fun startRecording(): Boolean {
    stopRecording(discard = true)
    return runCatching {
      val dir = File(context.filesDir, "voice_notes").apply { mkdirs() }
      val target = File(dir, "voice_${UUID.randomUUID()}.m4a")
      val mr =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          MediaRecorder(context)
        } else {
          @Suppress("DEPRECATION")
          MediaRecorder()
        }
      mr.setAudioSource(MediaRecorder.AudioSource.MIC)
      mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
      mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
      mr.setAudioSamplingRate(44100)
      mr.setAudioEncodingBitRate(96000)
      mr.setOutputFile(target.absolutePath)
      mr.prepare()
      mr.start()
      recorder = mr
      currentRecordingFile = target
      recordStartMs = System.currentTimeMillis()
      true
    }.getOrDefault(false)
  }

  /**
   * Stops recording and returns Pair(filePath, durationSeconds), or null if discarded/too short.
   */
  fun stopRecording(discard: Boolean = false): Pair<String, Int>? {
    val mr = recorder ?: return null
    val file = currentRecordingFile
    val elapsedSec = (((System.currentTimeMillis() - recordStartMs) / 1000L).toInt()).coerceAtLeast(1)
    recorder = null
    currentRecordingFile = null
    runCatching {
      mr.stop()
      mr.reset()
      mr.release()
    }.onFailure {
      runCatching { mr.release() }
    }
    if (discard || file == null || !file.exists()) {
      runCatching { file?.delete() }
      return null
    }
    return file.absolutePath to elapsedSec
  }

  fun togglePlay(
    elementId: String,
    filePath: String?,
    onStateChanged: (playingId: String?) -> Unit,
  ) {
    if (activePlayingElementId == elementId) {
      stopPlayback()
      onStateChanged(null)
      return
    }
    stopPlayback()
    if (filePath.isNullOrBlank()) {
      onStateChanged(null)
      return
    }
    val file = File(filePath)
    if (!file.exists()) {
      onStateChanged(null)
      return
    }
    runCatching {
      val mp = MediaPlayer()
      mp.setDataSource(file.absolutePath)
      mp.setOnCompletionListener {
        stopPlayback()
        onStateChanged(null)
      }
      mp.prepare()
      mp.start()
      player = mp
      activePlayingElementId = elementId
      onStateChanged(elementId)
    }.onFailure {
      stopPlayback()
      onStateChanged(null)
    }
  }

  fun stopPlayback() {
    val mp = player ?: return
    player = null
    activePlayingElementId = null
    runCatching {
      if (mp.isPlaying) mp.stop()
      mp.reset()
      mp.release()
    }
  }

  fun release() {
    stopRecording(discard = true)
    stopPlayback()
  }
}

fun formatDurationMmSs(seconds: Int): String {
  val safe = seconds.coerceAtLeast(0)
  val m = safe / 60
  val s = safe % 60
  return "%d:%02d".format(m, s)
}
