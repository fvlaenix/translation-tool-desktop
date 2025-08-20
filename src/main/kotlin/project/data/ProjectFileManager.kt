package project.data

import core.base.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

class ProjectFileManager : Repository {

  suspend fun createProjectStructure(project: Project): Result<Unit> = safeCall {
    withContext(Dispatchers.IO) {
      val projectPath = project.path

      // Create main project directory if it doesn't exist
      projectPath.createDirectories()

      // Create subdirectories based on project type
      when (project.data) {
        is ImagesProjectData -> {
          createImageProjectStructure(projectPath, project.data)
        }
      }
    }
  }

  suspend fun validateProjectIntegrity(project: Project): Result<Boolean> = safeCall {
    withContext(Dispatchers.IO) {
      val projectPath = project.path

      // Check if project directory exists
      if (!projectPath.exists() || !projectPath.isDirectory()) {
        return@withContext false
      }

      // Check if project.json exists
      val projectFile = projectPath.resolve("project.json")
      if (!projectFile.exists()) {
        return@withContext false
      }

      // Additional checks based on project type
      when (project.data) {
        is ImagesProjectData -> {
          validateImageProjectStructure(projectPath, project.data)
        }
      }
    }
  }

  suspend fun getProjectSubdirectory(project: Project, subdirectoryName: String): Result<Path> = safeCall {
    val subdirectoryPath = project.path.resolve(subdirectoryName)
    withContext(Dispatchers.IO) {
      subdirectoryPath.createDirectories()
    }
    subdirectoryPath
  }

  private fun createImageProjectStructure(projectPath: Path, data: ImagesProjectData) {
    // Create image directories
    projectPath.resolve("untranslated").createDirectories()
    projectPath.resolve("cleaned").createDirectories()
    projectPath.resolve("edited").createDirectories()
  }

  private fun validateImageProjectStructure(projectPath: Path, data: ImagesProjectData): Boolean {
    return projectPath.resolve("untranslated").exists() ||
        projectPath.resolve("cleaned").exists() ||
        projectPath.resolve("edited").exists()
  }
}