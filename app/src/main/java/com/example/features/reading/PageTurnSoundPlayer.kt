package com.example.features.reading

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates an offline soft paper-rustle PCM WAV sound in app cache on first use
 * and plays it via SoundPool when Page-Turn Sound is enabled.
 */
class PageTurnSoundPlayer(context: Context) {
  private var soundPool: SoundPool? = null
  private var soundId: Int = 0
  private var loaded: Boolean = false

  init {
    runCatching {
      val wavFile = File(context.cacheDir, "sankalan_page_turn_v1.wav")
      if (!wavFile.exists() || wavFile.length() < 128L) {
        writeSoftPaperRustleWav(wavFile)
      }
      val pool =
        SoundPool.Builder()
          .setMaxStreams(2)
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build()
          )
          .build()
      pool.setOnLoadCompleteListener { _, id, status ->
        if (status == 0 && id == soundId) {
          loaded = true
        }
      }
      soundPool = pool
      soundId = pool.load(wavFile.absolutePath, 1)
    }
  }

  fun playIfEnabled(enabled: Boolean) {
    if (!enabled || !loaded || soundId == 0) return
    runCatching { soundPool?.play(soundId, 0.34f, 0.34f, 1, 0, 1.0f) }
  }

  fun release() {
    runCatching { soundPool?.release() }
    soundPool = null
    loaded = false
  }

  private fun writeSoftPaperRustleWav(file: File) {
    val sampleRate = 22050
    val durationMs = 140
    val numSamples = (sampleRate * durationMs) / 1000
    val pcm = ByteArray(numSamples * 2)
    val rng = Random(2026)
    var prevFilter = 0.0

    for (i in 0 until numSamples) {
      val t = i.toDouble() / numSamples
      // Crisp envelope peaking near ~35% then decaying softly
      val envelope = sin(PI * t) * exp(-2.2 * t)
      val white = rng.nextDouble(-1.0, 1.0)
      // Gentle high-pass + low-pass band-shaping for dry paper fiber sound
      val smoothed = prevFilter * 0.62 + white * 0.38
      val rustle = (white - smoothed * 0.72) * envelope * 0.42
      prevFilter = smoothed

      val sampleShort = (rustle.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
      pcm[i * 2] = (sampleShort.toInt() and 0xFF).toByte()
      pcm[i * 2 + 1] = ((sampleShort.toInt() shr 8) and 0xFF).toByte()
    }

    val header = ByteArrayOutputStream(44)
    val byteRate = sampleRate * 2
    fun writeIntLE(v: Int) {
      header.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array())
    }
    fun writeShortLE(v: Short) {
      header.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(v).array())
    }

    header.write("RIFF".toByteArray(Charsets.US_ASCII))
    writeIntLE(36 + pcm.size)
    header.write("WAVE".toByteArray(Charsets.US_ASCII))
    header.write("fmt ".toByteArray(Charsets.US_ASCII))
    writeIntLE(16) // PCM chunk size
    writeShortLE(1) // PCM format
    writeShortLE(1) // Mono
    writeIntLE(sampleRate)
    writeIntLE(byteRate)
    writeShortLE(2) // Block align
    writeShortLE(16) // Bits per sample
    header.write("data".toByteArray(Charsets.US_ASCII))
    writeIntLE(pcm.size)

    FileOutputStream(file).use { out ->
      out.write(header.toByteArray())
      out.write(pcm)
    }
  }
}
