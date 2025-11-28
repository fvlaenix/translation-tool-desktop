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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import org.koin.compose.koinInject
import project.data.Project
import project.data.TextDataRepository
import project.data.TextType
import translation.data.BlockData
import translation.data.ImageData
import translation.data.TranslationRepository
import translation.data.WorkDataRepository
import java.awt.FileDialog
import java.nio.file.InvalidPathException
import java.nio.file.Path
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
  ocrCounter: MutableState<Int>,
  translationData: MutableState<TranslationData?>
) {
  val coroutineScope = rememberCoroutineScope()
  val translatorRepository: TranslationRepository = koinInject()

  var errorMessage by remember { mutableStateOf<String?>(null) }

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
    errorMessage?.let { error ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(8.dp)
          .border(1.dp, MaterialTheme.colors.error)
          .padding(8.dp)
      ) {
        Text(
          text = error,
          color = MaterialTheme.colors.error,
          modifier = Modifier.weight(1f)
        )
        Button(
          onClick = { errorMessage = null }
        ) {
          Text("Dismiss")
        }
      }
    }

    val currentData = translationData.value!!.untranslatedData.blockData
      .zip(translationData.value!!.translatedData.blockData)

    Button(
      onClick = {
        ocrCounter.value++
        coroutineScope.launch {
          val result = withContext(Dispatchers.IO) {
            translatorRepository.translateBatch(currentData.map { it.first.text })
          }
          result
            .onSuccess { translation ->
              translationData.value = translationData.value!!.copy(
                translatedData = translationData.value!!.translatedData.copy(
                  blockData = translationData.value!!.translatedData.blockData.zip(translation)
                    .map { (block, translationText) ->
                      block.copy(text = translationText)
                    }
                )
              )
              errorMessage = null
            }
            .onFailure { exception ->
              errorMessage = "Batch translation failed: ${exception.message}"
            }
          ocrCounter.value--
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
            ocrCounter.value++
            coroutineScope.launch {
              val result = withContext(Dispatchers.IO) {
                translatorRepository.translateText(untranslatedData.text)
              }
              result
                .onSuccess { translation ->
                  setTranslation(index, translation)
                  errorMessage = null
                }
                .onFailure { exception ->
                  errorMessage = "Translation failed: ${exception.message}"
                }
              ocrCounter.value--
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

  val savePath: MutableState<String> = remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var successMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(project) {
    if (project != null) {
      textDataRepository.getWorkDataPath(project, TextType.TRANSLATED)
        .onSuccess { path ->
          savePath.value = path.toAbsolutePath().toString()
        }
        .onFailure { exception ->
          errorMessage = "Failed to get path: ${exception.message}"
        }
    }
  }

  val parent = remember { ComposeWindow(null) }
  val scope = rememberCoroutineScope()

  Column(
    modifier = Modifier.verticalScroll(rememberScrollState())
  ) {
    errorMessage?.let { error ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(8.dp)
          .border(1.dp, MaterialTheme.colors.error)
          .padding(8.dp)
      ) {
        Text(
          text = error,
          color = MaterialTheme.colors.error,
          modifier = Modifier.weight(1f)
        )
        Button(onClick = { errorMessage = null }) {
          Text("Dismiss")
        }
      }
    }

    successMessage?.let { success ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(8.dp)
          .border(1.dp, MaterialTheme.colors.primary)
          .padding(8.dp)
      ) {
        Text(
          text = success,
          color = MaterialTheme.colors.primary,
          modifier = Modifier.weight(1f)
        )
        Button(onClick = { successMessage = null }) {
          Text("Dismiss")
        }
      }
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
          scope.launch {
            val files = withContext(Dispatchers.IO) {
              openFileDialog(parent, "Files to add", false, FileDialog.SAVE)
            }
            files.singleOrNull()?.let { savePath.value = it.absolutePath }
          }
        },
        enabled = project == null,
      ) {
        Text("Select output")
      }
    }

    Button(onClick = {
      scope.launch {
        errorMessage = null
        successMessage = null

        if (project == null) {
          workDataRepository.getWorkData()
            .onSuccess { currentWorkData ->
              if (currentWorkData == null) {
                errorMessage = "No work data found"
                return@launch
              }

              val updatedWorkData = currentWorkData.copy(
                imagesData = translationData.map { it.translatedData }
              )

              workDataRepository.setWorkData(updatedWorkData)
                .onSuccess {
                  try {
                    val path = Path.of(savePath.value)
                    path.writeText(JSON.encodeToString(updatedWorkData))
                    successMessage = "Translation saved successfully"
                    navigationController.navigateTo(NavigationDestination.MainMenu)
                  } catch (e: InvalidPathException) {
                    errorMessage = "Invalid path: ${e.message}"
                  } catch (e: Exception) {
                    errorMessage = "Failed to write file: ${e.message}"
                  }
                }
                .onFailure { exception ->
                  errorMessage = "Failed to save work data: ${exception.message}"
                }
            }
            .onFailure { exception ->
              errorMessage = "Failed to load work data: ${exception.message}"
            }
        } else {
          textDataRepository.loadWorkData(project, TextType.UNTRANSLATED)
            .onSuccess { untranslatedWorkData ->
              if (untranslatedWorkData == null) {
                errorMessage = "No untranslated work data found"
                return@launch
              }

              val updatedWorkData = untranslatedWorkData.copy(
                imagesData = translationData.map { it.translatedData }
              )

              textDataRepository.saveWorkData(project, TextType.TRANSLATED, updatedWorkData)
                .onSuccess {
                  try {
                    val path = Path.of(savePath.value)
                    path.writeText(JSON.encodeToString(updatedWorkData))
                    successMessage = "Translation saved successfully"
                    navigationController.navigateTo(NavigationDestination.MainMenu)
                  } catch (e: InvalidPathException) {
                    errorMessage = "Invalid path: ${e.message}"
                  } catch (e: Exception) {
                    errorMessage = "Failed to write file: ${e.message}"
                  }
                }
                .onFailure { exception ->
                  errorMessage = "Failed to save translated work data: ${exception.message}"
                }
            }
            .onFailure { exception ->
              errorMessage = "Failed to load untranslated work data: ${exception.message}"
            }
        }
      }
    }) {
      Text("Done")
    }
  }
}