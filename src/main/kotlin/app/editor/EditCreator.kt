package app.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import app.AppStateEnum
import app.batch.BatchService
import app.batch.ImagePathInfo
import app.block.BlockSettingsPanel
import app.block.SimpleLoadedImageDisplayer
import app.ocr.OCRService
import app.utils.PagesPanel
import bean.BlockSettings
import bean.ImageData
import io.github.vinceglb.filekit.core.FileKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import utils.FollowableMutableState
import utils.Text2ImageUtils
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import javax.imageio.ImageIO
import kotlin.concurrent.withLock
import kotlin.io.path.isDirectory

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
  val savePath: MutableState<String> = remember { mutableStateOf("") }
  val progressLock: ReentrantLock = remember { ReentrantLock() }
  var progress by remember { mutableStateOf(0f) }

  val scope = rememberCoroutineScope()

  Column(
    modifier = Modifier.verticalScroll(rememberScrollState())
  ) {
    Row {
      Text("Output")
      TextField(
        value = savePath.value,
        onValueChange = { savePath.value = it },
      )
      Button(
        onClick = {
          scope.launch(Dispatchers.IO) {
            val files = FileKit.pickDirectory("Directory to save")
            savePath.value = files?.file?.absolutePath ?: return@launch
          }
        }
      ) {
        Text("Select output")
      }
    }
    Row {
      Button(onClick = {
        scope.launch(Dispatchers.IO) {
          val path = Path.of(savePath.value)
          check(path.isDirectory())
          progress = 0.0f
          val part = 1.0 / cleanedImages.size.toFloat()
          cleanedImages.mapIndexed { index, (imagePathInfo, imageData) ->
            async {
              val image = imagePathInfo.image
              val blocksImages = imageData.blockData.map { blockData ->
                val settings = blockData.settings ?: imageData.settings
                async { blockData to Text2ImageUtils.textToImage(settings, blockData.copy(
                  blockPosition = blockData.blockPosition.copy(
                    x = .0,
                    y = .0
                  )
                )) }
              }.awaitAll()
              val graphics = image.createGraphics()
              blocksImages.forEach { (blockData, image) ->
                graphics.drawImage(image.image, blockData.blockPosition.x.toInt(), blockData.blockPosition.y.toInt(), null)
              }
              progressLock.withLock {
                progress += part.toFloat()
                progress = progress.coerceIn(0.0f, 1.0f)
              }
              val imagePath = path.resolve(imageData.imageName)
              ImageIO.write(image, "PNG", imagePath.toFile())
            }
          }.awaitAll()
          progress = 1.0f
          state.value = AppStateEnum.MAIN_MENU
        }
      }) {
        Text("Done")
      }
      CircularProgressIndicator(progress)
    }

  }
}