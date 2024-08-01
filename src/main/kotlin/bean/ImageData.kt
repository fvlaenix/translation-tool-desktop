package bean

import kotlinx.serialization.Serializable

@Serializable
data class ImageData(
  val image: String? = null,
  val imageName: String,
  val index: Int,
  val blockData: List<BlockData> = emptyList(),
  val settings: BlockSettings
)