package fonts.domain

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import core.base.BaseViewModel
import fonts.data.FontInfo
import fonts.data.FontRepository
import fonts.data.FontUtils
import kotlinx.coroutines.launch
import java.awt.Font
import java.nio.file.Path

/**
 * Font management view model. Handles font loading, adding, selection and validation.
 */
class FontViewModel(
  private val fontRepository: FontRepository
) : BaseViewModel() {

  private val _availableFonts = mutableStateOf<List<FontInfo>>(emptyList())
  val availableFonts: State<List<FontInfo>> = _availableFonts

  private val _selectedFont = mutableStateOf<String?>(null)
  val selectedFont: State<String?> = _selectedFont

  private val _validationErrors = mutableStateOf<Map<String, String>>(emptyMap())
  val validationErrors: State<Map<String, String>> = _validationErrors

  private val _isProcessing = mutableStateOf(false)
  val isProcessing: State<Boolean> = _isProcessing

  // Remove the init block - fonts will be loaded when needed

  fun loadFonts() {
    viewModelScope.launch {
      setLoading(true)
      clearError()

      fontRepository.loadFonts()
        .onSuccess {
          fontRepository.getAllFonts()
            .onSuccess { fonts ->
              _availableFonts.value = fonts
              // Set default selected font if none selected
              if (_selectedFont.value == null && fonts.isNotEmpty()) {
                _selectedFont.value = fonts.first().name
              }
            }
            .onFailure { exception ->
              setError("Failed to get fonts: ${exception.message}")
            }
        }
        .onFailure { exception ->
          setError("Failed to load fonts: ${exception.message}")
        }

      setLoading(false)
    }
  }

  fun addFont(name: String, path: Path) {
    if (!validateFontInput(name, path)) {
      return
    }

    viewModelScope.launch {
      _isProcessing.value = true
      clearError()

      fontRepository.addFont(name, path)
        .onSuccess {
          // Reload fonts to update the list
          loadFonts()
          clearValidationErrors()
        }
        .onFailure { exception ->
          setError("Failed to add font: ${exception.message}")
        }

      _isProcessing.value = false
    }
  }

  fun selectFont(name: String) {
    if (_availableFonts.value.any { it.name == name }) {
      _selectedFont.value = name
    }
  }

  fun deleteFont(name: String) {
    // Note: This would require additional repository method
    // For now, we'll just log that it's not implemented
    setError("Font deletion not implemented yet")
  }

  fun getDefaultFont(): String? {
    return _availableFonts.value.firstOrNull()?.name
  }

  fun getFontNotNull(name: String, size: Float): Font? {
    return try {
      FontUtils.getFontNotNull(_availableFonts.value, name, size)
    } catch (e: Exception) {
      setError("Font error: ${e.message}")
      null
    }
  }

  fun isFontsAdded(): Boolean {
    return _availableFonts.value.isNotEmpty()
  }

  fun refreshFonts() {
    loadFonts()
  }

  private fun validateFontInput(name: String, path: Path): Boolean {
    val errors = mutableMapOf<String, String>()

    // Validate font name
    if (name.isBlank()) {
      errors["name"] = "Font name cannot be empty"
    } else if (_availableFonts.value.any { it.name == name }) {
      errors["name"] = "Font name already exists"
    }

    // Validate font file
    if (!path.toFile().exists()) {
      errors["path"] = "Font file does not exist"
    } else if (!FontUtils.validateFontFile(path)) {
      errors["path"] = "Invalid font file"
    }

    _validationErrors.value = errors
    return errors.isEmpty()
  }

  private fun clearValidationErrors() {
    _validationErrors.value = emptyMap()
  }
}