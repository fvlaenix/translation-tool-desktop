package app.simple

import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.TopBar
import core.navigation.NavigationController
import org.koin.compose.koinInject
import translation.domain.SimpleTranslatorViewModel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

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

  val imagePaster = remember { mutableStateOf<ImageBitmap?>(null) }
  val currentSize = remember { mutableStateOf(IntSize.Zero) }

  // Convert BufferedImage to ImageBitmap for display
  LaunchedEffect(currentImage) {
    if (currentImage != null) {
      val outputStream = ByteArrayOutputStream()
      ImageIO.write(currentImage, "png", outputStream)
      val byteArray = outputStream.toByteArray()
      imagePaster.value = loadImageBitmap(ByteArrayInputStream(byteArray))
    } else {
      imagePaster.value = null
    }
  }

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
        imagePaster = imagePaster,
        currentSize = currentSize,
        ocrText = ocrText,
        translationText = translationText,
        isProcessingOCR = isProcessingOCR,
        isTranslating = isTranslating
      )
    }
  }
}

@Composable
private fun InsideSimpleTranslator(
  viewModel: SimpleTranslatorViewModel,
  imagePaster: MutableState<ImageBitmap?>,
  currentSize: MutableState<IntSize>,
  ocrText: String,
  translationText: String,
  isProcessingOCR: Boolean,
  isTranslating: Boolean
) {

  Row(
    modifier = Modifier
      .size(width = currentSize.value.width.dp / 3, height = currentSize.value.height.dp / 3)
      .focusable()
  ) {
    if (imagePaster.value != null) {
      Image(
        bitmap = imagePaster.value!!,
        contentDescription = null,
        modifier = Modifier.fillMaxSize()
      )
    }
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
      .height(currentSize.value.height.dp / 6)
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
      .height(currentSize.value.height.dp / 6)
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