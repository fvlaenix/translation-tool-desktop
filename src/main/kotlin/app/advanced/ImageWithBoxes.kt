package app.advanced

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.onClick
import androidx.compose.foundation.text.isTypedEvent
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.IntSize
import utils.ClipboardUtils.getClipboardImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Composable
fun ImageWithBoxes(
  image: MutableState<ImageBitmap?>,
  boxes: MutableState<List<BoxOnImageData>>,
  isEnabled: MutableState<Boolean>,
  currentSize: MutableState<IntSize>
) {
  val requester = remember { FocusRequester() }

  isEnabled.value = image.value != null

  Row(
    modifier = Modifier
      .fillMaxSize()
      .onKeyEvent { keyEvent ->
        if (keyEvent.type.toString() != "KeyUp") return@onKeyEvent true
        if (keyEvent.isCtrlPressed && keyEvent.key == Key.V) {
          val clipboardImage = getClipboardImage()
          if (clipboardImage == null) {
            println("Failed to get image")
          } else {
            val outputStream = ByteArrayOutputStream()
            ImageIO.write(clipboardImage, "png", outputStream)
            val byteArray = outputStream.toByteArray()
            image.value = loadImageBitmap(ByteArrayInputStream(byteArray))
          }
        }
        if (keyEvent.isCtrlPressed && keyEvent.key == Key.N) {
          if (image.value != null) {
            boxes.value += BoxOnImageData(0.0f, 0.0f, currentSize.value.width / 10, currentSize.value.height / 10)
          }
        }
        if (keyEvent.key == Key.Delete) {
          if (boxes.value.isNotEmpty()) boxes.value = boxes.value.dropLast(1)
        }
        false
      }
      .focusRequester(requester)
      .focusable()
  ) {
    val imageNotNull = image.value
    if (imageNotNull!= null) {
      Image(
        bitmap = imageNotNull,
        contentDescription = null,
        modifier = Modifier.fillMaxSize()
          .onSizeChanged { size -> currentSize.value = size },
        alignment = Alignment.TopStart
      )
    }
  }
  LaunchedEffect(Unit) {
    requester.requestFocus()
  }
  boxes.value.forEach { box ->
    BoxOnImage(box, currentSize)
  }
}