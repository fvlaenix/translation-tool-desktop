import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import app.App
import core.MangaTranslationApplication
import fonts.data.FontRepository
import org.koin.core.context.GlobalContext
import project.data.ProjectRepository

suspend fun main() {
  val app = MangaTranslationApplication()

  try {
    // Initialize DI first
    app.initialize()

    // Load fonts through repository
    val fontRepository = GlobalContext.get().get<FontRepository>()
    fontRepository.loadFonts().getOrElse {
      println("Failed to load fonts: ${it.message}")
    }

    // Load projects through repository
    val projectRepository = GlobalContext.get().get<ProjectRepository>()
    projectRepository.loadProjects().getOrElse {
      println("Failed to load projects: ${it.message}")
      // Continue with empty projects list
    }

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