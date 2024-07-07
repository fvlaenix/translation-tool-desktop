package app

import androidx.compose.animation.AnimatedContent
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
      modifier = Modifier
        .padding(8.dp),
      targetState = state
    ) { targetState ->
      when (targetState.value) {
        AppStateEnum.MAIN_MENU -> MainMenu(targetState)
        AppStateEnum.SIMPLE_VERSION -> SimpleTranslator()
        AppStateEnum.ADVANCED_VERSION -> TODO()
      }
    }
  }
}



enum class AppStateEnum {
  MAIN_MENU, SIMPLE_VERSION, ADVANCED_VERSION
}