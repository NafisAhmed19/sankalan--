package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.data.DiaryRepository
import com.example.core.data.local.SankalanDatabase
import com.example.core.model.CanvasElement
import com.example.core.model.DiaryInvariants
import com.example.core.model.PageBackground
import com.example.features.editor.CanvasPayloadCodec
import com.example.features.editor.CanvasSnapEngine
import com.example.features.editor.CanvasTransformMath
import com.example.features.editor.ElementHitZone
import com.example.features.editor.ImageFramePayload
import com.example.features.editor.PenKind
import com.example.features.editor.PhotoFrameStyle
import com.example.features.editor.StrokePayload
import com.example.features.editor.StrokePoint
import com.example.features.editor.UndoRedoHistory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CanvasEditorEngineTest {

  private lateinit var context: Context
  private lateinit var database: SankalanDatabase
  private lateinit var repository: DiaryRepository

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context.getSharedPreferences("sankalan_seed_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    database =
      Room.inMemoryDatabaseBuilder(context, SankalanDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    repository = DiaryRepository(context = context, dao = database.diaryDao())
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `undo redo history tracks commits, undoes, redoes, clears redo on branch, and bounds max depth`() {
    val history = UndoRedoHistory(initialState = "s0", maxDepth = 3)
    assertFalse(history.canUndo)
    assertFalse(history.canRedo)

    history.commit("s1")
    history.commit("s2")
    history.commit("s3")
    history.commit("s4") // exceeds depth 3 -> drops s0

    assertEquals("s4", history.current)
    assertTrue(history.canUndo)
    assertEquals("s3", history.undo())
    assertEquals("s2", history.undo())
    assertEquals("s1", history.undo())
    assertFalse(history.canUndo) // s0 dropped because maxDepth=3
    assertNull(history.undo())

    assertTrue(history.canRedo)
    assertEquals("s2", history.redo())
    assertEquals("s3", history.redo())

    // Branching commit clears redo stack
    history.commit("s3-branch")
    assertFalse(history.canRedo)
    assertNull(history.redo())
    assertEquals("s3-branch", history.current)
  }

  @Test
  fun `snap engine snaps element center to 0_5 and aligns to sibling edges`() {
    val elementW = 0.20f
    val elementH = 0.10f
    // Element top-left at 0.393 -> center is 0.493, within 0.016 of 0.5
    val centerSnap =
      CanvasSnapEngine.snapPosition(
        candidateX = 0.393f,
        candidateY = 0.445f,
        elementW = elementW,
        elementH = elementH,
        otherElements = emptyList(),
      )
    assertEquals(0.40f, centerSnap.snappedX, 0.001f)
    assertEquals(0.45f, centerSnap.snappedY, 0.001f)
    assertTrue(centerSnap.verticalGuides.any { kotlin.math.abs(it - 0.5f) < 0.001f })
    assertTrue(centerSnap.horizontalGuides.any { kotlin.math.abs(it - 0.5f) < 0.001f })

    // Sibling edge snap
    val sibling = CanvasElement(id = "sib-1", type = "shape", x = 0.15f, y = 0.25f, w = 0.30f, h = 0.10f)
    val siblingSnap =
      CanvasSnapEngine.snapPosition(
        candidateX = 0.158f, // within threshold of sibling left x=0.15
        candidateY = 0.60f,
        elementW = 0.20f,
        elementH = 0.10f,
        otherElements = listOf(sibling),
      )
    assertEquals(0.15f, siblingSnap.snappedX, 0.001f)
    assertTrue(siblingSnap.verticalGuides.any { kotlin.math.abs(it - 0.15f) < 0.001f })
  }

  @Test
  fun `payload codec roundtrips pressure stroke and torn paper image frame payloads`() {
    val stroke =
      StrokePayload(
        tool = PenKind.HIGHLIGHTER.id,
        colorHex = "#E9C46A",
        widthRatio = 0.018f,
        opacity = 0.42f,
        points = listOf(StrokePoint(0.1f, 0.2f, 0.35f), StrokePoint(0.8f, 0.4f, 0.92f)),
      )
    val strokeJson = CanvasPayloadCodec.encodeStroke(stroke)
    val decodedStroke = CanvasPayloadCodec.decodeStroke(strokeJson)
    assertNotNull(decodedStroke)
    assertEquals(PenKind.HIGHLIGHTER.id, decodedStroke!!.tool)
    assertEquals("#E9C46A", decodedStroke.colorHex)
    assertEquals(2, decodedStroke.points.size)
    assertEquals(0.92f, decodedStroke.points[1].pressure, 0.001f)

    val imagePayload =
      ImageFramePayload(
        frameStyle = PhotoFrameStyle.TORN_PAPER.id,
        cropZoom = 1.65f,
        cropOffsetX = -0.18f,
        cropOffsetY = 0.24f,
      )
    val imageJson = CanvasPayloadCodec.encodeImageFrame(imagePayload)
    val decodedImage = CanvasPayloadCodec.decodeImageFrame(imageJson)
    assertEquals(PhotoFrameStyle.TORN_PAPER.id, decodedImage.frameStyle)
    assertEquals(1.65f, decodedImage.cropZoom, 0.001f)
    assertEquals(-0.18f, decodedImage.cropOffsetX, 0.001f)
    assertEquals(0.24f, decodedImage.cropOffsetY, 0.001f)
  }

  @Test
  fun `repository savePageChanges persists layered text stroke photo elements and Bangla fonts`() =
    runBlocking {
      repository.seedDemoDiaryIfFirstLaunch()
      val initialDiary = repository.allDiariesFlow.first().first()
      val targetPage = initialDiary.pages[1]
      val strokeJson =
        CanvasPayloadCodec.encodeStroke(
          StrokePayload(
            tool = PenKind.PEN.id,
            colorHex = "#593B2B",
            widthRatio = 0.009f,
            opacity = 1f,
            points = listOf(StrokePoint(0.2f, 0.2f, 0.6f), StrokePoint(0.6f, 0.5f, 0.8f)),
          )
        )
      val photoJson =
        CanvasPayloadCodec.encodeImageFrame(
          ImageFramePayload(frameStyle = PhotoFrameStyle.TORN_PAPER.id, cropZoom = 1.4f)
        )

      val newElements =
        listOf(
          CanvasElement(
            id = "el-bn-text",
            type = "text",
            x = 0.10f,
            y = 0.16f,
            w = 0.78f,
            h = 0.22f,
            text = "সংকলন · মনের ডায়েরি",
            fontId = "galada",
            fontSize = 18f,
            colorHex = "#302C29",
            bold = true,
            zIndex = 1,
          ),
          CanvasElement(
            id = "el-stroke",
            type = "stroke",
            x = 0.15f,
            y = 0.35f,
            w = 0.55f,
            h = 0.25f,
            strokePointsJson = strokeJson,
            zIndex = 2,
          ),
          CanvasElement(
            id = "el-photo",
            type = "image",
            x = 0.22f,
            y = 0.52f,
            w = 0.48f,
            h = 0.32f,
            blobRef = "/tmp/sample.jpg",
            strokePointsJson = photoJson,
            zIndex = 3,
            groupId = "grp-memory-1",
          ),
        )

      val updatedPage =
        targetPage.copy(
          title = "সন্ধ্যার স্মৃতি",
          dateIso = "2026-11-12",
          mood = "Grateful",
          pinned = true,
          background = PageBackground(presetId = "antique_parchment", edgeStyle = "deckle"),
          elements = newElements,
        )

      val saveResult = repository.savePageChanges(initialDiary.diary.id, updatedPage)
      assertTrue(saveResult.isSuccess)

      val reloaded = repository.observeDiary(initialDiary.diary.id).first()!!
      assertTrue(DiaryInvariants.validateDiaryPages(reloaded.diary, reloaded.pages).isSuccess)

      val savedPage = reloaded.pages.first { it.id == targetPage.id }
      assertEquals("সন্ধ্যার স্মৃতি", savedPage.title)
      assertEquals("2026-11-12", savedPage.dateIso)
      assertEquals("Grateful", savedPage.mood)
      assertTrue(savedPage.pinned)
      assertEquals("deckle", savedPage.background.edgeStyle)
      assertEquals(3, savedPage.elements.size)

      val savedText = savedPage.elements.first { it.id == "el-bn-text" }
      assertEquals("galada", savedText.fontId)
      assertTrue(savedText.bold)

      val savedPhoto = savedPage.elements.first { it.id == "el-photo" }
      assertEquals("grp-memory-1", savedPhoto.groupId)
      assertEquals(
        PhotoFrameStyle.TORN_PAPER.id,
        CanvasPayloadCodec.decodeImageFrame(savedPhoto.strokePointsJson).frameStyle,
      )
    }

  @Test
  fun `canvas transform math handles rotated hit zones, rotation knob angles, and multi-touch pinch rotate`() {
    val element =
      CanvasElement(
        id = "sticker-1",
        type = "sticker",
        x = 0.30f,
        y = 0.30f,
        w = 0.24f,
        h = 0.20f,
        rotation = 0f,
      )
    // Center is (0.42, 0.40), top edge y is 0.30, rotate knob sits around (0.42, 0.245)
    assertEquals(
      ElementHitZone.ROTATE_HANDLE,
      CanvasTransformMath.hitTestElement(0.42f, 0.245f, element, canvasAspect = 0.72f, isSelected = true),
    )
    assertEquals(
      ElementHitZone.RESIZE_HANDLE,
      CanvasTransformMath.hitTestElement(0.54f, 0.50f, element, canvasAspect = 0.72f, isSelected = true),
    )
    assertEquals(
      ElementHitZone.BODY,
      CanvasTransformMath.hitTestElement(0.42f, 0.40f, element, canvasAspect = 0.72f, isSelected = true),
    )

    // Clockwise angles measured from top (12 o'clock)
    assertEquals(0f, CanvasTransformMath.computeRotationDegreesFromTop(100f, 50f, 100f, 100f), 0.1f)
    assertEquals(90f, CanvasTransformMath.computeRotationDegreesFromTop(150f, 100f, 100f, 100f), 0.1f)
    assertEquals(180f, CanvasTransformMath.computeRotationDegreesFromTop(100f, 150f, 100f, 100f), 0.1f)

    // Multi-touch pinch + rotate + pan
    val transformed =
      CanvasTransformMath.applyPinchRotateAndPan(
        el = element,
        zoomFactor = 1.25f,
        rotateDeltaDeg = 45f,
        panDxNorm = 0.05f,
        panDyNorm = -0.02f,
      )
    assertEquals(0.30f, transformed.w, 0.001f)
    assertEquals(0.25f, transformed.h, 0.001f)
    assertEquals(45f, transformed.rotation, 0.01f)
  }

  @Test
  fun `page body element vs diagram text box distinction and GPU LiveElementTransform commit`() {
    val bodyEl =
      CanvasElement(
        id = "p1-page-body",
        type = "text",
        assetId = com.example.features.editor.PAGE_BODY_ASSET_ID,
        x = 0.065f,
        y = 0.165f,
        w = 0.87f,
        h = 0.775f,
        text = "Daily page writing",
      )
    val legacyBodyEl =
      CanvasElement(
        id = "page-1-text-1",
        type = "text",
        assetId = null,
        x = 0.08f,
        y = 0.14f,
        w = 0.84f,
        h = 0.72f,
        text = "Legacy seeded page body",
      )
    val diagramBoxEl =
      CanvasElement(
        id = "diag-1",
        type = "text",
        assetId = com.example.features.editor.DIAGRAM_BOX_ASSET_ID,
        x = 0.22f,
        y = 0.36f,
        w = 0.48f,
        h = 0.16f,
        text = "Diagram Node A",
      )
    val stickerEl =
      CanvasElement(
        id = "sticker-move",
        type = "sticker",
        x = 0.30f,
        y = 0.40f,
        w = 0.20f,
        h = 0.20f,
        rotation = 10f,
      )

    assertTrue(com.example.features.editor.isPageBodyElement(bodyEl))
    assertFalse(com.example.features.editor.isDiagramTextBox(bodyEl))
    assertTrue(com.example.features.editor.isPageBodyElement(legacyBodyEl))
    assertFalse(com.example.features.editor.isPageBodyElement(diagramBoxEl))
    assertTrue(com.example.features.editor.isDiagramTextBox(diagramBoxEl))

    val live =
      com.example.features.editor.LiveElementTransform(
        targetIds = setOf(stickerEl.id),
        anchorElementId = stickerEl.id,
        dxNorm = 0.10f,
        dyNorm = -0.05f,
        scaleX = 1.5f,
        scaleY = 1.5f,
        rotationOverrideDeg = 90f,
        showRotationBadge = true,
      )
    val committed = CanvasTransformMath.commitLiveTransform(listOf(bodyEl, stickerEl), live)
    assertEquals(bodyEl, committed[0])
    val movedSticker = committed[1]
    assertEquals(0.30f, movedSticker.w, 0.001f)
    assertEquals(0.30f, movedSticker.h, 0.001f)
    assertEquals(0.35f, movedSticker.x, 0.001f)
    assertEquals(0.30f, movedSticker.y, 0.001f)
    assertEquals(90f, movedSticker.rotation, 0.001f)
  }
}
