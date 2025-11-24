package app.editor.domain

import androidx.compose.runtime.Immutable
import translation.data.BlockData
import translation.data.BlockPosition
import translation.data.BlockSettings
import java.awt.image.BufferedImage

@Immutable
data class EditCreatorStepUiState(
  val image: BufferedImage? = null,
  val boxes: List<BlockData> = emptyList(),
  val selectedBoxIndex: Int? = null,
  val currentSettings: BlockSettings? = null,
  val currentShape: BlockPosition.Shape? = null,
  val operationNumber: Int = 0,
  val isGenerating: Boolean = false,
  val validationErrors: Map<String, String> = emptyMap()
)