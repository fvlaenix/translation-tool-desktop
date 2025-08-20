package app.ocr

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.AppStateEnum
import app.batch.BatchService
import app.batch.ImageDataService
import app.batch.ImagePathInfo
import app.block.BlockSettingsPanelWithPreview
import app.block.SimpleLoadedImageDisplayer
import app.translation.TextDataService
import app.utils.PagesPanel
import app.utils.openFileDialog
import bean.*
import core.utils.FollowableMutableList
import core.utils.JSON
import core.utils.KotlinUtils.applyIf
import core.utils.ProtobufUtils
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
import java.awt.FileDialog
import java.awt.image.BufferedImage
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.writeText

@Composable
fun OCRCreator(state: MutableState<AppStateEnum>, project: Project? = null) {
  PagesPanel<ImageInfoWithBox>(
    name = "OCR Creator",
    state = state,
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
      OCRCreatorFinal(state, dataList, project)
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
  val coroutineScope = rememberCoroutineScope()
  val image = mutableStateOf<BufferedImage?>(imageInfoWithBox.value!!.imagePathInfo.image)
  val selectedBoxIndex = remember { mutableStateOf<Int?>(null) }
  val operationNumber = remember { mutableStateOf<Int>(0) }

  val boxes =
    FollowableMutableList(mutableStateListOf<OCRBoxData>())
      .apply {
        follow { newList ->
          imageInfoWithBox.value = imageInfoWithBox.value!!.copy(box = newList.toList())
        }
      }
      .apply { addAll(imageInfoWithBox.value!!.box) }

  val lazyListState = rememberReorderableLazyListState(onMove = { from, to ->
    if (from.index == 0) return@rememberReorderableLazyListState
    boxes.add(to.index - 1, boxes.removeAt(from.index - 1))
    operationNumber.value += 1
  })

  Row(
    modifier = Modifier
  ) {
    Column(modifier = Modifier.fillMaxWidth(0.7f)) {
      SimpleLoadedImageDisplayer(
        modifier = Modifier.fillMaxSize(0.9f),
        image = image,
        boxes = boxes,
        operationNumber = operationNumber,
        selectedBoxIndex = selectedBoxIndex
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
            onClick = {
              jobCounter.incrementAndGet()
              coroutineScope.launch(Dispatchers.IO) {
                try {
                  val currentOcrBoxes = ProtobufUtils.getBoxedOCR(imageInfoWithBox.value!!.imagePathInfo.image)
                  boxes.clear()
                  boxes.addAll(currentOcrBoxes)
                  operationNumber.value += 1
                } finally {
                  jobCounter.decrementAndGet()
                }
              }
            },
            enabled = jobCounter.get() == 0,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Try OCR")
          }
        }
      }
      items(boxes.size, { it }) { index ->
        ReorderableItem(lazyListState, key = index) { isDragging ->
          val elevation = animateDpAsState(if (isDragging) 16.dp else 0.dp)
          val box = boxes[index]
          val interactionSource = remember { MutableInteractionSource() }
          val isFocused by interactionSource.collectIsFocusedAsState()
          if (isFocused) selectedBoxIndex.value = index

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .applyIf(selectedBoxIndex.value == index) { it.border(1.dp, Color.Cyan) }
              .shadow(elevation.value)
          ) {
            Button(onClick = {
              val currentBox = boxes[index]
              val nextBox = boxes[index + 1]
              val minX = minOf(currentBox.box.x, nextBox.box.x)
              val minY = minOf(currentBox.box.y, nextBox.box.y)
              val maxX = maxOf(currentBox.box.x + currentBox.box.width, nextBox.box.x + nextBox.box.width)
              val maxY = maxOf(currentBox.box.y + currentBox.box.height, nextBox.box.y + nextBox.box.height)
              val newBox = OCRBoxData(
                box = BlockPosition(
                  x = minX,
                  y = minY,
                  width = maxX - minX,
                  height = maxY - minY,
                  shape = BlockPosition.Shape.Rectangle
                ),
                text = currentBox.text + " " + nextBox.text
              )
              boxes[index] = newBox
              boxes.removeAt(index + 1)
              operationNumber.value += 1
            }, enabled = jobCounter.get() == 0 && index < boxes.size - 1) {
              Text("Merge Down")
            }
            TextField(
              value = box.text,
              modifier = Modifier.fillMaxSize(0.9f).padding(10.dp),
              onValueChange = { boxes[index] = box.copy(text = it) },
              interactionSource = interactionSource
            )
            Button(
              onClick = {
                boxes.removeAt(index)
                operationNumber.value += 1
              }, enabled = jobCounter.get() == 0
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
  state: MutableState<AppStateEnum>,
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
        state.value = AppStateEnum.MAIN_MENU
      }
    }) {
      Text("Done")
    }
  }
}