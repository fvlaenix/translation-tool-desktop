package settings.data

interface SettingsRepository {
  suspend fun loadSettings(): Result<SettingsModel>
  suspend fun saveSettings(settings: SettingsModel): Result<Unit>
  suspend fun updateSetting(key: String, value: Any): Result<Unit>
}