package settings.data

import core.base.Repository
import core.utils.JSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

class SettingsRepositoryImpl(
  private val settingsFilePath: String
) : SettingsRepository, Repository {

  override suspend fun loadSettings(): Result<SettingsModel> = safeCall {
    withContext(Dispatchers.IO) {
      try {
        val text = Path.of(settingsFilePath).readText()
        JSON.decodeFromString<SettingsModel>(text)
      } catch (e: Exception) {
        // If file doesn't exist or is corrupted, return default settings
        SettingsModel.DEFAULT
      }
    }
  }

  override suspend fun saveSettings(settings: SettingsModel): Result<Unit> = safeCall {
    withContext(Dispatchers.IO) {
      Path.of(settingsFilePath).writeText(
        JSON.encodeToString(settings)
      )
    }
  }

  override suspend fun updateSetting(key: String, value: Any): Result<Unit> = safeCall {
    val currentSettings = loadSettings().getOrThrow()
    val updatedSettings = when (key) {
      "proxyServiceHostname" -> currentSettings.copy(proxyServiceHostname = value as String)
      "proxyServicePort" -> currentSettings.copy(proxyServicePort = value as Int)
      "apiKey" -> currentSettings.copy(apiKey = value as String)
      else -> throw IllegalArgumentException("Unknown setting key: $key")
    }
    saveSettings(updatedSettings).getOrThrow()
  }
}