package com.example.features.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.core.i18n.formatDigits
import com.example.core.model.CanvasElement
import com.example.core.model.Diary
import com.example.core.model.DiaryWithPages
import com.example.core.model.NumeralStyle
import com.example.core.model.Page
import com.example.core.model.ReadingLayout
import com.example.features.editor.CanvasPayloadCodec
import com.example.features.editor.PenKind
import com.example.features.editor.ShapeKind
import com.example.features.studio.findPaperPreset
import com.example.features.studio.findStudioAsset
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Step 10 Native Exporter:
 * Uses android.graphics.pdf.PdfDocument, android.graphics.Canvas, StaticLayout, and bundled TTF fonts
 * to render crisp Single-Page PDFs, Two-Page Book Spread PDFs, PNG/JPEG page images, and ZIP packs.
 */
object DiaryExporter {
  private const val PAGE_W = 792
  private const val PAGE_H = 1100

  fun exportDiaryToPdf(
    context: Context,
    diaryWithPages: DiaryWithPages,
    layout: ReadingLayout,
    numeralStyle: NumeralStyle,
    outputStream: OutputStream,
  ) {
    val pages = diaryWithPages.pages.filter { !it.isDeleted }.sortedBy { it.order }
    val nativeSuccess =
      runCatching {
        exportWithAndroidPdfDocument(context, diaryWithPages, pages, layout, numeralStyle, outputStream)
      }.isSuccess

    if (!nativeSuccess) {
      exportBitmapBackedPdf14(context, diaryWithPages, pages, layout, numeralStyle, outputStream)
    }
  }

  private fun exportWithAndroidPdfDocument(
    context: Context,
    diaryWithPages: DiaryWithPages,
    pages: List<Page>,
    layout: ReadingLayout,
    numeralStyle: NumeralStyle,
    outputStream: OutputStream,
  ) {
    val pdf = PdfDocument()
    try {
      if (layout == ReadingLayout.SINGLE) {
        val coverInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create()
        val coverPage = pdf.startPage(coverInfo)
        drawCoverPage(context, coverPage.canvas, diaryWithPages.diary, pages.size, numeralStyle, PAGE_W, PAGE_H)
        pdf.finishPage(coverPage)

        pages.forEachIndexed { idx, page ->
          val info = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, idx + 2).create()
          val pdfPage = pdf.startPage(info)
          drawJournalPage(context, pdfPage.canvas, page, numeralStyle, PAGE_W, PAGE_H)
          pdf.finishPage(pdfPage)
        }
      } else {
        val spreadW = PAGE_W * 2
        val coverInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create()
        val coverPdf = pdf.startPage(coverInfo)
        drawCoverPage(context, coverPdf.canvas, diaryWithPages.diary, pages.size, numeralStyle, PAGE_W, PAGE_H)
        pdf.finishPage(coverPdf)

        var sheetNum = 2
        var i = 0
        while (i < pages.size) {
          val left = pages[i]
          val right = pages.getOrNull(i + 1)
          val spreadInfo = PdfDocument.PageInfo.Builder(spreadW, PAGE_H, sheetNum++).create()
          val spreadPdf = pdf.startPage(spreadInfo)
          drawSpreadSheet(context, spreadPdf.canvas, left, right, numeralStyle, PAGE_W, PAGE_H)
          pdf.finishPage(spreadPdf)
          i += 2
        }
      }
      pdf.writeTo(outputStream)
    } finally {
      runCatching { pdf.close() }
    }
  }

  private fun drawSpreadSheet(
    context: Context,
    c: Canvas,
    left: Page,
    right: Page?,
    numeralStyle: NumeralStyle,
    singleW: Int,
    pageH: Int,
  ) {
    c.save()
    c.clipRect(0, 0, singleW, pageH)
    drawJournalPage(context, c, left, numeralStyle, singleW, pageH)
    c.restore()

    c.save()
    c.translate(singleW.toFloat(), 0f)
    c.clipRect(0, 0, singleW, pageH)
    if (right != null) {
      drawJournalPage(context, c, right, numeralStyle, singleW, pageH)
    } else {
      val emptyBg = Paint().apply { color = parseArgb("#E9DDC9", 0xFFE9DDC9.toInt()) }
      c.drawRect(0f, 0f, singleW.toFloat(), pageH.toFloat(), emptyBg)
    }
    c.restore()

    val gutter = Paint().apply {
      color = 0x33140E0C
      strokeWidth = 6f
    }
    c.drawLine(singleW.toFloat(), 0f, singleW.toFloat(), pageH.toFloat(), gutter)
  }

  private fun exportBitmapBackedPdf14(
    context: Context,
    diaryWithPages: DiaryWithPages,
    pages: List<Page>,
    layout: ReadingLayout,
    numeralStyle: NumeralStyle,
    outputStream: OutputStream,
  ) {
    data class RenderedSheet(val width: Int, val height: Int, val jpeg: ByteArray)

    fun bitmapToJpeg(bmp: Bitmap): ByteArray {
      val bos = java.io.ByteArrayOutputStream()
      bmp.compress(Bitmap.CompressFormat.JPEG, 90, bos)
      bmp.recycle()
      return bos.toByteArray()
    }

    val sheets = mutableListOf<RenderedSheet>()
    run {
      val bmp = Bitmap.createBitmap(PAGE_W, PAGE_H, Bitmap.Config.ARGB_8888)
      drawCoverPage(context, Canvas(bmp), diaryWithPages.diary, pages.size, numeralStyle, PAGE_W, PAGE_H)
      sheets.add(RenderedSheet(PAGE_W, PAGE_H, bitmapToJpeg(bmp)))
    }

    if (layout == ReadingLayout.SINGLE) {
      pages.forEach { page ->
        val bmp = Bitmap.createBitmap(PAGE_W, PAGE_H, Bitmap.Config.ARGB_8888)
        drawJournalPage(context, Canvas(bmp), page, numeralStyle, PAGE_W, PAGE_H)
        sheets.add(RenderedSheet(PAGE_W, PAGE_H, bitmapToJpeg(bmp)))
      }
    } else {
      val spreadW = PAGE_W * 2
      var i = 0
      while (i < pages.size) {
        val bmp = Bitmap.createBitmap(spreadW, PAGE_H, Bitmap.Config.ARGB_8888)
        drawSpreadSheet(context, Canvas(bmp), pages[i], pages.getOrNull(i + 1), numeralStyle, PAGE_W, PAGE_H)
        sheets.add(RenderedSheet(spreadW, PAGE_H, bitmapToJpeg(bmp)))
        i += 2
      }
    }

    // Write valid multi-page PDF 1.4 binary with JPEG XObjects
    val offsets = mutableListOf<Int>()
    val buf = java.io.ByteArrayOutputStream()
    fun writeAscii(s: String) = buf.write(s.toByteArray(Charsets.US_ASCII))

    writeAscii("%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n")
    // Object 1: Catalog, Object 2: Pages
    val pageCount = sheets.size
    val firstPageObjId = 3
    val pageIds = (0 until pageCount).map { firstPageObjId + it * 3 }

    offsets.add(buf.size()) // obj 1
    writeAscii("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

    offsets.add(buf.size()) // obj 2
    val kids = pageIds.joinToString(" ") { "$it 0 R" }
    writeAscii("2 0 obj\n<< /Type /Pages /Kids [$kids] /Count $pageCount >>\nendobj\n")

    sheets.forEachIndexed { idx, sheet ->
      val pageObjId = firstPageObjId + idx * 3
      val contentObjId = pageObjId + 1
      val imageObjId = pageObjId + 2
      val contentStream = "q ${sheet.width} 0 0 ${sheet.height} 0 0 cm /Im0 Do Q\n".toByteArray(Charsets.US_ASCII)

      offsets.add(buf.size())
      writeAscii(
        "$pageObjId 0 obj\n" +
          "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${sheet.width} ${sheet.height}] " +
          "/Resources << /XObject << /Im0 $imageObjId 0 R >> >> /Contents $contentObjId 0 R >>\n" +
          "endobj\n"
      )

      offsets.add(buf.size())
      writeAscii("$contentObjId 0 obj\n<< /Length ${contentStream.size} >>\nstream\n")
      buf.write(contentStream)
      writeAscii("\nendstream\nendobj\n")

      offsets.add(buf.size())
      writeAscii(
        "$imageObjId 0 obj\n" +
          "<< /Type /XObject /Subtype /Image /Width ${sheet.width} /Height ${sheet.height} " +
          "/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${sheet.jpeg.size} >>\nstream\n"
      )
      buf.write(sheet.jpeg)
      writeAscii("\nendstream\nendobj\n")
    }

    val xrefOffset = buf.size()
    val totalObjects = offsets.size + 1
    writeAscii("xref\n0 $totalObjects\n")
    writeAscii("0000000000 65535 f \n")
    offsets.forEach { off ->
      writeAscii(String.format(java.util.Locale.US, "%010d 00000 n \n", off))
    }
    writeAscii("trailer\n<< /Size $totalObjects /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n")
    outputStream.write(buf.toByteArray())
  }

  fun exportPageToBitmap(
    context: Context,
    page: Page,
    numeralStyle: NumeralStyle,
    width: Int = 1080,
    height: Int = 1500,
  ): Bitmap {
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    drawJournalPage(context, canvas, page, numeralStyle, width, height)
    return bmp
  }

  fun exportPageToImage(
    context: Context,
    page: Page,
    numeralStyle: NumeralStyle,
    format: Bitmap.CompressFormat,
    outputStream: OutputStream,
  ) {
    val bmp = exportPageToBitmap(context, page, numeralStyle)
    try {
      bmp.compress(format, 94, outputStream)
    } finally {
      bmp.recycle()
    }
  }

  fun exportDiaryImagesZip(
    context: Context,
    diaryWithPages: DiaryWithPages,
    numeralStyle: NumeralStyle,
    outputStream: OutputStream,
  ) {
    val pages = diaryWithPages.pages.filter { !it.isDeleted }.sortedBy { it.order }
    ZipOutputStream(outputStream).use { zip ->
      pages.forEachIndexed { idx, page ->
        val entry = ZipEntry("page_${String.format("%03d", idx + 1)}.png")
        zip.putNextEntry(entry)
        val bmp = exportPageToBitmap(context, page, numeralStyle, 900, 1250)
        try {
          bmp.compress(Bitmap.CompressFormat.PNG, 92, zip)
        } finally {
          bmp.recycle()
        }
        zip.closeEntry()
      }
    }
  }

  fun drawCoverPage(
    context: Context,
    canvas: Canvas,
    diary: Diary,
    pageCount: Int,
    numeralStyle: NumeralStyle,
    width: Int,
    height: Int,
  ) {
    val w = width.toFloat()
    val h = height.toFloat()
    val baseColor = parseArgb(diary.cover.colorHex, 0xFF593B2B.toInt())
    val accentColor = parseArgb(diary.cover.accentHex, 0xFFB08A4F.toInt())

    canvas.drawColor(baseColor)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      color = accentColor
      strokeWidth = w * 0.008f
    }
    val pad = w * 0.07f
    canvas.drawRoundRect(RectF(pad, pad, w - pad, h - pad), 18f, 18f, borderPaint)

    val serifBn = loadTypeface(context, R.font.noto_serif_bengali)
    val monoPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = serifBn
      color = accentColor
      textSize = w * 0.14f
      textAlign = Paint.Align.CENTER
    }
    canvas.drawText(diary.cover.monogram, w * 0.5f, h * 0.30f, monoPaint)

    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = serifBn
      color = 0xFFFAF6EE.toInt()
      textSize = w * 0.055f
    }
    drawMultilineText(canvas, diary.title, titlePaint, w * 0.12f, h * 0.40f, w * 0.76f, Layout.Alignment.ALIGN_CENTER)

    if (!diary.description.isNullOrBlank()) {
      val descPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = loadTypeface(context, R.font.noto_sans_bengali)
        color = 0xFFD9C9AC.toInt()
        textSize = w * 0.032f
      }
      drawMultilineText(canvas, diary.description, descPaint, w * 0.14f, h * 0.56f, w * 0.72f, Layout.Alignment.ALIGN_CENTER)
    }

    val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = serifBn
      color = accentColor
      textSize = w * 0.028f
      textAlign = Paint.Align.CENTER
    }
    canvas.drawText("${formatDigits(pageCount.toString(), numeralStyle)} pages · Sankalan-ৎ", w * 0.5f, h * 0.88f, footerPaint)
  }

  fun drawJournalPage(
    context: Context,
    canvas: Canvas,
    page: Page,
    numeralStyle: NumeralStyle,
    width: Int,
    height: Int,
  ) {
    val w = width.toFloat()
    val h = height.toFloat()
    val preset = findPaperPreset(page.background.presetId)
    val bgColor = parseArgb(page.background.tintHex, parseArgb(preset.tintHex, 0xFFF6EFE2.toInt()))
    val headerInk = if (preset.isDarkPaper) 0xFFFAF6EE.toInt() else 0xFF302C29.toInt()
    val lineInk = if (preset.isDarkPaper) 0x3DB08A4F else 0x20302C29

    // 1. Paper Fill & Pattern
    canvas.drawColor(bgColor)
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = lineInk
      strokeWidth = w * 0.0022f
    }
    when (preset.pattern) {
      "ruled" -> {
        var y = h * 0.14f
        val step = h * 0.055f
        while (y < h * 0.94f) {
          canvas.drawLine(w * 0.06f, y, w * 0.94f, y, linePaint)
          y += step
        }
      }
      "dot_grid" -> {
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = lineInk }
        var y = h * 0.10f
        while (y < h * 0.94f) {
          var x = w * 0.08f
          while (x < w * 0.93f) {
            canvas.drawCircle(x, y, w * 0.0028f, dotPaint)
            x += w * 0.06f
          }
          y += h * 0.045f
        }
      }
      "graph" -> {
        val step = w * 0.065f
        var x = w * 0.06f
        while (x < w * 0.94f) {
          canvas.drawLine(x, h * 0.06f, x, h * 0.94f, linePaint)
          x += step
        }
        var y = h * 0.06f
        while (y < h * 0.94f) {
          canvas.drawLine(w * 0.06f, y, w * 0.94f, y, linePaint)
          y += step
        }
      }
      "obsidian_gold" -> {
        val goldBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          color = 0x88B08A4F.toInt()
          strokeWidth = w * 0.003f
        }
        canvas.drawRect(w * 0.03f, h * 0.025f, w * 0.97f, h * 0.975f, goldBorder)
      }
    }

    // 2. Header (Date, Mood, Title)
    val metaLine = buildList {
      page.dateIso?.takeIf { it.isNotBlank() }?.let { add(formatDigits(it, numeralStyle)) }
      page.mood?.takeIf { it.isNotBlank() }?.let { add(it) }
    }.joinToString("  ·  ")

    if (metaLine.isNotEmpty()) {
      val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = loadTypeface(context, R.font.noto_sans_bengali)
        color = headerInk
        textSize = w * 0.026f
      }
      canvas.drawText(metaLine, w * 0.06f, h * 0.055f, metaPaint)
    }

    if (!page.title.isNullOrBlank()) {
      val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = loadTypeface(context, R.font.noto_serif_bengali)
        color = headerInk
        textSize = w * 0.038f
        isFakeBoldText = true
      }
      canvas.drawText(page.title, w * 0.06f, h * 0.098f, titlePaint)
      val dividerPaint = Paint().apply {
        color = 0x55B08A4F
        strokeWidth = 2f
      }
      canvas.drawLine(w * 0.06f, h * 0.112f, w * 0.94f, h * 0.112f, dividerPaint)
    }

    // 3. Elements sorted by zIndex
    val scalePx = w / 360f
    page.elements.filter { !it.hidden }.sortedBy { it.zIndex }.forEach { el ->
      canvas.save()
      val alphaInt = (el.opacity.coerceIn(0.05f, 1f) * 255).toInt()
      if (el.type == "stroke") {
        val payload = CanvasPayloadCodec.decodeStroke(el.strokePointsJson)
        if (payload != null && payload.points.isNotEmpty()) {
          val pen = PenKind.fromId(payload.tool)
          val penPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseArgb(payload.colorHex, 0xFF302C29.toInt())
            alpha =
              when (pen) {
                PenKind.HIGHLIGHTER -> (0.34f * alphaInt).toInt()
                PenKind.PENCIL -> (0.72f * alphaInt).toInt()
                else -> alphaInt
              }
            strokeCap = if (pen == PenKind.MARKER) Paint.Cap.SQUARE else Paint.Cap.ROUND
            strokeWidth = (payload.widthRatio * w * 1.1f).coerceAtLeast(2f)
          }
          val pts = payload.points
          if (pts.size == 1) {
            canvas.drawCircle(pts[0].x * w, pts[0].y * h, penPaint.strokeWidth / 2f, penPaint)
          } else {
            for (k in 0 until pts.lastIndex) {
              canvas.drawLine(pts[k].x * w, pts[k].y * h, pts[k + 1].x * w, pts[k + 1].y * h, penPaint)
            }
          }
        }
      } else {
        val left = el.x * w
        val top = el.y * h
        val ew = (el.w * w).coerceAtLeast(24f)
        val eh = (el.h * h).coerceAtLeast(20f)
        canvas.translate(left, top)
        if (el.rotation != 0f) {
          canvas.rotate(el.rotation, ew / 2f, eh / 2f)
        }
        drawElementOnCanvas(context, canvas, el, ew, eh, scalePx, alphaInt)
      }
      canvas.restore()
    }
  }

  private fun drawElementOnCanvas(
    context: Context,
    canvas: Canvas,
    el: CanvasElement,
    ew: Float,
    eh: Float,
    scalePx: Float,
    alphaInt: Int,
  ) {
    when (el.type) {
      "text" -> {
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
          typeface = loadEditorFont(context, el.fontId, el.bold, el.italic)
          color = parseArgb(el.colorHex ?: "#302C29", 0xFF302C29.toInt())
          alpha = alphaInt
          textSize = ((el.fontSize ?: 15f) * scalePx * 0.92f).coerceAtLeast(10f)
          isUnderlineText = el.underline
        }
        val align =
          when (el.align) {
            "center" -> Layout.Alignment.ALIGN_CENTER
            "right" -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_NORMAL
          }
        drawMultilineText(canvas, el.text.orEmpty(), tp, 6f, 6f, (ew - 12f).coerceAtLeast(12f), align)
      }

      "image" -> {
        val path = el.blobRef
        if (!path.isNullOrBlank() && File(path).exists()) {
          val bmp = BitmapFactory.decodeFile(path)
          if (bmp != null) {
            val dest = RectF(0f, 0f, ew, eh)
            val frameInfo = CanvasPayloadCodec.decodeImageFrame(el.strokePointsJson)
            if (frameInfo.frameStyle == "polaroid") {
              val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFAF6EE.toInt()
                alpha = alphaInt
              }
              canvas.drawRoundRect(dest, 8f, 8f, cardPaint)
              canvas.drawBitmap(bmp, null, RectF(8f, 8f, ew - 8f, eh - eh * 0.16f), Paint().apply { alpha = alphaInt })
            } else {
              canvas.drawBitmap(bmp, null, dest, Paint().apply { alpha = alphaInt })
            }
            bmp.recycle()
          }
        }
      }

      "shape" -> {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = parseArgb(el.colorHex ?: "#B08A4F", 0xFFB08A4F.toInt())
          alpha = alphaInt
        }
        when (el.assetId) {
          ShapeKind.DIVIDER_RULE.id -> {
            p.strokeWidth = 3f
            canvas.drawLine(0f, eh / 2f, ew, eh / 2f, p)
            canvas.drawCircle(ew / 2f, eh / 2f, 6f, p)
          }
          ShapeKind.ROUNDED_CARD.id -> {
            p.style = Paint.Style.STROKE
            p.strokeWidth = 3f
            canvas.drawRoundRect(RectF(2f, 2f, ew - 2f, eh - 2f), 14f, 14f, p)
          }
          else -> {
            p.style = Paint.Style.STROKE
            p.strokeWidth = 3f
            canvas.drawOval(RectF(2f, 2f, ew - 2f, eh - 2f), p)
          }
        }
      }

      "tape" -> {
        val asset = findStudioAsset(el.assetId)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = parseArgb(el.colorHex ?: asset?.colorHex ?: "#B08A4F", 0xFFB08A4F.toInt())
          alpha = (alphaInt * 0.78f).toInt()
        }
        canvas.drawRect(0f, 0f, ew, eh, p)
      }

      "frame" -> {
        val asset = findStudioAsset(el.assetId)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          color = parseArgb(el.colorHex ?: asset?.colorHex ?: "#B08A4F", 0xFFB08A4F.toInt())
          strokeWidth = 3f
          alpha = alphaInt
          if (el.assetId == "frame_vintage_stamp" || el.assetId == "frame_stitched_card" || el.assetId == "ephem_deckle_tab") {
            pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
          }
        }
        canvas.drawRoundRect(RectF(4f, 4f, ew - 4f, eh - 4f), 8f, 8f, p)
      }

      "label" -> {
        val asset = findStudioAsset(el.assetId)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = parseArgb(el.colorHex ?: asset?.colorHex ?: "#B08A4F", 0xFFB08A4F.toInt())
          alpha = alphaInt
        }
        canvas.drawRoundRect(RectF(0f, 0f, ew, eh), 10f, 10f, bg)
        val labelText = el.text?.takeIf { it.isNotBlank() } ?: asset?.defaultTextEn.orEmpty()
        if (labelText.isNotEmpty()) {
          val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = loadTypeface(context, R.font.noto_serif_bengali)
            color = parseArgb(asset?.secondaryHex ?: "#241A16", 0xFF241A16.toInt())
            textSize = (eh * 0.44f).coerceIn(12f, 24f)
            isFakeBoldText = true
          }
          drawMultilineText(canvas, labelText, tp, 8f, eh * 0.2f, (ew - 16f).coerceAtLeast(10f), Layout.Alignment.ALIGN_CENTER)
        }
      }

      "sticker" -> {
        if (!el.blobRef.isNullOrBlank() && File(el.blobRef).exists()) {
          val bmp = BitmapFactory.decodeFile(el.blobRef)
          if (bmp != null) {
            canvas.drawBitmap(bmp, null, RectF(0f, 0f, ew, eh), Paint().apply { alpha = alphaInt })
            bmp.recycle()
          }
        } else {
          val asset = findStudioAsset(el.assetId)
          val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseArgb(el.colorHex ?: asset?.colorHex ?: "#A83E2B", 0xFFA83E2B.toInt())
            alpha = alphaInt
          }
          val cx = ew / 2f
          val cy = eh / 2f
          val r = min(ew, eh) * 0.42f
          canvas.drawCircle(cx, cy, r, p)
          val innerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = parseArgb(asset?.secondaryHex ?: "#FAF6EE", 0xFFFAF6EE.toInt())
            strokeWidth = 2f
          }
          canvas.drawCircle(cx, cy, r * 0.78f, innerStroke)
          val stampText = el.text?.takeIf { it.isNotBlank() } ?: asset?.defaultTextEn
          if (!stampText.isNullOrBlank()) {
            val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
              typeface = loadTypeface(context, R.font.noto_serif_bengali)
              color = parseArgb(asset?.secondaryHex ?: "#FAF6EE", 0xFFFAF6EE.toInt())
              textSize = if (stampText.length <= 2) r * 0.75f else r * 0.34f
              textAlign = Paint.Align.CENTER
              isFakeBoldText = true
            }
            canvas.drawText(stampText, cx, cy + tp.textSize * 0.34f, tp)
          }
        }
      }

      "voice" -> {
        val pill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = 0xFF241A16.toInt()
          alpha = alphaInt
        }
        canvas.drawRoundRect(RectF(0f, 0f, ew, eh), eh / 2f, eh / 2f, pill)
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
          typeface = loadTypeface(context, R.font.noto_sans_bengali)
          color = 0xFFDFC690.toInt()
          textSize = (eh * 0.38f).coerceIn(11f, 20f)
        }
        canvas.drawText("▶  ${el.text ?: "Voice Memory"}", 16f, eh * 0.62f, tp)
      }
    }
  }

  private fun drawMultilineText(
    canvas: Canvas,
    text: String,
    paint: TextPaint,
    x: Float,
    y: Float,
    maxWidth: Float,
    alignment: Layout.Alignment,
  ) {
    if (maxWidth <= 4f || text.isEmpty()) return
    val layout =
      StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth.toInt())
        .setAlignment(alignment)
        .setLineSpacing(0f, 1.38f)
        .build()
    canvas.save()
    canvas.translate(x, y)
    layout.draw(canvas)
    canvas.restore()
  }

  private fun loadEditorFont(context: Context, fontId: String?, bold: Boolean, italic: Boolean): Typeface {
    val resId =
      when (fontId) {
        "playfair" -> R.font.playfair_display
        "inter" -> R.font.inter
        "noto_serif_bn" -> R.font.noto_serif_bengali
        "caveat" -> R.font.caveat
        "galada" -> R.font.galada
        "atma" -> R.font.atma
        else -> R.font.noto_sans_bengali
      }
    val base = loadTypeface(context, resId)
    val style =
      when {
        bold && italic -> Typeface.BOLD_ITALIC
        bold -> Typeface.BOLD
        italic -> Typeface.ITALIC
        else -> Typeface.NORMAL
      }
    return Typeface.create(base, style)
  }

  private fun loadTypeface(context: Context, resId: Int): Typeface =
    runCatching { ResourcesCompat.getFont(context, resId) }.getOrNull() ?: Typeface.DEFAULT

  @ColorInt
  private fun parseArgb(hex: String?, fallback: Int): Int {
    if (hex.isNullOrBlank()) return fallback
    return runCatching { android.graphics.Color.parseColor(hex.trim()) }.getOrDefault(fallback)
  }
}
