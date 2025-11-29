package app.simple

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.TopBar
import core.image.ImageCanvas
import core.navigation.NavigationController
import org.koin.compose.koinInject
import translation.domain.SimpleTranslatorViewModel

/**
 * Simple translation workflow with single image ocr and translation functionality.
 */
@Composable
fun SimpleTranslator(navigationController: NavigationController) {
  val viewModel: SimpleTranslatorViewModel = koinInject()

  val currentImage by viewModel.currentImage
  val ocrText by viewModel.ocrText
  val translationText by viewModel.translationText
  val isProcessingOCR by viewModel.isProcessingOCR
  val isTranslating by viewModel.isTranslating
  val error by viewModel.error
  val statusMessage by viewModel.statusMessage

  val currentSize = remember { mutableStateOf(IntSize.Zero) }

  TopBar(navigationController, "Simple Translator") {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .onSizeChanged { size -> currentSize.value = size }
        .padding(16.dp)
        .onKeyEvent { keyEvent ->
          if (keyEvent.isCtrlPressed && keyEvent.key == Key.V) {
            viewModel.loadImageFromClipboard()
            true
          } else {
            false
          }
        }
    ) {
      Text(statusMessage)
      InsideSimpleTranslator(
        viewModel = viewModel,
        currentImage = currentImage,
        currentSize = currentSize,
        ocrText = ocrText,
        translationText = translationText,
        isProcessingOCR = isProcessingOCR,
        isTranslating = isTranslating
      )
    }
  }
}

/**
 * internal ui components for simple translator including image display, ocr and translation controls.
 */
@Composable
private fun InsideSimpleTranslator(
  viewModel: SimpleTranslatorViewModel,
  currentImage: java.awt.image.BufferedImage?,
  currentSize: MutableState<IntSize>,
  ocrText: String,
  translationText: String,
  isProcessingOCR: Boolean,
  isTranslating: Boolean
) {
  val density = LocalDensity.current
  val widthDp = with(density) { (currentSize.value.width / 3).toDp() }
  val heightDp = with(density) { (currentSize.value.height / 3).toDp() }
  val textHeightDp = with(density) { (currentSize.value.height / 6).toDp() }

  Row(
    modifier = Modifier
      .size(width = widthDp, height = heightDp)
      .focusable()
  ) {
    ImageCanvas(
      image = currentImage,
      modifier = Modifier.fillMaxSize()
    )
  }

  Row {
    Button(
      onClick = { viewModel.processOCR() },
      enabled = !isProcessingOCR && !isTranslating
    ) {
      if (isProcessingOCR) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp))
      } else {
        Text("Try OCR")
      }
    }
  }

  TextField(
    value = ocrText,
    onValueChange = { viewModel.updateOcrText(it) },
    modifier = Modifier
      .height(textHeightDp)
      .fillMaxWidth(),
    enabled = !isProcessingOCR && !isTranslating
  )

  Row {
    Button(
      onClick = { viewModel.translate() },
      enabled = !isProcessingOCR && !isTranslating && ocrText.isNotBlank()
    ) {
      if (isTranslating) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp))
      } else {
        Text("Try Translate")
      }
    }
  }

  TextField(
    value = translationText,
    onValueChange = { viewModel.updateTranslationText(it) },
    modifier = Modifier
      .height(textHeightDp)
      .fillMaxWidth(),
    enabled = !isProcessingOCR && !isTranslating
  )

  Row {
    Button(
      onClick = { viewModel.clear() },
      enabled = !isProcessingOCR && !isTranslating
    ) {
      Text("Clear")
    }
  }
}