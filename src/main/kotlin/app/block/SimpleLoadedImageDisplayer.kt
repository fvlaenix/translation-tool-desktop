package app.block

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.IntSize
import core.utils.FollowableMutableState
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.min

@Composable
fun <T> SimpleLoadedImageDisplayer(
  modifier: Modifier = Modifier,
  image: MutableState<BufferedImage?>,
  displayableKey: T? = null,
  displayableOnImage: @Composable ((FollowableMutableState<IntSize>, IntSize, T) -> Unit)? = null
) {
  val imageSize = remember { FollowableMutableState(mutableStateOf(IntSize.Zero)) }
  val imagePaster = remember { mutableStateOf<ImageBitmap?>(null) }

  LaunchedEffect(image.value) {
    val image = image.value ?: return@LaunchedEffect
    val outputStream = ByteArrayOutputStream()
    ImageIO.write(image, "png", outputStream)
    val byteArray = outputStream.toByteArray()
    imagePaster.value = loadImageBitmap(ByteArrayInputStream(byteArray))
  }

  if (imagePaster.value != null && image.value != null) {
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
      // TODO deal with displayKey!!
      if (imageSize.value.width == 0 || imageSize.value.height == 0) return@Box
      if (displayableOnImage != null) displayableOnImage(imageSize, imageOriginalSize, displayableKey!!)
    }
  } else {
    CircularProgressIndicator(
      modifier = Modifier.fillMaxSize(0.5f),
    )
  }
}