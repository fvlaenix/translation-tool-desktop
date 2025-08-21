package app.advanced

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import core.utils.FollowableMutableState
import org.koin.compose.koinInject
import translation.data.BlockPosition

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
        if (keyEvent.type.toString() != "KeyUp") return@onKeyEvent true
        if (keyEvent.isCtrlPressed && keyEvent.key == Key.V) {
          viewModel.loadImageFromClipboard()
        }
        if (keyEvent.isCtrlPressed && keyEvent.key == Key.N) {
          viewModel.addNewBox()
        }
        if (keyEvent.key == Key.Delete) {
          viewModel.deleteLastBox()
        }
        false
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

  Box(modifier = Modifier.fillMaxSize()) {
    Image(
      bitmap = image,
      contentDescription = null,
      modifier = Modifier.fillMaxSize()
        .onSizeChanged { size -> onSizeChanged(size) },
      alignment = Alignment.TopStart
    )

    BoxOverlayContainer(
      boxes = boxes,
      imageOriginalSize = imageOriginalSize,
      displayImageSize = currentSize,
      selectedBoxIndex = selectedBoxIndex,
      onBoxSelect = onBoxSelect,
      onBoxUpdate = onBoxUpdate
    )
  }
}

@Composable
private fun BoxOverlayContainer(
  boxes: List<BlockPosition>,
  imageOriginalSize: IntSize,
  displayImageSize: IntSize,
  selectedBoxIndex: Int?,
  onBoxSelect: (Int?) -> Unit,
  onBoxUpdate: (Int, BlockPosition) -> Unit
) {
  boxes.forEachIndexed { index, box ->
    val boxFollowable = FollowableMutableState(mutableStateOf(box))
    val selectedBoxIndexState = remember { mutableStateOf(selectedBoxIndex) }

    LaunchedEffect(selectedBoxIndex) {
      selectedBoxIndexState.value = selectedBoxIndex
    }

    boxFollowable.follow { _, after ->
      onBoxUpdate(index, after)
    }

    Box(
      modifier = Modifier.clickable { onBoxSelect(index) }
    ) {
      BoxOnImage(
        index = index,
        imageSize = imageOriginalSize,
        displayImageSize = displayImageSize,
        blockData = boxFollowable,
        selectedBoxIndex = selectedBoxIndexState
      )
    }
  }
}

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