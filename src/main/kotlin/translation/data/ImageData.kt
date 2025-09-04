package translation.data

import kotlinx.serialization.Serializable

/**
 * Represents an image with associated text blocks and default settings.
 */
@Serializable
data class ImageData(
  val index: Int,
  val imageName: String,
  val image: String? = null,
  val blockData: List<BlockData> = emptyList(),
  val settings: BlockSettings
)