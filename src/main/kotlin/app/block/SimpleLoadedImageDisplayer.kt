package app.block

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import app.advanced.BlockOnImage
import app.advanced.BoxOnImage
import app.ocr.OCRBoxData
import bean.BlockData
import bean.BlockSettings
import utils.FollowableMutableState
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun SimpleLoadedImageDisplayerWithBlocks(
  jobCounter: AtomicInteger,
  baseSettings: BlockSettings,
  image: MutableState<BufferedImage?>,
  boxes: MutableState<List<BlockData>>,
  operationNumber: MutableState<Int>,
  selectedBoxIndex: MutableState<Int?>
) {
  data class KeyClass(
    val baseSettings: BlockSettings,
    val boxes: MutableState<List<BlockData>>,
    val selectedBoxIndex: MutableState<Int?>
  )

  val key = KeyClass(baseSettings, boxes, selectedBoxIndex)

  val zoom = 1.0f
  val offset = Offset(0f, 0f)

  SimpleLoadedImageDisplayer(
    modifier = Modifier.fillMaxSize(0.8f),
    image = image,
    displayableKey = key,
    zoom = zoom,
    offset = offset,
    displayableOnImage = { imageSize, imageOriginalSize, (baseSettings, boxes, selectedBoxIndex) ->
      boxes.value.forEachIndexed { index, box ->
        key(Triple(image.value?.hashCode(), operationNumber.value, index)) {
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
            selectedBoxIndex = selectedBoxIndex,
            zoom = zoom,
            offset = offset,
          )
        }
      }
    }
  )
}

@Composable
fun SimpleLoadedImageDisplayerWithBoxes(
  modifier: Modifier = Modifier,
  image: MutableState<BufferedImage?>,
  boxes: MutableList<OCRBoxData>,
  operationNumber: MutableState<Int>,
  selectedBoxIndex: MutableState<Int?>
) {
  val zoom = 1f
  val offset = Offset(0f, 0f)
  SimpleLoadedImageDisplayer(
    modifier = modifier,
    image = image,
    displayableKey = boxes,
    zoom = zoom,
    offset = offset,
    displayableOnImage = { imageSize, imageOriginalSize, boxes ->
      boxes.forEachIndexed { index, box ->
        key(Triple(image.value?.hashCode(), operationNumber.value, index)) {
          val boxFollowable = FollowableMutableState(mutableStateOf(box.box))
          boxFollowable.follow { _, after ->
            boxes[index] = boxes[index].copy(box = after)
          }
          BoxOnImage(
            index = index,
            imageSize = imageOriginalSize,
            displayImageSize = imageSize.value,
            blockData = boxFollowable,
            selectedBoxIndex = selectedBoxIndex,
            zoom = zoom,
            offset = offset
          )
        }
      }
    }
  )
}

@Composable
fun <T> SimpleLoadedImageDisplayer(
  modifier: Modifier = Modifier,
  image: MutableState<BufferedImage?>,
  zoom: Float = 1f,
  offset: Offset = Offset.Zero,
  displayableKey: T? = null,
  displayableOnImage: @Composable ((FollowableMutableState<IntSize>, IntSize, T) -> Unit)? = null
) {
  val imageSize = remember { FollowableMutableState(mutableStateOf(IntSize.Zero)) }
  val imagePaster = remember { mutableStateOf<ImageBitmap?>(null) }

  val clampedZoom = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)

  LaunchedEffect(image.value, zoom, offset.x, offset.y) {
    val sourceImage = image.value ?: return@LaunchedEffect

    // Вычисляем размеры видимой области
    val visibleSourceWidth = (sourceImage.width * clampedZoom).toInt() // Сколько пикселей исходника мы хотим видеть
    val visibleSourceHeight = (sourceImage.height * clampedZoom).toInt()

    // Создаем новое изображение размером с оригинал (так как мы растягиваем видимую часть на весь размер)
    val croppedImage = BufferedImage(sourceImage.width, sourceImage.height, BufferedImage.TYPE_INT_ARGB)
    val g2d = croppedImage.createGraphics()

    try {
      g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)

      // Вычисляем область исходного изображения для отображения
      val sourceX = offset.x.toInt()
      val sourceY = offset.y.toInt()

      // Отрисовываем часть изображения на весь размер
      g2d.drawImage(
        sourceImage,
        0, 0, sourceImage.width, sourceImage.height,  // на весь размер
        sourceX, sourceY, sourceX + visibleSourceWidth, sourceY + visibleSourceHeight,  // берем только часть
        null
      )
    } finally {
      g2d.dispose()
    }

    val outputStream = ByteArrayOutputStream()
    ImageIO.write(croppedImage, "png", outputStream)
    val byteArray = outputStream.toByteArray()
    imagePaster.value = loadImageBitmap(ByteArrayInputStream(byteArray))
  }

  if (imagePaster.value != null && image.value != null) {
    val imageOriginalSize = IntSize(image.value!!.width, image.value!!.height)

    Box(modifier = modifier) {
      Image(
        bitmap = imagePaster.value!!,
        contentDescription = null,
        modifier = Modifier
          .fillMaxSize()
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

      if (imageSize.value.width == 0 || imageSize.value.height == 0) return@Box
      if (displayableOnImage != null) displayableOnImage(imageSize, imageOriginalSize, displayableKey!!)
    }
  } else {
    CircularProgressIndicator(
      modifier = Modifier.fillMaxSize(0.5f),
    )
  }
}

private const val MIN_ZOOM = 0.1f
private const val MAX_ZOOM = 1.0f