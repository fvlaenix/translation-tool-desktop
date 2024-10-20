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
import app.advanced.BlockOnImage
import app.advanced.BoxOnImage
import app.ocr.OCRBoxData
import bean.BlockData
import bean.BlockSettings
import utils.FollowableMutableState
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO
import kotlin.math.min

@Composable
fun SimpleLoadedImageDisplayer(
  jobCounter: AtomicInteger,
  baseSettings: BlockSettings,
  image: MutableState<BufferedImage?>,
  boxes: MutableState<List<BlockData>>,
  selectedBoxIndex: MutableState<Int?>
) {
  data class KeyClass(
    val baseSettings: BlockSettings,
    val boxes: MutableState<List<BlockData>>,
    val selectedBoxIndex: MutableState<Int?>
  )

  val key = KeyClass(baseSettings, boxes, selectedBoxIndex)

  SimpleLoadedImageDisplayer(
    modifier = Modifier.fillMaxSize(0.8f),
    image = image,
    displayableKey = key,
    displayableOnImage = { imageSize, imageOriginalSize, (baseSettings, boxes, selectedBoxIndex) ->
      boxes.value.forEachIndexed { index, box ->
        val boxFollowable = FollowableMutableState(mutableStateOf(box))
        boxFollowable.follow { _, after ->
          boxes.value = boxes.value.toMutableList().apply {
            this[index] = after
          }
        }
        BlockOnImage(
          jobCounter = jobCounter,
          imageSize = imageOriginalSize,
          displayImageSize = imageSize.value,
          basicSettings = baseSettings,
          blockData = boxFollowable,
          index = index,
          selectedBoxIndex = selectedBoxIndex
        )
      }
    }
  )
}

@Composable
fun SimpleLoadedImageDisplayer(
  modifier: Modifier = Modifier,
  image: MutableState<BufferedImage?>,
  boxes: MutableList<OCRBoxData>
) {
  SimpleLoadedImageDisplayer(
    modifier = modifier,
    image = image,
    displayableKey = boxes,
    displayableOnImage = { imageSize, imageOriginalSize, boxes ->
      boxes.forEachIndexed { index, box ->
        val boxFollowable = remember { FollowableMutableState(mutableStateOf(box.box)) }
        boxFollowable.follow { _, after ->
          boxes[index] = boxes[index].copy(box = after)
        }
        BoxOnImage(imageOriginalSize, imageSize.value, boxFollowable)
      }
    }
  )
}

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
      if (displayableOnImage != null) displayableOnImage(imageSize, imageOriginalSize, displayableKey!!)
    }
  } else {
    CircularProgressIndicator(
      modifier = Modifier.fillMaxSize(0.5f),
    )
  }
}