package app.editor.domain

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import core.base.BaseViewModel
import fonts.domain.FontResolver
import kotlinx.coroutines.launch
import translation.data.BlockData
import translation.data.BlockPosition
import translation.data.BlockSettings
import java.awt.image.BufferedImage

class EditCreatorStepViewModel(
  private val fontResolver: FontResolver
) : BaseViewModel() {

  private val _uiState = mutableStateOf(EditCreatorStepUiState())
  val uiState: State<EditCreatorStepUiState> = _uiState

  fun loadImageData(image: BufferedImage, blockData: List<BlockData>, settings: BlockSettings) {
    viewModelScope.launch {
      // Resolve font for global settings
      val resolvedSettings = fontResolver.resolveFont(settings)

      // Resolve fonts for each box's individual settings
      val resolvedBoxes = blockData.map { box ->
        val boxSettings = box.settings
        if (boxSettings != null) {
          box.copy(settings = fontResolver.resolveFont(boxSettings))
        } else {
          box
        }
      }

      _uiState.value = _uiState.value.copy(
        image = image,
        boxes = resolvedBoxes,
        currentSettings = resolvedSettings,
        selectedBoxIndex = null,
        currentShape = null
      )
    }
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
    val selectedIndex = _uiState.value.selectedBoxIndex

    viewModelScope.launch {
      // Resolve font for the new settings
      val resolvedSettings = fontResolver.resolveFont(settings)

      // Read current state AFTER font resolution (state might have changed)
      val currentState = _uiState.value

      if (selectedIndex == null) {
        // Update global settings (no box selected) - atomic update
        _uiState.value = currentState.copy(
          currentSettings = resolvedSettings,
          operationNumber = currentState.operationNumber + 1
        )
      } else {
        // Update ONLY box-specific settings, NOT the global currentSettings
        val currentBoxes = currentState.boxes.toMutableList()
        if (selectedIndex in currentBoxes.indices) {
          val box = currentBoxes[selectedIndex]
          currentBoxes[selectedIndex] = box.copy(settings = resolvedSettings)
          // Atomic update: boxes and operation number together
          _uiState.value = currentState.copy(
            boxes = currentBoxes,
            operationNumber = currentState.operationNumber + 1
          )
        }
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
      // Atomic update: boxes, shape, and operation number together
      _uiState.value = state.copy(
        boxes = currentBoxes,
        currentShape = shape,
        operationNumber = state.operationNumber + 1
      )
    }
  }

  fun updateBox(index: Int, blockData: BlockData) {
    val state = _uiState.value
    val currentBoxes = state.boxes.toMutableList()
    if (index in currentBoxes.indices) {
      currentBoxes[index] = blockData
      // Atomic update: boxes and operation number together
      _uiState.value = state.copy(
        boxes = currentBoxes,
        operationNumber = state.operationNumber + 1
      )
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
    viewModelScope.launch {
      val resolvedSettings = fontResolver.resolveFont(settings)
      _uiState.value = _uiState.value.copy(currentSettings = resolvedSettings)
    }
  }
}