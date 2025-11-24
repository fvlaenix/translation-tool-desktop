package core.image

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

/**
 * Centralized coordinate system transformations for image display.
 *
 * Coordinate Systems:
 * - Image Coordinates: Original image pixels (0,0 to imageWidth, imageHeight)
 * - Canvas Coordinates: Display coordinates within ImageCanvas bounds
 * - Screen Coordinates: Actual screen pixels (handled by Compose, used for input events)
 *
 * This replaces all scattered coordinate transformation logic throughout the app.
 */
class CoordinateTransformer(private val state: ImageCanvasState) {

  /**
   * Convert a point from image coordinates to canvas coordinates
   */
  fun imageToCanvas(imagePoint: Offset): Offset {
    if (!state.hasImage) return Offset.Zero

    val scaledX = imagePoint.x * state.imageToCanvasScale + state.imageOffsetInCanvas.x
    val scaledY = imagePoint.y * state.imageToCanvasScale + state.imageOffsetInCanvas.y

    return Offset(scaledX, scaledY)
  }

  /**
   * Convert a point from canvas coordinates to image coordinates
   */
  fun canvasToImage(canvasPoint: Offset): Offset {
    if (!state.hasImage || state.imageToCanvasScale == 0f) return Offset.Zero

    val imageX = (canvasPoint.x - state.imageOffsetInCanvas.x) / state.imageToCanvasScale
    val imageY = (canvasPoint.y - state.imageOffsetInCanvas.y) / state.imageToCanvasScale

    return Offset(imageX, imageY)
  }

  /**
   * Convert a rectangle from image coordinates to canvas coordinates
   */
  fun imageRectToCanvas(imageRect: Rect): Rect {
    if (!state.hasImage) return Rect.Zero

    val topLeft = imageToCanvas(imageRect.topLeft)
    val bottomRight = imageToCanvas(imageRect.bottomRight)

    return Rect(
      offset = topLeft,
      size = Size(
        width = bottomRight.x - topLeft.x,
        height = bottomRight.y - topLeft.y
      )
    )
  }

  /**
   * Convert a rectangle from canvas coordinates to image coordinates
   */
  fun canvasRectToImage(canvasRect: Rect): Rect {
    if (!state.hasImage || state.imageToCanvasScale == 0f) return Rect.Zero

    val topLeft = canvasToImage(canvasRect.topLeft)
    val bottomRight = canvasToImage(canvasRect.bottomRight)

    return Rect(
      offset = topLeft,
      size = Size(
        width = bottomRight.x - topLeft.x,
        height = bottomRight.y - topLeft.y
      )
    )
  }

  /**
   * Scale a dimension from image coordinates to canvas coordinates
   */
  fun imageDistanceToCanvas(imageDistance: Float): Float {
    return imageDistance * state.imageToCanvasScale
  }

  /**
   * Scale a dimension from canvas coordinates to image coordinates
   */
  fun canvasDistanceToImage(canvasDistance: Float): Float {
    if (state.imageToCanvasScale == 0f) return 0f
    return canvasDistance / state.imageToCanvasScale
  }

  /**
   * Check if a point in image coordinates is within the image bounds
   */
  fun isPointInImageBounds(imagePoint: Offset): Boolean {
    if (!state.hasImage) return false

    return imagePoint.x >= 0 && imagePoint.y >= 0 &&
        imagePoint.x <= state.imageSize.width && imagePoint.y <= state.imageSize.height
  }

  /**
   * Check if a rectangle in image coordinates is within the image bounds
   */
  fun isRectInImageBounds(imageRect: Rect): Boolean {
    if (!state.hasImage) return false

    return imageRect.left >= 0 && imageRect.top >= 0 &&
        imageRect.right <= state.imageSize.width && imageRect.bottom <= state.imageSize.height
  }

  /**
   * Clamp a point in image coordinates to image bounds
   */
  fun clampPointToImageBounds(imagePoint: Offset): Offset {
    if (!state.hasImage) return Offset.Zero

    val clampedX = imagePoint.x.coerceIn(0f, state.imageSize.width.toFloat())
    val clampedY = imagePoint.y.coerceIn(0f, state.imageSize.height.toFloat())

    return Offset(clampedX, clampedY)
  }

  /**
   * Clamp a rectangle in image coordinates to image bounds
   */
  fun clampRectToImageBounds(imageRect: Rect): Rect {
    if (!state.hasImage) return Rect.Zero

    val imageWidth = state.imageSize.width.toFloat()
    val imageHeight = state.imageSize.height.toFloat()

    val clampedLeft = imageRect.left.coerceIn(0f, imageWidth)
    val clampedTop = imageRect.top.coerceIn(0f, imageHeight)
    val clampedRight = imageRect.right.coerceIn(clampedLeft, imageWidth)
    val clampedBottom = imageRect.bottom.coerceIn(clampedTop, imageHeight)

    return Rect(
      offset = Offset(clampedLeft, clampedTop),
      size = Size(
        width = clampedRight - clampedLeft,
        height = clampedBottom - clampedTop
      )
    )
  }

  /**
   * Get the bounds of the image within the canvas coordinate system
   */
  fun getImageBoundsInCanvas(): Rect {
    return state.getImageBoundsInCanvas()
  }

  /**
   * Get the visible portion of the image (currently the entire image, but will change with zoom/pan)
   */
  fun getVisibleImageRect(): Rect {
    if (!state.hasImage) return Rect.Zero

    return Rect(
      offset = Offset.Zero,
      size = Size(
        width = state.imageSize.width.toFloat(),
        height = state.imageSize.height.toFloat()
      )
    )
  }

  /**
   * Check if a point in canvas coordinates is within the displayed image area
   */
  fun isCanvasPointInImage(canvasPoint: Offset): Boolean {
    val imageBounds = getImageBoundsInCanvas()
    return imageBounds.contains(canvasPoint)
  }

  // TODO: Future methods for Phase 2+
  // fun screenToCanvas(screenPoint: Offset): Offset
  // fun canvasToScreen(canvasPoint: Offset): Offset
  // fun applyZoomPan(point: Offset): Offset
  // fun getViewportBounds(): Rect
  // fun worldToScreen(worldPoint: Offset): Offset (for complex transformations)

  /**
   * Debug method to log coordinate transformations for troubleshooting
   */
  fun debugLogTransformation(imagePoint: Offset, label: String = "") {
    if (!state.hasImage) {
      println("DEBUG $label: No image loaded")
      return
    }

    val canvasPoint = imageToCanvas(imagePoint)
    val backToImage = canvasToImage(canvasPoint)

    println("DEBUG $label:")
    println("  Image point: $imagePoint")
    println("  Canvas point: $canvasPoint")
    println("  Back to image: $backToImage")
    println("  Scale: ${state.imageToCanvasScale}")
    println("  Offset: ${state.imageOffsetInCanvas}")
    println(
      "  Round-trip error: ${
        Offset(
          kotlin.math.abs(backToImage.x - imagePoint.x),
          kotlin.math.abs(backToImage.y - imagePoint.y)
        )
      }"
    )
  }
}