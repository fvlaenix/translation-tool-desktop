package app.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.AppStateEnum
import app.batch.BatchService
import app.batch.ImagePathInfo
import app.block.BlockSettingsPanel
import app.block.SimpleLoadedImageDisplayer
import app.ocr.OCRService
import app.utils.PagesPanel
import bean.ImageData
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
  val settings = remember { currentImage.value!!.imageData.settings }
  val image = remember { mutableStateOf<BufferedImage?>(currentImage.value!!.imagePathInfo.image) }
  val blockData = remember { mutableStateOf(currentImage.value!!.imageData.blockData) }

  Row {
    Column(modifier = Modifier.fillMaxWidth(0.5f)) {
      SimpleLoadedImageDisplayer(
        imageEditsCounter,
        settings,
        image,
        blockData
      )
    }
    Column {
      BlockSettingsPanel(mutableStateOf(settings))
    }
  }
}

@Composable
private fun EditCreatorFinal(
  state: MutableState<AppStateEnum>,
  uncleanedImages: List<CleanedImageWithBlock>
) {
  TODO()
}