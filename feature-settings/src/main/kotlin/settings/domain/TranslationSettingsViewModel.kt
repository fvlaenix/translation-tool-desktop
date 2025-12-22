package settings.domain

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.fvlaenix.text.OpenAIModelProvider
import com.fvlaenix.text.OpenRouterModelProvider
import core.base.BaseViewModel
import kotlinx.coroutines.launch
import settings.data.*
import translation.data.TranslationServiceProvider

class TranslationSettingsViewModel(
  private val settingsRepository: SettingsRepository,
  private val translationServiceProvider: TranslationServiceProvider
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

  fun updateMode(mode: TranslationMode) {
    _currentSettings.value = _currentSettings.value.copy(translationMode = mode)
    validateSettings()
  }

  fun updateDirectProvider(provider: TranslationModelProvider) {
    val current = _currentSettings.value
    val defaultModel = when (provider) {
      TranslationModelProvider.OPENAI -> OpenAIModelProvider.GPT_4O.name
      TranslationModelProvider.OPENROUTER -> OpenRouterModelProvider.OPENAI_GPT_4O.name
      TranslationModelProvider.OTHER -> current.translationDirect.modelName
    }
    val defaultBaseUrl = when (provider) {
      TranslationModelProvider.OPENAI -> OpenAIModelProvider.HOST_URL
      TranslationModelProvider.OPENROUTER -> OpenRouterModelProvider.HOST_URL
      TranslationModelProvider.OTHER -> current.translationDirect.apiBaseUrl
    }
    val nextModelName = when (provider) {
      TranslationModelProvider.OPENAI -> {
        if (OpenAIModelProvider.models.containsKey(current.translationDirect.modelName)) {
          current.translationDirect.modelName
        } else {
          defaultModel
        }
      }

      TranslationModelProvider.OPENROUTER -> {
        if (OpenRouterModelProvider.models.containsKey(current.translationDirect.modelName)) {
          current.translationDirect.modelName
        } else {
          defaultModel
        }
      }

      TranslationModelProvider.OTHER -> current.translationDirect.modelName
    }
    _currentSettings.value = current.copy(
      translationDirect = current.translationDirect.copy(
        provider = provider,
        modelName = nextModelName,
        apiBaseUrl = if (provider == TranslationModelProvider.OTHER) {
          current.translationDirect.apiBaseUrl
        } else {
          defaultBaseUrl
        }
      )
    )
    validateSettings()
  }

  fun updateDirectApiKey(apiKey: String) {
    updateDirectSettings { it.copy(apiKey = apiKey) }
  }

  fun updateDirectModelName(modelName: String) {
    val previous = _currentSettings.value.translationDirect.modelName
    updateDirectSettings { it.copy(modelName = modelName) }
    if (modelName.isNotBlank() && modelName != previous) {
      showWarning("Changing model name is not fully supported yet.")
    }
  }

  fun updateDirectApiBaseUrl(apiBaseUrl: String) {
    updateDirectSettings { it.copy(apiBaseUrl = apiBaseUrl) }
  }

  fun updateDirectTimeoutSeconds(timeoutSeconds: Int) {
    updateDirectSettings { it.copy(timeoutSeconds = timeoutSeconds) }
  }

  private fun updateDirectSettings(update: (TranslationDirectSettings) -> TranslationDirectSettings) {
    val current = _currentSettings.value
    _currentSettings.value = current.copy(translationDirect = update(current.translationDirect))
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
          showSuccess("Translation settings saved successfully")
          translationServiceProvider.refresh(_currentSettings.value)
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

    if (settings.translationMode == TranslationMode.DIRECT) {
      if (settings.translationDirect.apiKey.isBlank()) {
        errors["directApiKey"] = "API key cannot be empty"
      }
      if (settings.translationDirect.modelName.isBlank()) {
        errors["directModelName"] = "Model name cannot be empty"
      }
      if (settings.translationDirect.timeoutSeconds <= 0) {
        errors["directTimeout"] = "Timeout must be greater than 0"
      }
      if (
        settings.translationDirect.provider == TranslationModelProvider.OTHER &&
        settings.translationDirect.apiBaseUrl.isBlank()
      ) {
        errors["directApiBaseUrl"] = "API base URL is required for Other provider"
      }
    }

    _validationErrors.value = errors
  }

  private fun isValid(): Boolean {
    validateSettings()
    return _validationErrors.value.isEmpty()
  }
}
