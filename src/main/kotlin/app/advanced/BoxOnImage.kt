package app.advanced

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val HANDLE_SIZE = 16

private fun isTwoPointsNear(x1: Double, y1: Double, x2: Double, y2: Double): Boolean =
  abs(x1 - x2) < HANDLE_SIZE && abs(y1 - y2) < HANDLE_SIZE

private fun insideRectangle(
  rectangleX1: Double,
  rectangleY1: Double,
  rectangleX2: Double,
  rectangleY2: Double,
  pointX: Double,
  pointY: Double
): Boolean {
  val leftUpX = min(rectangleX1, rectangleX2)
  val leftUpY = min(rectangleY1, rectangleY2)
  val rightDownX = max(rectangleX1, rectangleX2)
  val rightDownY = max(rectangleY1, rectangleY2)
  return pointX in leftUpX..rightDownX && pointY in leftUpY..rightDownY
}

@Composable
fun BoxOnImage(boxOnImageData: BoxOnImageWithSizeData) {
  with(boxOnImageData) {
    Box(
      modifier = Modifier
        .offset(this.xWithoutDensity.dp, this.yWithoutDensity.dp)
        .background(Color(Color.Blue.red, Color.Blue.green, Color.Blue.blue, 0.3f))
        .size(this.sizeXWithoutDensity.dp, this.sizeYWithoutDensity.dp)
        .pointerInput(Unit) {
          detectDragGestures { change, dragAmount ->
            val touchX = change.previousPosition.x + x
            val touchY = change.previousPosition.y + y

            val changeX = dragAmount.x
            val changeY = dragAmount.y

            if (!insideRectangle(x, y, x + sizeX, y + sizeY, touchX, touchY)) {
              return@detectDragGestures
            }

            change.consume()

            if (isLeftUpCorner(touchX, touchY)) {
              x += changeX
              y += changeY
              sizeX -= changeX
              sizeY -= changeY
              imageCorrection(displayImageSize)
              return@detectDragGestures
            }
            if (isRightUpCorner(touchX, touchY)) {
              y += changeY
              sizeX += changeX
              sizeY -= changeY
              imageCorrection(displayImageSize)
              return@detectDragGestures
            }
            if (isLeftDownCorner(touchX, touchY)) {
              x += changeX
              sizeX -= changeX
              sizeY += changeY
              imageCorrection(displayImageSize)
              return@detectDragGestures
            }
            if (isRightDownCorner(touchX, touchY)) {
              sizeX += changeX
              sizeY += changeY
              imageCorrection(displayImageSize)
              return@detectDragGestures
            }

            if (isUpBorder(touchX, touchY)) {
              y += changeY
              sizeY -= changeY
              imageCorrection(displayImageSize)
              return@detectDragGestures
            }
            if (isLeftBorder(touchX, touchY)) {
              x += changeX
              sizeX -= changeX
              imageCorrection(displayImageSize)
              return@detectDragGestures
            }
            if (isRightBorder(touchX, touchY)) {
              sizeX += changeX
              imageCorrection(displayImageSize)
              return@detectDragGestures
            }
            if (isDownBorder(touchX, touchY)) {
              sizeY += changeY
              imageCorrection(displayImageSize)
              return@detectDragGestures
            }

            if (insideRectangle(x, y, x + sizeX, y + sizeY, touchX, touchY)) {
              x += changeX
              y += changeY
              imageCorrection(displayImageSize)
            }
          }
        }
        // TODO pointerHoverIcon
    )
  }
}

data class BoxOnImageData(
  val stateX: MutableState<Int>,
  val stateY: MutableState<Int>,
  val stateSizeX: MutableState<Int>,
  val stateSizeY: MutableState<Int>,
) {
  constructor(offsetX: Int, offsetY: Int, sizeX: Int, sizeY: Int) : this(
    mutableStateOf(offsetX),
    mutableStateOf(offsetY),
    mutableStateOf(sizeX),
    mutableStateOf(sizeY)
  )

  var x: Int = stateX.value.toInt()
    get() = stateX.value.toInt()
    set(value) {
      stateX.value = value
      field = value
    }

  var y: Int = stateY.value.toInt()
    get() = stateY.value.toInt()
    set(value) {
      stateY.value = value
      field = value
    }

  var sizeX: Int = stateSizeX.value.toInt()
    get() = stateSizeX.value.toInt()
    set(value) {
      stateSizeX.value = value
      field = value
    }

  var sizeY: Int = stateSizeY.value.toInt()
    get() = stateSizeY.value.toInt()
    set(value) {
      stateSizeY.value = value
      field = value
    }
}

data class BoxOnImageWithSizeData(
  val boxOnImageData: BoxOnImageData,
  val density: Float,
  val displayImageSize: MutableState<IntSize>,
  val originalImageSize: IntSize
) {
  private fun scaleFromDisplayToOrigin(): Double = max(
    originalImageSize.width.toDouble() / displayImageSize.value.width / density,
    originalImageSize.height.toDouble() / displayImageSize.value.height / density
  )

  private fun scaleFromOriginToDisplay(): Double = min(
    displayImageSize.value.width.toDouble() / originalImageSize.width * density,
    displayImageSize.value.height.toDouble() / originalImageSize.height * density
  )

  private fun Int.convertToLocal(): Double = this * scaleFromOriginToDisplay()
  private fun Double.convertToGlobal(): Int = (this * scaleFromDisplayToOrigin()).toInt()

  var x: Double = boxOnImageData.x.convertToLocal()
    set(value) {
      field = value; boxOnImageData.x = value.convertToGlobal()
    }
  var y: Double = boxOnImageData.y.convertToLocal()
    set(value) {
      field = value; boxOnImageData.y = value.convertToGlobal()
    }
  var sizeX: Double = boxOnImageData.sizeX.convertToLocal()
    set(value) {
      field = value; boxOnImageData.sizeX = value.convertToGlobal()
    }
  var sizeY: Double = boxOnImageData.sizeY.convertToLocal()
    set(value) {
      field = value; boxOnImageData.sizeY = value.convertToGlobal()
    }

  val xWithoutDensity
    get() = x / density
  val yWithoutDensity
    get() = y / density
  val sizeXWithoutDensity
    get() = sizeX / density
  val sizeYWithoutDensity
    get() = sizeY / density

  fun isLeftUpCorner(x: Double, y: Double): Boolean =
    isTwoPointsNear(x, y, this.x, this.y)

  fun isLeftDownCorner(x: Double, y: Double): Boolean =
    isTwoPointsNear(x, y, this.x, this.y + this.sizeY)

  fun isRightUpCorner(x: Double, y: Double): Boolean =
    isTwoPointsNear(x, y, this.x + this.sizeX, this.y)

  fun isRightDownCorner(x: Double, y: Double): Boolean =
    isTwoPointsNear(x, y, this.x + this.sizeX, this.y + this.sizeY)

  fun isLeftBorder(x: Double, y: Double): Boolean =
    insideRectangle(
      this.x - HANDLE_SIZE,
      this.y - HANDLE_SIZE,
      this.x + HANDLE_SIZE,
      this.y + this.sizeY + HANDLE_SIZE,
      x,
      y
    )

  fun isUpBorder(x: Double, y: Double): Boolean =
    insideRectangle(
      this.x - HANDLE_SIZE,
      this.y - HANDLE_SIZE,
      this.x + this.sizeX + HANDLE_SIZE,
      this.y + HANDLE_SIZE,
      x,
      y
    )

  fun isRightBorder(x: Double, y: Double): Boolean =
    insideRectangle(
      this.x + this.sizeX - HANDLE_SIZE,
      this.y - HANDLE_SIZE,
      this.x + this.sizeX + HANDLE_SIZE,
      this.y + this.sizeY + HANDLE_SIZE,
      x,
      y
    )

  fun isDownBorder(x: Double, y: Double): Boolean =
    insideRectangle(
      this.x - HANDLE_SIZE,
      this.y + this.sizeY - HANDLE_SIZE,
      this.x + this.sizeX + HANDLE_SIZE,
      this.y + this.sizeY + HANDLE_SIZE,
      x,
      y
    )

  fun isNearOrInRectangle(x: Double, y: Double): Boolean =
    insideRectangle(
      this.x - HANDLE_SIZE,
      this.y - HANDLE_SIZE,
      this.x + this.sizeX + HANDLE_SIZE,
      this.y + this.sizeY + HANDLE_SIZE,
      x,
      y
    )

  fun imageCorrection(imageSize: State<IntSize>) {
    if (this.x + sizeX > imageSize.value.width) {
      sizeX = imageSize.value.width - this.x
    }
    if (this.y + sizeY > imageSize.value.height) {
      sizeY = imageSize.value.height - this.y
    }
    if (this.x < 0) {
      this.sizeX += this.x
      this.x = .0
    }
    if (this.y < 0) {
      this.sizeY += this.y
      this.y = .0
    }
  }
}