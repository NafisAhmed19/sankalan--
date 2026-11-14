package com.example.features.bookshelf

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FlipCameraAndroid
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.i18n.LocalStrings
import com.example.core.i18n.formatNumber
import com.example.core.model.CoverStyle
import com.example.core.model.Diary
import com.example.core.model.DiaryWithPages
import com.example.core.model.NumeralStyle
import com.example.core.model.SpineStyle
import com.example.ui.theme.LocalSankalanPalette
import com.example.ui.theme.NotoSerifBengaliFontFamily
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.SankalanSpacing
import com.example.ui.theme.SankalanTokens

fun parseHexColor(hex: String, fallback: Color = SankalanTokens.Leather): Color {
  return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)
}

/**
 * Renders a realistic 3D wooden walnut bookshelf cabinet row with:
 * - Recessed cabinet interior wall + overhead warm library spotlight
 * - Standing 3D physical books with realistic spines, ribs, page-edge bevels & cast shadows
 * - Thick 3D beveled wooden shelf ledge + antique-gold inlay hairline + under-shelf shadow
 */
@Composable
fun WalnutShelfCabinetRow(
  books: List<DiaryWithPages>,
  selectedBookId: String?,
  numeralStyle: NumeralStyle,
  onSelectBook: (DiaryWithPages) -> Unit,
  modifier: Modifier = Modifier,
  emptyShelfLabel: String = "",
  onEmptyShelfClick: (() -> Unit)? = null,
) {
  val palette = LocalSankalanPalette.current
  val shelfColorDark = if (palette.isDark) Color(0xFF1A110E) else Color(0xFF3E2B22)
  val shelfWoodTop = if (palette.isDark) SankalanTokens.Walnut700 else Color(0xFF6B4D3B)
  val shelfWoodFront = if (palette.isDark) SankalanTokens.Walnut800 else Color(0xFF4F372B)
  val shelfShadow = Color.Black.copy(alpha = if (palette.isDark) 0.55f else 0.28f)

  Surface(
    shape = MaterialTheme.shapes.medium,
    color = shelfColorDark,
    border = BorderStroke(1.5.dp, palette.borderGold),
    shadowElevation = 6.dp,
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Recessed cabinet back + spotlight + standing books (or empty interactive slot)
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(196.dp)
            .background(
              Brush.verticalGradient(
                colors =
                  listOf(
                    shelfColorDark,
                    if (palette.isDark) Color(0xFF281B16) else Color(0xFF4A3428),
                    Color(0xFF160E0B),
                  )
              )
            ),
        contentAlignment = Alignment.BottomStart,
      ) {
        // Warm overhead library lamp cone + side cabinet stiles
        Canvas(modifier = Modifier.matchParentSize()) {
          // Overhead warm cone light
          drawRect(
            brush =
              Brush.radialGradient(
                colors = listOf(SankalanTokens.AntiqueGold.copy(alpha = 0.14f), Color.Transparent),
                center = Offset(size.width * 0.5f, 0f),
                radius = size.width * 0.65f,
              )
          )
          // Left & right 3D side cabinet stiles (depth walls)
          val stileW = 10.dp.toPx()
          drawRect(
            brush =
              Brush.horizontalGradient(
                colors = listOf(SankalanTokens.Walnut700, Color.Black.copy(alpha = 0.45f))
              ),
            topLeft = Offset.Zero,
            size = Size(stileW, size.height),
          )
          drawRect(
            brush =
              Brush.horizontalGradient(
                colors = listOf(Color.Black.copy(alpha = 0.45f), SankalanTokens.Walnut700)
              ),
            topLeft = Offset(size.width - stileW, 0f),
            size = Size(stileW, size.height),
          )
        }

        if (books.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center,
          ) {
            if (onEmptyShelfClick != null && emptyShelfLabel.isNotBlank()) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.24f),
                border = BorderStroke(1.dp, SankalanTokens.AntiqueGold.copy(alpha = 0.42f)),
                modifier =
                  Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onEmptyShelfClick),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = SankalanTokens.GoldHighlight,
                    modifier = Modifier.size(18.dp),
                  )
                  Text(
                    text = emptyShelfLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = SankalanTokens.Cream.copy(alpha = 0.88f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        } else {
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
          ) {
            books.forEachIndexed { index, item ->
              BookSpine3D(
                diaryWithPages = item,
                isPulledOut = item.diary.id == selectedBookId,
                heightVariant =
                  when (index % 3) {
                    0 -> 174.dp
                    1 -> 162.dp
                    else -> 168.dp
                  },
                numeralStyle = numeralStyle,
                onClick = { onSelectBook(item) },
              )
            }
            if (onEmptyShelfClick != null && emptyShelfLabel.isNotBlank()) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.Black.copy(alpha = 0.20f),
                border = BorderStroke(1.dp, SankalanTokens.AntiqueGold.copy(alpha = 0.35f)),
                modifier =
                  Modifier
                    .width(44.dp)
                    .height(152.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(role = Role.Button, onClick = onEmptyShelfClick),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = emptyShelfLabel,
                    tint = SankalanTokens.GoldHighlight.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
            }
          }
        }
      }

      // 3D Walnut Wooden Shelf Ledge (Top bevel + Gold Inlay + Front Face + Cast Drop Shadow)
      Canvas(modifier = Modifier.fillMaxWidth().height(26.dp)) {
        val topBevelH = 6.dp.toPx()
        val frontH = 14.dp.toPx()
        val shadowH = 6.dp.toPx()

        // Top receding horizontal wood plane (lighter due to overhead light)
        val topPlane =
          Path().apply {
            moveTo(6.dp.toPx(), 0f)
            lineTo(size.width - 6.dp.toPx(), 0f)
            lineTo(size.width, topBevelH)
            lineTo(0f, topBevelH)
            close()
          }
        drawPath(
          path = topPlane,
          brush = Brush.verticalGradient(colors = listOf(shelfWoodTop.copy(alpha = 0.85f), shelfWoodTop)),
        )

        // Front thick shelf face
        drawRect(
          brush = Brush.verticalGradient(colors = listOf(shelfWoodFront, SankalanTokens.Walnut900)),
          topLeft = Offset(0f, topBevelH),
          size = Size(size.width, frontH),
        )

        // Antique-gold inlay trim line along front shelf edge
        drawLine(
          color = SankalanTokens.AntiqueGold.copy(alpha = 0.55f),
          start = Offset(10.dp.toPx(), topBevelH + 4.dp.toPx()),
          end = Offset(size.width - 10.dp.toPx(), topBevelH + 4.dp.toPx()),
          strokeWidth = 1.dp.toPx(),
        )

        // Deep drop shadow beneath shelf ledge
        drawRect(
          brush = Brush.verticalGradient(colors = listOf(shelfShadow, Color.Transparent)),
          topLeft = Offset(0f, topBevelH + frontH),
          size = Size(size.width, shadowH),
        )
      }
    }
  }
}

/**
 * Standing physical book on the 3D walnut shelf showing:
 * - Spine width scaled gently by page count (5..200 pages)
 * - Top 3D page-edge thickness peeking above the binding
 * - Cylindrical spine shading, raised gold ribs or stitched cloth texture, 'ৎ' crest, and title
 * - Pull-out 3D lift animation when tapped before opening the cover inspector
 */
@Composable
fun BookSpine3D(
  diaryWithPages: DiaryWithPages,
  isPulledOut: Boolean,
  heightVariant: Dp,
  numeralStyle: NumeralStyle,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalStrings.current
  val diary = diaryWithPages.diary
  val pageCount = diaryWithPages.pages.size
  val baseColor = parseHexColor(diary.cover.colorHex, SankalanTokens.Leather)
  val accentColor = parseHexColor(diary.cover.accentHex, SankalanTokens.AntiqueGold)

  val spineWidth = (42 + ((pageCount - 5).coerceAtLeast(0) / 10)).coerceIn(42, 62).dp
  val pullLift by
    animateDpAsState(
      targetValue = if (isPulledOut) (-10).dp else 0.dp,
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      label = "spineLift",
    )

  Box(
    modifier =
      modifier
        .offset(y = pullLift)
        .width(spineWidth + 10.dp) // Extra width for 3D right page-edge thickness!
        .height(heightVariant)
        .clickable(role = Role.Button, onClick = onClick)
        .testTag("book_spine_${diary.id}"),
    contentAlignment = Alignment.BottomStart,
  ) {
    // Subtle 3D side cover edge + stacked paper thickness peeking at top-right
    Canvas(modifier = Modifier.fillMaxSize()) {
      val spineW = spineWidth.toPx()
      val sideDepth = 8.dp.toPx()
      val topSkim = 7.dp.toPx()

      // Drop shadow on cabinet wall behind book
      drawRoundRect(
        color = Color.Black.copy(alpha = 0.42f),
        topLeft = Offset(4.dp.toPx(), topSkim + 2.dp.toPx()),
        size = Size(spineW + sideDepth, size.height - topSkim - 2.dp.toPx()),
        cornerRadius = CornerRadius(3.dp.toPx()),
      )

      // Top angled cream paper-block edge (visible 3D thickness)
      val paperBlock =
        Path().apply {
          moveTo(3.dp.toPx(), topSkim)
          lineTo(spineW + sideDepth - 2.dp.toPx(), 0f)
          lineTo(spineW + sideDepth - 2.dp.toPx(), size.height - 6.dp.toPx())
          lineTo(spineW - 1.dp.toPx(), size.height)
          lineTo(spineW - 1.dp.toPx(), topSkim)
          close()
        }
      drawPath(
        path = paperBlock,
        brush = Brush.horizontalGradient(colors = listOf(SankalanTokens.Paper, SankalanTokens.PaperShadow)),
      )

      // Fine horizontal page sheet lines on the top-right edge
      var y = topSkim + 8.dp.toPx()
      while (y < size.height - 8.dp.toPx()) {
        drawLine(
          color = SankalanTokens.Ink.copy(alpha = 0.14f),
          start = Offset(spineW, y),
          end = Offset(spineW + sideDepth - 3.dp.toPx(), y - 3.dp.toPx()),
          strokeWidth = 0.7.dp.toPx(),
        )
        y += 6.dp.toPx()
      }

      // Main cylindrical 3D spine body
      val spineRectTop = topSkim
      val spineRectHeight = size.height - topSkim
      drawRoundRect(
        brush =
          Brush.horizontalGradient(
            colors =
              listOf(
                baseColor.copy(alpha = 0.82f),
                baseColor,
                baseColor.copy(red = (baseColor.red * 1.15f).coerceAtMost(1f), green = (baseColor.green * 1.15f).coerceAtMost(1f), blue = (baseColor.blue * 1.15f).coerceAtMost(1f)),
                baseColor.copy(alpha = 0.78f),
              ),
            startX = 0f,
            endX = spineW,
          ),
        topLeft = Offset(0f, spineRectTop),
        size = Size(spineW, spineRectHeight),
        cornerRadius = CornerRadius(4.dp.toPx()),
      )

      // Spine craftsmanship details (Ribbed Gold bands, Smooth Leather bevel, or Stitched Cloth)
      when (diary.cover.spineStyle) {
        SpineStyle.RIBBED_GOLD -> {
          val ribPositions = listOf(0.14f, 0.20f, 0.80f, 0.86f)
          ribPositions.forEach { frac ->
            val ry = spineRectTop + spineRectHeight * frac
            drawLine(
              color = accentColor.copy(alpha = 0.85f),
              start = Offset(3.dp.toPx(), ry),
              end = Offset(spineW - 3.dp.toPx(), ry),
              strokeWidth = 2.dp.toPx(),
            )
            drawLine(
              color = Color.Black.copy(alpha = 0.35f),
              start = Offset(3.dp.toPx(), ry + 2.dp.toPx()),
              end = Offset(spineW - 3.dp.toPx(), ry + 2.dp.toPx()),
              strokeWidth = 1.dp.toPx(),
            )
          }
        }
        SpineStyle.STITCHED_CLOTH -> {
          val inset = 5.dp.toPx()
          drawLine(
            color = accentColor.copy(alpha = 0.6f),
            start = Offset(inset, spineRectTop + 10.dp.toPx()),
            end = Offset(inset, size.height - 10.dp.toPx()),
            strokeWidth = 1.dp.toPx(),
          )
        }
        SpineStyle.SMOOTH_LEATHER -> {
          drawRoundRect(
            color = accentColor.copy(alpha = 0.45f),
            topLeft = Offset(2.dp.toPx(), spineRectTop + 4.dp.toPx()),
            size = Size(spineW - 4.dp.toPx(), spineRectHeight - 8.dp.toPx()),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(width = 0.8.dp.toPx()),
          )
        }
      }
    }

    // Spine Monogram ('ৎ'), Vertical Title, and Page Count badge
    Column(
      modifier =
        Modifier
          .width(spineWidth)
          .fillMaxHeight()
          .padding(top = 12.dp, bottom = 8.dp, start = 4.dp, end = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = diary.cover.monogram,
        fontFamily = NotoSerifBengaliFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = accentColor,
      )

      // Book Title printed vertically along the spine
      Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = diary.title,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.SemiBold,
          color = SankalanTokens.Cream.copy(alpha = 0.94f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
          modifier = Modifier.requiredWidth(88.dp).rotate(-90f),
        )
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (diary.isDemo) {
          Text(
            text = strings.demoBadge,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
          )
        }
        Text(
          text = formatNumber(pageCount, numeralStyle),
          fontSize = 9.sp,
          color = SankalanTokens.Cream.copy(alpha = 0.72f),
        )
      }
    }
  }
}

/**
 * Interactive 3D Front & Back Cover inspection modal when user taps any book on the shelf.
 * Lets user flip between Front Cover and Back Cover, Open Diary, Rename, Duplicate, Archive, Delete.
 */
@Composable
fun BookCoverInspectionDialog(
  diaryWithPages: DiaryWithPages,
  numeralStyle: NumeralStyle,
  onDismiss: () -> Unit,
  onOpenDiary: () -> Unit,
  onManagePages: () -> Unit,
  onExport: () -> Unit,
  onRename: () -> Unit,
  onDuplicate: () -> Unit,
  onToggleArchive: () -> Unit,
  onDelete: () -> Unit,
) {
  val strings = LocalStrings.current
  val palette = LocalSankalanPalette.current
  val diary = diaryWithPages.diary
  val pageCount = diaryWithPages.pages.size

  var showBackCover by remember { mutableStateOf(false) }
  val flipAngle by
    animateFloatAsState(
      targetValue = if (showBackCover) 180f else 0f,
      animationSpec = tween(durationMillis = 480),
      label = "coverFlip",
    )

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = MaterialTheme.shapes.large,
      color = palette.cardSurface,
      border = BorderStroke(1.dp, palette.borderGold),
      modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
    ) {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(SankalanSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SankalanSpacing.md),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = palette.shelfSurface,
            border = BorderStroke(1.dp, palette.borderSubtle),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = palette.goldAccent,
                modifier = Modifier.size(13.dp),
              )
              Text(
                text = strings.visibilityPrivate,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textPrimary,
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = { showBackCover = !showBackCover },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("cover_flip_button"),
            ) {
              Icon(
                imageVector = Icons.Outlined.FlipCameraAndroid,
                contentDescription = if (showBackCover) strings.frontCoverTag else strings.backCoverTag,
                tint = palette.goldAccent,
              )
            }
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("cover_close_button"),
            ) {
              Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = strings.closeLabel,
                tint = palette.textSecondary,
              )
            }
          }
        }

        // 3D Flippable Book Cover Preview
        Box(
          modifier =
            Modifier
              .width(210.dp)
              .height(278.dp)
              .graphicsLayer {
                rotationY = flipAngle
                cameraDistance = 14f * density
              }
              .clickable(role = Role.Button) { showBackCover = !showBackCover },
          contentAlignment = Alignment.Center,
        ) {
          if (flipAngle <= 90f) {
            BookCoverArt(
              diary = diary,
              pageCount = pageCount,
              numeralStyle = numeralStyle,
              isBack = false,
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            Box(modifier = Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
              BookCoverArt(
                diary = diary,
                pageCount = pageCount,
                numeralStyle = numeralStyle,
                isBack = true,
                modifier = Modifier.fillMaxSize(),
              )
            }
          }
        }

        Text(
          text = if (showBackCover) strings.backCoverTag else strings.frontCoverTag,
          style = MaterialTheme.typography.labelSmall,
          color = palette.textSecondary,
        )

        // Primary Open Diary + Page Manager CTAs
        Button(
          onClick = onOpenDiary,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = palette.goldAccent,
              contentColor = SankalanTokens.Walnut900,
            ),
          shape = MaterialTheme.shapes.small,
          modifier =
            Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("cover_open_diary_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.AutoStories,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = strings.openDiary, style = MaterialTheme.typography.labelLarge)
        }

        OutlinedButton(
          onClick = onManagePages,
          border = BorderStroke(1.dp, palette.borderGold),
          shape = MaterialTheme.shapes.small,
          modifier =
            Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("cover_page_manager_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.CollectionsBookmark,
            contentDescription = null,
            tint = palette.goldAccent,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = strings.pageManager,
            style = MaterialTheme.typography.labelLarge,
            color = palette.textPrimary,
          )
        }

        OutlinedButton(
          onClick = onExport,
          border = BorderStroke(1.dp, palette.borderGold),
          shape = MaterialTheme.shapes.small,
          modifier =
            Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("cover_export_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.AutoStories,
            contentDescription = null,
            tint = palette.goldAccent,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = strings.exportDiaryTitle,
            style = MaterialTheme.typography.labelLarge,
            color = palette.textPrimary,
          )
        }

        // Secondary book actions (Rename, Duplicate, Archive, Delete)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(SankalanSpacing.xs),
        ) {
          BookMiniAction(
            icon = Icons.Outlined.Edit,
            label = strings.renameDiary,
            onClick = onRename,
            modifier = Modifier.weight(1f).testTag("cover_rename_button"),
          )
          BookMiniAction(
            icon = Icons.Outlined.ContentCopy,
            label = strings.duplicateDiary,
            onClick = onDuplicate,
            modifier = Modifier.weight(1f).testTag("cover_duplicate_button"),
          )
          BookMiniAction(
            icon = if (diary.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
            label = if (diary.archived) strings.unarchiveDiary else strings.archiveDiary,
            onClick = onToggleArchive,
            modifier = Modifier.weight(1f).testTag("cover_archive_button"),
          )
          BookMiniAction(
            icon = Icons.Outlined.DeleteOutline,
            label = strings.deleteDiary,
            onClick = onDelete,
            isDestructive = true,
            modifier = Modifier.weight(1f).testTag("cover_delete_button"),
          )
        }
      }
    }
  }
}

@Composable
private fun BookMiniAction(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isDestructive: Boolean = false,
) {
  val palette = LocalSankalanPalette.current
  val tint =
    if (isDestructive) {
      if (palette.isDark) SankalanTokens.ErrorDark else SankalanTokens.ErrorLight
    } else {
      palette.textPrimary
    }
  OutlinedButton(
    onClick = onClick,
    border = BorderStroke(1.dp, palette.borderSubtle),
    shape = MaterialTheme.shapes.small,
    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp),
    modifier = modifier.minimumInteractiveComponentSize(),
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(16.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = tint,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

/**
 * Tactile physical Front / Back cover artwork for Luxury, Artistic, and Minimal styles.
 */
@Composable
fun BookCoverArt(
  diary: Diary,
  pageCount: Int,
  numeralStyle: NumeralStyle,
  isBack: Boolean,
  modifier: Modifier = Modifier,
) {
  val strings = LocalStrings.current
  val baseColor = parseHexColor(diary.cover.colorHex, SankalanTokens.Leather)
  val accentColor = parseHexColor(diary.cover.accentHex, SankalanTokens.AntiqueGold)
  val textOnCover =
    if (diary.cover.style == CoverStyle.MINIMAL && baseColor.red > 0.6f) SankalanTokens.Ink
    else SankalanTokens.Cream

  Box(
    modifier =
      modifier
        .shadow(10.dp, RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp, topStart = 4.dp, bottomStart = 4.dp))
        .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp, topStart = 4.dp, bottomStart = 4.dp))
        .background(
          Brush.linearGradient(
            colors =
              listOf(
                baseColor,
                baseColor.copy(alpha = 0.88f),
                baseColor.copy(red = (baseColor.red * 0.78f), green = (baseColor.green * 0.78f), blue = (baseColor.blue * 0.78f)),
              )
          )
        )
        .border(
          width = 1.5.dp,
          color = accentColor.copy(alpha = 0.7f),
          shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp, topStart = 4.dp, bottomStart = 4.dp),
        )
  ) {
    // Binding spine crease + cover style ornament
    Canvas(modifier = Modifier.fillMaxSize()) {
      // Spine hinge groove on left (or right if back cover)
      val grooveX = if (isBack) size.width - 14.dp.toPx() else 14.dp.toPx()
      drawLine(
        color = Color.Black.copy(alpha = 0.32f),
        start = Offset(grooveX, 0f),
        end = Offset(grooveX, size.height),
        strokeWidth = 2.dp.toPx(),
      )
      drawLine(
        color = accentColor.copy(alpha = 0.25f),
        start = Offset(grooveX + 2.dp.toPx(), 0f),
        end = Offset(grooveX + 2.dp.toPx(), size.height),
        strokeWidth = 1.dp.toPx(),
      )

      when (diary.cover.style) {
        CoverStyle.LUXURY -> {
          // Gold foil inner frame + corner brackets
          val pad = 16.dp.toPx()
          drawRoundRect(
            color = accentColor.copy(alpha = 0.72f),
            topLeft = Offset(pad + (if (isBack) 0f else 8.dp.toPx()), pad),
            size = Size(size.width - pad * 2 - 8.dp.toPx(), size.height - pad * 2),
            cornerRadius = CornerRadius(6.dp.toPx()),
            style = Stroke(width = 1.2.dp.toPx()),
          )
        }
        CoverStyle.ARTISTIC -> {
          // Subtle botanical leaf arcs & warm paper collage band
          drawCircle(
            color = accentColor.copy(alpha = 0.16f),
            radius = size.width * 0.32f,
            center = Offset(size.width * 0.65f, size.height * 0.30f),
          )
          drawCircle(
            color = SankalanTokens.Terracotta.copy(alpha = 0.20f),
            radius = size.width * 0.22f,
            center = Offset(size.width * 0.35f, size.height * 0.72f),
          )
        }
        CoverStyle.MINIMAL -> {
          // Restrained single hairline rule
          drawLine(
            color = accentColor.copy(alpha = 0.55f),
            start = Offset(28.dp.toPx(), size.height * 0.48f),
            end = Offset(size.width - 28.dp.toPx(), size.height * 0.48f),
            strokeWidth = 1.dp.toPx(),
          )
        }
      }
    }

    Column(
      modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 18.dp, top = 22.dp, bottom = 18.dp),
      verticalArrangement = Arrangement.SpaceBetween,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      if (!isBack) {
        // Monogram crest
        Surface(
          shape = RoundedCornerShape(50),
          color = Color.Black.copy(alpha = 0.22f),
          border = BorderStroke(1.dp, accentColor.copy(alpha = 0.7f)),
        ) {
          Text(
            text = diary.cover.monogram,
            fontFamily = NotoSerifBengaliFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = accentColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
          )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = diary.title,
            fontFamily = PlayfairDisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            color = textOnCover,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
          )
          if (!diary.description.isNullOrBlank()) {
            Text(
              text = diary.description,
              style = MaterialTheme.typography.bodyMedium,
              fontSize = 11.sp,
              lineHeight = 16.sp,
              color = textOnCover.copy(alpha = 0.76f),
              textAlign = TextAlign.Center,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Text(
          text = "${formatNumber(pageCount, numeralStyle)} ${strings.pagesUnit}",
          style = MaterialTheme.typography.labelSmall,
          color = accentColor,
        )
      } else {
        // Back cover colophon
        Spacer(modifier = Modifier.height(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = strings.brandLogoWordmark,
            fontFamily = PlayfairDisplayFontFamily,
            fontSize = 16.sp,
            color = accentColor,
          )
          Text(
            text = strings.tagline,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 11.sp,
            color = textOnCover.copy(alpha = 0.78f),
            textAlign = TextAlign.Center,
          )
        }
        Text(
          text = "NRST.tech",
          style = MaterialTheme.typography.labelSmall,
          color = accentColor.copy(alpha = 0.7f),
        )
      }
    }
  }
}

/**
 * Grid View card showing front cover + quick metadata.
 */
@Composable
fun BookGridCard(
  diaryWithPages: DiaryWithPages,
  numeralStyle: NumeralStyle,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalSankalanPalette.current
  val strings = LocalStrings.current
  val diary = diaryWithPages.diary

  Surface(
    shape = MaterialTheme.shapes.medium,
    color = palette.cardSurface,
    border = BorderStroke(1.dp, palette.borderSubtle),
    modifier = modifier.clip(MaterialTheme.shapes.medium).clickable(role = Role.Button, onClick = onClick),
  ) {
    Column(
      modifier = Modifier.padding(SankalanSpacing.md),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(SankalanSpacing.sm),
    ) {
      BookCoverArt(
        diary = diary,
        pageCount = diaryWithPages.pages.size,
        numeralStyle = numeralStyle,
        isBack = false,
        modifier = Modifier.width(126.dp).height(166.dp),
      )
      Text(
        text = diary.title,
        style = MaterialTheme.typography.titleMedium,
        color = palette.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
      )
      Text(
        text = "${formatNumber(diaryWithPages.pages.size, numeralStyle)} ${strings.pagesUnit}",
        style = MaterialTheme.typography.labelSmall,
        color = palette.textSecondary,
      )
    }
  }
}
