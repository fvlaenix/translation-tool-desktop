package app.block

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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.IntSize
import app.advanced.BoxOnImage
import app.advanced.BoxOnImageWithSizeData
import app.ocr.OCRBoxData
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.min

@Composable
fun SimpleLoadedImageDisplayer(
  image: MutableState<BufferedImage?>,
  boxes: SnapshotStateList<OCRBoxData>
) {
  SimpleLoadedImageDisplayer(
    image = image,
    displayableOnImage = { imageSize, imageOriginalSize ->
      val imageBoxes = boxes.map {
        BoxOnImageWithSizeData(it.box, LocalDensity.current.density, imageSize, imageOriginalSize)
      }
      imageBoxes.forEach { box -> BoxOnImage(box) }
    }
  )
}

@Composable
fun SimpleLoadedImageDisplayer(
  modifier: Modifier = Modifier.fillMaxSize(0.9f),
  image: MutableState<BufferedImage?>,
  displayableOnImage: @Composable ((MutableState<IntSize>, IntSize) -> Unit)? = null
) {
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
    val imageOriginalSize = IntSize(image.value!!.width, image.value!!.height)

    Box(modifier = modifier) {
      Image(
        bitmap = imagePaster.value!!,
        contentDescription = null,
        modifier = Modifier.fillMaxSize()
          .onSizeChanged { imageBoxSize ->
            val scaleImageToBox = min(
              imageBoxSize.width.toDouble() / imageOriginalSize.width,
              imageBoxSize.height.toDouble() / imageOriginalSize.height
            )

            val newWidth = (imageOriginalSize.width * scaleImageToBox).toInt()
            val newHeight = (imageOriginalSize.height * scaleImageToBox).toInt()

            val newIntSize = IntSize(newWidth, newHeight)
            imageSize.value = newIntSize
          },
        alignment = Alignment.TopStart
      )
      if (displayableOnImage != null) displayableOnImage(imageSize, imageOriginalSize)
    }
  } else {
    CircularProgressIndicator(
      modifier = Modifier.fillMaxSize(0.5f),
    )
  }
}