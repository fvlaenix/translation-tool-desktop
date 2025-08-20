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
import app.settings.Settings
import app.simple.SimpleTranslator
import app.translation.TranslationCreator
import core.error.ErrorHandler
import core.error.ErrorOverlay
import core.navigation.NavigationController
import core.utils.AnimatedContentUtils.horizontalSpec
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {
  val navigationController: NavigationController = koinInject()
  val errorHandler: ErrorHandler = koinInject()

  val currentAppState by navigationController.currentAppState

  // Cleanup error handler when App is disposed
  DisposableEffect(errorHandler) {
    onDispose {
      errorHandler.cleanup()
    }
  }

  MaterialTheme {
    ErrorOverlay(errorHandler = errorHandler) {
      AnimatedContent(
        targetState = currentAppState,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = horizontalSpec<AppStateEnum>()
      ) { targetState ->
        when (targetState) {
          AppStateEnum.MAIN_MENU -> MainMenu(navigationController)

          AppStateEnum.SIMPLE_VERSION -> SimpleTranslator(navigationController)

          AppStateEnum.ADVANCED_VERSION -> AdvancedTranslator(navigationController)

          AppStateEnum.BATCH_CREATOR -> ImageDataCreator(navigationController)
          AppStateEnum.OCR_CREATOR -> OCRCreator(navigationController)
          AppStateEnum.LOAD_OCR_CREATOR -> LoadOCR(navigationController)
          AppStateEnum.TRANSLATION_CREATOR -> TranslationCreator(navigationController)
          AppStateEnum.EDIT_CREATOR -> EditCreator(navigationController)

          AppStateEnum.NEW_PROJECT -> NewProjectPanel(navigationController)
          AppStateEnum.PROJECT -> ProjectPanel(navigationController)

          AppStateEnum.SETTINGS -> Settings(navigationController)
          AppStateEnum.FONT_SETTINGS -> FontsSettings(navigationController)
        }
      }
    }
  }
}

enum class AppStateEnum {
  MAIN_MENU,

  SIMPLE_VERSION,
  ADVANCED_VERSION,
  BATCH_CREATOR, OCR_CREATOR, LOAD_OCR_CREATOR, TRANSLATION_CREATOR, EDIT_CREATOR,

  NEW_PROJECT, PROJECT,

  SETTINGS, FONT_SETTINGS
}