package core.image.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bean.Alignment
import core.image.CoordinateTransformer
import core.image.ImageCanvasState
import translation.data.BlockData
import translation.data.BlockPosition
import translation.data.BlockSettings
import java.util.*

/**
 * Box overlay that replaces BoxOnImage/BlockOnImage with migration-friendly interface.
 *
 * Maintains all existing functionality:
 * - Drag and resize handling with border detection
 * - Click selection
 * - Text rendering (optional)
 * - Shape support (Rectangle/Oval)
 * - Focus management
 * - Callback compatibility
 */
class BoxOverlay private constructor(
  private val index: Int,
  private val blockData: BlockData,
  private val settings: BlockSettings,
  private val isSelected: Boolean = false,
  private val showText: Boolean = false,
  private val onBoxUpdate: (Int, BlockData) -> Unit = { _, _ -> },
  private val onBoxSelect: (Int) -> Unit = {},
  private val onHeavyChange: (() -> Unit)? = null
) : ImageOverlay {

  override val id = "box_${index}_${blockData.hashCode()}"
  override val isVisible = true
  override val renderOrder = 10

  companion object {
    private const val HANDLE_SIZE = 20f

    /**
     * Create BoxOverlay from current BoxOnImage parameters for easy migration
     */
    fun fromBoxOnImage(
      index: Int,
      blockPosition: BlockPosition,
      imageSize: IntSize,
      isSelected: Boolean,
      onPositionUpdate: (BlockPosition) -> Unit,
      onBoxSelect: (Int) -> Unit = {}
    ): BoxOverlay {
      val blockData = BlockData(
        id = UUID.randomUUID().toString(),
        blockPosition = blockPosition,
        text = "",
        settings = null
      )
      return BoxOverlay(
        index = index,
        blockData = blockData,
        settings = BlockSettings("Arial"), // default settings
        isSelected = isSelected,
        showText = false,
        onBoxUpdate = { _, newBlockData -> onPositionUpdate(newBlockData.blockPosition) },
        onBoxSelect = onBoxSelect
      )
    }

    /**
     * Create BoxOverlay from current BlockOnImage parameters for easy migration
     */
    fun fromBlockOnImage(
      index: Int,
      blockData: BlockData,
      basicSettings: BlockSettings,
      isSelected: Boolean,
      onDataUpdate: (BlockData) -> Unit,
      onBoxSelect: (Int) -> Unit = {},
      onHeavyChange: () -> Unit = {}
    ): BoxOverlay {
      return BoxOverlay(
        index = index,
        blockData = blockData,
        settings = blockData.settings ?: basicSettings,
        isSelected = isSelected,
        showText = true,
        onBoxUpdate = { _, newData -> onDataUpdate(newData) },
        onBoxSelect = onBoxSelect,
        onHeavyChange = onHeavyChange
      )
    }

    /**
     * Create simple BoxOverlay for new code (migration target)
     */
    fun create(
      index: Int,
      blockData: BlockData,
      settings: BlockSettings,
      isSelected: Boolean,
      onBoxUpdate: (Int, BlockData) -> Unit = { _, _ -> },
      onBoxSelect: (Int) -> Unit = {}
    ): BoxOverlay {
      return BoxOverlay(
        index = index,
        blockData = blockData,
        settings = settings,
        isSelected = isSelected,
        showText = false,
        onBoxUpdate = onBoxUpdate,
        onBoxSelect = onBoxSelect
      )
    }
  }

  @Composable
  override fun Render(state: ImageCanvasState, transformer: CoordinateTransformer) {
    if (!state.hasImage) return

    var currentBlockData by remember(blockData) { mutableStateOf(blockData) }
    val density = LocalDensity.current

    val canvasRect by remember {
      derivedStateOf {
        val imageRect = Rect(
          offset = Offset(
            currentBlockData.blockPosition.x.toFloat(),
            currentBlockData.blockPosition.y.toFloat()
          ),
          size = Size(
            currentBlockData.blockPosition.width.toFloat(),
            currentBlockData.blockPosition.height.toFloat()
          )
        )
        transformer.imageRectToCanvas(imageRect)
      }
    }

    val offsetX = with(density) { canvasRect.topLeft.x.toDp() }
    val offsetY = with(density) { canvasRect.topLeft.y.toDp() }
    val width = with(density) { canvasRect.width.toDp() }
    val height = with(density) { canvasRect.height.toDp() }

    val focusRequester = remember { FocusRequester() }

    Box(
      modifier = Modifier
        .offset(offsetX, offsetY)
        .size(width, height)
        .background(
          if (isSelected)
            Color.Blue.copy(alpha = 0.3f)
          else
            Color.Red.copy(alpha = 0.2f)
        )
        .border(
          width = 2.dp,
          color = if (isSelected) Color.Blue else Color.Red,
          shape = if (currentBlockData.blockPosition.shape is BlockPosition.Shape.Oval)
            RoundedCornerShape(50.dp)
          else
            RoundedCornerShape(0.dp)
        )
        .focusRequester(focusRequester)
        .clickable {
          onBoxSelect(index)
          focusRequester.requestFocus()
        }
        .focusable()
        .pointerInput(blockData.id, state.isSpacePressed) {
          detectDragGestures(
            onDragEnd = {
              onHeavyChange?.invoke()
            }
          ) { change, dragAmount ->
            if (state.isSpacePressed) {
              return@detectDragGestures
            }

            val imageSize = state.imageSize

            // change.previousPosition is relative to the Box (not the canvas)
            // because this pointerInput is on a Box with .offset()
            // We only need to convert the scale from canvas pixels to image pixels
            val scale = state.imageToCanvasScale * state.zoomScale
            val relativeX = change.previousPosition.x / scale
            val relativeY = change.previousPosition.y / scale

            // Convert drag amount from canvas pixels to image pixels
            val dragImageAmount = Offset(
              dragAmount.x / scale,
              dragAmount.y / scale
            )

            val newPosition = handleDragGesture(
              currentBlockData.blockPosition,
              relativeX,
              relativeY,
              dragImageAmount,
              imageSize
            )

            val newBlockData = currentBlockData.copy(blockPosition = newPosition)
            currentBlockData = newBlockData
            onBoxUpdate(index, newBlockData)

            change.consume()
          }
        }
    ) {
      if (showText && currentBlockData.text.isNotEmpty()) {
        val imageWidth = currentBlockData.blockPosition.width.toFloat()
        val scale = state.imageToCanvasScale * state.zoomScale
        val baseFontSize = settings.fontSize * (imageWidth / 100f)
        val displayFontSize = (baseFontSize * scale).coerceAtLeast(8f)

        // Scale padding and border with zoom (use image-space values)
        val basePadding = settings.border.toFloat()
        val displayPadding = (basePadding * scale).coerceAtLeast(1f)

        // Convert alignment
        val textAlign = when (settings.alignment) {
          Alignment.LEFT -> TextAlign.Left
          Alignment.CENTER -> TextAlign.Center
          Alignment.RIGHT -> TextAlign.Right
        }

        // Background color
        val bgColor = Color(
          red = settings.backgroundColor.r / 255f,
          green = settings.backgroundColor.g / 255f,
          blue = settings.backgroundColor.b / 255f,
          alpha = settings.backgroundColor.a / 255f
        )

        Text(
          text = currentBlockData.text,
          modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(displayPadding.dp),
          textAlign = textAlign,
          color = Color(
            red = settings.fontColor.r / 255f,
            green = settings.fontColor.g / 255f,
            blue = settings.fontColor.b / 255f,
            alpha = settings.fontColor.a / 255f
          ),
          fontSize = displayFontSize.sp,
          lineHeight = (displayFontSize * 1.2f).sp
        )
      }
    }
  }

  /**
   * Handle drag gestures with border detection (replicates current complex logic)
   */
  private fun handleDragGesture(
    position: BlockPosition,
    relativeX: Float,
    relativeY: Float,
    dragAmount: Offset,
    imageSize: IntSize
  ): BlockPosition {
    val handleSize = HANDLE_SIZE
    val isLeftBorder = relativeX < handleSize
    val isUpBorder = relativeY < handleSize
    val isRightBorder = relativeX > position.width - handleSize
    val isDownBorder = relativeY > position.height - handleSize

    var newX = position.x
    var newY = position.y
    var newWidth = position.width
    var newHeight = position.height

    when {
      isLeftBorder && isUpBorder -> {
        newX += dragAmount.x
        newY += dragAmount.y
        newWidth -= dragAmount.x
        newHeight -= dragAmount.y
      }

      isRightBorder && isUpBorder -> {
        newY += dragAmount.y
        newWidth += dragAmount.x
        newHeight -= dragAmount.y
      }

      isLeftBorder && isDownBorder -> {
        newX += dragAmount.x
        newWidth -= dragAmount.x
        newHeight += dragAmount.y
      }

      isRightBorder && isDownBorder -> {
        newWidth += dragAmount.x
        newHeight += dragAmount.y
      }

      isUpBorder -> {
        newY += dragAmount.y
        newHeight -= dragAmount.y
      }

      isLeftBorder -> {
        newX += dragAmount.x
        newWidth -= dragAmount.x
      }

      isRightBorder -> {
        newWidth += dragAmount.x
      }

      isDownBorder -> {
        newHeight += dragAmount.y
      }

      else -> {
        newX += dragAmount.x
        newY += dragAmount.y
      }
    }

    newX = newX.coerceAtLeast(0.0)
    newY = newY.coerceAtLeast(0.0)
    newWidth = newWidth.coerceAtLeast(10.0)
    newHeight = newHeight.coerceAtLeast(10.0)

    if (newX + newWidth > imageSize.width) {
      newWidth = imageSize.width - newX
    }
    if (newY + newHeight > imageSize.height) {
      newHeight = imageSize.height - newY
    }

    return position.copy(
      x = newX,
      y = newY,
      width = newWidth,
      height = newHeight
    )
  }
}

/**
 * Migration helper functions for easy conversion from existing components
 */
object BoxOverlayMigration {

  /**
   * Create overlays from existing BoxOnImage usage pattern
   */
  fun createBoxOverlays(
    boxes: List<BlockPosition>,
    selectedBoxIndex: Int?,
    onBoxUpdate: (Int, BlockPosition) -> Unit,
    onBoxSelect: (Int?) -> Unit
  ): List<BoxOverlay> {
    return boxes.mapIndexed { index, position ->
      BoxOverlay.fromBoxOnImage(
        index = index,
        blockPosition = position,
        imageSize = IntSize(1000, 1000),
        isSelected = selectedBoxIndex == index,
        onPositionUpdate = { newPosition -> onBoxUpdate(index, newPosition) },
        onBoxSelect = { onBoxSelect(index) }
      )
    }
  }

  /**
   * Create overlays from existing BlockOnImage usage pattern
   */
  fun createBlockOverlays(
    blocks: List<BlockData>,
    basicSettings: BlockSettings,
    selectedBoxIndex: Int?,
    onBlockUpdate: (Int, BlockData) -> Unit,
    onBoxSelect: (Int?) -> Unit,
    onHeavyChange: () -> Unit = {}
  ): List<BoxOverlay> {
    return blocks.mapIndexed { index, blockData ->
      BoxOverlay.fromBlockOnImage(
        index = index,
        blockData = blockData,
        basicSettings = basicSettings,
        isSelected = selectedBoxIndex == index,
        onDataUpdate = { newData -> onBlockUpdate(index, newData) },
        onBoxSelect = { onBoxSelect(index) },
        onHeavyChange = onHeavyChange
      )
    }
  }
}