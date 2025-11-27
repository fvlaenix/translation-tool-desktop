package core.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Converts a BufferedImage to Compose ImageBitmap asynchronously.
 * This function performs the conversion off the main thread to avoid blocking the UI.
 *
 * @return ImageBitmap converted from this BufferedImage
 * @throws Exception if the conversion fails
 */
suspend fun BufferedImage.toComposeBitmap(): ImageBitmap = withContext(Dispatchers.IO) {
  val outputStream = ByteArrayOutputStream()
  ImageIO.write(this@toComposeBitmap, "png", outputStream)
  val byteArray = outputStream.toByteArray()
  loadImageBitmap(ByteArrayInputStream(byteArray))
}
