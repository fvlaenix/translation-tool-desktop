package core.image

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import java.awt.image.BufferedImage
import kotlin.math.min

/**
 * Manages image display state with transformations and scaling for canvas rendering.
 */
@Stable
class ImageCanvasState {
  private val _bufferedImage = mutableStateOf<BufferedImage?>(null)
  private val _imageBitmap = mutableStateOf<ImageBitmap?>(null)

  private val _imageSourceType = mutableStateOf<ImageSourceType>(ImageSourceType.NONE)
  
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

  val bufferedImage: BufferedImage?
    get() = _bufferedImage.value

  val imageBitmap: ImageBitmap?
    get() = _imageBitmap.value

  val imageSize: IntSize
    get() = when (_imageSourceType.value) {
      ImageSourceType.BUFFERED_IMAGE -> _bufferedImage.value?.let { IntSize(it.width, it.height) } ?: IntSize.Zero
      ImageSourceType.IMAGE_BITMAP -> _imageBitmap.value?.let { IntSize(it.width, it.height) } ?: IntSize.Zero
      ImageSourceType.NONE -> IntSize.Zero
    }

  val hasImage: Boolean
    get() = _bufferedImage.value != null || _imageBitmap.value != null

  /**
   * Sets buffered image and updates canvas state.
   */
  fun setImage(newImage: BufferedImage?) {
    if (_bufferedImage.value != newImage) {
      _isLoading.value = newImage == null && hasImage
      _bufferedImage.value = newImage
      _imageBitmap.value = null
      _imageSourceType.value = if (newImage != null) ImageSourceType.BUFFERED_IMAGE else ImageSourceType.NONE
      recalculateTransformations()
      _isLoading.value = false
    }
  }

  /**
   * Sets image bitmap and updates canvas state.
   */
  fun setImage(newImage: ImageBitmap?) {
    if (_imageBitmap.value != newImage) {
      _isLoading.value = newImage == null && hasImage
      _imageBitmap.value = newImage
      _bufferedImage.value = null
      _imageSourceType.value = if (newImage != null) ImageSourceType.IMAGE_BITMAP else ImageSourceType.NONE
      recalculateTransformations()
      _isLoading.value = false
    }
  }

  /**
   * Gets image bitmap suitable for compose rendering.
   */
  fun getImageBitmapForRendering(): ImageBitmap? {
    return when (_imageSourceType.value) {
      ImageSourceType.IMAGE_BITMAP -> _imageBitmap.value
      ImageSourceType.BUFFERED_IMAGE -> {
        _bufferedImage.value?.let { bufferedImg ->
          try {
            val outputStream = java.io.ByteArrayOutputStream()
            javax.imageio.ImageIO.write(bufferedImg, "png", outputStream)
            val byteArray = outputStream.toByteArray()
            androidx.compose.ui.res.loadImageBitmap(java.io.ByteArrayInputStream(byteArray))
          } catch (e: Exception) {
            println("Error converting BufferedImage to ImageBitmap: ${e.message}")
            null
          }
        }
      }

      ImageSourceType.NONE -> null
    }
  }

  /**
   * Gets buffered image for overlay processing.
   */
  fun getBufferedImageForOverlays(): BufferedImage? {
    return when (_imageSourceType.value) {
      ImageSourceType.BUFFERED_IMAGE -> _bufferedImage.value
      ImageSourceType.IMAGE_BITMAP -> {
        null
      }

      ImageSourceType.NONE -> null
    }
  }

  /**
   * Updates canvas size and recalculates transformations.
   */
  fun updateCanvasSize(size: IntSize) {
    if (_canvasSize.value != size) {
      _canvasSize.value = size
      recalculateTransformations()
    }
  }

  /**
   * Sets loading state for image operations.
   */
  fun setLoading(loading: Boolean) {
    _isLoading.value = loading
  }

  private fun recalculateTransformations() {
    if (!hasImage || _canvasSize.value.width <= 0 || _canvasSize.value.height <= 0) {
      _imageDisplaySize.value = IntSize.Zero
      _imageToCanvasScale.value = 1f
      _imageOffsetInCanvas.value = Offset.Zero
      return
    }

    val currentImageSize = imageSize
    if (currentImageSize.width <= 0 || currentImageSize.height <= 0) {
      _imageDisplaySize.value = IntSize.Zero
      _imageToCanvasScale.value = 1f
      _imageOffsetInCanvas.value = Offset.Zero
      return
    }

    val scaleX = _canvasSize.value.width.toFloat() / currentImageSize.width
    val scaleY = _canvasSize.value.height.toFloat() / currentImageSize.height
    val scale = min(scaleX, scaleY)

    val displayWidth = (currentImageSize.width * scale).toInt()
    val displayHeight = (currentImageSize.height * scale).toInt()

    val offsetX = (_canvasSize.value.width - displayWidth) / 2f
    val offsetY = (_canvasSize.value.height - displayHeight) / 2f

    _imageToCanvasScale.value = scale
    _imageDisplaySize.value = IntSize(displayWidth, displayHeight)
    _imageOffsetInCanvas.value = Offset(offsetX, offsetY)
  }

  /**
   * Gets image boundaries within canvas coordinates.
   */
  fun getImageBoundsInCanvas(): androidx.compose.ui.geometry.Rect {
    return androidx.compose.ui.geometry.Rect(
      offset = _imageOffsetInCanvas.value,
      size = androidx.compose.ui.geometry.Size(
        _imageDisplaySize.value.width.toFloat(),
        _imageDisplaySize.value.height.toFloat()
      )
    )
  }

  /**
   * Validates internal state consistency for debugging.
   */
  fun debugValidateState(): Boolean {
    if (!hasImage) return true

    if (_canvasSize.value.width <= 0 || _canvasSize.value.height <= 0) return true

    val currentImageSize = imageSize
    val expectedScale = min(
      _canvasSize.value.width.toFloat() / currentImageSize.width,
      _canvasSize.value.height.toFloat() / currentImageSize.height
    )

    val scaleDiff = kotlin.math.abs(_imageToCanvasScale.value - expectedScale)
    if (scaleDiff > 0.001f) {
      println("DEBUG: Scale mismatch - expected: $expectedScale, actual: ${_imageToCanvasScale.value}")
      return false
    }

    return true
  }

  private enum class ImageSourceType {
    NONE, BUFFERED_IMAGE, IMAGE_BITMAP
  }
}