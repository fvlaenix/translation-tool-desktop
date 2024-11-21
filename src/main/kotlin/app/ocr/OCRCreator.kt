package app.ocr

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import project.Project
import utils.FollowableMutableList
import utils.FontService
import utils.JSON
import utils.KotlinUtils.applyIf
import utils.ProtobufUtils
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
            box = texts.getOrNull(index)?.blockData?.map { block -> OCRBoxData(block.blockPosition, block.text) } ?: emptyList()
          )
        }
      } else {
        val images = ImageDataService.getInstance(project, ImageDataService.UNTRANSLATED).get().toList()
        val texts = TextDataService.getInstance(project, TextDataService.UNTRANSLATED).workData!!.imagesData.toMutableList()
        images.mapIndexed { index, image ->
          ImageInfoWithBox(
            imagePathInfo = image,
            box = texts.getOrNull(index)?.blockData?.map { block -> OCRBoxData(block.blockPosition, block.text) } ?: emptyList()
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

  val boxes =
    FollowableMutableList(mutableStateListOf<OCRBoxData>())
      .apply {
        follow { newList ->
          imageInfoWithBox.value = imageInfoWithBox.value!!.copy(box = newList.toList())
        }
      }
      .apply { addAll(imageInfoWithBox.value!!.box) }

  Row(
    modifier = Modifier
  ) {
    Column(modifier = Modifier.fillMaxWidth(0.7f)) {
      SimpleLoadedImageDisplayer(Modifier.fillMaxSize(0.9f), image, boxes, selectedBoxIndex)
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
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused by interactionSource.collectIsFocusedAsState()
        if (isFocused) selectedBoxIndex.value = index

        Row(modifier = Modifier.fillMaxWidth().applyIf(selectedBoxIndex.value == index) { it.border(1.dp, Color.Cyan) }) {
          TextField(
            value = box.text,
            modifier = Modifier.fillMaxSize(0.9f).padding(10.dp),
            onValueChange = { boxes[index] = box.copy(text = it) },
            interactionSource = interactionSource
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
  dataList: List<ImageInfoWithBox>,
  project: Project?
) {
  val image: MutableState<BufferedImage?> = remember { mutableStateOf(null) }
  // TODO make font take better
  val settings: MutableState<BlockSettings> =
    remember { mutableStateOf(BlockSettings(FontService.getInstance().getDefaultFont())) }
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
            settings = settings.value,
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
    }) {
      Text("Done")
    }
  }


}