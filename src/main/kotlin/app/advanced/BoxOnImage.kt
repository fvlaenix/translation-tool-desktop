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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val HANDLE_SIZE = 16

private fun isTwoPointsNear(x1: Int, y1: Int, x2: Int, y2: Int): Boolean =
  abs(x1 - x2) < HANDLE_SIZE && abs(y1 - y2) < HANDLE_SIZE

private fun insideRectangle(rectangleX1: Int, rectangleY1: Int, rectangleX2: Int, rectangleY2: Int, pointX: Int, pointY: Int): Boolean {
  val leftUpX = min(rectangleX1, rectangleX2)
  val leftUpY = min(rectangleY1, rectangleY2)
  val rightDownX = max(rectangleX1, rectangleX2)
  val rightDownY = max(rectangleY1, rectangleY2)
  return pointX in leftUpX..rightDownX && pointY in leftUpY..rightDownY
}

@Composable
fun BoxOnImage(boxOnImageData: BoxOnImageData, imageSize: State<IntSize>) {
  with(boxOnImageData) {
    Box(
      modifier = Modifier
        .offset { IntOffset(offsetX.value.toInt(), offsetY.value.toInt()) }
        .background(Color(Color.Blue.red, Color.Blue.green, Color.Blue.blue, 0.3f))
        .size(sizeX.value.dp, sizeY.value.dp)
        .pointerInput(Unit) {
          detectDragGestures { change, dragAmount ->
            val touchX = change.previousPosition.x.toInt() + boxOnImageData.x
            val touchY = change.previousPosition.y.toInt() + boxOnImageData.y

            val changeX = dragAmount.x.toInt()
            val changeY = dragAmount.y.toInt()

            if (!insideRectangle(boxOnImageData.x, boxOnImageData.y, boxOnImageData.x + boxOnImageData.sizeX.value, boxOnImageData.y + boxOnImageData.sizeY.value, touchX, touchY)) {
              return@detectDragGestures
            }

            change.consume()

            if (isLeftUpCorner(touchX, touchY)) {
              boxOnImageData.offsetX.value += changeX
              boxOnImageData.offsetY.value += changeY
              boxOnImageData.sizeX.value -= changeX
              boxOnImageData.sizeY.value -= changeY
              imageCorrection(imageSize)
              return@detectDragGestures
            }
            if (isRightUpCorner(touchX, touchY)) {
              boxOnImageData.offsetY.value += changeY
              boxOnImageData.sizeX.value += changeX
              boxOnImageData.sizeY.value -= changeY
              imageCorrection(imageSize)
              return@detectDragGestures
            }
            if (isLeftDownCorner(touchX, touchY)) {
              boxOnImageData.offsetX.value += changeX
              boxOnImageData.sizeX.value -= changeX
              boxOnImageData.sizeY.value += changeY
              imageCorrection(imageSize)
              return@detectDragGestures
            }
            if (isRightDownCorner(touchX, touchY)) {
              boxOnImageData.sizeX.value += changeX
              boxOnImageData.sizeY.value += changeY
              imageCorrection(imageSize)
              return@detectDragGestures
            }

            if (isUpBorder(touchX, touchY)) {
              boxOnImageData.offsetY.value += changeY
              boxOnImageData.sizeY.value -= changeY
              imageCorrection(imageSize)
              return@detectDragGestures
            }
            if (isLeftBorder(touchX, touchY)) {
              boxOnImageData.offsetX.value += changeX
              boxOnImageData.sizeX.value -= changeX
              imageCorrection(imageSize)
              return@detectDragGestures
            }
            if (isRightBorder(touchX, touchY)) {
              boxOnImageData.sizeX.value += changeX
              imageCorrection(imageSize)
              return@detectDragGestures
            }
            if (isDownBorder(touchX, touchY)) {
              boxOnImageData.sizeY.value += changeY
              imageCorrection(imageSize)
              return@detectDragGestures
            }

            if (insideRectangle(boxOnImageData.x, boxOnImageData.y, boxOnImageData.x + boxOnImageData.sizeX.value, boxOnImageData.y + boxOnImageData.sizeY.value, touchX, touchY)) {
              boxOnImageData.offsetX.value += changeX
              boxOnImageData.offsetY.value += changeY
              imageCorrection(imageSize)
            }
          }
        }
        // TODO pointerHoverIcon
    )
  }
}

data class BoxOnImageData(
  val offsetX: MutableState<Float>,
  val offsetY: MutableState<Float>,
  val sizeX: MutableState<Int>,
  val sizeY: MutableState<Int>,
) {
  val x
    get() = offsetX.value.toInt()
  val y
    get() = offsetY.value.toInt()

  constructor(offsetX: Float, offsetY: Float, sizeX: Int, sizeY: Int) : this(
    mutableStateOf(offsetX),
    mutableStateOf(offsetY),
    mutableStateOf(sizeX),
    mutableStateOf(sizeY)
  )

  fun isLeftUpCorner(x: Int, y: Int): Boolean =
    isTwoPointsNear(x, y, this.x, this.y)

  fun isLeftDownCorner(x: Int, y: Int): Boolean =
    isTwoPointsNear(x, y, this.x, this.y + this.sizeY.value)

  fun isRightUpCorner(x: Int, y: Int): Boolean =
    isTwoPointsNear(x, y, this.x + this.sizeX.value, this.y)

  fun isRightDownCorner(x: Int, y: Int): Boolean =
    isTwoPointsNear(x, y, this.x + this.sizeX.value, this.y + this.sizeY.value)

  fun isLeftBorder(x: Int, y: Int): Boolean =
    insideRectangle(this.x - HANDLE_SIZE, this.y - HANDLE_SIZE, this.x + HANDLE_SIZE, this.y + this.sizeY.value + HANDLE_SIZE, x, y)

  fun isUpBorder(x: Int, y: Int): Boolean =
    insideRectangle(this.x - HANDLE_SIZE, this.y - HANDLE_SIZE, this.x + this.sizeX.value + HANDLE_SIZE, this.y + HANDLE_SIZE, x, y)

  fun isRightBorder(x: Int, y: Int): Boolean =
    insideRectangle(this.x + this.sizeX.value - HANDLE_SIZE, this.y - HANDLE_SIZE, this.x + this.sizeX.value + HANDLE_SIZE, this.y + this.sizeY.value + HANDLE_SIZE, x, y)

  fun isDownBorder(x: Int, y: Int): Boolean =
    insideRectangle(this.x - HANDLE_SIZE, this.y + this.sizeY.value - HANDLE_SIZE, this.x + this.sizeX.value + HANDLE_SIZE, this.y + this.sizeY.value + HANDLE_SIZE, x, y)

  fun isNearOrInRectangle(x: Int, y: Int): Boolean =
    insideRectangle(this.x - HANDLE_SIZE, this.y - HANDLE_SIZE, this.x + this.sizeX.value + HANDLE_SIZE, this.y + this.sizeY.value + HANDLE_SIZE, x, y)

  fun imageCorrection(imageSize: State<IntSize>) {
    if (this.x + sizeX.value > imageSize.value.width) {
      sizeX.value = imageSize.value.width - this.x
    }
    if (this.y + sizeY.value > imageSize.value.height) {
      sizeY.value = imageSize.value.height - this.y
    }
    if (this.x < 0) {
      this.offsetX.value = 0f
    }
    if (this.y < 0) {
      this.offsetY.value = 0f
    }
  }
}