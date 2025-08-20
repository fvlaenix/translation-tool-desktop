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
import app.utils.PagesPanel
import app.utils.openFileDialog
import core.navigation.NavigationController
import core.navigation.NavigationDestination
import core.utils.JSON
import core.utils.ProtobufUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import org.koin.compose.koinInject
import project.data.Project
import project.data.TextDataRepository
import project.data.TextType
import translation.data.BlockData
import translation.data.ImageData
import translation.data.WorkData
import translation.data.WorkDataRepository
import java.awt.FileDialog
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.writeText

@Composable
fun TranslationCreator(navigationController: NavigationController, project: Project? = null) {
  val workDataRepository: WorkDataRepository = koinInject()
  val textDataRepository: TextDataRepository = koinInject()

  PagesPanel<TranslationData>(
    name = "Translation creator",
    navigationController = navigationController,
    dataExtractor = {
      if (project == null) {
        val workData = workDataRepository.getWorkData().getOrNull()
          ?: throw IllegalStateException("Work data is null")
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
      } else {
        val untranslatedWorkData = textDataRepository.loadWorkData(project, TextType.UNTRANSLATED).getOrNull()
          ?: throw IllegalStateException("Untranslated work data is null")

        val translatedWorkData = textDataRepository.loadWorkData(project, TextType.TRANSLATED).getOrNull()
        val translatedImagesData =
          translatedWorkData?.imagesData ?: MutableList(untranslatedWorkData.imagesData.size) { null }

        untranslatedWorkData.imagesData.zip(translatedImagesData).map { (untranslatedImageData, translatedImageData) ->
          TranslationData(
            untranslatedData = untranslatedImageData,
            translatedData = translatedImageData ?: untranslatedImageData.copy(
              blockData = untranslatedImageData.blockData.map { blockData ->
                blockData.copy(text = "")
              }
            )
          )
        }
      }
    },
    stepWindow = { jobCounter, data ->
      TranslatorCreatorStep(jobCounter, data)
    },
    finalWindow = { translatedImageDatas ->
      TranslatorCreatorFinal(navigationController, translatedImageDatas, project)
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
    Button(
      onClick = {
        ocrCounter.incrementAndGet()
        coroutineScope.launch(Dispatchers.IO) {
          try {
            val translation = ProtobufUtils.getTranslation(currentData.map { it.first.text })
            translationData.value = translationData.value!!.copy(
              translatedData = translationData.value!!.translatedData.copy(
                blockData = translationData.value!!.translatedData.blockData.zip(translation)
                  .map { (block, translation) ->
                    block.copy(text = translation)
                  }
              )
            )
          } finally {
            ocrCounter.decrementAndGet()
          }
        }
      }
    ) {
      Text("Translate All")
    }
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
  navigationController: NavigationController,
  translationData: SnapshotStateList<TranslationData>,
  project: Project?
) {
  val workDataRepository: WorkDataRepository = koinInject()
  val textDataRepository: TextDataRepository = koinInject()

  val savePath: MutableState<String> = remember {
    mutableStateOf("")
  }

  LaunchedEffect(project) {
    if (project != null) {
      // TODO resolve getOrThrow somehow
      val path = textDataRepository.getWorkDataPath(project, TextType.TRANSLATED).getOrThrow()
      savePath.value = path.toAbsolutePath().toString()
    }
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
      scope.launch {
        val newWorkData: WorkData = if (project == null) {
          val currentWorkData = workDataRepository.getWorkData().getOrNull()
            ?: throw IllegalStateException("No work data found")

          val updatedWorkData = currentWorkData.copy(
            imagesData = translationData.map { it.translatedData }
          )

          workDataRepository.setWorkData(updatedWorkData).fold(
            onSuccess = { /* Success */ },
            onFailure = { exception ->
              println("Error saving work data: ${exception.message}")
            }
          )
          updatedWorkData
        } else {
          // Replace TextDataService with TextDataRepository
          val untranslatedWorkData = textDataRepository.loadWorkData(project, TextType.UNTRANSLATED).getOrNull()
            ?: throw IllegalStateException("No untranslated work data found")

          val updatedWorkData = untranslatedWorkData.copy(
            imagesData = translationData.map { it.translatedData }
          )

          textDataRepository.saveWorkData(project, TextType.TRANSLATED, updatedWorkData).fold(
            onSuccess = { /* Success */ },
            onFailure = { exception ->
              println("Error saving translated work data: ${exception.message}")
            }
          )
          updatedWorkData
        }

        try {
          val path = Path.of(savePath.value)
          path.writeText(JSON.encodeToString(newWorkData))
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