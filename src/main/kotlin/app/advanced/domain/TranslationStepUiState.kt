package app.advanced.domain

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.IntSize
import app.advanced.TranslationInfo

@Immutable
data class TranslationStepUiState(
  val translationInfos: List<TranslationInfo> = emptyList(),
  val parentSize: IntSize = IntSize.Zero,
  val isProcessing: Boolean = false,
  val processingIndex: Int? = null,
  val processingType: ProcessingType? = null
)

enum class ProcessingType {
  OCR, TRANSLATION
}