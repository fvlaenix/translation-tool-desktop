package app.advanced

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color.Companion.Cyan
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.Magenta
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import app.advanced.domain.ImageWithBoxesViewModel
import core.image.ImageCanvas
import core.image.overlays.BoxOverlay
import org.koin.compose.koinInject
import translation.data.BlockPosition

/**
 * Composable for drawing/editing text selection boxes on images. supports box creation, selection, and manipulation.
 */
@Composable
fun ImageWithBoxes(
  viewModel: ImageWithBoxesViewModel = koinInject(),
  onStateChange: (List<BlockPosition>) -> Unit = {}
) {
  val uiState by viewModel.uiState
  val requester = remember { FocusRequester() }

  LaunchedEffect(uiState.boxes) {
    onStateChange(uiState.boxes)
  }

  Row(
    modifier = Modifier
      .fillMaxSize()
      .onKeyEvent { keyEvent ->
        if (keyEvent.type != KeyEventType.KeyUp) return@onKeyEvent false
        when {
          keyEvent.isCtrlPressed && keyEvent.key == Key.V -> {
            viewModel.loadImageFromClipboard()
            true
          }

          keyEvent.isCtrlPressed && keyEvent.key == Key.N -> {
            viewModel.addNewBox()
            true
          }

          keyEvent.key == Key.Delete -> {
            viewModel.deleteLastBox()
            true
          }

          else -> false
        }
      }
      .focusRequester(requester)
      .focusable()
  ) {
    if (uiState.image != null) {
      ImageDisplayArea(
        image = uiState.image!!,
        boxes = uiState.boxes,
        selectedBoxIndex = uiState.selectedBoxIndex,
        currentSize = uiState.currentSize,
        onSizeChanged = { viewModel.updateCurrentSize(it) },
        onBoxSelect = { viewModel.selectBox(it) },
        onBoxUpdate = { index, box -> viewModel.updateBox(index, box) }
      )
    } else {
      EmptyImageState(uiState.emptyText)
    }
  }

  LaunchedEffect(Unit) {
    requester.requestFocus()
  }
}

/**
 * Renders image with draggable/resizable overlay boxes for text region selection.
 */
@Composable
private fun ImageDisplayArea(
  image: ImageBitmap,
  boxes: List<BlockPosition>,
  selectedBoxIndex: Int?,
  currentSize: IntSize,
  onSizeChanged: (IntSize) -> Unit,
  onBoxSelect: (Int?) -> Unit,
  onBoxUpdate: (Int, BlockPosition) -> Unit
) {
  val imageOriginalSize = IntSize(image.width, image.height)

  val overlays = remember(boxes, selectedBoxIndex) {
    boxes.mapIndexed { index, position ->
      BoxOverlay.fromBoxOnImage(
        index = index,
        blockPosition = position,
        imageSize = imageOriginalSize,
        isSelected = selectedBoxIndex == index,
        onPositionUpdate = { newPosition -> onBoxUpdate(index, newPosition) },
        onBoxSelect = { onBoxSelect(index) }
      )
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    ImageCanvas(
      image = image,
      overlays = overlays,
      modifier = Modifier.fillMaxSize()
        .onSizeChanged { size -> onSizeChanged(size) }
    )
  }
}

/**
 * Displays placeholder text with gradient styling when no image is loaded.
 */
@Composable
private fun EmptyImageState(emptyText: String) {
  val gradientColors = listOf(Cyan, Gray, Magenta)
  Text(
    text = emptyText,
    modifier = Modifier.fillMaxSize(),
    textAlign = TextAlign.Center,
    fontSize = 20.sp,
    style = TextStyle(
      brush = Brush.linearGradient(colors = gradientColors)
    )
  )
}