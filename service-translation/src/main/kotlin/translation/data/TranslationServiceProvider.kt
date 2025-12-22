package translation.data

import kotlinx.coroutines.runBlocking
import settings.data.SettingsModel
import settings.data.SettingsRepository
import settings.data.TranslationDirectSettings
import settings.data.TranslationMode

class TranslationServiceProvider(
  private val grpcRepository: TranslationRepository,
  private val settingsRepository: SettingsRepository,
  private val directFactory: (TranslationDirectSettings) -> TranslationRepository
) {

  @Volatile
  private var current: TranslationRepository = grpcRepository

  init {
    refreshFromSettings()
  }

  fun refresh(settings: SettingsModel) {
    current = when (settings.translationMode) {
      TranslationMode.GRPC -> grpcRepository
      TranslationMode.DIRECT -> directFactory(settings.translationDirect)
    }
  }

  fun refreshFromSettings() {
    val settings = runBlocking {
      settingsRepository.loadSettings().getOrElse { SettingsModel.DEFAULT }
    }
    refresh(settings)
  }

  fun get(): TranslationRepository = current
}
