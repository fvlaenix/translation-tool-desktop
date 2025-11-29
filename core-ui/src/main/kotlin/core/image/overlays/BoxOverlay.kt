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
        .pointerInput(currentBlockData, state.isSpacePressed) {
          detectDragGestures(
            onDragEnd = {
              onHeavyChange?.invoke()
            }
          ) { change, dragAmount ->
            if (state.isSpacePressed) {
              return@detectDragGestures
            }

            val imageSize = state.imageSize

            val touchCanvasPos = change.previousPosition
            val touchImagePos = transformer.canvasToImage(touchCanvasPos)
            val dragImageAmount = transformer.canvasToImage(dragAmount) - transformer.canvasToImage(Offset.Zero)

            val relativeX = touchImagePos.x - currentBlockData.blockPosition.x
            val relativeY = touchImagePos.y - currentBlockData.blockPosition.y

            val newPosition = handleDragGesture(
              currentBlockData.blockPosition,
              relativeX.toFloat(),
              relativeY.toFloat(),
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
        Text(
          text = currentBlockData.text,
          modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
          textAlign = TextAlign.Center,
          color = Color(
            red = settings.fontColor.r / 255f,
            green = settings.fontColor.g / 255f,
            blue = settings.fontColor.b / 255f,
            alpha = settings.fontColor.a / 255f
          ),
          fontSize = (settings.fontSize * (width.value / 100f)).coerceAtLeast(8f).sp
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