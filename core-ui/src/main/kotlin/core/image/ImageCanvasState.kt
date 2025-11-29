package core.image

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
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

  private val _zoomScale = mutableStateOf(1f)
  val zoomScale: Float by _zoomScale

  private val _panOffset = mutableStateOf(Offset.Zero)
  val panOffset: Offset by _panOffset

  var minZoom = 0.2f
  var maxZoom = 6f

  private val _isSpacePressed = mutableStateOf(false)
  val isSpacePressed: Boolean by _isSpacePressed

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
      resetView()
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
      resetView()
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
            e.printStackTrace()
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
   * Zoom by a factor delta, centered on a focal point in canvas coordinates.
   */
  fun zoomBy(factorDelta: Float, focalCanvasPoint: Offset) {
    val newScale = (_zoomScale.value * factorDelta).coerceIn(minZoom, maxZoom)
    applyZoom(newScale, focalCanvasPoint)
  }

  /**
   * Set zoom to an absolute scale value, centered on a focal point in canvas coordinates.
   */
  fun setZoom(scale: Float, focalCanvasPoint: Offset = getCanvasCenter()) {
    applyZoom(scale.coerceIn(minZoom, maxZoom), focalCanvasPoint)
  }

  /**
   * Pan the view by a delta in canvas coordinates.
   */
  fun panBy(delta: Offset) {
    _panOffset.value += delta
    clampPanToVisibilityBounds()
  }

  /**
   * Reset zoom and pan to default (fit-to-canvas).
   */
  fun resetView() {
    _zoomScale.value = 1f
    _panOffset.value = Offset.Zero
  }

  /**
   * Get the center point of the canvas in canvas coordinates.
   */
  fun getCanvasCenter(): Offset {
    return Offset(
      _canvasSize.value.width / 2f,
      _canvasSize.value.height / 2f
    )
  }

  /**
   * Update Space key pressed state for pan mode.
   */
  fun setSpacePressed(pressed: Boolean) {
    _isSpacePressed.value = pressed
  }

  /**
   * Apply zoom while keeping the focal point stationary.
   * The image pixel under the focal point stays at the same canvas position.
   */
  private fun applyZoom(newScale: Float, focalPoint: Offset) {
    if (_zoomScale.value == 0f || _imageToCanvasScale.value == 0f) return

    val canvasCenter = getCanvasCenter()
    val oldZoom = _zoomScale.value
    val baseScale = _imageToCanvasScale.value

    val oldTotalScale = baseScale * oldZoom
    val imageX =
      (focalPoint.x - _imageOffsetInCanvas.value.x * oldZoom - canvasCenter.x * (1f - oldZoom) - _panOffset.value.x) / oldTotalScale
    val imageY =
      (focalPoint.y - _imageOffsetInCanvas.value.y * oldZoom - canvasCenter.y * (1f - oldZoom) - _panOffset.value.y) / oldTotalScale

    _zoomScale.value = newScale

    val newTotalScale = baseScale * newScale
    val newPanOffsetX =
      focalPoint.x - imageX * newTotalScale - _imageOffsetInCanvas.value.x * newScale - canvasCenter.x * (1f - newScale)
    val newPanOffsetY =
      focalPoint.y - imageY * newTotalScale - _imageOffsetInCanvas.value.y * newScale - canvasCenter.y * (1f - newScale)

    _panOffset.value = Offset(newPanOffsetX, newPanOffsetY)

    clampPanToVisibilityBounds()
  }

  /**
   * Clamp pan offset to ensure at least 20% of the image remains visible.
   */
  private fun clampPanToVisibilityBounds() {
    if (!hasImage || _canvasSize.value.width <= 0 || _canvasSize.value.height <= 0) return

    val totalScale = _imageToCanvasScale.value * _zoomScale.value
    val displayWidth = imageSize.width * totalScale
    val displayHeight = imageSize.height * totalScale
    val canvasCenter = getCanvasCenter()

    val minVisibleFraction = 0.2f
    val maxOffsetX = _canvasSize.value.width - (displayWidth * minVisibleFraction)
    val minOffsetX = -(displayWidth * (1f - minVisibleFraction))
    val maxOffsetY = _canvasSize.value.height - (displayHeight * minVisibleFraction)
    val minOffsetY = -(displayHeight * (1f - minVisibleFraction))

    val totalOffsetX = _imageOffsetInCanvas.value.x * _zoomScale.value +
        canvasCenter.x * (1f - _zoomScale.value) +
        _panOffset.value.x
    val totalOffsetY = _imageOffsetInCanvas.value.y * _zoomScale.value +
        canvasCenter.y * (1f - _zoomScale.value) +
        _panOffset.value.y

    val clampedTotalX = totalOffsetX.coerceIn(minOffsetX, maxOffsetX)
    val clampedTotalY = totalOffsetY.coerceIn(minOffsetY, maxOffsetY)

    _panOffset.value = Offset(
      clampedTotalX - _imageOffsetInCanvas.value.x * _zoomScale.value - canvasCenter.x * (1f - _zoomScale.value),
      clampedTotalY - _imageOffsetInCanvas.value.y * _zoomScale.value - canvasCenter.y * (1f - _zoomScale.value)
    )
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

    val floatDisplayWidth = currentImageSize.width * scale
    val floatDisplayHeight = currentImageSize.height * scale

    val offsetX = (_canvasSize.value.width - floatDisplayWidth) / 2f
    val offsetY = (_canvasSize.value.height - floatDisplayHeight) / 2f

    _imageToCanvasScale.value = scale
    _imageDisplaySize.value = IntSize(displayWidth, displayHeight)
    _imageOffsetInCanvas.value = Offset(offsetX, offsetY)
  }

  /**
   * Gets image boundaries within canvas coordinates.
   */
  fun getImageBoundsInCanvas(): Rect {
    val totalScale = _imageToCanvasScale.value * _zoomScale.value
    val displayWidth = imageSize.width * totalScale
    val displayHeight = imageSize.height * totalScale
    val canvasCenter = getCanvasCenter()
    val offsetX = _imageOffsetInCanvas.value.x * _zoomScale.value +
        canvasCenter.x * (1f - _zoomScale.value) +
        _panOffset.value.x
    val offsetY = _imageOffsetInCanvas.value.y * _zoomScale.value +
        canvasCenter.y * (1f - _zoomScale.value) +
        _panOffset.value.y

    return Rect(
      offset = Offset(offsetX, offsetY),
      size = Size(displayWidth, displayHeight)
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
      return false
    }

    return true
  }

  private enum class ImageSourceType {
    NONE, BUFFERED_IMAGE, IMAGE_BITMAP
  }
}
