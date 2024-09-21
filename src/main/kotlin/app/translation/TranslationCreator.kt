package app.translation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import app.AppStateEnum
import app.TopBar
import app.ocr.OCRService
import app.utils.openFileDialog
import bean.BlockData
import bean.ImageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import utils.JSON
import utils.ProtobufUtils
import java.awt.FileDialog
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.writeText

@Composable
fun TranslationCreator(state: MutableState<AppStateEnum>) {
  val ocrCounter = remember { AtomicInteger(0) }

  var index by remember { mutableStateOf(-1) }

  val untranslatedImageDatas = remember {
    val workData = OCRService.getInstance().workData ?: throw IllegalStateException("Work data is null")
    mutableStateListOf(*workData.imagesData.toTypedArray())
  }
  val translatedImageDatas = remember {
    val workData = OCRService.getInstance().workData ?: throw IllegalStateException("Work data is null")
    val imagesData = workData.imagesData.map { imageData: ImageData ->
      ImageData(
        index = imageData.index,
        imageName = imageData.imageName,
        image = imageData.image,
        blockData = imageData.blockData.map { blockData: BlockData ->
          BlockData(
            blockType = blockData.blockType,
            text = "",
            settings = blockData.settings,
          )
        },
        settings = imageData.settings
      )
    }
    mutableStateListOf(*imagesData.toTypedArray())
  }

  val currentUntranslatedData = remember { mutableStateOf(emptyList<BlockData>()) }
  val currentTranslatedData = remember { mutableStateOf(emptyList<BlockData>()) }

  fun setIndex(newIndex: Int) {
    if (newIndex == -1) index = -1
    else {
      // save result
      if (index in translatedImageDatas.indices) {
        translatedImageDatas[index] = translatedImageDatas[index].copy(
          blockData = currentTranslatedData.value
        )
      }

      // clean
      currentTranslatedData.value = emptyList()
      currentUntranslatedData.value = emptyList()

      // set new index
      index = newIndex

      if (newIndex !in translatedImageDatas.indices) return

      // show new
      currentUntranslatedData.value = untranslatedImageDatas[index].blockData
      currentTranslatedData.value = translatedImageDatas[index].blockData
    }
  }

  fun isWorkingInProgress(): Boolean = ocrCounter.get() > 0

  TopBar(state, "Translation creator",
    bottomBar = {
      BottomAppBar {
        Row {
          Button(onClick = { setIndex(index - 1) }, enabled = index > 0 && !isWorkingInProgress()) { Text("Previous") }
          Button(onClick = { setIndex(index + 1) }, enabled = index + 1 < untranslatedImageDatas.size && !isWorkingInProgress()) { Text("Next") }
          Button(onClick = { setIndex(untranslatedImageDatas.size) }, enabled = index != untranslatedImageDatas.size && !isWorkingInProgress()) { Text("Done") }
        }
      }
    }
  ) {
    when (index) {
      -1 -> Text("Click next if you want to continue")
      untranslatedImageDatas.size -> TranslatorCreatorFinal(state, translatedImageDatas)
      else -> TranslatorCreatorStep(ocrCounter, currentUntranslatedData, currentTranslatedData)
    }
  }
}

@Composable
private fun TranslatorCreatorStep(
  ocrCounter: AtomicInteger,
  currentUntranslatedData: MutableState<List<BlockData>>,
  currentTranslatedData: MutableState<List<BlockData>>
) {
  val coroutineScope = rememberCoroutineScope()
  Column {
    val currentData = currentUntranslatedData.value.zip(currentTranslatedData.value)
    currentData.forEachIndexed { index, (untranslatedData, translatedData) ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(4.dp)
          .border(1.dp, MaterialTheme.colors.primary)
      ) {
        TextField(
          value = untranslatedData.text,
          onValueChange = {},
          modifier = Modifier.weight(1f),
          enabled = false
        )
        Button(
          onClick = {
            ocrCounter.incrementAndGet()
            coroutineScope.launch(Dispatchers.IO) {
              try {
                val translation = ProtobufUtils.getTranslation(untranslatedData.text)
                currentTranslatedData.value = currentTranslatedData.value.toMutableList().apply {
                  set(index, translatedData.copy(text = translation))
                }
              } finally {
                ocrCounter.decrementAndGet()
              }
            }
          },
          modifier = Modifier.weight(0.1f)
        ) {
          Text("Translate")
        }
        TextField(
          value = translatedData.text,
          onValueChange = { newText ->
            currentTranslatedData.value = currentTranslatedData.value.toMutableList().apply {
              set(index, translatedData.copy(text = newText))
            }
          },
          modifier = Modifier.weight(1f)
        )
      }

    }
  }
}

@Composable
private fun TranslatorCreatorFinal(
  state: MutableState<AppStateEnum>,
  translatedImageDatas: List<ImageData>,
) {
  val savePath: MutableState<String> = remember { mutableStateOf("") }

  val parent = remember { ComposeWindow(null) }
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
      val newWorkData = OCRService.getInstance().workData!!.copy(
        imagesData = translatedImageDatas
      )
      service.workData = newWorkData
      try {
        val path = Path.of(savePath.value)
        path.writeText(JSON.encodeToString(newWorkData))
      } catch (e: InvalidPathException) {
        println(e)
      }
      state.value = AppStateEnum.MAIN_MENU
    }) {
      Text("Done")
    }
  }
}