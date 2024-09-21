package app.ocr

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.AppStateEnum
import app.TopBar
import app.advanced.BoxOnImageData
import app.batch.BatchService
import app.batch.ImagePathInfo
import app.block.BlockSettingsPanelWithPreview
import app.block.SimpleLoadedImageDisplayer
import app.utils.openFileDialog
import bean.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import utils.FontService
import utils.JSON
import utils.ProtobufUtils
import java.awt.FileDialog
import java.awt.image.BufferedImage
import java.nio.file.InvalidPathException
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.writeText

@Composable
fun OCRCreator(state: MutableState<AppStateEnum>) {
  val coroutineScope = rememberCoroutineScope()
  var lockedByTask = remember { mutableStateOf(false) }

  val imageSize = remember { mutableStateOf(IntSize.Zero) }

  val images = BatchService.getInstance().get().toList()
  var index by remember { mutableStateOf(-1) }
  val imagesBoxes = remember { mutableStateListOf<List<OCRBoxData>>() }

  val currentImage = remember { mutableStateOf<BufferedImage?>(null) }
  val boxes = remember { mutableStateListOf<OCRBoxData>() }
  var selectedBox by remember { mutableStateOf<BoxOnImageData?>(null) }

  fun setIndex(newIndex: Int) {
    if (newIndex == -1) index = -1
    else {
      // save result
      if (index >= 0) {
        while (imagesBoxes.size <= index) {
          imagesBoxes.add(listOf())
        }
        imagesBoxes[index] = boxes.toList()
      }

      // clean
      selectedBox = null
      currentImage.value = null
      boxes.clear()

      // set new index
      index = newIndex

      if (newIndex == images.size) return
      // show new
      boxes.addAll(imagesBoxes.getOrElse(index) { listOf() })
      currentImage.value = images[index].image
    }
  }

  TopBar(state, "OCR Creator",
    bottomBar = {
      BottomAppBar {
        Row {
          Button(onClick = { setIndex(index - 1) }, enabled = index > 0 && !lockedByTask.value) { Text("Previous") }
          Button(onClick = { setIndex(index + 1) }, enabled = index + 1 < images.size && !lockedByTask.value) { Text("Next") }
          Button(onClick = { setIndex(images.size) }, enabled = index != images.size && !lockedByTask.value) { Text("Done") }
        }
      }
    }
  ) {
    if (index == -1) Text("Click next if you want to continue")
    else if (index == images.size) OCRCreatorFinal(state, images, imagesBoxes)
    else OCRCreatorStep(imageSize, currentImage, boxes, lockedByTask, coroutineScope)
  }
}

@Composable
private fun OCRCreatorStep(
  imageSize: MutableState<IntSize>,
  currentImage: MutableState<BufferedImage?>,
  boxes: SnapshotStateList<OCRBoxData>,
  lockedByTask: MutableState<Boolean>,
  coroutineScope: CoroutineScope
) {
  Row(
    modifier = Modifier
  ) {
    Column(modifier = Modifier.fillMaxWidth(0.7f).onSizeChanged { imageSize.value = it }) {
      SimpleLoadedImageDisplayer(currentImage, boxes)
    }
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
      Row(modifier = Modifier.fillMaxWidth()) {
        Button(
          onClick = {
            lockedByTask.value = true
            coroutineScope.launch(Dispatchers.IO) {
              val currentOcrBoxes = ProtobufUtils.getBoxedOCR(currentImage.value!!)
              boxes.clear()
              boxes.addAll(currentOcrBoxes)
              lockedByTask.value = false
            }
          },
          enabled = !lockedByTask.value,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Try OCR")
        }
      }
      boxes.forEachIndexed { index, box ->
        Row(modifier = Modifier.fillMaxWidth().height((imageSize.value.height / 5).dp)) {
          TextField(
            value = box.text,
            modifier = Modifier.fillMaxSize(0.9f).padding(10.dp),
            onValueChange = { boxes[index] = box.copy(text = it) }
          )
          Button(onClick = { boxes.removeAt(index) }, enabled = !lockedByTask.value) {
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

@Composable
private fun OCRCreatorFinal(
  state: MutableState<AppStateEnum>,
  images: List<ImagePathInfo>,
  imageBoxes: SnapshotStateList<List<OCRBoxData>>,
) {
  val image: MutableState<BufferedImage?> = remember { mutableStateOf(null) }
  // TODO make font take better
  val settings: MutableState<BlockSettings> = remember { mutableStateOf(BlockSettings(FontService.getInstance().getDefaultFont())) }
  val author: MutableState<String> = remember { mutableStateOf("") }
  val savePath: MutableState<String> = remember { mutableStateOf("") }

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
      )
      Button(
        onClick = {
          scope.launch(Dispatchers.IO) {
            val files = openFileDialog(parent, "Files to add", false, FileDialog.SAVE)
            savePath.value = files.single().absolutePath
          }
        }
      ) {
        Text("Select output")
      }
    }

    Button(onClick = {
      val service = OCRService.getInstance()
      val imagesBoxes = imageBoxes + List(images.size - imageBoxes.size) { listOf() }
      val compilation = images.zip(imagesBoxes)

      val workData = WorkData(
        1,
        author.value,
        compilation.mapIndexed { index, (imagePathInfo, imageBoxes) ->
          ImageData(
            index = index,
            imageName = imagePathInfo.path.name,
            image = null,
            blockData = imageBoxes.map { box ->
              BlockData(
                blockType = BlockType.Rectangle(box.box.x, box.box.y, box.box.sizeX, box.box.sizeY),
                text = box.text,
                settings = null
              )
            },
            settings = settings.value,
          )
        }
      )

      service.workData = workData
      try {
        val path = Path.of(savePath.value)
        path.writeText(JSON.encodeToString(workData))
      } catch (e: InvalidPathException) {
        println(e)
      }
      state.value = AppStateEnum.MAIN_MENU
    }) {
      Text("Done")
    }
  }


}