import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import app.App
import project.ProjectsInfoService
import utils.FontService

suspend fun main() {
  FontService.getInstance().load()
  ProjectsInfoService.getInstance().load()
  application {
    Window(
      onCloseRequest = ::exitApplication,
      title = "Translator"
    ) {
      App()
    }
  }
}
