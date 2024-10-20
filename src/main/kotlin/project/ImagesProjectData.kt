package project

import kotlinx.serialization.Serializable

@Serializable
data class ImagesProjectData(
  val uneditedImagesFolderName: String = "unedited",
  val postfixTextDataFileName: String = "-text.json",
  val cleanImagesFolderName: String = "cleaned"
): ProjectData