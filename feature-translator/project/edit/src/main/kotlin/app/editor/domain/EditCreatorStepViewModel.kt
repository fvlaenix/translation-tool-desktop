package app.editor.domain

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import core.base.BaseViewModel
import translation.data.BlockData
import translation.data.BlockPosition
import translation.data.BlockSettings
import java.awt.image.BufferedImage

class EditCreatorStepViewModel : BaseViewModel() {

  private val _uiState = mutableStateOf(EditCreatorStepUiState())
  val uiState: State<EditCreatorStepUiState> = _uiState

  fun loadImageData(image: BufferedImage, blockData: List<BlockData>, settings: BlockSettings) {
    _uiState.value = _uiState.value.copy(
      image = image,
      boxes = blockData,
      currentSettings = settings,
      selectedBoxIndex = null,
      currentShape = null
    )
  }

  fun selectBox(index: Int?) {
    val state = _uiState.value

    // Update current settings and shape based on selection
    val newSettings = if (index == null) {
      // No box selected, use current global settings
      state.currentSettings
    } else {
      if (state.boxes.indices.contains(index)) {
        state.boxes[index].settings ?: state.currentSettings
      } else {
        // Invalid selection, clear it
        selectBox(null)
        return
      }
    }

    val newShape = if (index == null) {
      null
    } else {
      if (state.boxes.indices.contains(index)) {
        state.boxes[index].blockPosition.shape
      } else {
        null
      }
    }

    _uiState.value = state.copy(
      selectedBoxIndex = index,
      currentSettings = newSettings,
      currentShape = newShape
    )
  }

  fun updateBoxText(boxIndex: Int, text: String) {
    val currentBoxes = _uiState.value.boxes.toMutableList()
    if (boxIndex in currentBoxes.indices) {
      val box = currentBoxes[boxIndex]
      currentBoxes[boxIndex] = box.copy(text = text)
      _uiState.value = _uiState.value.copy(boxes = currentBoxes)
    }
  }

  fun updateSettings(settings: BlockSettings) {
    val state = _uiState.value
    val selectedIndex = state.selectedBoxIndex

    if (selectedIndex == null) {
      // Update global settings
      _uiState.value = state.copy(currentSettings = settings)
    } else {
      // Update box-specific settings
      val currentBoxes = state.boxes.toMutableList()
      if (selectedIndex in currentBoxes.indices) {
        val box = currentBoxes[selectedIndex]
        currentBoxes[selectedIndex] = box.copy(settings = settings)
        _uiState.value = state.copy(
          boxes = currentBoxes,
          currentSettings = settings
        )
      }
    }
  }

  fun updateBoxShape(shape: BlockPosition.Shape) {
    val state = _uiState.value
    val selectedIndex = state.selectedBoxIndex

    if (selectedIndex != null && selectedIndex in state.boxes.indices) {
      val currentBoxes = state.boxes.toMutableList()
      val box = currentBoxes[selectedIndex]
      currentBoxes[selectedIndex] = box.copy(
        blockPosition = box.blockPosition.copy(shape = shape)
      )
      _uiState.value = state.copy(
        boxes = currentBoxes,
        currentShape = shape
      )
    }
  }

  fun updateBox(index: Int, blockData: BlockData) {
    val currentBoxes = _uiState.value.boxes.toMutableList()
    if (index in currentBoxes.indices) {
      currentBoxes[index] = blockData
      _uiState.value = _uiState.value.copy(boxes = currentBoxes)
      incrementOperationNumber()
    }
  }

  fun incrementOperationNumber() {
    _uiState.value = _uiState.value.copy(
      operationNumber = _uiState.value.operationNumber + 1
    )
  }

  fun setGenerating(generating: Boolean) {
    _uiState.value = _uiState.value.copy(isGenerating = generating)
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

  fun clearAllValidationErrors() {
    _uiState.value = _uiState.value.copy(validationErrors = emptyMap())
  }

  // Method to get current boxes for parent to access
  fun getCurrentBoxes(): List<BlockData> {
    return _uiState.value.boxes
  }

  // Method to handle global settings changes from parent
  fun updateGlobalSettings(settings: BlockSettings) {
    _uiState.value = _uiState.value.copy(currentSettings = settings)
  }
}