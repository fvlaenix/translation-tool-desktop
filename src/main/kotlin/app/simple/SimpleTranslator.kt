package app.simple

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import app.AppStateEnum
import kotlinx.coroutines.launch
import utils.ClipboardUtils.getClipboardImage
import utils.ProtobufUtils
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Composable
fun SimpleTranslator(mutableState: MutableState<AppStateEnum>) {
  val imageBuffered = remember { mutableStateOf<BufferedImage?>(null) }
  val imagePaster = remember { mutableStateOf<ImageBitmap?>(null) }

  val currentSize = remember { mutableStateOf(IntSize.Zero) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Simple Translator") },
        navigationIcon = {
          IconButton(onClick = { mutableState.value = AppStateEnum.MAIN_MENU }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Menu")
          }
        }
      )
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize().onSizeChanged { size -> currentSize.value = size }
        .padding(16.dp)
        .onKeyEvent { keyEvent ->
          if (keyEvent.isCtrlPressed && keyEvent.key == Key.V) {
            val image = getClipboardImage()
            if (image == null) {
              println("Failed to get image")
            } else {
              val outputStream = ByteArrayOutputStream()
              ImageIO.write(image, "png", outputStream)
              val byteArray = outputStream.toByteArray()
              imageBuffered.value = image
              imagePaster.value = loadImageBitmap(ByteArrayInputStream(byteArray))
            }
          }
          false
        }
    ) {
      InsideSimpleTranslator(imageBuffered, imagePaster, currentSize)
    }
  }
}

@Composable
private fun InsideSimpleTranslator(imageBuffered: MutableState<BufferedImage?>, imagePaster: MutableState<ImageBitmap?>, currentSize: MutableState<IntSize>) {
  val commentText by remember { mutableStateOf("Press CTRL+V for insert picture") }

  var ocrText by remember { mutableStateOf("") }
  var translationText by remember { mutableStateOf("") }

  val scope = rememberCoroutineScope()

  Text(commentText)
  Row(modifier = Modifier
    .size(width = currentSize.value.width.dp / 3, height = currentSize.value.height.dp / 3)
  ) {
    if (imagePaster.value != null) {
      Image(
        bitmap = imagePaster.value!!,
        contentDescription = null,
        modifier = Modifier.fillMaxSize()
      )
    }
  }
  Button(onClick = {
    scope.launch {
      val localBufferedImage = imageBuffered.value
      if (localBufferedImage != null) {
        val previous = if (ocrText.isBlank()) "" else ocrText + "\n\n"
        ocrText = previous + ProtobufUtils.getOCR(localBufferedImage)
      }
    }
  }) {
    Text("Try OCR")
  }

  TextField(
    value = ocrText,
    onValueChange = { ocrText = it },
    modifier = Modifier
      .height(currentSize.value.height.dp / 6)
      .fillMaxWidth()
  )
  Button(onClick = {
    scope.launch {
      val previous = if (translationText.isBlank()) "" else translationText + "\n\n"
      translationText = previous + ProtobufUtils.getTranslation(ocrText)
    }
  }) {
    Text("Try Translate")
  }
  TextField(
    value = translationText,
    onValueChange = { translationText = it },
    modifier = Modifier
      .height(currentSize.value.height.dp / 6)
      .fillMaxWidth()
  )
  Button(onClick = {
    imageBuffered.value = null
    imagePaster.value = null
    ocrText = ""
    translationText = ""
  }) {
    Text("Clear")
  }
}