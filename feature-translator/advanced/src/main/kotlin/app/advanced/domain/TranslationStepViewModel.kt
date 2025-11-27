package app.advanced.domain

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.IntSize
import app.advanced.TranslationInfo
import core.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import translation.data.OCRRepository
import translation.data.TranslationRepository

/**
 * View model for advanced translation step. Manages per-box OCR and translation operations.
 */
class TranslationStepViewModel(
  val ocrRepository: OCRRepository,
  val translationRepository: TranslationRepository
) : BaseViewModel() {

  private val _uiState = mutableStateOf(TranslationStepUiState())
  val uiState: State<TranslationStepUiState> = _uiState

  fun setTranslationInfos(infos: List<TranslationInfo>) {
    _uiState.value = _uiState.value.copy(translationInfos = infos)
  }

  fun updateParentSize(size: IntSize) {
    _uiState.value = _uiState.value.copy(parentSize = size)
  }

  /**
   * Processes OCR for specific text box and appends result to existing text.
   */
  fun processOCRForInfo(index: Int) {
    val info = _uiState.value.translationInfos.getOrNull(index) ?: return

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(
        isProcessing = true,
        processingIndex = index,
        processingType = ProcessingType.OCR
      )

      val result = withContext(Dispatchers.IO) {
        ocrRepository.processImage(info.subImage)
      }

      result.onSuccess { ocrResult ->
        updateOcrText(index, ocrResult)
        clearError()
      }
        .onFailure { exception ->
          setError("OCR processing failed: ${exception.message}")
        }

      _uiState.value = _uiState.value.copy(
        isProcessing = false,
        processingIndex = null,
        processingType = null
      )
    }
  }

  /**
   * Translates OCR text for specific box via external service.
   */
  fun translateInfo(index: Int) {
    val info = _uiState.value.translationInfos.getOrNull(index) ?: return
    if (info.ocr.isBlank()) return

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(
        isProcessing = true,
        processingIndex = index,
        processingType = ProcessingType.TRANSLATION
      )

      val result = withContext(Dispatchers.IO) {
        translationRepository.translateText(info.ocr)
      }

      result.onSuccess { translationResult ->
        updateTranslationText(index, translationResult)
        clearError()
      }
        .onFailure { exception ->
          setError("Translation failed: ${exception.message}")
        }

      _uiState.value = _uiState.value.copy(
        isProcessing = false,
        processingIndex = null,
        processingType = null
      )
    }
  }

  fun updateOcrText(index: Int, text: String) {
    val currentInfos = _uiState.value.translationInfos.toMutableList()
    if (index in currentInfos.indices) {
      val info = currentInfos[index]
      val previous = if (info.ocr.isBlank()) "" else info.ocr + "\n\n"
      currentInfos[index] = info.copy(ocr = previous + text)
      _uiState.value = _uiState.value.copy(translationInfos = currentInfos)
    }
  }

  fun updateTranslationText(index: Int, text: String) {
    val currentInfos = _uiState.value.translationInfos.toMutableList()
    if (index in currentInfos.indices) {
      val info = currentInfos[index]
      val previous = if (info.translation.isBlank()) "" else info.translation + "\n\n"
      currentInfos[index] = info.copy(translation = previous + text)
      _uiState.value = _uiState.value.copy(translationInfos = currentInfos)
    }
  }

  fun setOcrTextDirectly(index: Int, text: String) {
    val currentInfos = _uiState.value.translationInfos.toMutableList()
    if (index in currentInfos.indices) {
      currentInfos[index] = currentInfos[index].copy(ocr = text)
      _uiState.value = _uiState.value.copy(translationInfos = currentInfos)
    }
  }

  fun setTranslationTextDirectly(index: Int, text: String) {
    val currentInfos = _uiState.value.translationInfos.toMutableList()
    if (index in currentInfos.indices) {
      currentInfos[index] = currentInfos[index].copy(translation = text)
      _uiState.value = _uiState.value.copy(translationInfos = currentInfos)
    }
  }

  fun isProcessingInfo(index: Int): Boolean {
    return _uiState.value.isProcessing && _uiState.value.processingIndex == index
  }
}