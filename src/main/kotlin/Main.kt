import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import app.App
import utils.FontService

fun main() = application {
  FontService.getInstance()
  Window(
    onCloseRequest = ::exitApplication,
    title = "Translator"
  ) {
    App()
  }
}
