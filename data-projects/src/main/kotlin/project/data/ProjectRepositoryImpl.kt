package project.data

import core.base.Repository
import core.utils.JSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Project repository implementation. Manages project loading, creation, and persistence to json file.
 */
class ProjectRepositoryImpl(
  private val projectsFilePath: String
) : ProjectRepository, Repository {

  /**
   * Loads all projects from json file, sorted by last change time.
   */
  override suspend fun loadProjects(): Result<List<ProjectInfo>> = safeCall {
    withContext(Dispatchers.IO) {
      try {
        val text = Path.of(projectsFilePath).readText()
        JSON.decodeFromString<List<ProjectInfo>>(text).sortedByDescending { it.lastTimeChange }
      } catch (_: SerializationException) {
        emptyList()
      } catch (_: IllegalArgumentException) {
        emptyList()
      } catch (_: IOException) {
        emptyList()
      }
    }
  }

  /**
   * Saves or updates project information in the projects list.
   */
  override suspend fun saveProject(project: ProjectInfo): Result<Unit> = safeCall {
    val currentProjects = loadProjects().getOrElse { emptyList() }.toMutableList()

    // Remove existing project with same path if it exists
    currentProjects.removeAll { it.stringPath == project.stringPath }

    // Add the new/updated project
    currentProjects.add(project)

    withContext(Dispatchers.IO) {
      Path.of(projectsFilePath).writeText(JSON.encodeToString(currentProjects))
    }
  }

  /**
   * Creates new project directory with initial structure and files.
   */
  override suspend fun createProject(name: String, path: String): Result<ProjectInfo> = safeCall {
    val projectPath = Path.of(path)

    withContext(Dispatchers.IO) {
      // Create project directory
      projectPath.createDirectories()

      // Create project data
      val imagesProjectData = ImagesProjectData()
      val projectData = BaseProjectData(name, imagesProjectData)

      // Write project.json
      val projectFile = projectPath.resolve("project.json")
      projectFile.writeText(JSON.encodeToString(projectData))

      // Create and save project info
      val projectInfo = ProjectInfo(name, projectPath.absolutePathString())
      saveProject(projectInfo).getOrThrow()

      projectInfo
    }
  }

  /**
   * Loads project data from project.json file in project directory.
   */
  override suspend fun getProject(projectInfo: ProjectInfo): Result<Project> = safeCall {
    withContext(Dispatchers.IO) {
      if (!projectInfo.exists) {
        throw IllegalStateException("Project directory does not exist: ${projectInfo.stringPath}")
      }

      val projectFile = projectInfo.path.resolve("project.json")
      if (!projectFile.exists()) {
        throw IllegalStateException("project.json not found in: ${projectInfo.stringPath}")
      }

      val projectData = JSON.decodeFromString<BaseProjectData>(projectFile.readText())
      Project(
        name = projectData.name,
        stringPath = projectInfo.stringPath,
        data = projectData.data
      )
    }
  }

  /**
   * Removes project from the projects list without deleting files.
   */
  override suspend fun deleteProject(projectInfo: ProjectInfo): Result<Unit> = safeCall {
    val currentProjects = loadProjects().getOrElse { emptyList() }.toMutableList()
    currentProjects.removeAll { it.stringPath == projectInfo.stringPath }

    withContext(Dispatchers.IO) {
      Path.of(projectsFilePath).writeText(JSON.encodeToString(currentProjects))
    }
  }

  /**
   * Validates that project path exists and project directory doesn't exist yet.
   */
  override suspend fun validateProjectPath(path: String, projectName: String): Result<Boolean> = safeCall {
    val basePath = Path.of(path)
    val projectPath = basePath.resolve(projectName.replace(Regex("[^a-zA-Z0-9]"), "-").lowercase())

    basePath.exists() && !projectPath.exists()
  }
}