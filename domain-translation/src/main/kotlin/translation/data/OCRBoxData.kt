package translation.data

data class OCRBoxData(
  val id: String,
  val box: BlockPosition,
  val text: String
)
