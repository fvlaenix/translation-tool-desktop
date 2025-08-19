import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import app.App
import core.utils.FontService
import project.ProjectsInfoService

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
