package app.translation.domain

import androidx.compose.runtime.Immutable
import app.ocr.OCRBoxData
import java.awt.image.BufferedImage

@Immutable
data class OCRCreatorStepUiState(
  val image: BufferedImage? = null,
  val boxes: List<OCRBoxData> = emptyList(),
  val selectedBoxIndex: Int? = null,
  val operationNumber: Int = 0,
  val isReorderingEnabled: Boolean = true,
  val validationErrors: Map<String, String> = emptyMap()
)