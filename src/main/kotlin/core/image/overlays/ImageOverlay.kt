package core.image.overlays

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import core.image.CoordinateTransformer
import core.image.ImageCanvasState

/**
 * Base interface for all image overlays.
 *
 * Replaces the current scattered overlay system with a unified approach.
 * Each overlay is responsible for rendering itself using the provided transformer.
 */
interface ImageOverlay {
  /**
   * Unique identifier for this overlay
   */
  val id: String

  /**
   * Whether this overlay should be rendered
   */
  val isVisible: Boolean

  /**
   * Rendering order - higher values render on top
   * Suggested values:
   * - Background overlays: 0-9
   * - Box/shape overlays: 10-19
   * - Text overlays: 20-29
   * - Selection/UI overlays: 30-39
   * - Debug overlays: 40+
   */
  val renderOrder: Int

  /**
   * Render this overlay using Compose
   */
  @Composable
  fun Render(state: ImageCanvasState, transformer: CoordinateTransformer)

  // TODO: Future methods for Phase 2+
  // fun handleInput(event: PointerEvent, transformer: CoordinateTransformer): Boolean
  // fun getBounds(): Rect
  // fun getInteractionRegion(): Rect?
  // fun onSelected()
  // fun onDeselected()
}

/**
 * Base implementation providing common overlay functionality.
 *
 * Most overlays should extend this instead of implementing ImageOverlay directly.
 */
abstract class BaseImageOverlay : ImageOverlay {

  override val isVisible: Boolean = true
  override val renderOrder: Int = 10

  /**
   * Render this overlay using Canvas with automatic transformation handling
   */
  @Composable
  override fun Render(state: ImageCanvasState, transformer: CoordinateTransformer) {
    if (!isVisible || !state.hasImage) return

    Canvas(modifier = Modifier.fillMaxSize()) {
      drawWithTransformation(this, transformer)
    }
  }

  /**
   * Override this method to implement the actual drawing logic.
   * The DrawScope is already set up with proper transformations.
   */
  protected abstract fun drawWithTransformation(drawScope: DrawScope, transformer: CoordinateTransformer)

  /**
   * Helper method for common drawing operations with transformation
   */
  protected fun drawTransformed(
    drawScope: DrawScope,
    transformer: CoordinateTransformer,
    block: DrawScope.() -> Unit
  ) {
    with(drawScope) {
      // Apply any common transformations here
      // For now, just execute the block directly
      // TODO: In Phase 2+, this could apply zoom/pan transformations
      block()
    }
  }
}

/**
 * Simple overlay implementation for custom drawing without extending BaseImageOverlay
 */
class CustomDrawOverlay(
  override val id: String,
  override val isVisible: Boolean = true,
  override val renderOrder: Int = 10,
  private val drawFunction: DrawScope.(CoordinateTransformer) -> Unit
) : ImageOverlay {

  @Composable
  override fun Render(state: ImageCanvasState, transformer: CoordinateTransformer) {
    if (!isVisible || !state.hasImage) return

    Canvas(modifier = Modifier.fillMaxSize()) {
      drawFunction(transformer)
    }
  }
}

/**
 * Empty overlay for testing or placeholder purposes
 */
class EmptyOverlay(
  override val id: String,
  override val isVisible: Boolean = false,
  override val renderOrder: Int = 0
) : ImageOverlay {

  @Composable
  override fun Render(state: ImageCanvasState, transformer: CoordinateTransformer) {
    // Intentionally empty
  }
}