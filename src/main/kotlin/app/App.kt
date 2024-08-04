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
import app.main.MainMenu
import app.settings.Settings
import app.simple.SimpleTranslator
import app.ocr.OCRCreator

@Composable
@Preview
fun App() {
  val state = remember { mutableStateOf(AppStateEnum.MAIN_MENU) }

  MaterialTheme {
    AnimatedContent(
      targetState = state.value,
      modifier = Modifier.fillMaxSize(),
      transitionSpec = {
        if (targetState.ordinal > initialState.ordinal) {
          slideInHorizontally { height -> height } togetherWith slideOutHorizontally { height -> -height }
        } else {
          slideInHorizontally { height -> -height } togetherWith slideOutHorizontally { height -> height }
        }.using(
          SizeTransform(clip = false)
        )
      }
    ) { targetState ->
      when (targetState) {
        AppStateEnum.MAIN_MENU -> MainMenu(state)
        AppStateEnum.SIMPLE_VERSION -> SimpleTranslator(state)
        AppStateEnum.ADVANCED_VERSION -> AdvancedTranslator(state)
        AppStateEnum.BATCH_CREATOR -> BatchCreator(state)
        AppStateEnum.TRANSLATION_CREATOR -> OCRCreator(state)
        AppStateEnum.SETTINGS -> Settings(state)
      }
    }
  }
}

enum class AppStateEnum {
  MAIN_MENU,
  SIMPLE_VERSION,
  ADVANCED_VERSION,
  BATCH_CREATOR, TRANSLATION_CREATOR, //EDIT_CREATOR,
  SETTINGS,
}