package app.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import app.batch.ImagePathInfo
import app.block.BlockSettingsPanel
import app.editor.domain.EditCreatorStepUiState
import app.editor.domain.EditCreatorStepViewModel
import app.utils.ChipSelector
import app.utils.PagesPanel
import core.image.ImageCanvas
import core.image.overlays.BoxOverlayMigration
import core.navigation.NavigationController
import core.navigation.NavigationDestination
import core.utils.ImageUtils.deepCopy
import core.utils.Text2ImageUtils
import io.github.vinceglb.filekit.core.FileKit
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.koin.compose.koinInject
import project.data.*
import translation.data.*
import java.nio.file.Path
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

      // MORE ROBUST VALIDATION: Instead of failing, handle mismatched data gracefully
      if (cleanedImages.isEmpty()) {
        throw IllegalStateException("No cleaned images found. Please run the cleaning process first.")
      }

      if (imagesData.isEmpty()) {
        throw IllegalStateException("No translated data found. Please run the translation process first.")
      }

      if (cleanedImages.size != imagesData.size) {
        throw IllegalStateException(
          "Data mismatch: Found ${cleanedImages.size} cleaned images but ${imagesData.size} translated images. " +
              "Please ensure translation process completed successfully for all images."
        )
      }

      cleanedImages.zip(imagesData)
        .map { (imagePathInfo, imageData) -> CleanedImageWithBlock(imagePathInfo, imageData) }
    },
    stepWindow = { jobCounter, data ->
      EditCreatorStep(currentImage = data)
    },
    finalWindow = { dataList ->
      EditCreatorFinal(navigationController, dataList, project)
    }
  )
}

data class CleanedImageWithBlock(
  val imagePathInfo: ImagePathInfo,
  val imageData: ImageData
)

@Composable
private fun EditCreatorStep(
  currentImage: MutableState<CleanedImageWithBlock?>
) {
  val viewModel: EditCreatorStepViewModel = koinInject()

  EditCreatorStep(
    viewModel = viewModel,
    currentImage = currentImage,
    onDataChange = { updatedData ->
      currentImage.value = updatedData
    }
  )
}

@Composable
fun EditCreatorStep(
  viewModel: EditCreatorStepViewModel = koinInject(),
  currentImage: MutableState<CleanedImageWithBlock?>,
  onDataChange: (CleanedImageWithBlock) -> Unit
) {
  val uiState by viewModel.uiState

  // Only reload when the actual IMAGE changes (not when data is synced back)
  // Use the image reference as key, not the whole currentImage.value
  LaunchedEffect(currentImage.value?.imagePathInfo?.image) {
    currentImage.value?.let { data ->
      viewModel.loadImageData(
        image = data.imagePathInfo.image,
        blockData = data.imageData.blockData,
        settings = data.imageData.settings
      )
    }
  }

  // Sync changes back to parent (this updates currentImage.value but shouldn't trigger reload above)
  LaunchedEffect(uiState.boxes, uiState.currentSettings) {
    currentImage.value?.let { current ->
      val updatedImageData = current.imageData.copy(
        blockData = uiState.boxes,
        settings = uiState.currentSettings ?: current.imageData.settings
      )
      val updatedData = current.copy(imageData = updatedImageData)
      onDataChange(updatedData)
    }
  }

  Row(
    modifier = Modifier
      .onKeyEvent { keyEvent ->
        // Only handle key down events
        if (keyEvent.type != KeyEventType.KeyDown) {
          return@onKeyEvent false
        }
        // Escape to deselect box
        if (keyEvent.key == Key.Escape) {
          viewModel.selectBox(null)
          return@onKeyEvent true
        }
        false
      }
  ) {
    ImageEditingArea(
      uiState = uiState,
      onBoxUpdate = { index, blockData -> viewModel.updateBox(index, blockData) },
      onBoxSelect = { index -> viewModel.selectBox(index) }
    )

    EditControlsPanel(
      uiState = uiState,
      onBoxTextUpdate = { index, text -> viewModel.updateBoxText(index, text) },
      onSettingsUpdate = { viewModel.updateSettings(it) },
      onShapeUpdate = { viewModel.updateBoxShape(it) }
    )
  }
}

@Composable
private fun ImageEditingArea(
  uiState: EditCreatorStepUiState,
  onBoxUpdate: (Int, BlockData) -> Unit,
  onBoxSelect: (Int?) -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth(0.5f)) {
    val currentSettings = uiState.currentSettings
    if (currentSettings != null) {
      val overlays = remember(
        uiState.boxes.size,
        uiState.selectedBoxIndex,
        uiState.operationNumber,
        currentSettings
      ) {
        BoxOverlayMigration.createBlockOverlays(
          blocks = uiState.boxes,
          basicSettings = currentSettings,
          selectedBoxIndex = uiState.selectedBoxIndex,
          onBlockUpdate = onBlockUpdate@{ index, blockData ->
            onBoxUpdate(index, blockData)
          },
          onBoxSelect = { index ->
            onBoxSelect(index)
          },
          onHeavyChange = {
          }
        )
      }

      ImageCanvas(
        image = uiState.image,
        overlays = overlays,
        modifier = Modifier.fillMaxSize()
      )
    }
  }
}

@Composable
private fun EditControlsPanel(
  uiState: EditCreatorStepUiState,
  onBoxTextUpdate: (Int, String) -> Unit,
  onSettingsUpdate: (BlockSettings) -> Unit,
  onShapeUpdate: (BlockPosition.Shape) -> Unit
) {
  Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
    TextEditingField(
      selectedBoxIndex = uiState.selectedBoxIndex,
      boxes = uiState.boxes,
      onTextUpdate = onBoxTextUpdate
    )

    SettingsConfigurationPanel(
      currentSettings = uiState.currentSettings,
      selectedBoxIndex = uiState.selectedBoxIndex,
      onSettingsUpdate = onSettingsUpdate
    )

    ShapeSelectionPanel(
      selectedBoxIndex = uiState.selectedBoxIndex,
      currentShape = uiState.currentShape,
      onShapeUpdate = onShapeUpdate
    )
  }
}

@Composable
private fun TextEditingField(
  selectedBoxIndex: Int?,
  boxes: List<translation.data.BlockData>,
  onTextUpdate: (Int, String) -> Unit
) {
  if (selectedBoxIndex == null) {
    TextField(
      value = "",
      onValueChange = {},
      enabled = false
    )
  } else {
    val box = boxes.getOrNull(selectedBoxIndex)
    if (box != null) {
      TextField(
        value = box.text,
        onValueChange = { onTextUpdate(selectedBoxIndex, it) }
      )
    }
  }
}

@Composable
private fun SettingsConfigurationPanel(
  currentSettings: BlockSettings?,
  selectedBoxIndex: Int?,
  onSettingsUpdate: (BlockSettings) -> Unit
) {
  if (currentSettings != null) {
    // Use selectedBoxIndex as key so state resets when selection changes
    val settingsState = remember(selectedBoxIndex) { mutableStateOf(currentSettings) }

    // Only sync from external when selection changes (handled by remember key above)
    // Don't sync on every currentSettings change to avoid overwriting user edits

    // Update callback when user changes settings in the panel
    LaunchedEffect(settingsState.value) {
      // Only call update if settings actually differ from current
      if (settingsState.value != currentSettings) {
        onSettingsUpdate(settingsState.value)
      }
    }

    BlockSettingsPanel(settingsState)
  }
}

@Composable
private fun ShapeSelectionPanel(
  selectedBoxIndex: Int?,
  currentShape: BlockPosition.Shape?,
  onShapeUpdate: (BlockPosition.Shape) -> Unit
) {
  if (selectedBoxIndex != null && currentShape != null) {
    val types = listOf("Rectangle", "Oval")
    val selectedType = when (currentShape) {
      is BlockPosition.Shape.Rectangle -> "Rectangle"
      is BlockPosition.Shape.Oval -> "Oval"
    }
    val chipsState = ChipSelector.rememberChipSelectorState(types, listOf(selectedType)) { typeString ->
      val shape = when (typeString) {
        "Rectangle" -> BlockPosition.Shape.Rectangle
        "Oval" -> BlockPosition.Shape.Oval
        else -> BlockPosition.Shape.Rectangle
      }
      onShapeUpdate(shape)
    }
    ChipSelector.ChipsSelector(chipsState, modifier = Modifier.fillMaxWidth())
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
            val selectedPath = files?.file?.absolutePath ?: return@launch
            withContext(Dispatchers.Main) {
              savePath.value = selectedPath
            }
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
          withContext(Dispatchers.Main) { progress = 0.0f }
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
                val newProgress = progressLock.withLock {
                  val updated = progress + part.toFloat()
                  updated.coerceIn(0.0f, 1.0f)
                }
                withContext(Dispatchers.Main) {
                  progress = newProgress
                }
                val imagePath =
                  path.resolve("${(index + 1).toString().padStart(cleanedImages.size.toString().length + 1, '0')}.png")
                ImageIO.write(image, "PNG", imagePath.toFile())
              }
            }
          }.awaitAll()
          withContext(Dispatchers.Main) { progress = 1.0f }
          withContext(Dispatchers.Main) {
            navigationController.navigateTo(NavigationDestination.MainMenu)
          }
        }
      }) {
        Text("Done")
      }
      CircularProgressIndicator(progress)
    }
  }
}