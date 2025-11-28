package app.advanced.domain

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.IntSize
import app.advanced.TranslationInfo
import core.base.BaseViewModel
import core.utils.ClipboardUtils.getClipboardImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import translation.data.BlockPosition
import translation.data.clampToImageBounds
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Viewmodel for managing image with text selection boxes functionality, handles image loading and box manipulation.
 */
class ImageWithBoxesViewModel : BaseViewModel() {

  private val _uiState = mutableStateOf(ImageWithBoxesUiState())
  val uiState: State<ImageWithBoxesUiState> = _uiState

  fun loadImageFromClipboard() {
    updateEmptyText("Image is loading")

    viewModelScope.launch {
      try {
        val imageBitmap = withContext(Dispatchers.IO) {
          val clipboardImage = getClipboardImage()
          if (clipboardImage == null) {
            return@withContext null
          }
          val outputStream = ByteArrayOutputStream()
          ImageIO.write(clipboardImage, "png", outputStream)
          val byteArray = outputStream.toByteArray()
          loadImageBitmap(ByteArrayInputStream(byteArray))
        }

        if (imageBitmap == null) {
          updateEmptyText("Can't take image from clipboard")
        } else {
          _uiState.value = _uiState.value.copy(
            image = imageBitmap,
            isEnabled = true
          )
        }
      } catch (e: Exception) {
        setError("Failed to load image from clipboard: ${e.message}")
        updateEmptyText("Can't take image from clipboard")
      }
    }
  }

  fun addNewBox() {
    val image = _uiState.value.image ?: return
    val imageSize = IntSize(image.width, image.height)

    val newBox = BlockPosition(
      x = 0.0,
      y = 0.0,
      width = image.width.toDouble() / 10,
      height = image.height.toDouble() / 10,
      shape = BlockPosition.Shape.Rectangle
    ).clampToImageBounds(imageSize)

    _uiState.value = _uiState.value.copy(
      boxes = _uiState.value.boxes + newBox
    )
  }

  fun deleteLastBox() {
    val currentBoxes = _uiState.value.boxes
    if (currentBoxes.isNotEmpty()) {
      _uiState.value = _uiState.value.copy(
        boxes = currentBoxes.dropLast(1)
      )
    }
  }

  fun updateBox(index: Int, box: BlockPosition) {
    val image = _uiState.value.image ?: return
    val imageSize = IntSize(image.width, image.height)

    val currentBoxes = _uiState.value.boxes.toMutableList()
    if (index in currentBoxes.indices) {
      currentBoxes[index] = box.clampToImageBounds(imageSize)
      _uiState.value = _uiState.value.copy(boxes = currentBoxes)
    }
  }

  fun selectBox(index: Int?) {
    _uiState.value = _uiState.value.copy(selectedBoxIndex = index)
  }

  fun updateCurrentSize(size: IntSize) {
    _uiState.value = _uiState.value.copy(currentSize = size)
  }

  private fun updateEmptyText(text: String) {
    _uiState.value = _uiState.value.copy(emptyText = text)
  }

  fun clearState() {
    _uiState.value = ImageWithBoxesUiState()
  }

  fun prepareTranslationInfos() {
    val currentImage = _uiState.value.image
    if (currentImage == null) {
      setError("No image loaded")
      return
    }

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(
        isPreparingTranslation = true,
        preparedTranslationInfos = null
      )

      try {
        val translationInfos = withContext(Dispatchers.Default) {
          val fullBufferedImage = currentImage.toAwtImage()

          if (_uiState.value.boxes.isEmpty()) {
            listOf(TranslationInfo(fullBufferedImage))
          } else {
            _uiState.value.boxes.map { boxData ->
              val subImage = fullBufferedImage.getSubimage(
                boxData.x.toInt(),
                boxData.y.toInt(),
                boxData.width.toInt(),
                boxData.height.toInt()
              )
              TranslationInfo(subImage)
            }
          }
        }

        _uiState.value = _uiState.value.copy(
          isPreparingTranslation = false,
          preparedTranslationInfos = translationInfos
        )
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(isPreparingTranslation = false)
        setError("Failed to prepare translation: ${e.message}")
      }
    }
  }
}