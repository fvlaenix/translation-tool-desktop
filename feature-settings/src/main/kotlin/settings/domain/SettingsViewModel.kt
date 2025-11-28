package settings.domain

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import core.base.BaseViewModel
import kotlinx.coroutines.launch
import settings.data.SettingsModel
import settings.data.SettingsRepository

class SettingsViewModel(
  private val settingsRepository: SettingsRepository
) : BaseViewModel() {

  private val _currentSettings = mutableStateOf(SettingsModel.DEFAULT)
  val currentSettings: State<SettingsModel> = _currentSettings

  private val _validationErrors = mutableStateOf<Map<String, String>>(emptyMap())
  val validationErrors: State<Map<String, String>> = _validationErrors

  private val _isSaving = mutableStateOf(false)
  val isSaving: State<Boolean> = _isSaving

  private val _saveSuccess = mutableStateOf(false)
  val saveSuccess: State<Boolean> = _saveSuccess

  init {
    loadSettings()
  }

  fun loadSettings() {
    viewModelScope.launch {
      setLoading(true)
      clearError()

      settingsRepository.loadSettings()
        .onSuccess { settings ->
          _currentSettings.value = settings
        }
        .onFailure { exception ->
          setError("Failed to load settings: ${exception.message}")
        }

      setLoading(false)
    }
  }

  fun updateHostname(hostname: String) {
    _currentSettings.value = _currentSettings.value.copy(proxyServiceHostname = hostname)
    validateSettings()
  }

  fun updatePort(port: Int) {
    _currentSettings.value = _currentSettings.value.copy(proxyServicePort = port)
    validateSettings()
  }

  fun updateApiKey(apiKey: String) {
    _currentSettings.value = _currentSettings.value.copy(apiKey = apiKey)
    validateSettings()
  }

  fun saveSettings() {
    if (!isValid()) {
      setError("Please fix validation errors before saving")
      return
    }

    viewModelScope.launch {
      _isSaving.value = true
      _saveSuccess.value = false
      clearError()

      settingsRepository.saveSettings(_currentSettings.value)
        .onSuccess {
          showSuccess("Settings saved successfully")
          _saveSuccess.value = true
        }
        .onFailure { exception ->
          setError("Failed to save settings: ${exception.message}")
        }

      _isSaving.value = false
    }
  }

  private fun validateSettings() {
    val errors = mutableMapOf<String, String>()
    val settings = _currentSettings.value

    // Validate hostname
    if (settings.proxyServiceHostname.isBlank()) {
      errors["hostname"] = "Hostname cannot be empty"
    }

    // Validate port
    if (settings.proxyServicePort !in 1..65535) {
      errors["port"] = "Port must be between 1 and 65535"
    }

    // Validate API key
    if (settings.apiKey.isBlank()) {
      errors["apiKey"] = "API key cannot be empty"
    }

    _validationErrors.value = errors
  }

  private fun isValid(): Boolean {
    validateSettings()
    return _validationErrors.value.isEmpty()
  }
}