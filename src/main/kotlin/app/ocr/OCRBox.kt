package app.ocr

import app.advanced.BoxOnImageData

data class OCRBoxData(
  val box: BoxOnImageData,
  val text: String
)