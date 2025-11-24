package project.data

import kotlinx.serialization.Serializable
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime

/**
 * Represents project metadata stored in the projects list
 */
@Serializable
data class ProjectInfo(
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

/**
 * Complete project data loaded from project.json
 */
@Serializable
data class BaseProjectData(
  val name: String,
  val data: ProjectData
)

/**
 * Base interface for different project types
 */
@Serializable
sealed interface ProjectData

/**
 * Project data for image translation projects
 */
@Serializable
data class ImagesProjectData(
  val uneditedImagesFolderName: String = "unedited",
  val cleanImagesFolderName: String = "cleaned"
) : ProjectData

/**
 * Complete project with metadata and data
 */
data class Project(
  val name: String,
  val stringPath: String,
  val data: ProjectData,
) {
  val path: Path = Path.of(stringPath)
}