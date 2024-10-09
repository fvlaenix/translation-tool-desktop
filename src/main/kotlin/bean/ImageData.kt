package bean

import kotlinx.serialization.Serializable

@Serializable
data class ImageData(
  val index: Int,
  val imageName: String,
  val image: String? = null,
  val blockData: List<BlockData> = emptyList(),
  val settings: BlockSettings
)