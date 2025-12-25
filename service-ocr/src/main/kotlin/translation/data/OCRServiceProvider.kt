package translation.data

import kotlinx.coroutines.runBlocking
import settings.data.OCRDirectSettings
import settings.data.OCRMode
import settings.data.SettingsModel
import settings.data.SettingsRepository

class OCRServiceProvider(
  private val grpcRepository: OCRRepository,
  private val settingsRepository: SettingsRepository,
  private val directFactory: (OCRDirectSettings) -> OCRRepository
) {

  @Volatile
  private var current: OCRRepository = grpcRepository

  init {
    refreshFromSettings()
  }

  fun refresh(settings: SettingsModel) {
    current = when (settings.ocrMode) {
      OCRMode.GRPC -> grpcRepository
      OCRMode.DIRECT -> directFactory(settings.ocrDirect)
    }
  }

  fun refreshFromSettings() {
    val settings = runBlocking {
      settingsRepository.loadSettings().getOrElse { SettingsModel.DEFAULT }
    }
    refresh(settings)
  }

  fun get(): OCRRepository = current
}
