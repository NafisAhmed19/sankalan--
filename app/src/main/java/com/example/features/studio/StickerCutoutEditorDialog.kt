package com.example.features.studio

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.BorderStyle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.i18n.LocalStrings
import com.example.ui.theme.LocalSankalanPalette
import com.example.ui.theme.SankalanSpacing
import com.example.ui.theme.SankalanTokens
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class StickerEditorTab {
  CROP,
  CUTOUT,
  BORDER,
}

private enum class CutoutTouchTool {
  LASSO,
  KEEP,
  ERASE,
}

private enum class CropDragHandle {
  TOP_LEFT,
  TOP_RIGHT,
  BOTTOM_LEFT,
  BOTTOM_RIGHT,
  MOVE,
}

private val BORDER_SWATCHES =
  listOf(
    "#FFFDF8",
    "#F5EBD8",
    "#C79A3E",
    "#2A211C",
    "#B64D38",
    "#627D5E",
  )

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StickerCutoutEditorDialog(
  sourceBitmap: Bitmap,
  initialTitle: String,
  onDismiss: () -> Unit,
  onSaveSticker: (stickerBitmap: Bitmap, title: String) -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val scope = rememberCoroutineScope()

  var activeTab by remember { mutableStateOf(StickerEditorTab.CUTOUT) }
  var stickerTitle by remember(initialTitle) { mutableStateOf(initialTitle) }

  // Stage 1: Crop & Turn state
  var rotationDegrees by remember { mutableIntStateOf(0) }
  var cropRect by remember { mutableStateOf(StickerCropRect(0f, 0f, 1f, 1f)) }

  // Stage 2: Object Identification & Refinement state
  var cutoutMode by remember { mutableStateOf(StickerCutoutMode.AUTO_SUBJECT) }
  var touchTool by remember { mutableStateOf(CutoutTouchTool.LASSO) }
  var lassoPolygon by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
  var activeLassoTrace by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
  var brushStrokes by remember { mutableStateOf<List<StickerBrushStroke>>(emptyList()) }
  var activeBrushPoints by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
  var sensitivity by remember { mutableFloatStateOf(0.55f) }

  // Stage 3: Sticker Border state (Initially ON per requirement!)
  var borderOptions by remember {
    mutableStateOf(
      StickerBorderOptions(
        enabled = true,
        thicknessPx = 14,
        colorHex = "#FFFDF8",
        doubleGoldTrim = false,
        shadowEnabled = true,
      )
    )
  }

  var isProcessingPreview by remember { mutableStateOf(false) }
  var isSaving by remember { mutableStateOf(false) }

  // Working cropped bitmap for Crop Tab vs Cutout/Border preview
  var croppedWorkingBitmap by remember(sourceBitmap) { mutableStateOf<Bitmap?>(null) }
  var rotatedSourceForCrop by remember(sourceBitmap) { mutableStateOf<Bitmap?>(null) }
  var previewStickerBitmap by remember(sourceBitmap) { mutableStateOf<Bitmap?>(null) }

  // Update rotated bitmap for Crop Tab
  LaunchedEffect(sourceBitmap, rotationDegrees) {
    if (sourceBitmap.isRecycled) return@LaunchedEffect
    val nextRot =
      withContext(Dispatchers.Default) {
        StickerCutoutEngine.cropAndRotateBitmap(
          source = sourceBitmap,
          cropRect = StickerCropRect(0f, 0f, 1f, 1f),
          rotationDegrees = rotationDegrees,
          maxDim = 640,
        )
      }
    rotatedSourceForCrop = nextRot
  }

  // Update cropped working bitmap when crop or rotation changes
  LaunchedEffect(sourceBitmap, cropRect, rotationDegrees) {
    if (sourceBitmap.isRecycled) return@LaunchedEffect
    val nextCrop =
      withContext(Dispatchers.Default) {
        StickerCutoutEngine.cropAndRotateBitmap(
          source = sourceBitmap,
          cropRect = cropRect,
          rotationDegrees = rotationDegrees,
          maxDim = 340,
        )
      }
    croppedWorkingBitmap = nextCrop
  }

  // Re-run smart object cutout + border preview whenever parameters change
  LaunchedEffect(croppedWorkingBitmap, cutoutMode, lassoPolygon, brushStrokes, sensitivity, borderOptions) {
    val cropped = croppedWorkingBitmap?.takeIf { !it.isRecycled } ?: return@LaunchedEffect
    isProcessingPreview = true
    try {
      val generated =
        withContext(Dispatchers.Default) {
          StickerCutoutEngine.renderStickerBitmap(
            cropped = cropped,
            mode = cutoutMode,
            lassoPolygon = lassoPolygon,
            brushStrokes = brushStrokes,
            sensitivity = sensitivity,
            border = borderOptions,
            trimOutput = false,
          )
        }
      previewStickerBitmap = generated
    } finally {
      isProcessingPreview = false
    }
  }

  Dialog(
    onDismissRequest = { if (!isSaving) onDismiss() },
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    Surface(
      color = palette.background,
      modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).testTag("sticker_cutout_editor"),
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Top Action Bar
        Surface(
          color = palette.shelfSurface,
          border = BorderStroke(1.dp, palette.borderSubtle),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = SankalanSpacing.md, vertical = SankalanSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            IconButton(
              onClick = { if (!isSaving) onDismiss() },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("sticker_editor_close_btn"),
            ) {
              Icon(Icons.Outlined.Close, contentDescription = strings.cancel, tint = palette.textPrimary)
            }

            Text(
              text = strings.stickerEditorTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = palette.textPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )

            Button(
              onClick = {
                if (isSaving) return@Button
                if (sourceBitmap.isRecycled) {
                  onDismiss()
                  return@Button
                }
                isSaving = true
                scope.launch {
                  val exportBmp =
                    try {
                      withContext(Dispatchers.Default) {
                        val fallbackSource =
                          if (!sourceBitmap.isRecycled) {
                            sourceBitmap
                          } else {
                            croppedWorkingBitmap?.takeIf { !it.isRecycled }
                              ?: rotatedSourceForCrop?.takeIf { !it.isRecycled }
                          }
                        if (fallbackSource == null) return@withContext null
                        val hiResCropped =
                          StickerCutoutEngine.cropAndRotateBitmap(
                            source = fallbackSource,
                            cropRect = cropRect,
                            rotationDegrees = rotationDegrees,
                            maxDim = 560,
                          )
                        val scaleFactor = max(1f, hiResCropped.width.toFloat() / 340f)
                        val scaledBorder =
                          borderOptions.copy(
                            thicknessPx = (borderOptions.thicknessPx * scaleFactor).toInt().coerceIn(0, 38)
                          )
                        val result =
                          StickerCutoutEngine.renderStickerBitmap(
                            cropped = hiResCropped,
                            mode = cutoutMode,
                            lassoPolygon = lassoPolygon,
                            brushStrokes = brushStrokes,
                            sensitivity = sensitivity,
                            border = scaledBorder,
                            trimOutput = true,
                          )
                        if (hiResCropped !== result && hiResCropped !== fallbackSource && !hiResCropped.isRecycled) {
                          hiResCropped.recycle()
                        }
                        result
                      }
                    } finally {
                      isSaving = false
                    }
                  if (exportBmp != null) {
                    onSaveSticker(exportBmp, stickerTitle)
                  } else {
                    onDismiss()
                  }
                }
              },
              enabled = !isSaving && croppedWorkingBitmap != null,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = palette.goldAccent,
                  contentColor = SankalanTokens.Walnut900,
                ),
              modifier = Modifier.minimumInteractiveComponentSize().testTag("sticker_editor_save_btn"),
            ) {
              if (isSaving) {
                CircularProgressIndicator(
                  modifier = Modifier.size(16.dp),
                  color = SankalanTokens.Walnut900,
                  strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.stickerSavingBtn)
              } else {
                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.stickerSaveBtn)
              }
            }
          }
        }

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
          val isWide = maxWidth >= 760.dp

          if (isWide) {
            Row(modifier = Modifier.fillMaxSize()) {
              Box(modifier = Modifier.weight(1.1f).fillMaxHeight().padding(SankalanSpacing.md)) {
                StickerStageViewport(
                  activeTab = activeTab,
                  rotatedSource = rotatedSourceForCrop,
                  croppedWorking = croppedWorkingBitmap,
                  previewSticker = previewStickerBitmap,
                  cropRect = cropRect,
                  onCropRectChange = { cropRect = it },
                  cutoutMode = cutoutMode,
                  touchTool = touchTool,
                  lassoPolygon = lassoPolygon,
                  activeLassoTrace = activeLassoTrace,
                  brushStrokes = brushStrokes,
                  activeBrushPoints = activeBrushPoints,
                  borderOptions = borderOptions,
                  isProcessing = isProcessingPreview,
                  onLassoDrag = { pts -> activeLassoTrace = pts },
                  onLassoFinish = { pts ->
                    activeLassoTrace = emptyList()
                    if (pts.size >= 4) {
                      lassoPolygon = pts
                      cutoutMode = StickerCutoutMode.LASSO_SMART_SNAP
                    }
                  },
                  onBrushDrag = { pts -> activeBrushPoints = pts },
                  onBrushFinish = { action, pts ->
                    activeBrushPoints = emptyList()
                    if (pts.isNotEmpty()) {
                      brushStrokes = brushStrokes + StickerBrushStroke(action = action, points = pts)
                    }
                  },
                )
              }
              Column(
                modifier =
                  Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(SankalanSpacing.md),
                verticalArrangement = Arrangement.spacedBy(SankalanSpacing.md),
              ) {
                StickerEditorControlsPanel(
                  activeTab = activeTab,
                  onTabChange = { activeTab = it },
                  stickerTitle = stickerTitle,
                  onTitleChange = { stickerTitle = it },
                  cropRect = cropRect,
                  onSetSquareCrop = {
                    cropRect = StickerCropRect(0.12f, 0.12f, 0.88f, 0.88f)
                  },
                  onResetCrop = {
                    cropRect = StickerCropRect(0f, 0f, 1f, 1f)
                    rotationDegrees = 0
                  },
                  onRotate90 = { rotationDegrees = (rotationDegrees + 90) % 360 },
                  cutoutMode = cutoutMode,
                  onCutoutModeChange = { next ->
                    cutoutMode = next
                    if (next == StickerCutoutMode.LASSO_SMART_SNAP) {
                      touchTool = CutoutTouchTool.LASSO
                    }
                  },
                  touchTool = touchTool,
                  onTouchToolChange = { touchTool = it },
                  hasLasso = lassoPolygon.isNotEmpty(),
                  canUndoStroke = brushStrokes.isNotEmpty(),
                  onUndoStroke = {
                    if (brushStrokes.isNotEmpty()) brushStrokes = brushStrokes.dropLast(1)
                  },
                  onClearLasso = {
                    lassoPolygon = emptyList()
                    if (cutoutMode == StickerCutoutMode.LASSO_SMART_SNAP) {
                      cutoutMode = StickerCutoutMode.AUTO_SUBJECT
                    }
                  },
                  sensitivity = sensitivity,
                  onSensitivityChange = { sensitivity = it },
                  borderOptions = borderOptions,
                  onBorderOptionsChange = { borderOptions = it },
                )
              }
            }
          } else {
            Column(modifier = Modifier.fillMaxSize()) {
              Box(
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .weight(0.46f)
                    .padding(horizontal = SankalanSpacing.md, vertical = SankalanSpacing.sm),
              ) {
                StickerStageViewport(
                  activeTab = activeTab,
                  rotatedSource = rotatedSourceForCrop,
                  croppedWorking = croppedWorkingBitmap,
                  previewSticker = previewStickerBitmap,
                  cropRect = cropRect,
                  onCropRectChange = { cropRect = it },
                  cutoutMode = cutoutMode,
                  touchTool = touchTool,
                  lassoPolygon = lassoPolygon,
                  activeLassoTrace = activeLassoTrace,
                  brushStrokes = brushStrokes,
                  activeBrushPoints = activeBrushPoints,
                  borderOptions = borderOptions,
                  isProcessing = isProcessingPreview,
                  onLassoDrag = { pts -> activeLassoTrace = pts },
                  onLassoFinish = { pts ->
                    activeLassoTrace = emptyList()
                    if (pts.size >= 4) {
                      lassoPolygon = pts
                      cutoutMode = StickerCutoutMode.LASSO_SMART_SNAP
                    }
                  },
                  onBrushDrag = { pts -> activeBrushPoints = pts },
                  onBrushFinish = { action, pts ->
                    activeBrushPoints = emptyList()
                    if (pts.isNotEmpty()) {
                      brushStrokes = brushStrokes + StickerBrushStroke(action = action, points = pts)
                    }
                  },
                )
              }

              Column(
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .weight(0.54f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SankalanSpacing.md, vertical = SankalanSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
              ) {
                StickerEditorControlsPanel(
                  activeTab = activeTab,
                  onTabChange = { activeTab = it },
                  stickerTitle = stickerTitle,
                  onTitleChange = { stickerTitle = it },
                  cropRect = cropRect,
                  onSetSquareCrop = {
                    cropRect = StickerCropRect(0.12f, 0.12f, 0.88f, 0.88f)
                  },
                  onResetCrop = {
                    cropRect = StickerCropRect(0f, 0f, 1f, 1f)
                    rotationDegrees = 0
                  },
                  onRotate90 = { rotationDegrees = (rotationDegrees + 90) % 360 },
                  cutoutMode = cutoutMode,
                  onCutoutModeChange = { next ->
                    cutoutMode = next
                    if (next == StickerCutoutMode.LASSO_SMART_SNAP) {
                      touchTool = CutoutTouchTool.LASSO
                    }
                  },
                  touchTool = touchTool,
                  onTouchToolChange = { touchTool = it },
                  hasLasso = lassoPolygon.isNotEmpty(),
                  canUndoStroke = brushStrokes.isNotEmpty(),
                  onUndoStroke = {
                    if (brushStrokes.isNotEmpty()) brushStrokes = brushStrokes.dropLast(1)
                  },
                  onClearLasso = {
                    lassoPolygon = emptyList()
                    if (cutoutMode == StickerCutoutMode.LASSO_SMART_SNAP) {
                      cutoutMode = StickerCutoutMode.AUTO_SUBJECT
                    }
                  },
                  sensitivity = sensitivity,
                  onSensitivityChange = { sensitivity = it },
                  borderOptions = borderOptions,
                  onBorderOptionsChange = { borderOptions = it },
                )
                Spacer(modifier = Modifier.height(12.dp))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StickerStageViewport(
  activeTab: StickerEditorTab,
  rotatedSource: Bitmap?,
  croppedWorking: Bitmap?,
  previewSticker: Bitmap?,
  cropRect: StickerCropRect,
  onCropRectChange: (StickerCropRect) -> Unit,
  cutoutMode: StickerCutoutMode,
  touchTool: CutoutTouchTool,
  lassoPolygon: List<Pair<Float, Float>>,
  activeLassoTrace: List<Pair<Float, Float>>,
  brushStrokes: List<StickerBrushStroke>,
  activeBrushPoints: List<Pair<Float, Float>>,
  borderOptions: StickerBorderOptions,
  isProcessing: Boolean,
  onLassoDrag: (List<Pair<Float, Float>>) -> Unit,
  onLassoFinish: (List<Pair<Float, Float>>) -> Unit,
  onBrushDrag: (List<Pair<Float, Float>>) -> Unit,
  onBrushFinish: (StickerBrushAction, List<Pair<Float, Float>>) -> Unit,
) {
  val palette = LocalSankalanPalette.current
  val displayBitmap =
    when (activeTab) {
      StickerEditorTab.CROP -> rotatedSource
      StickerEditorTab.CUTOUT, StickerEditorTab.BORDER -> previewSticker ?: croppedWorking
    }

  Surface(
    shape = MaterialTheme.shapes.medium,
    color = palette.shelfSurface,
    border = BorderStroke(1.2.dp, palette.borderGold),
    modifier = Modifier.fillMaxSize(),
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      // Transparency Checkerboard Backdrop
      Canvas(modifier = Modifier.fillMaxSize()) {
        val cell = 14.dp.toPx()
        val c1 = Color(0xFF2B2420)
        val c2 = Color(0xFF382F29)
        var y = 0f
        var row = 0
        while (y < size.height) {
          var x = 0f
          var col = row % 2
          while (x < size.width) {
            drawRect(
              color = if (col % 2 == 0) c1 else c2,
              topLeft = Offset(x, y),
              size = Size(cell, cell),
            )
            x += cell
            col++
          }
          y += cell
          row++
        }
      }

      if (displayBitmap != null && !displayBitmap.isRecycled) {
        val ratio = (displayBitmap.width.toFloat() / max(1, displayBitmap.height)).coerceIn(0.35f, 2.8f)

        // When on Cutout Tab, render faint original cropped photo underneath so user sees background while circling/brushing!
        Box(
          modifier =
            Modifier
              .padding(SankalanSpacing.sm)
              .aspectRatio(ratio, matchHeightConstraintsFirst = true)
              .fillMaxSize(),
          contentAlignment = Alignment.Center,
        ) {
          if (
            activeTab == StickerEditorTab.CUTOUT &&
            croppedWorking != null &&
            !croppedWorking.isRecycled &&
            cutoutMode != StickerCutoutMode.ORIGINAL_RECT
          ) {
            Image(
              bitmap = croppedWorking.asImageBitmap(),
              contentDescription = null,
              contentScale = ContentScale.Fit,
              alpha = 0.28f,
              modifier = Modifier.fillMaxSize(),
            )
          }

          Image(
            bitmap = displayBitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
          )

          // Interactive Overlay Layer matching exact image bounds
          if (activeTab == StickerEditorTab.CROP) {
            CropOverlayCanvas(
              cropRect = cropRect,
              onCropRectChange = onCropRectChange,
            )
          } else if (activeTab == StickerEditorTab.CUTOUT) {
            // Adjust gesture mapping back to croppedWorking coordinates cleanly (accounting for border pad)
            val padRatio =
              if (borderOptions.enabled) {
                (borderOptions.thicknessPx + (if (borderOptions.shadowEnabled) 10 else 0) + 4).toFloat() /
                  max(1, (croppedWorking?.width ?: 340) + (borderOptions.thicknessPx + 14) * 2)
              } else {
                0f
              }

            CutoutTouchOverlay(
              touchTool = touchTool,
              padRatio = padRatio.coerceIn(0f, 0.25f),
              lassoPolygon = lassoPolygon,
              activeLassoTrace = activeLassoTrace,
              activeBrushPoints = activeBrushPoints,
              onLassoDrag = onLassoDrag,
              onLassoFinish = onLassoFinish,
              onBrushDrag = onBrushDrag,
              onBrushFinish = onBrushFinish,
            )
          }
        }
      }

      if (isProcessing) {
        Surface(
          shape = CircleShape,
          color = SankalanTokens.Walnut900.copy(alpha = 0.8f),
          border = BorderStroke(1.dp, palette.goldAccent),
          modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
        ) {
          CircularProgressIndicator(
            color = palette.goldAccent,
            strokeWidth = 2.dp,
            modifier = Modifier.padding(6.dp).size(18.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun CropOverlayCanvas(
  cropRect: StickerCropRect,
  onCropRectChange: (StickerCropRect) -> Unit,
) {
  var currentCrop by remember(cropRect) { mutableStateOf(cropRect) }
  var activeHandle by remember { mutableStateOf<CropDragHandle?>(null) }

  Canvas(
    modifier =
      Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
          detectDragGestures(
            onDragStart = { start ->
              val nx = (start.x / max(1f, size.width.toFloat())).coerceIn(0f, 1f)
              val ny = (start.y / max(1f, size.height.toFloat())).coerceIn(0f, 1f)
              val r = currentCrop
              val th = 0.10f
              activeHandle =
                when {
                  hypot(nx - r.left, ny - r.top) < th -> CropDragHandle.TOP_LEFT
                  hypot(nx - r.right, ny - r.top) < th -> CropDragHandle.TOP_RIGHT
                  hypot(nx - r.left, ny - r.bottom) < th -> CropDragHandle.BOTTOM_LEFT
                  hypot(nx - r.right, ny - r.bottom) < th -> CropDragHandle.BOTTOM_RIGHT
                  nx in r.left..r.right && ny in r.top..r.bottom -> CropDragHandle.MOVE
                  else -> CropDragHandle.MOVE
                }
            },
            onDrag = { change, dragAmount ->
              change.consume()
              val dx = dragAmount.x / max(1f, size.width.toFloat())
              val dy = dragAmount.y / max(1f, size.height.toFloat())
              val r = currentCrop
              val minSpan = 0.14f
              val next =
                when (activeHandle) {
                  CropDragHandle.TOP_LEFT ->
                    r.copy(
                      left = (r.left + dx).coerceIn(0f, r.right - minSpan),
                      top = (r.top + dy).coerceIn(0f, r.bottom - minSpan),
                    )
                  CropDragHandle.TOP_RIGHT ->
                    r.copy(
                      right = (r.right + dx).coerceIn(r.left + minSpan, 1f),
                      top = (r.top + dy).coerceIn(0f, r.bottom - minSpan),
                    )
                  CropDragHandle.BOTTOM_LEFT ->
                    r.copy(
                      left = (r.left + dx).coerceIn(0f, r.right - minSpan),
                      bottom = (r.bottom + dy).coerceIn(r.top + minSpan, 1f),
                    )
                  CropDragHandle.BOTTOM_RIGHT ->
                    r.copy(
                      right = (r.right + dx).coerceIn(r.left + minSpan, 1f),
                      bottom = (r.bottom + dy).coerceIn(r.top + minSpan, 1f),
                    )
                  CropDragHandle.MOVE, null -> {
                    val w = r.right - r.left
                    val h = r.bottom - r.top
                    val nl = (r.left + dx).coerceIn(0f, 1f - w)
                    val nt = (r.top + dy).coerceIn(0f, 1f - h)
                    StickerCropRect(nl, nt, nl + w, nt + h)
                  }
                }
              currentCrop = next
              onCropRectChange(next)
            },
            onDragEnd = { activeHandle = null },
            onDragCancel = { activeHandle = null },
          )
        }
        .testTag("sticker_crop_overlay"),
  ) {
    val l = currentCrop.left * size.width
    val t = currentCrop.top * size.height
    val r = currentCrop.right * size.width
    val b = currentCrop.bottom * size.height

    val scrim = Color.Black.copy(alpha = 0.56f)
    drawRect(scrim, topLeft = Offset.Zero, size = Size(size.width, t))
    drawRect(scrim, topLeft = Offset(0f, b), size = Size(size.width, size.height - b))
    drawRect(scrim, topLeft = Offset(0f, t), size = Size(l, b - t))
    drawRect(scrim, topLeft = Offset(r, t), size = Size(size.width - r, b - t))

    val gold = SankalanTokens.AntiqueGold
    drawRect(
      color = gold,
      topLeft = Offset(l, t),
      size = Size(r - l, b - t),
      style = Stroke(width = 2.dp.toPx()),
    )
    // Rule-of-thirds guides
    for (k in 1..2) {
      val gx = l + (r - l) * (k / 3f)
      val gy = t + (b - t) * (k / 3f)
      drawLine(gold.copy(alpha = 0.45f), Offset(gx, t), Offset(gx, b), strokeWidth = 1.dp.toPx())
      drawLine(gold.copy(alpha = 0.45f), Offset(l, gy), Offset(r, gy), strokeWidth = 1.dp.toPx())
    }
    // Corner handles
    val hr = 7.dp.toPx()
    listOf(Offset(l, t), Offset(r, t), Offset(l, b), Offset(r, b)).forEach { corner ->
      drawCircle(color = SankalanTokens.Walnut900, radius = hr + 2f, center = corner)
      drawCircle(color = gold, radius = hr, center = corner)
    }
  }
}

@Composable
private fun CutoutTouchOverlay(
  touchTool: CutoutTouchTool,
  padRatio: Float,
  lassoPolygon: List<Pair<Float, Float>>,
  activeLassoTrace: List<Pair<Float, Float>>,
  activeBrushPoints: List<Pair<Float, Float>>,
  onLassoDrag: (List<Pair<Float, Float>>) -> Unit,
  onLassoFinish: (List<Pair<Float, Float>>) -> Unit,
  onBrushDrag: (List<Pair<Float, Float>>) -> Unit,
  onBrushFinish: (StickerBrushAction, List<Pair<Float, Float>>) -> Unit,
) {
  var dragPts by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }

  fun mapOffsetToNorm(off: Offset, width: Float, height: Float): Pair<Float, Float> {
    val rawX = off.x / max(1f, width)
    val rawY = off.y / max(1f, height)
    val span = max(0.2f, 1f - 2f * padRatio)
    val nx = ((rawX - padRatio) / span).coerceIn(0f, 1f)
    val ny = ((rawY - padRatio) / span).coerceIn(0f, 1f)
    return nx to ny
  }

  fun normToCanvas(pt: Pair<Float, Float>, width: Float, height: Float): Offset {
    val span = max(0.2f, 1f - 2f * padRatio)
    return Offset(
      x = (padRatio + pt.first * span) * width,
      y = (padRatio + pt.second * span) * height,
    )
  }

  Canvas(
    modifier =
      Modifier
        .fillMaxSize()
        .pointerInput(touchTool, padRatio) {
          detectDragGestures(
            onDragStart = { pos ->
              val p = mapOffsetToNorm(pos, size.width.toFloat(), size.height.toFloat())
              dragPts = listOf(p)
              if (touchTool == CutoutTouchTool.LASSO) onLassoDrag(dragPts)
              else onBrushDrag(dragPts)
            },
            onDrag = { change, _ ->
              change.consume()
              val p = mapOffsetToNorm(change.position, size.width.toFloat(), size.height.toFloat())
              val last = dragPts.lastOrNull()
              if (last == null || hypot(p.first - last.first, p.second - last.second) > 0.012f) {
                dragPts = dragPts + p
                if (touchTool == CutoutTouchTool.LASSO) onLassoDrag(dragPts)
                else onBrushDrag(dragPts)
              }
            },
            onDragEnd = {
              val finished = dragPts
              dragPts = emptyList()
              when (touchTool) {
                CutoutTouchTool.LASSO -> onLassoFinish(finished)
                CutoutTouchTool.KEEP -> onBrushFinish(StickerBrushAction.KEEP_OBJECT, finished)
                CutoutTouchTool.ERASE -> onBrushFinish(StickerBrushAction.ERASE_BACKGROUND, finished)
              }
            },
            onDragCancel = {
              dragPts = emptyList()
              onLassoDrag(emptyList())
              onBrushDrag(emptyList())
            },
          )
        }
        .testTag("sticker_cutout_touch_canvas"),
  ) {
    // Saved lasso silhouette (subtle gold dashed ring)
    if (lassoPolygon.size >= 3 && activeLassoTrace.isEmpty()) {
      val path = Path()
      val first = normToCanvas(lassoPolygon.first(), size.width, size.height)
      path.moveTo(first.x, first.y)
      lassoPolygon.drop(1).forEach { pt ->
        val c = normToCanvas(pt, size.width, size.height)
        path.lineTo(c.x, c.y)
      }
      path.close()
      drawPath(
        path = path,
        color = SankalanTokens.AntiqueGold.copy(alpha = 0.52f),
        style =
          Stroke(
            width = 1.6.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
          ),
      )
    }

    // Active finger lasso path
    if (activeLassoTrace.size >= 2) {
      val path = Path()
      val first = normToCanvas(activeLassoTrace.first(), size.width, size.height)
      path.moveTo(first.x, first.y)
      activeLassoTrace.drop(1).forEach { pt ->
        val c = normToCanvas(pt, size.width, size.height)
        path.lineTo(c.x, c.y)
      }
      drawPath(
        path = path,
        color = SankalanTokens.AntiqueGold,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
      )
    }

    // Active Keep / Erase brush stroke feedback
    if (activeBrushPoints.size >= 2) {
      val path = Path()
      val first = normToCanvas(activeBrushPoints.first(), size.width, size.height)
      path.moveTo(first.x, first.y)
      activeBrushPoints.drop(1).forEach { pt ->
        val c = normToCanvas(pt, size.width, size.height)
        path.lineTo(c.x, c.y)
      }
      val strokeColor =
        if (touchTool == CutoutTouchTool.KEEP) Color(0xFF4CAF50).copy(alpha = 0.72f)
        else SankalanTokens.Terracotta.copy(alpha = 0.72f)
      drawPath(
        path = path,
        color = strokeColor,
        style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StickerEditorControlsPanel(
  activeTab: StickerEditorTab,
  onTabChange: (StickerEditorTab) -> Unit,
  stickerTitle: String,
  onTitleChange: (String) -> Unit,
  cropRect: StickerCropRect,
  onSetSquareCrop: () -> Unit,
  onResetCrop: () -> Unit,
  onRotate90: () -> Unit,
  cutoutMode: StickerCutoutMode,
  onCutoutModeChange: (StickerCutoutMode) -> Unit,
  touchTool: CutoutTouchTool,
  onTouchToolChange: (CutoutTouchTool) -> Unit,
  hasLasso: Boolean,
  canUndoStroke: Boolean,
  onUndoStroke: () -> Unit,
  onClearLasso: () -> Unit,
  sensitivity: Float,
  onSensitivityChange: (Float) -> Unit,
  borderOptions: StickerBorderOptions,
  onBorderOptionsChange: (StickerBorderOptions) -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current

  // 3 Step Tabs
  FlowRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    listOf(
      Triple(StickerEditorTab.CROP, strings.stickerTabCrop, Icons.Outlined.Crop),
      Triple(StickerEditorTab.CUTOUT, strings.stickerTabCutout, Icons.Outlined.AutoFixHigh),
      Triple(StickerEditorTab.BORDER, strings.stickerTabBorder, Icons.Outlined.BorderStyle),
    ).forEach { (tab, label, icon) ->
      FilterChip(
        selected = activeTab == tab,
        onClick = { onTabChange(tab) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
        label = { Text(label, maxLines = 1) },
        colors =
          FilterChipDefaults.filterChipColors(
            selectedContainerColor = palette.goldAccent,
            selectedLabelColor = SankalanTokens.Walnut900,
            selectedLeadingIconColor = SankalanTokens.Walnut900,
          ),
        modifier = Modifier.testTag("sticker_tab_${tab.name.lowercase()}"),
      )
    }
  }

  Surface(
    shape = MaterialTheme.shapes.medium,
    color = palette.cardSurface,
    border = BorderStroke(1.dp, palette.borderSubtle),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(SankalanSpacing.md),
      verticalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
    ) {
      when (activeTab) {
        StickerEditorTab.CROP -> {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = onResetCrop,
              border = BorderStroke(1.dp, palette.borderSubtle),
              modifier = Modifier.minimumInteractiveComponentSize().testTag("sticker_crop_reset_btn"),
            ) {
              Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(strings.stickerResetCrop)
            }
            OutlinedButton(
              onClick = onSetSquareCrop,
              border = BorderStroke(1.dp, palette.borderSubtle),
              modifier = Modifier.minimumInteractiveComponentSize().testTag("sticker_crop_square_btn"),
            ) {
              Text(strings.stickerAspectSquare)
            }
            OutlinedButton(
              onClick = onRotate90,
              border = BorderStroke(1.dp, palette.borderGold),
              modifier = Modifier.minimumInteractiveComponentSize().testTag("sticker_rotate_90_btn"),
            ) {
              Icon(Icons.AutoMirrored.Outlined.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(strings.stickerRotate90)
            }
          }
          Text(
            text = "${strings.stickerAspectFree}: ${((cropRect.right - cropRect.left) * 100).toInt()}% × ${((cropRect.bottom - cropRect.top) * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary,
          )
        }

        StickerEditorTab.CUTOUT -> {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            FilterChip(
              selected = cutoutMode == StickerCutoutMode.AUTO_SUBJECT,
              onClick = { onCutoutModeChange(StickerCutoutMode.AUTO_SUBJECT) },
              leadingIcon = { Icon(Icons.Outlined.AutoFixHigh, contentDescription = null, modifier = Modifier.size(15.dp)) },
              label = { Text(strings.stickerModeAuto) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = palette.goldAccent,
                  selectedLabelColor = SankalanTokens.Walnut900,
                ),
              modifier = Modifier.testTag("sticker_mode_auto"),
            )
            FilterChip(
              selected = cutoutMode == StickerCutoutMode.LASSO_SMART_SNAP,
              onClick = { onCutoutModeChange(StickerCutoutMode.LASSO_SMART_SNAP) },
              leadingIcon = { Icon(Icons.Outlined.Gesture, contentDescription = null, modifier = Modifier.size(15.dp)) },
              label = { Text(strings.stickerModeLasso) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = palette.goldAccent,
                  selectedLabelColor = SankalanTokens.Walnut900,
                ),
              modifier = Modifier.testTag("sticker_mode_lasso"),
            )
            FilterChip(
              selected = cutoutMode == StickerCutoutMode.ORIGINAL_RECT,
              onClick = { onCutoutModeChange(StickerCutoutMode.ORIGINAL_RECT) },
              leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(15.dp)) },
              label = { Text(strings.stickerModeOriginal) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = palette.goldAccent,
                  selectedLabelColor = SankalanTokens.Walnut900,
                ),
              modifier = Modifier.testTag("sticker_mode_original"),
            )
          }

          // Instruction Touch Tools: Finger Lasso / Keep + / Erase - / Undo / Clear
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            FilterChip(
              selected = touchTool == CutoutTouchTool.LASSO,
              onClick = { onTouchToolChange(CutoutTouchTool.LASSO) },
              leadingIcon = { Icon(Icons.Outlined.Gesture, contentDescription = null, modifier = Modifier.size(15.dp)) },
              label = { Text(strings.stickerModeLasso) },
            )
            FilterChip(
              selected = touchTool == CutoutTouchTool.KEEP,
              onClick = { onTouchToolChange(CutoutTouchTool.KEEP) },
              leadingIcon = { Icon(Icons.Outlined.AddCircleOutline, contentDescription = null, modifier = Modifier.size(15.dp)) },
              label = { Text(strings.stickerBrushKeep) },
              modifier = Modifier.testTag("sticker_brush_keep"),
            )
            FilterChip(
              selected = touchTool == CutoutTouchTool.ERASE,
              onClick = { onTouchToolChange(CutoutTouchTool.ERASE) },
              leadingIcon = { Icon(Icons.Outlined.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(15.dp)) },
              label = { Text(strings.stickerBrushErase) },
              modifier = Modifier.testTag("sticker_brush_erase"),
            )
            if (canUndoStroke) {
              OutlinedButton(
                onClick = onUndoStroke,
                modifier = Modifier.heightIn(min = 36.dp).testTag("sticker_undo_btn"),
              ) {
                Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.stickerUndoStroke, style = MaterialTheme.typography.labelMedium)
              }
            }
            if (hasLasso) {
              OutlinedButton(
                onClick = onClearLasso,
                modifier = Modifier.heightIn(min = 36.dp).testTag("sticker_clear_lasso_btn"),
              ) {
                Text(strings.stickerClearLasso, style = MaterialTheme.typography.labelMedium)
              }
            }
          }

          Text(
            text =
              if (touchTool == CutoutTouchTool.LASSO || cutoutMode == StickerCutoutMode.LASSO_SMART_SNAP) {
                strings.stickerLassoGuideHint
              } else {
                strings.stickerAutoGuideHint
              },
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
          )

          if (cutoutMode != StickerCutoutMode.ORIGINAL_RECT) {
            Column {
              Text(
                text = "${strings.stickerSensitivityLabel}: ${(sensitivity * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = palette.textPrimary,
              )
              Slider(
                value = sensitivity,
                onValueChange = onSensitivityChange,
                valueRange = 0.15f..0.85f,
                colors = SliderDefaults.colors(thumbColor = palette.goldAccent, activeTrackColor = palette.goldAccent),
                modifier = Modifier.testTag("sticker_sensitivity_slider"),
              )
            }
          }
        }

        StickerEditorTab.BORDER -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = strings.stickerBorderToggleLabel,
              style = MaterialTheme.typography.bodyLarge,
              color = palette.textPrimary,
              modifier = Modifier.weight(1f),
            )
            Switch(
              checked = borderOptions.enabled,
              onCheckedChange = { onBorderOptionsChange(borderOptions.copy(enabled = it)) },
              colors =
                SwitchDefaults.colors(
                  checkedThumbColor = SankalanTokens.Walnut900,
                  checkedTrackColor = palette.goldAccent,
                ),
              modifier = Modifier.testTag("sticker_border_toggle"),
            )
          }

          if (borderOptions.enabled) {
            Text(
              text = "${strings.stickerBorderThicknessLabel}: ${borderOptions.thicknessPx}px",
              style = MaterialTheme.typography.labelMedium,
              color = palette.textPrimary,
            )
            Slider(
              value = borderOptions.thicknessPx.toFloat(),
              onValueChange = { onBorderOptionsChange(borderOptions.copy(thicknessPx = it.toInt())) },
              valueRange = 4f..26f,
              colors = SliderDefaults.colors(thumbColor = palette.goldAccent, activeTrackColor = palette.goldAccent),
              modifier = Modifier.testTag("sticker_border_thickness_slider"),
            )

            Text(
              text = strings.stickerBorderColorLabel,
              style = MaterialTheme.typography.labelMedium,
              color = palette.textPrimary,
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              BORDER_SWATCHES.forEach { hex ->
                val swatchColor = Color(android.graphics.Color.parseColor(hex))
                val selected = borderOptions.colorHex.equals(hex, ignoreCase = true)
                Box(
                  modifier =
                    Modifier
                      .minimumInteractiveComponentSize()
                      .size(38.dp)
                      .clip(CircleShape)
                      .background(swatchColor)
                      .border(
                        width = if (selected) 3.dp else 1.dp,
                        color = if (selected) palette.goldAccent else palette.borderSubtle,
                        shape = CircleShape,
                      )
                      .clickable(role = Role.RadioButton) {
                        onBorderOptionsChange(borderOptions.copy(colorHex = hex))
                      }
                      .testTag("sticker_border_color_${hex.removePrefix("#")}"),
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = strings.stickerBorderGoldTrimLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textPrimary,
                modifier = Modifier.weight(1f),
              )
              Switch(
                checked = borderOptions.doubleGoldTrim,
                onCheckedChange = { onBorderOptionsChange(borderOptions.copy(doubleGoldTrim = it)) },
                colors =
                  SwitchDefaults.colors(
                    checkedThumbColor = SankalanTokens.Walnut900,
                    checkedTrackColor = palette.goldAccent,
                  ),
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = strings.stickerBorderShadowLabel,
              style = MaterialTheme.typography.bodyMedium,
              color = palette.textPrimary,
              modifier = Modifier.weight(1f),
            )
            Switch(
              checked = borderOptions.shadowEnabled,
              onCheckedChange = { onBorderOptionsChange(borderOptions.copy(shadowEnabled = it)) },
              colors =
                SwitchDefaults.colors(
                  checkedThumbColor = SankalanTokens.Walnut900,
                  checkedTrackColor = palette.goldAccent,
                ),
            )
          }

          OutlinedTextField(
            value = stickerTitle,
            onValueChange = { onTitleChange(it.take(40)) },
            label = { Text(strings.stickerTitleLabel) },
            placeholder = { Text(strings.stickerTitleHint) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("sticker_title_input"),
          )
        }
      }
    }
  }
}
