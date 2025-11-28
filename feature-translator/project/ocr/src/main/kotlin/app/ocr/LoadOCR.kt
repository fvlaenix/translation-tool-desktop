package app.ocr

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import app.TopBar
import app.utils.openFileDialog
import core.navigation.NavigationController
import core.navigation.NavigationDestination
import core.utils.JSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import translation.data.WorkData
import translation.data.WorkDataRepository
import java.io.IOException
import java.nio.file.InvalidPathException
import java.nio.file.Path
import kotlin.io.path.notExists
import kotlin.io.path.readText

@Composable
fun LoadOCR(navigationController: NavigationController) {
  val parent = remember { ComposeWindow(null) }
  val scope = rememberCoroutineScope()
  var isLoading by remember { mutableStateOf(false) }
  val workDataRepository: WorkDataRepository = koinInject()

  val file = remember { mutableStateOf("") }
  val error = remember { mutableStateOf<String?>(null) }

  TopBar(
    navigationController, "Load OCR",
    bottomBar = {
      BottomAppBar {
        Row {
          Button(onClick = {
            isLoading = true
            error.value = null
            scope.launch(Dispatchers.IO) {
              val path = try {
                Path.of(file.value)
              } catch (_: InvalidPathException) {
                withContext(Dispatchers.Main) {
                  error.value = "Invalid path"
                  isLoading = false
                }
                return@launch
              }
              if (path.notExists()) {
                withContext(Dispatchers.Main) {
                  error.value = "File not found"
                  isLoading = false
                }
                return@launch
              }
              val text = try {
                path.readText()
              } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                  error.value = "Error while reading file"
                  isLoading = false
                }
                return@launch
              }
              val workData = try {
                JSON.decodeFromString<WorkData>(text)
              } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                  error.value = "Error while parsing file: ${e.message}"
                  isLoading = false
                }
                return@launch
              }

              workDataRepository.setWorkData(workData).fold(
                onSuccess = {
                  withContext(Dispatchers.Main) {
                    navigationController.navigateTo(NavigationDestination.MainMenu)
                  }
                },
                onFailure = { exception ->
                  withContext(Dispatchers.Main) {
                    error.value = "Error saving work data: ${exception.message}"
                  }
                }
              )
              withContext(Dispatchers.Main) { isLoading = false }
            }
          }, enabled = !isLoading) { Text("Load") }
        }
        if (isLoading) {
          CircularProgressIndicator()
        }
      }
    }
  ) {
    Column {
      Row {
        Text("File: ")
        TextField(
          value = file.value,
          onValueChange = { file.value = it },
          label = { Text("Enter file path") },
          singleLine = true
        )
        Button(onClick = {
          val files = openFileDialog(parent, "Select file", false)
          file.value = files.firstOrNull()?.absolutePath ?: return@Button
        }) { Text("Select") }
      }
      Row {
        if (error.value != null) {
          Text(
            text = error.value ?: "",
            color = Color.Red,
            fontSize = 18.sp
          )
        }
      }
    }
  }
}