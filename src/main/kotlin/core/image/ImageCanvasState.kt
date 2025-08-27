package core.image

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import java.awt.image.BufferedImage
import kotlin.math.min

@Stable
class ImageCanvasState {
  private val _image = mutableStateOf<BufferedImage?>(null)
  val image: BufferedImage? by _image

  private val _canvasSize = mutableStateOf(IntSize.Zero)
  val canvasSize: IntSize by _canvasSize

  private val _imageDisplaySize = mutableStateOf(IntSize.Zero)
  val imageDisplaySize: IntSize by _imageDisplaySize

  private val _imageToCanvasScale = mutableStateOf(1f)
  val imageToCanvasScale: Float by _imageToCanvasScale

  private val _imageOffsetInCanvas = mutableStateOf(Offset.Zero)
  val imageOffsetInCanvas: Offset by _imageOffsetInCanvas

  private val _isLoading = mutableStateOf(false)
  val isLoading: Boolean by _isLoading

  val imageSize: IntSize
    get() = _image.value?.let { IntSize(it.width, it.height) } ?: IntSize.Zero

  val hasImage: Boolean
    get() = _image.value != null

  fun setImage(newImage: BufferedImage?) {
    if (_image.value != newImage) {
      _isLoading.value = newImage == null && _image.value != null
      _image.value = newImage
      recalculateTransformations()
      _isLoading.value = false
    }
  }

  fun updateCanvasSize(size: IntSize) {
    if (_canvasSize.value != size) {
      _canvasSize.value = size
      recalculateTransformations()
    }
  }

  fun setLoading(loading: Boolean) {
    _isLoading.value = loading
  }

  private fun recalculateTransformations() {
    val currentImage = _image.value
    if (currentImage == null || _canvasSize.value.width <= 0 || _canvasSize.value.height <= 0) {
      _imageDisplaySize.value = IntSize.Zero
      _imageToCanvasScale.value = 1f
      _imageOffsetInCanvas.value = Offset.Zero
      return
    }

    val imageWidth = currentImage.width
    val imageHeight = currentImage.height

    if (imageWidth <= 0 || imageHeight <= 0) {
      _imageDisplaySize.value = IntSize.Zero
      _imageToCanvasScale.value = 1f
      _imageOffsetInCanvas.value = Offset.Zero
      return
    }

    val scaleX = _canvasSize.value.width.toFloat() / imageWidth
    val scaleY = _canvasSize.value.height.toFloat() / imageHeight
    val scale = min(scaleX, scaleY)

    val displayWidth = (imageWidth * scale).toInt()
    val displayHeight = (imageHeight * scale).toInt()

    val offsetX = (_canvasSize.value.width - displayWidth) / 2f
    val offsetY = (_canvasSize.value.height - displayHeight) / 2f

    _imageToCanvasScale.value = scale
    _imageDisplaySize.value = IntSize(displayWidth, displayHeight)
    _imageOffsetInCanvas.value = Offset(offsetX, offsetY)
  }

  fun getImageBoundsInCanvas(): androidx.compose.ui.geometry.Rect {
    return androidx.compose.ui.geometry.Rect(
      offset = _imageOffsetInCanvas.value,
      size = androidx.compose.ui.geometry.Size(
        _imageDisplaySize.value.width.toFloat(),
        _imageDisplaySize.value.height.toFloat()
      )
    )
  }

  fun debugValidateState(): Boolean {
    val currentImage = _image.value ?: return true

    if (_canvasSize.value.width <= 0 || _canvasSize.value.height <= 0) return true

    val expectedScale = min(
      _canvasSize.value.width.toFloat() / currentImage.width,
      _canvasSize.value.height.toFloat() / currentImage.height
    )

    val scaleDiff = kotlin.math.abs(_imageToCanvasScale.value - expectedScale)
    if (scaleDiff > 0.001f) {
      println("DEBUG: Scale mismatch - expected: $expectedScale, actual: ${_imageToCanvasScale.value}")
      return false
    }

    return true
  }
}