package app.ocr

import translation.data.BlockPosition

data class OCRBoxData(
  val box: BlockPosition,
  val text: String
)