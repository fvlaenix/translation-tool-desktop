package core.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import core.image.overlays.ImageOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage

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
  overlays: List<ImageOverlay> = emptyList()
) {
  val state = remember { ImageCanvasState() }
  val transformer = remember(state) { CoordinateTransformer(state) }

  LaunchedEffect(image) {
    state.setImage(image)
    image?.let { onImageLoad(it) }
  }

  ImageCanvasContent(
    state = state,
    transformer = transformer,
    overlays = overlays,
    modifier = modifier
  )
}

@Composable
fun ImageCanvas(
  image: ImageBitmap?,
  modifier: Modifier = Modifier,
  onImageLoad: (ImageBitmap) -> Unit = {},
  overlays: List<ImageOverlay> = emptyList()
) {
  val state = remember { ImageCanvasState() }
  val transformer = remember(state) { CoordinateTransformer(state) }

  LaunchedEffect(image) {
    state.setImage(image)
    image?.let { onImageLoad(it) }
  }

  ImageCanvasContent(
    state = state,
    transformer = transformer,
    overlays = overlays,
    modifier = modifier
  )
}

@Composable
private fun ImageCanvasContent(
  state: ImageCanvasState,
  transformer: CoordinateTransformer,
  overlays: List<ImageOverlay>,
  modifier: Modifier
) {
  Box(
    modifier = modifier
      .onSizeChanged { newSize ->
        state.updateCanvasSize(newSize)
      }
  ) {
    if (state.hasImage) {
      ImageRenderer(state = state, transformer = transformer)
    }

    val sortedOverlays = remember(overlays) {
      overlays.filter { it.isVisible }.sortedBy { it.renderOrder }
    }

    sortedOverlays.forEach { overlay ->
      key(overlay.id) {
        overlay.Render(state = state, transformer = transformer)
      }
    }

    if (state.isLoading) {
      CircularProgressIndicator(
        modifier = Modifier.align(Alignment.Center)
      )
    }
  }
}

/**
 * Internal component responsible for rendering the actual image
 */
@Composable
private fun ImageRenderer(state: ImageCanvasState, transformer: CoordinateTransformer) {
  val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }

  LaunchedEffect(state.bufferedImage, state.imageBitmap) {
    imageBitmap.value = when {
      state.imageBitmap != null -> state.imageBitmap
      state.bufferedImage != null -> withContext(Dispatchers.IO) {
        state.bufferedImage?.toComposeBitmap()
      }

      else -> null
    }
  }

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

@Composable
fun SimpleImageCanvas(
  image: ImageBitmap?,
  modifier: Modifier = Modifier,
  onImageLoad: (ImageBitmap) -> Unit = {}
) {
  ImageCanvas(
    image = image,
    modifier = modifier,
    onImageLoad = onImageLoad,
    overlays = emptyList()
  )
}

@Composable
private fun Float.toDp(): Dp {
  return Dp(this / LocalDensity.current.density)
}

/**
 * Converts BufferedImage to Compose ImageBitmap.
 * This operation is expensive and should be called off the main thread.
 */
private fun BufferedImage.toComposeBitmap(): ImageBitmap? {
  return try {
    val outputStream = java.io.ByteArrayOutputStream()
    javax.imageio.ImageIO.write(this, "png", outputStream)
    val byteArray = outputStream.toByteArray()
    androidx.compose.ui.res.loadImageBitmap(java.io.ByteArrayInputStream(byteArray))
  } catch (e: Exception) {
    println("Error converting BufferedImage to ImageBitmap: ${e.message}")
    e.printStackTrace()
    null
  }
}