package app.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.TopBar
import app.project.ProjectListPanel
import core.navigation.NavigationController
import core.navigation.NavigationDestination
import fonts.data.FontRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Main navigation menu with buttons for different translation workflows and project management.
 */
@Composable
fun MainMenu(navigationController: NavigationController) {
  val fontRepository: FontRepository = koinInject()
  val scope = rememberCoroutineScope()

  var isFontsAdded by remember { mutableStateOf(false) }

  // Check fonts availability
  LaunchedEffect(Unit) {
    scope.launch {
      fontRepository.isFontsAdded()
        .onSuccess { hasFont -> isFontsAdded = hasFont }
        .onFailure { isFontsAdded = false }
    }
  }

  TopBar(navigationController, "Main Menu", true) {
    Row(
      modifier = Modifier.padding(16.dp)
    ) {
      Column(modifier = Modifier.fillMaxWidth(0.5f)) {
        Button(onClick = { navigationController.navigateTo(NavigationDestination.SimpleTranslator) }) {
          Text("Simple Translator")
        }
        Button(onClick = { navigationController.navigateTo(NavigationDestination.AdvancedTranslator) }) {
          Text("Advanced Translator")
        }
        Button(onClick = { navigationController.navigateTo(NavigationDestination.BatchCreator) }) {
          Text("Batch Creator")
        }
        Button(
          onClick = { navigationController.navigateTo(NavigationDestination.OCRCreator) },
          enabled = isFontsAdded
        ) {
          Text("OCR Creator")
        }
        Button(
          onClick = { navigationController.navigateTo(NavigationDestination.LoadOCRCreator) }
        ) {
          Text("Load OCR")
        }
        Button(
          onClick = { navigationController.navigateTo(NavigationDestination.TranslationCreator) }
        ) {
          Text("Translation Creator")
        }
        Button(
          onClick = { navigationController.navigateTo(NavigationDestination.EditCreator) }
        ) {
          Text("Edit creator")
        }
      }
      Column {
        ProjectListPanel(navigationController)
      }
    }
  }
}