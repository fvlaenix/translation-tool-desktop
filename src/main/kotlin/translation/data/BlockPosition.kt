package translation.data

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.IntSize
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Represents the position and dimensions of a text block on an image.
 */
@Serializable
@Immutable
data class BlockPosition(
  val x: Double,
  val y: Double,
  val width: Double,
  val height: Double,
  val shape: Shape
) {
  /**
   * Defines the shape of the text block.
   */
  @Serializable
  sealed interface Shape {
    @Serializable
    data object Rectangle : Shape

    @Serializable
    data object Oval : Shape
  }

  @Transient
  var heavyChangeListener: HeavyChangeListener? = null

  interface HeavyChangeListener {
    fun onChange()
  }
}

/**
 * Validates and clamps this BlockPosition to be within image boundaries.
 * All coordinates are stored in Image Coordinate system.
 */
fun BlockPosition.clampToImageBounds(imageSize: IntSize): BlockPosition {
  val originalPosition = this

  val clampedX = x.coerceIn(0.0, imageSize.width.toDouble())
  val clampedY = y.coerceIn(0.0, imageSize.height.toDouble())

  val maxWidth = imageSize.width.toDouble() - clampedX
  val maxHeight = imageSize.height.toDouble() - clampedY

  val clampedWidth = width.coerceIn(1.0, maxWidth)
  val clampedHeight = height.coerceIn(1.0, maxHeight)

  val wasChanged = clampedX != x || clampedY != y || clampedWidth != width || clampedHeight != height

  if (wasChanged) {
    println("WARNING: BlockPosition clamped to image bounds (${imageSize.width}x${imageSize.height})")
    println("  Original: x=$x, y=$y, width=$width, height=$height")
    println("  Clamped:  x=$clampedX, y=$clampedY, width=$clampedWidth, height=$clampedHeight")
  }

  return copy(
    x = clampedX,
    y = clampedY,
    width = clampedWidth,
    height = clampedHeight
  )
}

/**
 * Validates that this BlockPosition is within image boundaries.
 */
fun BlockPosition.isValidForImage(imageSize: IntSize): Boolean {
  return x >= 0 && y >= 0 &&
      x + width <= imageSize.width &&
      y + height <= imageSize.height
}