package settings.domain

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import core.base.BaseViewModel
import kotlinx.coroutines.launch
import settings.data.OCRDirectSettings
import settings.data.OCRMode
import settings.data.SettingsModel
import settings.data.SettingsRepository
import translation.data.OCRServiceProvider
import java.io.File

class OCRSettingsViewModel(
  private val settingsRepository: SettingsRepository,
  private val ocrServiceProvider: OCRServiceProvider
) : BaseViewModel() {

  private val _currentSettings = mutableStateOf(SettingsModel.DEFAULT)
  val currentSettings: State<SettingsModel> = _currentSettings

  private val _validationErrors = mutableStateOf<Map<String, String>>(emptyMap())
  val validationErrors: State<Map<String, String>> = _validationErrors

  private val _isSaving = mutableStateOf(false)
  val isSaving: State<Boolean> = _isSaving

  private val _saveSuccess = mutableStateOf(false)
  val saveSuccess: State<Boolean> = _saveSuccess

  private val _isTesting = mutableStateOf(false)
  val isTesting: State<Boolean> = _isTesting

  private val _testResult = mutableStateOf<String?>(null)
  val testResult: State<String?> = _testResult

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

  fun updateMode(mode: OCRMode) {
    _currentSettings.value = _currentSettings.value.copy(ocrMode = mode)
    validateSettings()
  }

  fun updateGrpcApiKey(apiKey: String) {
    val current = _currentSettings.value
    _currentSettings.value = current.copy(
      ocrGrpc = current.ocrGrpc.copy(apiKey = apiKey)
    )
    validateSettings()
  }

  fun updateDirectCredentialsPath(path: String) {
    updateDirectSettings { it.copy(credentialsPath = path) }
  }

  fun updateDirectTimeoutSeconds(timeoutSeconds: Int) {
    updateDirectSettings { it.copy(timeoutSeconds = timeoutSeconds) }
  }

  private fun updateDirectSettings(update: (OCRDirectSettings) -> OCRDirectSettings) {
    val current = _currentSettings.value
    _currentSettings.value = current.copy(ocrDirect = update(current.ocrDirect))
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
          showSuccess("OCR settings saved successfully")
          ocrServiceProvider.refresh(_currentSettings.value)
          _saveSuccess.value = true
        }
        .onFailure { exception ->
          setError("Failed to save settings: ${exception.message}")
        }

      _isSaving.value = false
    }
  }

  fun testConnection() {
    if (!isValid()) {
      setError("Please fix validation errors before testing")
      return
    }

    viewModelScope.launch {
      _isTesting.value = true
      _testResult.value = null
      clearError()

      try {
        val testImage = createTestImage()
        val tempProvider = OCRServiceProvider(
          grpcRepository = translation.data.OCRRepositoryImpl(settingsRepository),
          settingsRepository = settingsRepository,
          directFactory = { directSettings -> translation.data.DirectOCRRepository(directSettings) }
        )
        tempProvider.refresh(_currentSettings.value)
        val repository = tempProvider.get()

        val result = repository.processImage(testImage)
        result.onSuccess { text ->
          _testResult.value =
            "Connection test successful! OCR returned: ${text.take(100)}${if (text.length > 100) "..." else ""}"
          showSuccess("Connection test successful")
        }.onFailure { exception ->
          _testResult.value = "Connection test failed: ${exception.message}"
          setError("Connection test failed: ${exception.message}")
        }
      } catch (e: Exception) {
        _testResult.value = "Connection test failed: ${e.message}"
        setError("Connection test failed: ${e.message}")
      } finally {
        _isTesting.value = false
      }
    }
  }

  private fun createTestImage(): java.awt.image.BufferedImage {
    val width = 200
    val height = 50
    val image = java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB)
    val graphics = image.createGraphics()

    graphics.color = java.awt.Color.WHITE
    graphics.fillRect(0, 0, width, height)

    graphics.color = java.awt.Color.BLACK
    graphics.font = java.awt.Font("Arial", java.awt.Font.PLAIN, 20)
    graphics.drawString("Test OCR", 10, 30)

    graphics.dispose()
    return image
  }

  private fun validateSettings() {
    val errors = mutableMapOf<String, String>()
    val settings = _currentSettings.value

    if (settings.ocrMode == OCRMode.GRPC) {
      if (settings.ocrGrpc.apiKey.isBlank()) {
        errors["grpcApiKey"] = "API key cannot be empty for gRPC mode"
      }
    }

    if (settings.ocrMode == OCRMode.DIRECT) {
      if (settings.ocrDirect.credentialsPath.isBlank()) {
        errors["directCredentialsPath"] = "Credentials path cannot be empty"
      } else {
        val credentialsFile = File(settings.ocrDirect.credentialsPath)
        if (!credentialsFile.exists()) {
          errors["directCredentialsPath"] = "Credentials file does not exist"
        }
      }
      if (settings.ocrDirect.timeoutSeconds <= 0) {
        errors["directTimeout"] = "Timeout must be greater than 0"
      }
    }

    _validationErrors.value = errors
  }

  private fun isValid(): Boolean {
    validateSettings()
    return _validationErrors.value.isEmpty()
  }
}
