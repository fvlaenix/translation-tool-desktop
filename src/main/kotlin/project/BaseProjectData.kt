package project

import kotlinx.serialization.Serializable

@Serializable
class BaseProjectData(
  val name: String,
  val data: ProjectData
)

// TODO rename
@Serializable
sealed interface ProjectData