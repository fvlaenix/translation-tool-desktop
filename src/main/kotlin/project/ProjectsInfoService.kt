package project

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import utils.JSON
import java.io.IOException
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.readText
import kotlin.io.path.writeText

class ProjectsInfoService(private val path: Path) {
  private val projects: MutableList<ProjectInfoData> = mutableListOf()
  var selectedProjectInfo: ProjectInfoData? = null

  suspend fun load() {
    projects.clear()
    val newProjects: List<ProjectInfoData> = withContext(Dispatchers.IO) {
      try {
        JSON.decodeFromString<List<ProjectInfoData>>(path.readText()).sortedByDescending { it.lastTimeChange }
      } catch (_: SerializationException) {
        // TODO
        emptyList()
      } catch (_: IllegalArgumentException) {
        // TODO
        emptyList()
      } catch (_: IOException) {
        // TODO
        emptyList()
      }

    }
    projects.addAll(newProjects)
  }

  private suspend fun save() {
    withContext(Dispatchers.IO) {
      try {
        path.writeText(JSON.encodeToString(projects.map { ProjectInfoData(it.name, it.stringPath) }))
      } catch (_: SerializationException) {
        // TODO
      } catch (_: IOException) {
        // TODO
      }
    }
  }

  suspend fun add(project: ProjectInfoData) {
    projects.add(project)
    save()
  }

  fun getProjects(): List<ProjectInfoData> = projects

  companion object {
    private val DEFAULT_PATH = Path.of("projects.json")

    private val INSTANCE = ProjectsInfoService(DEFAULT_PATH)

    fun getInstance(): ProjectsInfoService = INSTANCE
  }

  @Serializable
  data class ProjectInfoData(
    val name: String,
    val stringPath: String
  ) {
    val path: Path
      get() = Path.of(stringPath)

    val exists: Boolean
      get() = path.exists()

    val lastTimeChange: FileTime?
      get() = if (exists) path.getLastModifiedTime() else null
  }
}