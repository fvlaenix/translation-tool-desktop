package translation.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class BlockData(
  val blockPosition: BlockPosition,
  val text: String,
  val settings: BlockSettings? = null
)