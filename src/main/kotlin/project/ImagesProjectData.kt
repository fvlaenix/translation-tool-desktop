package project

import kotlinx.serialization.Serializable

@Serializable
class ImagesProjectData(
  private val uneditedImagesFolderName: String = "unedited",
  private val ocrFileName: String = "ocr.json",
  private val cleanImagesFolderName: String = "cleaned",
  private val translatedFileName: String = "translate.json",
  private val editedImagesFolderName: String = "edited"
): ProjectData