package translation.domain

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import core.base.BaseViewModel
import kotlinx.coroutines.launch
import translation.data.BlockPosition
import translation.data.OCRRepository
import translation.data.TranslationRepository
import java.awt.image.BufferedImage

/**
 * Data class holding sub-image with OCR and translation text for advanced workflow.
 */
data class AdvancedTranslationInfo(
  val subImage: BufferedImage,
  val ocrText: String = "",
  val translationText: String = ""
)

/**
 * Workflow states for advanced translator: image with boxes, translation step.
 */
enum class AdvancedTranslatorStep {
  IMAGE_WITH_BOXES,
  TRANSLATION_STEP
}

/**
 * View model for advanced translation workflow with multi-step process.
 */
class AdvancedTranslatorViewModel(
  private val ocrRepository: OCRRepository,
  private val translationRepository: TranslationRepository
) : BaseViewModel() {

  private val _currentStep = mutableStateOf(AdvancedTranslatorStep.IMAGE_WITH_BOXES)
  val currentStep: State<AdvancedTranslatorStep> = _currentStep

  private val _currentImage = mutableStateOf<BufferedImage?>(null)
  val currentImage: State<BufferedImage?> = _currentImage

  private val _textBoxes = mutableStateOf<List<BlockPosition>>(emptyList())
  val textBoxes: State<List<BlockPosition>> = _textBoxes

  private val _translationInfos = mutableStateOf<List<AdvancedTranslationInfo>>(emptyList())
  val translationInfos: State<List<AdvancedTranslationInfo>> = _translationInfos

  private val _selectedBoxIndex = mutableStateOf<Int?>(null)
  val selectedBoxIndex: State<Int?> = _selectedBoxIndex

  private val _isStepEnabled = mutableStateOf(false)
  val isStepEnabled: State<Boolean> = _isStepEnabled

  fun loadImage(image: BufferedImage) {
    _currentImage.value = image
    _textBoxes.value = emptyList()
    _translationInfos.value = emptyList()
    _selectedBoxIndex.value = null
    _currentStep.value = AdvancedTranslatorStep.IMAGE_WITH_BOXES
    _isStepEnabled.value = true
    clearError()
  }

  fun addTextBox() {
    val image = _currentImage.value ?: return

    val newBox = BlockPosition(
      x = 0.0,
      y = 0.0,
      width = image.width.toDouble() / 10,
      height = image.height.toDouble() / 10,
      shape = BlockPosition.Shape.Rectangle
    )

    _textBoxes.value = _textBoxes.value + newBox
  }

  fun updateBox(index: Int, box: BlockPosition) {
    val boxes = _textBoxes.value.toMutableList()
    if (index in boxes.indices) {
      boxes[index] = box
      _textBoxes.value = boxes
    }
  }

  fun deleteLastBox() {
    if (_textBoxes.value.isNotEmpty()) {
      _textBoxes.value = _textBoxes.value.dropLast(1)
    }
  }

  fun selectBox(index: Int?) {
    _selectedBoxIndex.value = index
  }

  fun nextStep() {
    when (_currentStep.value) {
      AdvancedTranslatorStep.IMAGE_WITH_BOXES -> {
        prepareTranslationStep()
        _currentStep.value = AdvancedTranslatorStep.TRANSLATION_STEP
      }

      AdvancedTranslatorStep.TRANSLATION_STEP -> {
        // Already at the last step
      }
    }
  }

  fun previousStep() {
    when (_currentStep.value) {
      AdvancedTranslatorStep.IMAGE_WITH_BOXES -> {
        // Already at the first step
      }

      AdvancedTranslatorStep.TRANSLATION_STEP -> {
        _currentStep.value = AdvancedTranslatorStep.IMAGE_WITH_BOXES
      }
    }
  }

  private fun prepareTranslationStep() {
    val fullImage = _currentImage.value ?: return

    _isStepEnabled.value = false

    val infos = if (_textBoxes.value.isEmpty()) {
      // No boxes defined, use the whole image
      listOf(AdvancedTranslationInfo(fullImage))
    } else {
      // Create sub-images from the defined boxes
      _textBoxes.value.map { box ->
        val subImage = fullImage.getSubimage(
          box.x.toInt(),
          box.y.toInt(),
          box.width.toInt(),
          box.height.toInt()
        )
        AdvancedTranslationInfo(subImage)
      }
    }

    _translationInfos.value = infos
    _isStepEnabled.value = true
  }

  fun processOCRForInfo(index: Int) {
    val info = _translationInfos.value.getOrNull(index) ?: return

    viewModelScope.launch {
      setLoading(true)
      clearError()

      ocrRepository.processImage(info.subImage)
        .onSuccess { ocrResult ->
          val updatedInfos = _translationInfos.value.toMutableList()
          val previous = if (info.ocrText.isBlank()) "" else info.ocrText + "\n\n"
          updatedInfos[index] = info.copy(ocrText = previous + ocrResult)
          _translationInfos.value = updatedInfos
        }
        .onFailure { exception ->
          setError("OCR processing failed: ${exception.message}")
        }

      setLoading(false)
    }
  }

  fun translateInfo(index: Int) {
    val info = _translationInfos.value.getOrNull(index) ?: return
    if (info.ocrText.isBlank()) return

    viewModelScope.launch {
      setLoading(true)
      clearError()

      translationRepository.translateText(info.ocrText)
        .onSuccess { translationResult ->
          val updatedInfos = _translationInfos.value.toMutableList()
          val previous = if (info.translationText.isBlank()) "" else info.translationText + "\n\n"
          updatedInfos[index] = info.copy(translationText = previous + translationResult)
          _translationInfos.value = updatedInfos
        }
        .onFailure { exception ->
          setError("Translation failed: ${exception.message}")
        }

      setLoading(false)
    }
  }

  fun updateInfoOcrText(index: Int, text: String) {
    val updatedInfos = _translationInfos.value.toMutableList()
    if (index in updatedInfos.indices) {
      updatedInfos[index] = updatedInfos[index].copy(ocrText = text)
      _translationInfos.value = updatedInfos
    }
  }

  fun updateInfoTranslationText(index: Int, text: String) {
    val updatedInfos = _translationInfos.value.toMutableList()
    if (index in updatedInfos.indices) {
      updatedInfos[index] = updatedInfos[index].copy(translationText = text)
      _translationInfos.value = updatedInfos
    }
  }

  fun canGoNext(): Boolean {
    return when (_currentStep.value) {
      AdvancedTranslatorStep.IMAGE_WITH_BOXES -> _isStepEnabled.value && _currentImage.value != null
      AdvancedTranslatorStep.TRANSLATION_STEP -> false
    }
  }

  fun canGoPrevious(): Boolean {
    return _currentStep.value != AdvancedTranslatorStep.IMAGE_WITH_BOXES
  }
}