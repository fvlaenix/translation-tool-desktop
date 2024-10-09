package app.ocr

import app.batch.ImagePathInfo
import bean.BlockSettings
import bean.ImageData
import bean.WorkData
import kotlin.io.path.name

class OCRService {
  var workData: WorkData? = null

  companion object {
    private val DEFAULT = OCRService()

    fun getInstance(): OCRService = DEFAULT
  }
}