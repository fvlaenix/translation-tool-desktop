package app

import androidx.compose.animation.*
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.advanced.AdvancedTranslator
import app.batch.BatchCreator
import app.fonts.FontsSettings
import app.main.MainMenu
import app.ocr.OCRCreator
import app.settings.Settings
import app.simple.SimpleTranslator
import app.translation.LoadOCR
import utils.AnimatedContentUtils.horizontalSpec

@Composable
@Preview
fun App() {
  val state = remember { mutableStateOf(AppStateEnum.MAIN_MENU) }

  MaterialTheme {
    AnimatedContent(
      targetState = state.value,
      modifier = Modifier.fillMaxSize(),
      transitionSpec = horizontalSpec<AppStateEnum>()
    ) { targetState ->
      when (targetState) {
        AppStateEnum.MAIN_MENU -> MainMenu(state)

        AppStateEnum.SIMPLE_VERSION -> SimpleTranslator(state)

        AppStateEnum.ADVANCED_VERSION -> AdvancedTranslator(state)

        AppStateEnum.BATCH_CREATOR -> BatchCreator(state)
        AppStateEnum.OCR_CREATOR -> OCRCreator(state)
        AppStateEnum.LOAD_OCR_CREATOR -> LoadOCR(state)

        AppStateEnum.SETTINGS -> Settings(state)
        AppStateEnum.FONT_SETTINGS -> FontsSettings(state)
      }
    }
  }
}

enum class AppStateEnum {
  MAIN_MENU,
  SIMPLE_VERSION,
  ADVANCED_VERSION,
  BATCH_CREATOR, OCR_CREATOR, LOAD_OCR_CREATOR, //EDIT_CREATOR,
  SETTINGS, FONT_SETTINGS
}