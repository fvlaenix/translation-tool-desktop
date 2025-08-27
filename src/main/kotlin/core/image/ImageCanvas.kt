package core.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.loadImageBitmap
import core.image.overlays.ImageOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Main image display component that replaces SimpleLoadedImageDisplayer.
 *
 * Features:
 * - Centralized state management via ImageCanvasState
 * - Unified coordinate transformation system
 * - Extensible overlay system
 * - Proper loading state handling
 * - Future-ready for zoom/pan/tools
 */
@Composable
fun ImageCanvas(
  image: BufferedImage?,
  modifier: Modifier = Modifier,
  onImageLoad: (BufferedImage) -> Unit = {},
  overlays: List<ImageOverlay> = emptyList(),
  // TODO: Future parameters for Phase 2+
  // onToolChange: (ImageTool?) -> Unit = {},
  // interactionEnabled: Boolean = true,
  // zoomEnabled: Boolean = false,
  // panEnabled: Boolean = false
) {
  // Create state and transformer - remember them so they persist across recompositions
  val state = remember { ImageCanvasState() }
  val transformer = remember(state) { CoordinateTransformer(state) }

  // Handle image changes
  LaunchedEffect(image) {
    if (image != null) {
      state.setImage(image)
      onImageLoad(image)
    } else {
      state.setImage(null)
    }
  }

  Box(
    modifier = modifier
      .onSizeChanged { newSize ->
        state.updateCanvasSize(newSize)
      }
    // TODO: Future input handling for Phase 2+
    // .pointerInput(state) {
    //     // Handle zoom, pan, tool interactions
    // }
  ) {
    // Image rendering
    if (state.hasImage) {
      ImageRenderer(state = state, transformer = transformer)
    }

    // Overlay rendering - sort by render order and render visible overlays
    val sortedOverlays = remember(overlays) {
      overlays.filter { it.isVisible }.sortedBy { it.renderOrder }
    }

    sortedOverlays.forEach { overlay ->
      key(overlay.id) {
        overlay.Render(state = state, transformer = transformer)
      }
    }

    // Loading indicator
    if (state.isLoading) {
      CircularProgressIndicator(
        modifier = Modifier.align(Alignment.Center)
      )
    }

    // TODO: Future UI elements for Phase 2+
    // if (showTools) { ToolPalette(...) }
    // if (showZoomControls) { ZoomControls(...) }
  }
}

/**
 * Internal component responsible for rendering the actual image
 */
@Composable
private fun ImageRenderer(state: ImageCanvasState, transformer: CoordinateTransformer) {
  val imageBitmap = remember(state.image) {
    mutableStateOf<ImageBitmap?>(null)
  }

  // Convert BufferedImage to ImageBitmap
  LaunchedEffect(state.image) {
    val bufferedImage = state.image
    if (bufferedImage != null) {
      try {
        withContext(Dispatchers.IO) {
          val outputStream = ByteArrayOutputStream()
          ImageIO.write(bufferedImage, "png", outputStream)
          val byteArray = outputStream.toByteArray()
          val bitmap = loadImageBitmap(ByteArrayInputStream(byteArray))
          imageBitmap.value = bitmap
        }
      } catch (e: Exception) {
        println("Error converting BufferedImage to ImageBitmap: ${e.message}")
        imageBitmap.value = null
      }
    } else {
      imageBitmap.value = null
    }
  }

  // Render the image if available
  imageBitmap.value?.let { bitmap ->
    val imageBounds = transformer.getImageBoundsInCanvas()

    Box(
      modifier = Modifier
        .fillMaxSize()
        .wrapContentSize(align = Alignment.TopStart)
    ) {
      Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = Modifier
          .size(
            width = imageBounds.width.toDp(),
            height = imageBounds.height.toDp()
          )
          .offset(
            x = imageBounds.left.toDp(),
            y = imageBounds.top.toDp()
          )
      )
    }
  }
}

/**
 * Convenience composable for simple image display without overlays
 */
@Composable
fun SimpleImageCanvas(
  image: BufferedImage?,
  modifier: Modifier = Modifier,
  onImageLoad: (BufferedImage) -> Unit = {}
) {
  ImageCanvas(
    image = image,
    modifier = modifier,
    onImageLoad = onImageLoad,
    overlays = emptyList()
  )
}

/**
 * Extension function to convert Float pixels to Dp for Compose layout
 */
@Composable
private fun Float.toDp(): androidx.compose.ui.unit.Dp {
  return androidx.compose.ui.unit.Dp(this / androidx.compose.ui.platform.LocalDensity.current.density)
}

// TODO: Future composables for Phase 2+
// @Composable
// fun InteractiveImageCanvas(...)
// 
// @Composable  
// fun ZoomableImageCanvas(...)
//
// @Composable
// fun ImageCanvasWithTools(...)