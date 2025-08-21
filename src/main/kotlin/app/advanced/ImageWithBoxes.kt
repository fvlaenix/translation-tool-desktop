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

  // Notify parent of state changes
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
    val image = uiState.image
    if (image != null) {
      val imageOriginalSize = IntSize(image.width, image.height)

      Box(modifier = Modifier.fillMaxSize()) {
        Image(
          bitmap = image,
          contentDescription = null,
          modifier = Modifier.fillMaxSize()
            .onSizeChanged { size -> viewModel.updateCurrentSize(size) },
          alignment = Alignment.TopStart
        )
        uiState.boxes.forEachIndexed { index, box ->
          val boxFollowable = FollowableMutableState(mutableStateOf(box))
          val selectedBoxIndexState = remember { mutableStateOf(uiState.selectedBoxIndex) }

          // Update selection state when UI state changes
          LaunchedEffect(uiState.selectedBoxIndex) {
            selectedBoxIndexState.value = uiState.selectedBoxIndex
          }

          boxFollowable.follow { _, after ->
            viewModel.updateBox(index, after)
          }

          // Handle box selection by wrapping BoxOnImage with click detection
          Box(
            modifier = Modifier.clickable { viewModel.selectBox(index) }
          ) {
            BoxOnImage(
              index = index,
              imageSize = imageOriginalSize,
              displayImageSize = uiState.currentSize,
              blockData = boxFollowable,
              selectedBoxIndex = selectedBoxIndexState
            )
          }
        }
      }
    } else {
      val gradientColors = listOf(Cyan, Gray, Magenta)
      Text(
        text = uiState.emptyText,
        modifier = Modifier.fillMaxSize(),
        textAlign = TextAlign.Center,
        fontSize = 20.sp,
        style = TextStyle(
          brush = Brush.linearGradient(
            colors = gradientColors
          )
        )
      )
    }
  }

  LaunchedEffect(Unit) {
    requester.requestFocus()
  }
}