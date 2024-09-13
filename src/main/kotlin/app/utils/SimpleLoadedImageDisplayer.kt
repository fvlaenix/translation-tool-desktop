package app.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.IntSize
import app.advanced.BoxOnImage
import app.ocr.OCRBoxData
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Composable
fun SimpleLoadedImageDisplayer(image: MutableState<BufferedImage?>, boxes: SnapshotStateList<OCRBoxData>) {
  val imageSize = remember { mutableStateOf(IntSize.Zero) }
  val imagePaster = remember { mutableStateOf<ImageBitmap?>(null) }

  LaunchedEffect(image.value) {
    val image = image.value
    val outputStream = ByteArrayOutputStream()
    ImageIO.write(image, "png", outputStream)
    val byteArray = outputStream.toByteArray()
    imagePaster.value = loadImageBitmap(ByteArrayInputStream(byteArray))
  }

  if (imagePaster.value != null) {
    Box(modifier = Modifier.fillMaxSize()) {
      Image(
        bitmap = imagePaster.value!!,
        contentDescription = null,
        modifier = Modifier.fillMaxSize()
          .onSizeChanged { imageSize.value = it },
        alignment = Alignment.TopStart
      )
      boxes.forEach { box -> BoxOnImage(box.box, imageSize); println(box.box) }
    }
  } else {
    CircularProgressIndicator(
      modifier = Modifier.fillMaxSize()
    )
  }
}

@Composable
fun SimpleLoadedImageDisplayer(image: MutableState<BufferedImage?>) {
  val imageSize = remember { mutableStateOf(IntSize.Zero) }

  val image = image.value
  if (image != null) {
    val imagePaster = remember { mutableStateOf<ImageBitmap?>(null) }
    val outputStream = ByteArrayOutputStream()
    ImageIO.write(image, "png", outputStream)
    val byteArray = outputStream.toByteArray()
    imagePaster.value = loadImageBitmap(ByteArrayInputStream(byteArray))
    Image(
      bitmap = imagePaster.value!!,
      contentDescription = null,
      modifier = Modifier.fillMaxSize().onSizeChanged { imageSize.value = it }
    )
  } else {
    CircularProgressIndicator(
      modifier = Modifier.fillMaxSize()
    )
  }
}