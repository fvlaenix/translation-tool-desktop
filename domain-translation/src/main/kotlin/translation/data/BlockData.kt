package translation.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Represents a text block with position, content and formatting settings.
 */
@Serializable
@Immutable
data class BlockData(
  val blockPosition: BlockPosition,
  val text: String,
  val settings: BlockSettings? = null
)