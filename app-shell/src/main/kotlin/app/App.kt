package app

import androidx.compose.animation.AnimatedContent
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.advanced.AdvancedTranslator
import app.batch.ImageDataCreator
import app.editor.EditCreator
import app.fonts.FontsSettings
import app.main.MainMenu
import app.ocr.LoadOCR
import app.ocr.OCRCreator
import app.project.NewProjectPanel
import app.project.ProjectPanel
import app.settings.OCRSettings
import app.settings.Settings
import app.settings.TranslationSettings
import app.simple.SimpleTranslator
import app.translation.TranslationCreator
import core.error.ErrorHandler
import core.error.ErrorOverlay
import core.navigation.NavigationController
import core.navigation.NavigationDestination
import core.utils.AnimatedContentUtils.horizontalSpec
import org.koin.compose.koinInject

/**
 * Main Compose UI root. Handles navigation between screens using animated content and manages error overlay.
 */
@Composable
@Preview
fun App() {
  val navigationController: NavigationController = koinInject()
  val errorHandler: ErrorHandler = koinInject()

  val currentDestination by navigationController.currentDestination

  // Cleanup error handler when App is disposed
  DisposableEffect(errorHandler) {
    onDispose {
      errorHandler.cleanup()
    }
  }

  MaterialTheme {
    ErrorOverlay(errorHandler = errorHandler) {
      AnimatedContent(
        targetState = currentDestination,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = horizontalSpec<NavigationDestination>()
      ) { targetState ->
        when (targetState) {
          NavigationDestination.MainMenu -> MainMenu(navigationController)

          NavigationDestination.SimpleTranslator -> SimpleTranslator(navigationController)

          NavigationDestination.AdvancedTranslator -> AdvancedTranslator(navigationController)

          NavigationDestination.BatchCreator -> ImageDataCreator(navigationController)
          NavigationDestination.OCRCreator -> OCRCreator(navigationController)
          NavigationDestination.LoadOCRCreator -> LoadOCR(navigationController)
          NavigationDestination.TranslationCreator -> TranslationCreator(navigationController)
          NavigationDestination.EditCreator -> EditCreator(navigationController)

          NavigationDestination.NewProject -> NewProjectPanel(navigationController)
          NavigationDestination.Project -> ProjectPanel(navigationController)

          NavigationDestination.Settings -> Settings(navigationController)
          NavigationDestination.FontSettings -> FontsSettings(navigationController)
          NavigationDestination.TranslationSettings -> TranslationSettings(navigationController)
          NavigationDestination.OCRSettings -> OCRSettings(navigationController)
        }
      }
    }
  }
}
