package translation.domain

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import core.base.BaseViewModel
import kotlinx.coroutines.launch
import translation.data.OCRRepository
import translation.data.TranslationRepository
import java.awt.image.BufferedImage

class SimpleTranslatorViewModel(
  private val ocrRepository: OCRRepository,
  private val translationRepository: TranslationRepository
) : BaseViewModel() {

  private val _currentImage = mutableStateOf<BufferedImage?>(null)
  val currentImage: State<BufferedImage?> = _currentImage

  private val _ocrText = mutableStateOf("")
  val ocrText: State<String> = _ocrText

  private val _translationText = mutableStateOf("")
  val translationText: State<String> = _translationText

  private val _isProcessingOCR = mutableStateOf(false)
  val isProcessingOCR: State<Boolean> = _isProcessingOCR

  private val _isTranslating = mutableStateOf(false)
  val isTranslating: State<Boolean> = _isTranslating

  fun loadImage(image: BufferedImage) {
    _currentImage.value = image
    clearError()
  }

  fun processOCR() {
    val image = _currentImage.value ?: return

    viewModelScope.launch {
      _isProcessingOCR.value = true
      clearError()

      ocrRepository.processImage(image)
        .onSuccess { result ->
          val previous = if (_ocrText.value.isBlank()) "" else _ocrText.value + "\n\n"
          _ocrText.value = previous + result
        }
        .onFailure { exception ->
          setError("OCR processing failed: ${exception.message}")
        }

      _isProcessingOCR.value = false
    }
  }

  fun translate() {
    if (_ocrText.value.isBlank()) return

    viewModelScope.launch {
      _isTranslating.value = true
      clearError()

      translationRepository.translateText(_ocrText.value)
        .onSuccess { result ->
          val previous = if (_translationText.value.isBlank()) "" else _translationText.value + "\n\n"
          _translationText.value = previous + result
        }
        .onFailure { exception ->
          setError("Translation failed: ${exception.message}")
        }

      _isTranslating.value = false
    }
  }

  fun updateOcrText(text: String) {
    _ocrText.value = text
  }

  fun updateTranslationText(text: String) {
    _translationText.value = text
  }

  fun clear() {
    _currentImage.value = null
    _ocrText.value = ""
    _translationText.value = ""
    clearError()
  }
}