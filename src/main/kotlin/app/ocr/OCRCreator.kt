package app.ocr

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import app.AppStateEnum
import app.batch.BatchService
import app.batch.ImagePathInfo
import app.block.BlockSettingsPanelWithPreview
import app.block.SimpleLoadedImageDisplayer
import app.utils.PagesPanel
import app.utils.openFileDialog
import bean.*
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
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.name
import kotlin.io.path.writeText

@Composable
fun OCRCreator(state: MutableState<AppStateEnum>) {
  PagesPanel<ImageInfoWithBox>(
    name = "OCR Creator",
    state = state,
    dataExtractor = {
      val images = BatchService.getInstance().get().toList()
      images.map { imagePathInfo ->
        ImageInfoWithBox(
          imagePathInfo = imagePathInfo,
          box = emptyList()
        )
      }
    },
    stepWindow = { counter, data ->
      OCRCreatorStep(counter, data)
    },
    finalWindow = { dataList ->
      OCRCreatorFinal(state, dataList)
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
  val image = remember { mutableStateOf<BufferedImage?>(imageInfoWithBox.value!!.imagePathInfo.image) }
  val boxes = remember { mutableStateListOf<OCRBoxData>().apply { addAll(imageInfoWithBox.value!!.box) } }

  Row(
    modifier = Modifier
  ) {
    Column(modifier = Modifier.fillMaxWidth(0.7f)) {
      SimpleLoadedImageDisplayer(Modifier.fillMaxSize(0.9f), image, boxes)
    }
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
      Row(modifier = Modifier.fillMaxWidth()) {
        Button(
          onClick = {
            jobCounter.incrementAndGet()
            coroutineScope.launch(Dispatchers.IO) {
              try {
                val currentOcrBoxes = ProtobufUtils.getBoxedOCR(imageInfoWithBox.value!!.imagePathInfo.image)
                boxes.clear()
                boxes.addAll(currentOcrBoxes)

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
      boxes.forEachIndexed { index, box ->
        Row(modifier = Modifier.fillMaxWidth()) {
          TextField(
            value = box.text,
            modifier = Modifier.fillMaxSize(0.9f).padding(10.dp),
            onValueChange = { boxes[index] = box.copy(text = it) }
          )
          Button(onClick = { boxes.removeAt(index) }, enabled = jobCounter.get() == 0) {
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
  dataList: List<ImageInfoWithBox>
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

      val workData = WorkData(
        1,
        author.value,
        dataList.mapIndexed { index, (imagePathInfo, imageBoxes) ->
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