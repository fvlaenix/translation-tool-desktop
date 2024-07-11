package app

import androidx.compose.animation.*
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import app.advanced.AdvancedTranslator
import app.main.MainMenu
import app.settings.Settings
import app.simple.SimpleTranslator

@Composable
@Preview
fun App() {
  val state = remember { mutableStateOf(AppStateEnum.MAIN_MENU) }

  MaterialTheme {
    AnimatedContent(
      targetState = state.value,
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
        AppStateEnum.SETTINGS -> Settings(state)
      }
    }
  }
}

enum class AppStateEnum {
  MAIN_MENU, SIMPLE_VERSION, ADVANCED_VERSION, SETTINGS
}