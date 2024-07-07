package app

import androidx.compose.animation.*
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import app.main.MainMenu
import app.simple.SimpleTranslator

const val OCR_SERVICE_HOSTNAME = "192.168.31.102" // "localhost"
const val TRANSLATION_SERVICE_HOSTNAME = "192.168.31.102" // "localhost"

@Composable
@Preview
fun App() {
  val state = remember { mutableStateOf(AppStateEnum.MAIN_MENU) }

  MaterialTheme {
    AnimatedContent(
      targetState = state,
    ) { targetState ->
      when (targetState.value) {
        AppStateEnum.MAIN_MENU -> MainMenu(targetState)
        AppStateEnum.SIMPLE_VERSION -> SimpleTranslator(targetState)
        AppStateEnum.ADVANCED_VERSION -> TODO()
      }
    }
  }
}

enum class AppStateEnum {
  MAIN_MENU, SIMPLE_VERSION, ADVANCED_VERSION
}