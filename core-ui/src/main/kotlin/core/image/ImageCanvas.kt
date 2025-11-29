package core.image

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.image.overlays.ImageOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import kotlin.math.roundToInt

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
private fun ZoomControls(
  zoomPercent: Int,
  onZoomOut: () -> Unit,
  onZoomIn: () -> Unit,
  onReset: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
      .padding(4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    IconButton(
      onClick = onZoomOut,
      modifier = Modifier.size(32.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Remove,
        contentDescription = "Zoom out",
        tint = Color.White
      )
    }

    Text(
      text = "$zoomPercent%",
      color = Color.White,
      fontSize = 14.sp,
      modifier = Modifier.widthIn(min = 48.dp).wrapContentWidth()
    )

    IconButton(
      onClick = onZoomIn,
      modifier = Modifier.size(32.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Add,
        contentDescription = "Zoom in",
        tint = Color.White
      )
    }

    Spacer(modifier = Modifier.width(4.dp))

    androidx.compose.material.Button(
      onClick = onReset,
      modifier = Modifier.height(32.dp),
      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    ) {
      Text("Fit", fontSize = 12.sp)
    }
  }
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
      .border(2.dp, Color.DarkGray, RectangleShape)
      .clipToBounds()
      .onSizeChanged { newSize ->
        state.updateCanvasSize(newSize)
      }
      .onPreviewKeyEvent { keyEvent ->
        if (keyEvent.type != KeyEventType.KeyDown) {
          if (keyEvent.key == Key.Spacebar && keyEvent.type == KeyEventType.KeyUp) {
            state.setSpacePressed(false)
            return@onPreviewKeyEvent true
          }
          return@onPreviewKeyEvent false
        }

        when {
          keyEvent.key == Key.Spacebar -> {
            state.setSpacePressed(true)
            true
          }

          keyEvent.isCtrlPressed && keyEvent.key == Key.Equals -> {
            state.zoomBy(1.125f, state.getCanvasCenter())
            true
          }

          keyEvent.isCtrlPressed && keyEvent.key == Key.Minus -> {
            state.zoomBy(0.875f, state.getCanvasCenter())
            true
          }

          keyEvent.isCtrlPressed && keyEvent.key == Key.Zero -> {
            state.resetView()
            true
          }

          else -> false
        }
      }
      .pointerInput(state) {
        awaitPointerEventScope {
          while (true) {
            val event = awaitPointerEvent()

            event.changes.firstOrNull()?.scrollDelta?.let { scrollDelta ->
              if (scrollDelta.y != 0f) {
                val focalPoint = event.changes.first().position
                
                val zoomFactor = 1f + (-scrollDelta.y * 0.1f)
                state.zoomBy(zoomFactor, focalPoint)
                event.changes.forEach { it.consume() }
              }
            }
          }
        }
      }
      .pointerInput(state) {
        detectDragGestures { change, dragAmount ->
          if (state.isSpacePressed) {
            state.panBy(dragAmount)
            change.consume()
          } else {
            state.panBy(dragAmount)
          }
        }
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

    if (state.hasImage) {
      val zoomPercent = (state.zoomScale * 100).toInt()
      ZoomControls(
        zoomPercent = zoomPercent,
        onZoomOut = { state.zoomBy(0.875f, state.getCanvasCenter()) },
        onZoomIn = { state.zoomBy(1.125f, state.getCanvasCenter()) },
        onReset = { state.resetView() },
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(16.dp)
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
    val imageBounds by remember {
      derivedStateOf { transformer.getImageBoundsInCanvas() }
    }

    val translateX = imageBounds.left
    val translateY = imageBounds.top

    Canvas(
      modifier = Modifier.fillMaxSize()
    ) {
      translate(left = translateX, top = translateY) {
        drawImage(
          image = bitmap,
          dstSize = IntSize(
            imageBounds.width.roundToInt(),
            imageBounds.height.roundToInt()
          )
        )
      }
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
