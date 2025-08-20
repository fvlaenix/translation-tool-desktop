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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import app.batch.ImagePathInfo
import app.block.BlockSettingsPanel
import app.block.SimpleLoadedImageDisplayer
import app.utils.ChipSelector
import app.utils.PagesPanel
import core.navigation.NavigationController
import core.navigation.NavigationDestination
import core.utils.FollowableMutableState
import core.utils.ImageUtils.deepCopy
import core.utils.Text2ImageUtils
import io.github.vinceglb.filekit.core.FileKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.koin.compose.koinInject
import project.data.*
import translation.data.BlockPosition
import translation.data.BlockSettings
import translation.data.ImageData
import translation.data.WorkDataRepository
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import javax.imageio.ImageIO
import kotlin.concurrent.withLock
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory

@Composable
fun EditCreator(navigationController: NavigationController, project: Project? = null) {
  val imageDataRepository: ImageDataRepository = koinInject()
  val workDataRepository: WorkDataRepository = koinInject()
  val textDataRepository: TextDataRepository = koinInject()

  PagesPanel(
    name = "Edit Creator",
    navigationController = navigationController,
    dataExtractor = {
      val (cleanedImages, imagesData) = if (project == null) {
        val batchImages = imageDataRepository.getBatchImages().getOrElse { emptyList() }
        val workData = workDataRepository.getWorkData().getOrNull()
        batchImages to (workData?.imagesData?.toMutableList() ?: mutableListOf())
      } else {
        val cleanedImages = imageDataRepository.loadImages(project, ImageType.CLEANED).getOrElse { emptyList() }
        val translatedWorkData = textDataRepository.loadWorkData(project, TextType.TRANSLATED).getOrNull()
        val imagesData = translatedWorkData?.imagesData?.toMutableList() ?: mutableListOf()

        cleanedImages to imagesData
      }
      check(cleanedImages.size == imagesData.size)

      cleanedImages.zip(imagesData)
        .map { (imagePathInfo, imageData) -> CleanedImageWithBlock(imagePathInfo, imageData) }
    },
    stepWindow = { jobCounter, data ->
      EditCreatorStep(jobCounter, data)
    },
    finalWindow = { dataList ->
      EditCreatorFinal(navigationController, dataList, project)
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
  val operationNumber = remember { mutableStateOf<Int>(0) }

  val boxes = remember {
    FollowableMutableState(mutableStateOf(currentImage.value!!.imageData.blockData)).apply {
      follow { old, new ->
        if (old.size != new.size) {
          // TODO make check
          selectedBoxIndex.value = null
          return@follow
        }
      }
      follow { _, new ->
        currentImage.value = currentImage.value!!.copy(
          imageData = currentImage.value!!.imageData.copy(
            blockData = new
          )
        )
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

  fun currentShape(): BlockPosition.Shape? =
    if (selectedBoxIndex.value == null) {
      null
    } else {
      val boxIndex = selectedBoxIndex.value!!
      if (boxes.value.indices.contains(boxIndex)) {
        boxes.value[boxIndex].blockPosition.shape
      } else {
        selectedBoxIndex.value = null
        null
      }
    }

  val settings = remember { mutableStateOf(currentSettings()) }
  val boxType = remember { mutableStateOf(currentShape()) }
  val image = remember { mutableStateOf<BufferedImage?>(currentImage.value!!.imagePathInfo.image) }

  LaunchedEffect(currentImage.value) {
    boxes.value = currentImage.value!!.imageData.blockData
    image.value = currentImage.value!!.imagePathInfo.image
  }

  LaunchedEffect(selectedBoxIndex.value) {
    settings.value = currentSettings()
    boxType.value = currentShape()
  }

  LaunchedEffect(boxes.value) {
    settings.value = currentSettings()
    boxType.value = currentShape()
  }

  LaunchedEffect(settings.value) {
    val index = selectedBoxIndex.value

    if (index == null) {
      val currentImageValue = currentImage.value!!
      currentImage.value =
        currentImageValue.copy(imageData = currentImageValue.imageData.copy(settings = settings.value))
    } else {
      val box = boxes.value[index]
      boxes.value = boxes.value.toMutableList().apply { set(index, box.copy(settings = settings.value)) }
    }
  }

  LaunchedEffect(boxType.value) {
    val index = selectedBoxIndex.value
    val boxTypeValue = boxType.value

    if (index != null && boxTypeValue != null) {
      val box = boxes.value[index]
      boxes.value = boxes.value.toMutableList()
        .apply { set(index, box.copy(blockPosition = box.blockPosition.copy(shape = boxTypeValue))) }
    }
  }

  Row(
    modifier = Modifier
      .onKeyEvent { keyEvent ->
        if (keyEvent.key != Key.Escape) return@onKeyEvent true
        selectedBoxIndex.value = null
        false
      }
  ) {
    Column(modifier = Modifier.fillMaxWidth(0.5f)) {
      SimpleLoadedImageDisplayer(
        imageEditsCounter,
        currentImage.value!!.imageData.settings,
        image,
        boxes,
        operationNumber,
        selectedBoxIndex
      )
    }
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
      if (selectedBoxIndex.value == null) {
        TextField(
          value = "",
          onValueChange = {},
          enabled = false
        )
      } else {
        val boxIndex = selectedBoxIndex.value!!
        val box = boxes.value[boxIndex]
        TextField(
          value = box.text,
          onValueChange = { boxes.value = boxes.value.toMutableList().apply { set(boxIndex, box.copy(text = it)) } }
        )
      }
      BlockSettingsPanel(settings)
      if (selectedBoxIndex.value != null && boxType.value != null) {
        val types = listOf("Rectangle", "Oval")
        val selectedType = when (boxType.value!!) {
          is BlockPosition.Shape.Rectangle -> "Rectangle"
          is BlockPosition.Shape.Oval -> "Oval"
        }
        val chipsState = ChipSelector.rememberChipSelectorState(types, listOf(selectedType)) {
          boxType.value = when (it) {
            "Rectangle" -> BlockPosition.Shape.Rectangle
            "Oval" -> BlockPosition.Shape.Oval
            else -> throw IllegalStateException("Unknown type $it")
          }
        }
        chipsState.selectedChips
        ChipSelector.ChipsSelector(chipsState, modifier = Modifier.fillMaxWidth())
      }
    }
  }
}

@Composable
private fun EditCreatorFinal(
  navigationController: NavigationController,
  cleanedImages: List<CleanedImageWithBlock>,
  project: Project?
) {
  val imageDataRepository: ImageDataRepository = koinInject()
  val textDataRepository: TextDataRepository = koinInject()

  val savePath: MutableState<String> = remember {
    mutableStateOf("")
  }

  LaunchedEffect(project) {
    if (project != null) {
      // TODO remove getOrThrow
      val path = imageDataRepository.getWorkDataPath(project, ImageType.EDITED).getOrThrow()
      savePath.value = path.toAbsolutePath().toString()
    }
  }

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
        enabled = project == null
      )
      Button(
        onClick = {
          scope.launch(Dispatchers.IO) {
            val files = FileKit.pickDirectory("Directory to save")
            savePath.value = files?.file?.absolutePath ?: return@launch
          }
        },
        enabled = project == null
      ) {
        Text("Select output")
      }
    }
    Row {
      Button(onClick = {
        scope.launch(Dispatchers.IO) {
          if (project != null) {
            val currentWorkData = textDataRepository.loadWorkData(project, TextType.TRANSLATED).getOrNull()
            if (currentWorkData != null) {
              val updatedWorkData = currentWorkData.copy(
                imagesData = cleanedImages.map { it.imageData }
              )
              textDataRepository.saveWorkData(project, TextType.TRANSLATED, updatedWorkData).fold(
                onSuccess = { /* Success */ },
                onFailure = { exception ->
                  println("Error saving translated work data: ${exception.message}")
                }
              )
            }
          }

          val path = Path.of(savePath.value)
          path.createDirectories()
          check(path.isDirectory())
          progress = 0.0f
          val part = 1.0 / cleanedImages.size.toFloat()
          val semaphore = Semaphore(4)
          cleanedImages.mapIndexed { index, (imagePathInfo, imageData) ->
            async {
              semaphore.withPermit {
                val image = imagePathInfo.image.deepCopy()
                val blocksImages = imageData.blockData.map { blockData ->
                  val settings = blockData.settings ?: imageData.settings
                  async {
                    blockData to Text2ImageUtils.textToImage(
                      settings, blockData.copy(
                        blockPosition = blockData.blockPosition.copy(
                          x = .0,
                          y = .0
                        )
                      )
                    )
                  }
                }.awaitAll()
                val graphics = image.createGraphics()
                blocksImages.forEach { (blockData, image) ->
                  graphics.drawImage(
                    image.image,
                    blockData.blockPosition.x.toInt(),
                    blockData.blockPosition.y.toInt(),
                    null
                  )
                }
                progressLock.withLock {
                  progress += part.toFloat()
                  progress = progress.coerceIn(0.0f, 1.0f)
                }
                val imagePath =
                  path.resolve("${(index + 1).toString().padStart(cleanedImages.size.toString().length + 1, '0')}.png")
                ImageIO.write(image, "PNG", imagePath.toFile())
              }
            }
          }.awaitAll()
          progress = 1.0f
          navigationController.navigateTo(NavigationDestination.MainMenu)
        }
      }) {
        Text("Done")
      }
      CircularProgressIndicator(progress)
    }
  }
}