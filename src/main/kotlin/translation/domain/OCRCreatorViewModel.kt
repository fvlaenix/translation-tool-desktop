package translation.domain

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import app.batch.ImagePathInfo
import app.ocr.OCRBoxData
import app.translation.domain.OCRCreatorStepUiState
import core.base.BaseViewModel
import kotlinx.coroutines.launch
import project.data.ImageDataRepository
import project.data.Project
import project.data.TextDataRepository
import project.data.TextType
import translation.data.*

class OCRCreatorViewModel(
  private val ocrRepository: OCRRepository,
  private val workDataRepository: WorkDataRepository,
  private val imageDataRepository: ImageDataRepository,
  private val textDataRepository: TextDataRepository
) : BaseViewModel() {

  private val _currentImage = mutableStateOf<ImagePathInfo?>(null)
  val currentImage: State<ImagePathInfo?> = _currentImage

  private val _ocrBoxes = mutableStateOf<List<OCRBoxData>>(emptyList())
  val ocrBoxes: State<List<OCRBoxData>> = _ocrBoxes

  private val _isProcessingOCR = mutableStateOf(false)
  val isProcessingOCR: State<Boolean> = _isProcessingOCR

  private val _operationNumber = mutableStateOf(0)
  val operationNumber: State<Int> = _operationNumber

  private val _selectedBoxIndex = mutableStateOf<Int?>(null)
  val selectedBoxIndex: State<Int?> = _selectedBoxIndex

  private val _uiState = mutableStateOf(OCRCreatorStepUiState())
  val uiState: State<OCRCreatorStepUiState> = _uiState

  private fun syncUiState() {
    _uiState.value = _uiState.value.copy(
      image = _currentImage.value?.image,
      boxes = _ocrBoxes.value,
      selectedBoxIndex = _selectedBoxIndex.value,
      operationNumber = _operationNumber.value
    )
  }

  fun loadImage(imagePathInfo: ImagePathInfo) {
    _currentImage.value = imagePathInfo
    _ocrBoxes.value = emptyList()
    _selectedBoxIndex.value = null
    clearError()
    syncUiState()
  }

  fun processOCR() {
    val image = _currentImage.value?.image ?: return

    viewModelScope.launch {
      _isProcessingOCR.value = true
      clearError()

      ocrRepository.getBoxedOCR(image)
        .onSuccess { boxes ->
          _ocrBoxes.value = boxes
          incrementOperationNumber()
          syncUiState()
        }
        .onFailure { exception ->
          setError("OCR processing failed: ${exception.message}")
        }

      _isProcessingOCR.value = false
    }
  }

  fun updateBoxText(index: Int, text: String) {
    val currentBoxes = _ocrBoxes.value.toMutableList()
    if (index in currentBoxes.indices) {
      val oldBox = currentBoxes[index]
      currentBoxes[index] = oldBox.copy(text = text)
      _ocrBoxes.value = currentBoxes
      syncUiState()
    }
  }

  fun mergeBoxes(index: Int) {
    val currentBoxes = _ocrBoxes.value.toMutableList()
    if (index < currentBoxes.size - 1) {
      val currentBox = currentBoxes[index]
      val nextBox = currentBoxes[index + 1]

      val minX = minOf(currentBox.box.x, nextBox.box.x)
      val minY = minOf(currentBox.box.y, nextBox.box.y)
      val maxX = maxOf(currentBox.box.x + currentBox.box.width, nextBox.box.x + nextBox.box.width)
      val maxY = maxOf(currentBox.box.y + currentBox.box.height, nextBox.box.y + nextBox.box.height)

      val mergedBox = OCRBoxData(
        box = BlockPosition(
          x = minX,
          y = minY,
          width = maxX - minX,
          height = maxY - minY,
          shape = BlockPosition.Shape.Rectangle
        ),
        text = currentBox.text + " " + nextBox.text
      )

      currentBoxes[index] = mergedBox
      currentBoxes.removeAt(index + 1)
      _ocrBoxes.value = currentBoxes
      incrementOperationNumber()
      syncUiState()
    }
  }

  fun removeBox(index: Int) {
    val currentBoxes = _ocrBoxes.value.toMutableList()
    if (index in currentBoxes.indices) {
      currentBoxes.removeAt(index)
      _ocrBoxes.value = currentBoxes
      incrementOperationNumber()

      // Clear selection if the selected box was removed
      if (_selectedBoxIndex.value == index) {
        _selectedBoxIndex.value = null
      } else if (_selectedBoxIndex.value != null && _selectedBoxIndex.value!! > index) {
        _selectedBoxIndex.value = _selectedBoxIndex.value!! - 1
      }
      syncUiState()
    }
  }

  fun selectBox(index: Int?) {
    _selectedBoxIndex.value = index
    syncUiState()
  }

  fun saveOCRResults(project: Project?, author: String, defaultSettings: BlockSettings) {
    viewModelScope.launch {
      setLoading(true)
      clearError()

      try {
        val imageData = createImageDataFromOCR(_currentImage.value!!, _ocrBoxes.value, defaultSettings)
        val workData = WorkData(
          version = 1,
          author = author,
          imagesData = listOf(imageData)
        )

        if (project == null) {
          // App-level save
          workDataRepository.setWorkData(workData).getOrThrow()
        } else {
          // Project-level save
          textDataRepository.saveWorkData(project, TextType.UNTRANSLATED, workData).getOrThrow()
        }
      } catch (e: Exception) {
        setError("Failed to save OCR results: ${e.message}")
      }

      setLoading(false)
    }
  }

  private fun createImageDataFromOCR(
    imagePathInfo: ImagePathInfo,
    ocrBoxes: List<OCRBoxData>,
    defaultSettings: BlockSettings
  ): ImageData {
    return ImageData(
      index = 0,
      imageName = imagePathInfo.name,
      image = null,
      blockData = ocrBoxes.map { ocrBox ->
        BlockData(
          blockPosition = ocrBox.box,
          text = ocrBox.text,
          settings = null
        )
      },
      settings = defaultSettings
    )
  }

  private fun incrementOperationNumber() {
    _operationNumber.value += 1
  }

  fun addValidationError(field: String, message: String) {
    val currentErrors = _uiState.value.validationErrors.toMutableMap()
    currentErrors[field] = message
    _uiState.value = _uiState.value.copy(validationErrors = currentErrors)
  }

  fun clearValidationError(field: String) {
    val currentErrors = _uiState.value.validationErrors.toMutableMap()
    currentErrors.remove(field)
    _uiState.value = _uiState.value.copy(validationErrors = currentErrors)
  }

  fun setReorderingEnabled(enabled: Boolean) {
    _uiState.value = _uiState.value.copy(isReorderingEnabled = enabled)
  }
}