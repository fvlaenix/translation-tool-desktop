import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import app.App
import core.MangaTranslationApplication
import core.utils.FontService
import project.ProjectsInfoService

suspend fun main() {
  val app = MangaTranslationApplication()

  try {
    // Initialize DI first
    app.initialize()

    // Then load services
    FontService.getInstance().load()
    ProjectsInfoService.getInstance().load()

    application {
      Window(
        onCloseRequest = {
          app.shutdown()
          exitApplication()
        },
        title = "Translator"
      ) {
        App()
      }
    }
  } catch (e: Exception) {
    println("Application startup failed: ${e.message}")
    e.printStackTrace()
    app.shutdown()
  }
}