package app.simple

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
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
import kotlinx.coroutines.launch
import utils.ClipboardUtils.getClipboardImage
import utils.ProtobufUtils
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Composable
fun SimpleTranslator() {
  val commentText by remember { mutableStateOf("Press CTRL+V for insert picture") }

  var imageBuffered by remember { mutableStateOf<BufferedImage?>(null) }
  var imagePaster by remember { mutableStateOf<ImageBitmap?>(null) }

  var ocrText by remember { mutableStateOf("") }
  var translationText by remember { mutableStateOf("") }

  var currentSize by remember { mutableStateOf(IntSize.Zero) }

  val scope = rememberCoroutineScope()
  Column(
    modifier = Modifier
      .fillMaxSize().onSizeChanged { size -> currentSize = size }
      .onKeyEvent { keyEvent ->
        if (keyEvent.isCtrlPressed && keyEvent.key == Key.V) {
          val image = getClipboardImage()
          if (image == null) {
            println("Failed to get image")
          } else {
            val outputStream = ByteArrayOutputStream()
            ImageIO.write(image, "png", outputStream)
            val byteArray = outputStream.toByteArray()
            imageBuffered = image
            imagePaster = loadImageBitmap(ByteArrayInputStream(byteArray))
          }
        }
        false
      }
  ) {
    Text(commentText)
    Row(modifier = Modifier
      .size(width = currentSize.width.dp / 3, height = currentSize.height.dp / 3)
    ) {
      if (imagePaster != null) {
        Image(
          bitmap = imagePaster!!,
          contentDescription = null,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
    Button(onClick = {
      scope.launch {
        val localBufferedImage = imageBuffered
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
        .height(currentSize.height.dp / 6)
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
        .height(currentSize.height.dp / 6)
        .fillMaxWidth()
    )
    Button(onClick = {
      imageBuffered = null
      imagePaster = null
      ocrText = ""
      translationText = ""
    }) {
      Text("Clear")
    }
  }
}