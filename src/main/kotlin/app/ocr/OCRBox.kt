package app.ocr

import bean.BlockPosition

data class OCRBoxData(
  val box: BlockPosition,
  val text: String
)