package app.fonts

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import app.AppStateEnum
import app.utils.openFileDialog
import kotlinx.coroutines.launch
import utils.AnimatedContentUtils.horizontalSpec
import utils.FontService
import java.awt.Font
import java.nio.file.Path

@Composable
fun FontsSettings(state: MutableState<AppStateEnum>) {
  val fontService = FontService.getInstance()
  val fontSettingsState = mutableStateOf(SettingsState.MAIN_MENU)

  Scaffold(
    modifier = Modifier
      .fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Settings") },
        navigationIcon = {
          IconButton(onClick = { state.value = AppStateEnum.MAIN_MENU }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Menu")
          } }
      )
    }
  ) {
    AnimatedContent(
      targetState = fontSettingsState.value,
      modifier = Modifier.padding(10.dp),
      transitionSpec = horizontalSpec<SettingsState>()
    ) { targetState ->
      when (targetState) {
        SettingsState.MAIN_MENU -> MainSettings(fontSettingsState, fontService)
        SettingsState.ADD_FONT -> AddFontSettings(fontSettingsState, fontService)
      }
    }
  }
}

@Composable
private fun MainSettings(state: MutableState<SettingsState>, fontsService: FontService) {
  val fontsState = fontsService.getMutableState()

  Column(
    modifier = Modifier.verticalScroll(rememberScrollState())
  ) {
    fontsState
      .forEach { (name: String, path: Path, font: Font) ->
        Row(
          modifier = Modifier.fillMaxWidth()
            .border(1.dp, MaterialTheme.colors.primary)
        ) {
          Column(
            modifier = Modifier
              .padding(10.dp)
          ) {
            Text(
              text = "Name: $name",
              style = MaterialTheme.typography.h6
            )
            Text(
              text = "Font Name: ${font.fontName}",
              style = MaterialTheme.typography.h6
            )
            Text(
              text = "Path: $path",
              style = MaterialTheme.typography.body2
            )
          }
        }
      }
    Button(onClick = {
      state.value = SettingsState.ADD_FONT
    }) {
      Text("Add Font")
    }
  }
}

@Composable
private fun AddFontSettings(state: MutableState<SettingsState>, fontsService: FontService) {
  val parent = remember { ComposeWindow(null) }
  val fontName = remember { mutableStateOf("") }
  val fontPath = remember { mutableStateOf("") }

  val coroutineScope = rememberCoroutineScope()
  val isFontAdding = remember { mutableStateOf(false) }

  Column {
    Row {
      Text("Name: ")
      TextField(fontName.value, onValueChange = { fontName.value = it })
    }
    Row {
      Text("Path: ")
      TextField(fontPath.value, onValueChange = { fontPath.value = it })
      Button(onClick = {
        val files = openFileDialog(parent, "Choose font", false)
        val file = files.firstOrNull() ?: return@Button
        fontPath.value = file.absolutePath
      }, enabled = !isFontAdding.value) {
        Text("Select")
      }
    }
    if (isFontAdding.value) {
      Row {
        CircularProgressIndicator()
        Text("Loading...")
      }
    }
    Row {
      Button(onClick = {
        isFontAdding.value = true
        coroutineScope.launch {
          fontsService.add(fontName.value, Path.of(fontPath.value))
          isFontAdding.value = false
          state.value = SettingsState.MAIN_MENU
        }
      }, enabled = !isFontAdding.value) {
        Text("Add Font")
      }
      Button(onClick = {
        state.value = SettingsState.MAIN_MENU
      }, enabled = !isFontAdding.value) {
        Text("Cancel")
      }
    }
  }
}

private enum class SettingsState {
  MAIN_MENU, ADD_FONT
}