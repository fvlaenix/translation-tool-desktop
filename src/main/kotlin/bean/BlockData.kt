package bean

import kotlinx.serialization.Serializable

@Serializable
data class BlockData(
  val blockType: BlockType,
  val text: String,
  val settings: BlockSettings? = null
)