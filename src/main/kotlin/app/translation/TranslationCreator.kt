package app.translation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import app.AppStateEnum
import app.ocr.OCRService
import app.utils.PagesPanel
import app.utils.openFileDialog
import bean.BlockData
import bean.ImageData
import bean.WorkData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import project.Project
import utils.JSON
import utils.ProtobufUtils
import java.awt.FileDialog
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.writeText

@Composable
fun TranslationCreator(state: MutableState<AppStateEnum>, project: Project? = null) {
  PagesPanel<TranslationData>(
    name = "Translation creator",
    state = state,
    dataExtractor = {
      val workData = if (project == null) {
        OCRService.getInstance().workData ?: throw IllegalStateException("Work data is null")
      } else {
        TextDataService.getInstance(project, TextDataService.UNTRANSLATED).workData
          ?: throw IllegalStateException("Work data is null")
      }
      workData.imagesData.map { imageData: ImageData ->
        TranslationData(
          untranslatedData = imageData,
          translatedData = imageData.copy(
            blockData = imageData.blockData.map { blockData: BlockData ->
              blockData.copy(
                text = ""
              )
            }
          )
        )
      }
    },
    stepWindow = { jobCounter, data ->
      TranslatorCreatorStep(jobCounter, data)
    },
    finalWindow = { translatedImageDatas ->
      TranslatorCreatorFinal(state, translatedImageDatas, project)
    },
  )
}

private data class TranslationData(
  val untranslatedData: ImageData,
  val translatedData: ImageData
)

@Composable
private fun TranslatorCreatorStep(
  ocrCounter: AtomicInteger,
  translationData: MutableState<TranslationData?>
) {
  val coroutineScope = rememberCoroutineScope()

  fun setTranslation(index: Int, text: String) {
    translationData.value = translationData.value!!.copy(
      translatedData = translationData.value!!.translatedData.copy(
        blockData = translationData.value!!.translatedData.blockData.toMutableList().apply {
          set(index, translationData.value!!.translatedData.blockData[index].copy(text = text))
        }
      )
    )
  }

  Column {
    val currentData = translationData.value!!.untranslatedData.blockData
      .zip(translationData.value!!.translatedData.blockData)
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
                setTranslation(index, translation)
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
            setTranslation(index, newText)
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
  translationData: SnapshotStateList<TranslationData>,
  project: Project?
) {
  val savePath: MutableState<String> = remember {
    val path: String = if (project != null) {
      TextDataService.getInstance(project, TextDataService.TRANSLATED).workDataPath.toAbsolutePath().toString()
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
        enabled = project == null,
      ) {
        Text("Select output")
      }
    }
    Button(onClick = {
      val newWorkData: WorkData = if (project == null) {
        val service = OCRService.getInstance()
        val newWorkData = service.workData!!.copy(
          imagesData = translationData.map { it.translatedData }
        )
        service.workData = newWorkData
        newWorkData
      } else {
        val service = TextDataService.getInstance(project, TextDataService.UNTRANSLATED)
        val newWorkData = service.workData!!.copy(
          imagesData = translationData.map { it.translatedData }
        )
        service.workData = newWorkData
        newWorkData
      }

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