package app.advanced

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.block.SimpleLoadedImageDisplayer
import bean.BlockData
import bean.BlockPosition
import bean.BlockSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import utils.KotlinUtils.applyIf
import utils.PreemptiveCoroutineScope
import utils.Text2ImageUtils
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min

private const val HANDLE_SIZE = 16

private fun Modifier.pointerInputForBox(
  rectangle: AbstractRectangle,
  convertToGlobal: Double.() -> Double,
  onClick: () -> Unit = {}
): Modifier {
  fun isLeftBorder(x: Float): Boolean = x < HANDLE_SIZE
  fun isUpBorder(y: Float): Boolean = y < HANDLE_SIZE
  fun isRightBorder(x: Float): Boolean = x > rectangle.width - HANDLE_SIZE
  fun isDownBorder(y: Float): Boolean = y > rectangle.height - HANDLE_SIZE

  return with(rectangle) {
    this@pointerInputForBox
      .pointerInput(Unit) {
        detectTapGestures { offset ->
          onClick()
        }
      }
      .pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
          val touchX = change.previousPosition.x.toDouble().convertToGlobal().toFloat()
          val touchY = change.previousPosition.y.toDouble().convertToGlobal().toFloat()

          val changeX = dragAmount.x.toDouble().convertToGlobal()
          val changeY = dragAmount.y.toDouble().convertToGlobal()

          change.consume()

          if (isLeftBorder(touchX) && isUpBorder(touchY)) {
            x += changeX
            y += changeY
            width -= changeX
            height -= changeY
            return@detectDragGestures
          }
          if (isRightBorder(touchX) && isUpBorder(touchY)) {
            y += changeY
            width += changeX
            height -= changeY
            return@detectDragGestures
          }
          if (isLeftBorder(touchX) && isDownBorder(touchY)) {
            x += changeX
            width -= changeX
            height += changeY
            return@detectDragGestures
          }
          if (isRightBorder(touchX) && isDownBorder(touchY)) {
            width += changeX
            height += changeY
            return@detectDragGestures
          }

          if (isUpBorder(touchY)) {
            y += changeY
            height -= changeY
            return@detectDragGestures
          }
          if (isLeftBorder(touchX)) {
            x += changeX
            width -= changeX
            return@detectDragGestures
          }
          if (isRightBorder(touchX)) {
            width += changeX
            return@detectDragGestures
          }
          if (isDownBorder(touchY)) {
            height += changeY
            return@detectDragGestures
          }
          x += changeX
          y += changeY
        }
      }
    // TODO pointer hover icon
  }
}

@Composable
fun BlockOnImage(
  jobCounter: AtomicInteger,
  imageSize: IntSize,
  displayImageSize: IntSize,
  basicSettings: BlockSettings,
  blockData: MutableState<BlockData>,
  index: Int,
  selectedBoxIndex: MutableState<Int?>
) {
  val image = remember { mutableStateOf<BufferedImage?>(null) }
  val isImageFit = remember { mutableStateOf(true) }
  val coroutineScope = rememberCoroutineScope()
  val preemptiveCoroutineScope = remember { PreemptiveCoroutineScope(coroutineScope) }

  val realSettings = blockData.value.settings ?: basicSettings

  LaunchedEffect(
    realSettings,
    blockData.value.blockPosition,
    blockData.value.text
  ) {
    jobCounter.incrementAndGet()

    preemptiveCoroutineScope.launch(Dispatchers.IO) {
      delay(100)
      image.value = null
      val (resultImage, resultIsImageFit) = Text2ImageUtils.textToImage(
        realSettings,
        blockData.value.copy(
          blockPosition = blockData.value.blockPosition.copy(
            x = .0,
            y = .0
          )
        )
      )
      image.value = resultImage
      isImageFit.value = resultIsImageFit
      jobCounter.decrementAndGet()
    }
  }

  if (image.value != null) {
    fun scaleFromDisplayToOrigin(): Double = max(
      imageSize.width.toDouble() / displayImageSize.width,
      imageSize.height.toDouble() / displayImageSize.height
    )

    fun scaleFromOriginToDisplay(): Double = min(
      displayImageSize.width.toDouble() / imageSize.width,
      displayImageSize.height.toDouble() / imageSize.height
    )

    fun Double.convertToLocal(): Double = this * scaleFromOriginToDisplay()
    fun Double.convertToGlobal(): Double = this * scaleFromDisplayToOrigin()

    val x: Double = blockData.value.blockPosition.x.convertToLocal()
    val y: Double = blockData.value.blockPosition.y.convertToLocal()
    val sizeX: Double = blockData.value.blockPosition.width.convertToLocal()
    val sizeY: Double = blockData.value.blockPosition.height.convertToLocal()

    val rectangle = BlockDataRectangle(blockData, imageSize)

    SimpleLoadedImageDisplayer<Unit>(
      modifier = Modifier
        .offset(x.dp, y.dp)
        .size(sizeX.dp, sizeY.dp)
        .applyIf(index == selectedBoxIndex.value) { it.border(2.dp, if (isImageFit.value) Color.Blue else Color.Red) }
        .pointerInputForBox(
          rectangle = rectangle,
          convertToGlobal = { convertToGlobal() },
          onClick = { selectedBoxIndex.value = index }),
      image = image
    )
  }
}

@Composable
fun BoxOnImage(
  imageSize: IntSize,
  displayImageSize: IntSize,
  blockData: MutableState<BlockPosition>
) {
  fun scaleFromDisplayToOrigin(): Double = max(
    imageSize.width.toDouble() / displayImageSize.width,
    imageSize.height.toDouble() / displayImageSize.height
  )

  fun scaleFromOriginToDisplay(): Double = min(
    displayImageSize.width.toDouble() / imageSize.width,
    displayImageSize.height.toDouble() / imageSize.height
  )

  fun Double.convertToLocal(): Double = this * scaleFromOriginToDisplay()
  fun Double.convertToGlobal(): Double = this * scaleFromDisplayToOrigin()

  val x: Double = blockData.value.x.convertToLocal()
  val y: Double = blockData.value.y.convertToLocal()
  val sizeX: Double = blockData.value.width.convertToLocal()
  val sizeY: Double = blockData.value.height.convertToLocal()

  val rectangle = BlockPositionRectangle(blockData, imageSize)

  Box(
    modifier = Modifier
      .offset(x.dp, y.dp)
      .background(Color(Color.Blue.red, Color.Blue.green, Color.Blue.blue, 0.3f))
      .size(sizeX.dp, sizeY.dp)
      .pointerInputForBox(rectangle = rectangle, convertToGlobal = { convertToGlobal() })
  )
}

interface AbstractRectangle {
  var x: Double
  var y: Double
  var width: Double
  var height: Double
}

class BlockDataRectangle(
  private val mutableState: MutableState<BlockData>,
  private val imageSize: IntSize
) : AbstractRectangle {
  override var x: Double
    get() = mutableState.value.blockPosition.x
    set(value) {
      mutableState.changeType { copy(x = value) }
    }
  override var y: Double
    get() = mutableState.value.blockPosition.y
    set(value) {
      mutableState.changeType { copy(y = value) }
    }
  override var width: Double
    get() = mutableState.value.blockPosition.width
    set(value) {
      mutableState.changeType { copy(width = value) }
    }
  override var height: Double
    get() = mutableState.value.blockPosition.height
    set(value) {
      mutableState.changeType { copy(height = value) }
    }

  private fun MutableState<BlockData>.changeData(block: BlockData.() -> BlockData) {
    this.value = block(this.value)
  }

  private fun MutableState<BlockData>.changeType(block: BlockPosition.() -> BlockPosition) {
    changeData {
      copy(blockPosition = block(blockPosition).imageCorrection(imageSize))
    }
  }
}

class BlockPositionRectangle(
  private val mutableState: MutableState<BlockPosition>,
  private val imageSize: IntSize
) : AbstractRectangle {
  private fun MutableState<BlockPosition>.changeType(block: BlockPosition.() -> BlockPosition) {
    this.value = block(this.value).imageCorrection(imageSize)
  }

  override var x: Double
    get() = mutableState.value.x
    set(value) {
      mutableState.changeType { copy(x = value) }
    }
  override var y: Double
    get() = mutableState.value.y
    set(value) {
      mutableState.changeType { copy(y = value) }
    }
  override var width: Double
    get() = mutableState.value.width
    set(value) {
      mutableState.changeType { copy(width = value) }
    }
  override var height: Double
    get() = mutableState.value.height
    set(value) {
      mutableState.changeType { copy(height = value) }
    }

}

private fun BlockPosition.imageCorrection(imageSize: IntSize): BlockPosition {
  var sizeX = this.width
  var sizeY = this.height
  var x = this.x
  var y = this.y
  if (this.x + this.width > imageSize.width) {
    sizeX = imageSize.width - this.x
  }
  if (this.y + this.height > imageSize.height) {
    sizeY = imageSize.height - this.y
  }
  if (this.x < 0) {
    sizeX += this.x
    x = .0
  }
  if (this.y < 0) {
    sizeY += this.y
    y = .0
  }
  return copy(
    x = x,
    y = y,
    width = sizeX,
    height = sizeY
  )
}