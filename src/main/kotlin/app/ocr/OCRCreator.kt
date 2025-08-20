package app.ocr

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.batch.BatchService
import app.batch.ImageDataService
import app.batch.ImagePathInfo
import app.block.BlockSettingsPanelWithPreview
import app.block.SimpleLoadedImageDisplayer
import app.translation.TextDataService
import app.utils.PagesPanel
import app.utils.openFileDialog
import core.navigation.NavigationController
import core.navigation.NavigationDestination
import core.utils.JSON
import core.utils.KotlinUtils.applyIf
import fonts.domain.FontResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import org.koin.compose.koinInject
import project.data.Project
import translation.data.*
import translation.domain.OCRCreatorViewModel
import java.awt.FileDialog
import java.awt.image.BufferedImage
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.writeText

@Composable
fun OCRCreator(navigationController: NavigationController, project: Project? = null) {
  PagesPanel<ImageInfoWithBox>(
    name = "OCR Creator",
    navigationController = navigationController,
    dataExtractor = {
      if (project == null) {
        val images = BatchService.getInstance().get().toList()
        val texts = OCRService.getInstance().workData!!.imagesData.toMutableList()
        images.mapIndexed { index, image ->
          ImageInfoWithBox(
            imagePathInfo = image,
            box = texts.getOrNull(index)?.blockData?.map { block -> OCRBoxData(block.blockPosition, block.text) }
              ?: emptyList()
          )
        }
      } else {
        val images = ImageDataService.getInstance(project, ImageDataService.UNTRANSLATED).get().toList()
        val texts =
          TextDataService.getInstance(project, TextDataService.UNTRANSLATED).workData?.imagesData?.toMutableList()
            ?: mutableListOf()
        images.mapIndexed { index, image ->
          ImageInfoWithBox(
            imagePathInfo = image,
            box = texts.getOrNull(index)?.blockData?.map { block -> OCRBoxData(block.blockPosition, block.text) }
              ?: emptyList()
          )
        }
      }
    },
    stepWindow = { counter, data ->
      OCRCreatorStep(counter, data)
    },
    finalWindow = { dataList ->
      OCRCreatorFinal(navigationController, dataList, project)
    }
  )
}

private data class ImageInfoWithBox(
  val imagePathInfo: ImagePathInfo,
  val box: List<OCRBoxData>
)

@Composable
private fun OCRCreatorStep(
  jobCounter: AtomicInteger,
  imageInfoWithBox: MutableState<ImageInfoWithBox?>
) {
  val viewModel: OCRCreatorViewModel = koinInject()

  val currentImage by viewModel.currentImage
  val ocrBoxes by viewModel.ocrBoxes
  val isProcessingOCR by viewModel.isProcessingOCR
  val operationNumber by viewModel.operationNumber
  val selectedBoxIndex by viewModel.selectedBoxIndex
  val error by viewModel.error

  val image = mutableStateOf<BufferedImage?>(imageInfoWithBox.value!!.imagePathInfo.image)
  val boxes = mutableStateListOf<OCRBoxData>().apply {
    addAll(imageInfoWithBox.value!!.box)
  }

  // Load image into ViewModel when it changes
  LaunchedEffect(imageInfoWithBox.value) {
    imageInfoWithBox.value?.let { info ->
      viewModel.loadImage(info.imagePathInfo)
      // If there are existing boxes, load them
      if (info.box.isNotEmpty()) {
        // Note: In a real implementation, you might want to set these boxes in the ViewModel
        // For now, we'll keep the existing box management
      }
    }
  }

  // Update job counter based on ViewModel loading state
  LaunchedEffect(isProcessingOCR) {
    if (isProcessingOCR) {
      jobCounter.incrementAndGet()
    } else {
      jobCounter.decrementAndGet()
    }
  }

  // Sync ViewModel boxes with local state
  LaunchedEffect(ocrBoxes) {
    boxes.clear()
    boxes.addAll(ocrBoxes)
    imageInfoWithBox.value = imageInfoWithBox.value!!.copy(box = boxes.toList())
  }

  // Show errors
  error?.let { errorMessage ->
    LaunchedEffect(errorMessage) {
      // Handle error display - you might want to show a snackbar or similar
      println("OCR Error: $errorMessage")
    }
  }

  val lazyListState = rememberReorderableLazyListState(onMove = { from, to ->
    if (from.index == 0) return@rememberReorderableLazyListState
    val fromIndex = from.index - 1
    val toIndex = to.index - 1
    if (fromIndex in boxes.indices && toIndex in boxes.indices) {
      val item = boxes.removeAt(fromIndex)
      boxes.add(toIndex, item)
      imageInfoWithBox.value = imageInfoWithBox.value!!.copy(box = boxes.toList())
    }
  })

  Row {
    Column(modifier = Modifier.fillMaxWidth(0.7f)) {
      SimpleLoadedImageDisplayer(
        modifier = Modifier.fillMaxSize(0.9f),
        image = image,
        boxes = boxes,
        operationNumber = mutableStateOf(operationNumber),
        selectedBoxIndex = mutableStateOf(selectedBoxIndex)
      )
    }
    LazyColumn(
      state = lazyListState.listState,
      modifier = Modifier
        .fillMaxWidth()
        .reorderable(lazyListState)
        .detectReorderAfterLongPress(lazyListState)
    ) {
      item {
        Row(modifier = Modifier.fillMaxWidth()) {
          Button(
            onClick = { viewModel.processOCR() },
            enabled = !isProcessingOCR,
            modifier = Modifier.fillMaxWidth()
          ) {
            if (isProcessingOCR) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp))
            } else {
              Text("Try OCR")
            }
          }
        }
      }
      items(boxes.size, { it }) { index ->
        ReorderableItem(lazyListState, key = index) { isDragging ->
          val elevation = animateDpAsState(if (isDragging) 16.dp else 0.dp)
          val box = boxes[index]
          val interactionSource = remember { MutableInteractionSource() }
          val isFocused by interactionSource.collectIsFocusedAsState()

          if (isFocused) {
            viewModel.selectBox(index)
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .applyIf(selectedBoxIndex == index) { it.border(1.dp, Color.Cyan) }
              .shadow(elevation.value)
          ) {
            Button(
              onClick = {
                if (index < boxes.size - 1) {
                  viewModel.mergeBoxes(index)
                  // Update local state from ViewModel
                  boxes.clear()
                  boxes.addAll(ocrBoxes)
                  imageInfoWithBox.value = imageInfoWithBox.value!!.copy(box = boxes.toList())
                }
              },
              enabled = !isProcessingOCR && index < boxes.size - 1
            ) {
              Text("Merge Down")
            }
            TextField(
              value = box.text,
              modifier = Modifier.fillMaxSize(0.9f).padding(10.dp),
              onValueChange = {
                viewModel.updateBoxText(index, it)
                // Update local state
                boxes[index] = box.copy(text = it)
                imageInfoWithBox.value = imageInfoWithBox.value!!.copy(box = boxes.toList())
              },
              interactionSource = interactionSource
            )
            Button(
              onClick = {
                viewModel.removeBox(index)
                // Update local state from ViewModel
                boxes.clear()
                boxes.addAll(ocrBoxes)
                imageInfoWithBox.value = imageInfoWithBox.value!!.copy(box = boxes.toList())
              },
              enabled = !isProcessingOCR
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Trash"
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun OCRCreatorFinal(
  navigationController: NavigationController,
  dataList: List<ImageInfoWithBox>,
  project: Project?
) {
  val image: MutableState<BufferedImage?> = remember { mutableStateOf(null) }
  val fontResolver: FontResolver = koinInject()

  // Create default settings using FontResolver
  val settings: MutableState<BlockSettings> = remember {
    mutableStateOf(BlockSettings("Arial")) // temporary default
  }

  // Load default settings
  LaunchedEffect(Unit) {
    settings.value = fontResolver.createDefaultSettings()
  }

  val author: MutableState<String> = remember { mutableStateOf("") }
  val savePath: MutableState<String> = remember {
    val path: String = if (project != null) {
      TextDataService.getInstance(project, TextDataService.UNTRANSLATED).workDataPath.toAbsolutePath().toString()
    } else {
      ""
    }
    mutableStateOf(path)
  }

  val parent = remember { ComposeWindow(null) }
  val scope = rememberCoroutineScope()

  Column(
    modifier = Modifier.verticalScroll(rememberScrollState())
  ) {
    Text("Please, select default settings for text")

    BlockSettingsPanelWithPreview(settings, image)

    Row {
      Text("Author")

      TextField(
        value = author.value,
        onValueChange = { author.value = it },
        modifier = Modifier.fillMaxWidth().padding(8.dp)
      )
    }
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
            val files = openFileDialog(parent, "Files to add", false, FileDialog.SAVE)
            savePath.value = files.single().absolutePath
          }
        },
        enabled = project == null
      ) {
        Text("Select output")
      }
    }

    Button(onClick = {
      scope.launch {
        // Resolve font for the final settings
        val resolvedSettings = fontResolver.resolveFont(settings.value)

        val workData = WorkData(
          1,
          author.value,
          dataList.mapIndexed { index, (imagePathInfo, imageBoxes) ->
            ImageData(
              index = index,
              imageName = imagePathInfo.name,
              image = null,
              blockData = imageBoxes.map { box ->
                BlockData(
                  blockPosition = BlockPosition(
                    box.box.x,
                    box.box.y,
                    box.box.width,
                    box.box.height,
                    BlockPosition.Shape.Rectangle
                  ),
                  text = box.text,
                  settings = null
                )
              },
              settings = resolvedSettings,
            )
          }
        )

        if (project == null) {
          OCRService.getInstance().workData = workData
        } else {
          TextDataService.getInstance(project, TextDataService.UNTRANSLATED).workData = workData
        }
        try {
          val path = Path.of(savePath.value)
          path.writeText(JSON.encodeToString(workData))
        } catch (e: InvalidPathException) {
          println(e)
        }
        navigationController.navigateTo(NavigationDestination.MainMenu)
      }
    }) {
      Text("Done")
    }
  }
}