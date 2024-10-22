package project

import kotlinx.serialization.Serializable

@Serializable
data class ImagesProjectData(
  val uneditedImagesFolderName: String = "unedited",
  val cleanImagesFolderName: String = "cleaned"
) : ProjectData