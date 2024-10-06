package app.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import app.AppStateEnum
import app.batch.BatchService
import app.batch.ImagePathInfo
import app.block.BlockSettingsPanel
import app.block.SimpleLoadedImageDisplayer
import app.ocr.OCRService
import app.utils.PagesPanel
import bean.BlockSettings
import bean.ImageData
import utils.FollowableMutableState
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicInteger

@Composable
fun EditCreator(state: MutableState<AppStateEnum>) {
  PagesPanel(
    name = "Edit Creator",
    state = state,
    dataExtractor = {
      val cleanedImages = BatchService.getInstance().get().toList()
      val imagesData = OCRService.getInstance().workData!!.imagesData.toMutableList()
      check(cleanedImages.size == imagesData.size)

      cleanedImages.zip(imagesData).map { (imagePathInfo, imageData) -> CleanedImageWithBlock(imagePathInfo, imageData) }
    },
    stepWindow = { jobCounter, data ->
      EditCreatorStep(jobCounter, data)
    },
    finalWindow = { dataList ->
      EditCreatorFinal(state, dataList)
    }
  )
}

private data class CleanedImageWithBlock(
  val imagePathInfo: ImagePathInfo,
  val imageData: ImageData
)

@Composable
private fun EditCreatorStep(
  imageEditsCounter: AtomicInteger,
  currentImage: MutableState<CleanedImageWithBlock?>
) {
  val selectedBoxIndex = remember { mutableStateOf<Int?>(null) }

  val boxes = remember {
    FollowableMutableState(mutableStateOf(currentImage.value!!.imageData.blockData)).apply {
      follow { old, new ->
        if (old.size != new.size) {
          // TODO make check
          selectedBoxIndex.value = null
          return@follow
        }
      }
    }
  }

  fun currentSettings(): BlockSettings =
    if (selectedBoxIndex.value == null) {
      currentImage.value!!.imageData.settings
    } else {
      val boxIndex = selectedBoxIndex.value!!
      if (boxes.value.indices.contains(boxIndex)) {
        boxes.value[boxIndex].settings ?: currentImage.value!!.imageData.settings
      } else {
        selectedBoxIndex.value = null
        currentImage.value!!.imageData.settings
      }
    }

  val settings = remember { mutableStateOf(currentSettings()) }
  val image = remember { mutableStateOf<BufferedImage?>(currentImage.value!!.imagePathInfo.image) }

  LaunchedEffect(selectedBoxIndex.value) {
    settings.value = currentSettings()
  }

  LaunchedEffect(boxes.value) {
    currentSettings()
  }

  Row {
    Column(modifier = Modifier.fillMaxWidth(0.5f)) {
      SimpleLoadedImageDisplayer(
        imageEditsCounter,
        settings.value,
        image,
        boxes,
        selectedBoxIndex
      )
    }
    Column {
      BlockSettingsPanel(settings)
    }
  }
}

@Composable
private fun EditCreatorFinal(
  state: MutableState<AppStateEnum>,
  cleanedImages: List<CleanedImageWithBlock>
) {
  TODO()
}