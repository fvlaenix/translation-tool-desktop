package app.ocr

import bean.WorkData

class OCRService private constructor() {
  var workData: WorkData? = null

  companion object {
    private val DEFAULT = OCRService()

    fun getInstance(): OCRService = DEFAULT
  }
}