package app.fonts

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import app.utils.openFileDialog
import core.navigation.NavigationController
import core.navigation.NavigationDestination
import fonts.domain.FontViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.nio.file.Path

@Composable
fun FontsSettings(navigationController: NavigationController) {
  val fontViewModel: FontViewModel = koinInject()
  val fontSettingsState = mutableStateOf(SettingsState.MAIN_MENU)

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Font Settings") },
        navigationIcon = {
          IconButton(onClick = { navigationController.navigateTo(NavigationDestination.MainMenu) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Menu")
          }
        }
      )
    }
  ) {
    AnimatedContent(
      targetState = fontSettingsState.value,
      modifier = Modifier.padding(10.dp)
    ) { targetState ->
      when (targetState) {
        SettingsState.MAIN_MENU -> MainSettings(fontSettingsState, fontViewModel)
        SettingsState.ADD_FONT -> AddFontSettings(fontSettingsState, fontViewModel)
      }
    }
  }
}

@Composable
private fun MainSettings(state: MutableState<SettingsState>, fontViewModel: FontViewModel) {
  val availableFonts by fontViewModel.availableFonts
  val isLoading by fontViewModel.isLoading
  val error by fontViewModel.error

  // Load fonts when this composable is first displayed
  LaunchedEffect(Unit) {
    fontViewModel.loadFonts()
  }

  Column(
    modifier = Modifier.verticalScroll(rememberScrollState())
  ) {
    if (isLoading) {
      CircularProgressIndicator()
      Text("Loading fonts...")
    } else {
      availableFonts.forEach { fontInfo ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colors.primary)
        ) {
          Column(
            modifier = Modifier.padding(10.dp)
          ) {
            Text(
              text = "Name: ${fontInfo.name}",
              style = MaterialTheme.typography.h6
            )
            Text(
              text = "Font Name: ${fontInfo.font.fontName}",
              style = MaterialTheme.typography.h6
            )
            Text(
              text = "Path: ${fontInfo.path}",
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

      Button(onClick = {
        fontViewModel.refreshFonts()
      }) {
        Text("Refresh Fonts")
      }

      error?.let { errorMessage ->
        Text(
          text = errorMessage,
          color = MaterialTheme.colors.error,
          modifier = Modifier.padding(top = 8.dp)
        )
      }
    }
  }
}

@Composable
private fun AddFontSettings(state: MutableState<SettingsState>, fontViewModel: FontViewModel) {
  val parent = remember { ComposeWindow(null) }
  val fontName = remember { mutableStateOf("") }
  val fontPath = remember { mutableStateOf("") }
  val scope = rememberCoroutineScope()

  val isProcessing by fontViewModel.isProcessing
  val validationErrors by fontViewModel.validationErrors
  val error by fontViewModel.error

  Column {
    Row {
      Text("Name: ")
      TextField(
        value = fontName.value,
        onValueChange = { fontName.value = it },
        isError = validationErrors.containsKey("name"),
        enabled = !isProcessing
      )
    }

    validationErrors["name"]?.let { errorMessage ->
      Text(
        text = errorMessage,
        color = MaterialTheme.colors.error,
        modifier = Modifier.padding(start = 8.dp)
      )
    }

    Row {
      Text("Path: ")
      TextField(
        value = fontPath.value,
        onValueChange = { fontPath.value = it },
        isError = validationErrors.containsKey("path"),
        enabled = !isProcessing
      )
      Button(
        onClick = {
          val files = openFileDialog(parent, "Choose font", false)
          val file = files.firstOrNull() ?: return@Button
          fontPath.value = file.absolutePath
        },
        enabled = !isProcessing
      ) {
        Text("Select")
      }
    }

    validationErrors["path"]?.let { errorMessage ->
      Text(
        text = errorMessage,
        color = MaterialTheme.colors.error,
        modifier = Modifier.padding(start = 8.dp)
      )
    }

    if (isProcessing) {
      Row {
        CircularProgressIndicator()
        Text("Processing...")
      }
    }

    Row {
      Button(
        onClick = {
          scope.launch {
            fontViewModel.addFont(fontName.value, Path.of(fontPath.value))
            // If successful (no validation errors), go back to main menu
            if (validationErrors.isEmpty() && error == null) {
              state.value = SettingsState.MAIN_MENU
            }
          }
        },
        enabled = !isProcessing && fontName.value.isNotBlank() && fontPath.value.isNotBlank()
      ) {
        Text("Add Font")
      }
      Button(
        onClick = {
          state.value = SettingsState.MAIN_MENU
        },
        enabled = !isProcessing
      ) {
        Text("Cancel")
      }
    }

    error?.let { errorMessage ->
      Text(
        text = errorMessage,
        color = MaterialTheme.colors.error,
        modifier = Modifier.padding(top = 8.dp)
      )
    }
  }
}

private enum class SettingsState {
  MAIN_MENU, ADD_FONT
}