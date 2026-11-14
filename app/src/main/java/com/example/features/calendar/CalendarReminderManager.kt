package com.example.features.calendar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.core.model.DiaryWithPages
import com.example.core.model.Page
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Generates RFC 5545 compliant .ics (iCalendar) exports containing dated diary entries
 * and optionally a daily recurring writing reminder event.
 */
object CalendarIcsExporter {
  fun buildIcs(
    diaries: List<DiaryWithPages>,
    datedPages: List<Page>,
    reminderEnabled: Boolean,
    reminderHour: Int,
    reminderMinute: Int,
    includeBanglaTitle: Boolean,
  ): String {
    val diaryMap = diaries.associateBy { it.diary.id }
    val utcStampFmt = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
    val nowStamp = utcStampFmt.format(Date())
    val sb = StringBuilder()

    fun line(raw: String) {
      sb.append(raw).append("\r\n")
    }

    fun escapeIcs(text: String): String =
      text
        .replace("\\", "\\\\")
        .replace("\r\n", "\\n")
        .replace("\n", "\\n")
        .replace(",", "\\,")
        .replace(";", "\\;")

    line("BEGIN:VCALENDAR")
    line("VERSION:2.0")
    line("PRODID:-//NRST.tech//Sankalan-t 1.0//EN")
    line("CALSCALE:GREGORIAN")
    line("METHOD:PUBLISH")

    // Recurring daily writing reminder if enabled
    if (reminderEnabled) {
      val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, reminderHour.coerceIn(0, 23))
        set(Calendar.MINUTE, reminderMinute.coerceIn(0, 59))
        set(Calendar.SECOND, 0)
      }
      val localStart = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US).format(cal.time)
      val summary = if (includeBanglaTitle) "সংকলন-ৎ — ডায়েরি লেখার সময়" else "Sankalan-t Daily Writing Moment"
      val desc =
        if (includeBanglaTitle) "আজকের অনুভূতি ও স্মৃতি নিজের ডায়েরিতে লিখে রাখুন।"
        else "Take a quiet evening minute to write a page in your personal Sankalan-t journal."

      line("BEGIN:VEVENT")
      line("UID:sankalan-daily-reminder@nrstech.local")
      line("DTSTAMP:$nowStamp")
      line("DTSTART:$localStart")
      line("DURATION:PT15M")
      line("RRULE:FREQ=DAILY")
      line("SUMMARY:${escapeIcs(summary)}")
      line("DESCRIPTION:${escapeIcs(desc)}")
      line("END:VEVENT")
    }

    // Dated diary pages as All-Day VEVENT milestones
    datedPages
      .filter { !it.isDeleted && !it.dateIso.isNullOrBlank() && it.diaryId in diaryMap }
      .forEach { page ->
        val dateCompact = page.dateIso!!.replace("-", "")
        if (dateCompact.length == 8) {
          val diaryTitle = diaryMap[page.diaryId]?.diary?.title.orEmpty()
          val pageTitle = page.title?.takeIf { it.isNotBlank() } ?: "Journal Entry"
          val excerpt =
            page.elements
              .firstOrNull { !it.hidden && !it.text.isNullOrBlank() }
              ?.text
              .orEmpty()
              .take(240)
          line("BEGIN:VEVENT")
          line("UID:sankalan-page-${page.id}@nrstech.local")
          line("DTSTAMP:$nowStamp")
          line("DTSTART;VALUE=DATE:$dateCompact")
          line("SUMMARY:${escapeIcs("$diaryTitle: $pageTitle")}")
          if (excerpt.isNotBlank()) {
            line("DESCRIPTION:${escapeIcs(excerpt)}")
          }
          line("END:VEVENT")
        }
      }

    line("END:VCALENDAR")
    return sb.toString()
  }
}

object ReminderNotifier {
  private const val CHANNEL_ID = "sankalan_daily_reminder"

  fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      val existing = manager.getNotificationChannel(CHANNEL_ID)
      if (existing == null) {
        val channel =
          NotificationChannel(
            CHANNEL_ID,
            "Sankalan-t Writing Reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
          ).apply {
            description = "Gentle offline reminders to write in your journal."
          }
        manager.createNotificationChannel(channel)
      }
    }
  }

  fun showGentleReminderNotification(context: Context, isBangla: Boolean) {
    ensureChannel(context)
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
    val title = if (isBangla) "সংকলন-ৎ — ডায়েরি লেখার সময়" else "Sankalan-t · Quiet Writing Time"
    val body =
      if (isBangla) "আজকের একটি ছোট স্মৃতি বা অনুভূতি আপনার ডায়েরিতে ধরে রাখুন।"
      else "A calm page awaits your thoughts and memories today."
    val notification =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(body)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()
    runCatching { manager.notify(1009, notification) }
  }
}
