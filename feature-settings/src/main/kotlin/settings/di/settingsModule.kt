package settings.di

import org.koin.dsl.module
import settings.data.SettingsRepository
import settings.data.SettingsRepositoryImpl
import settings.domain.OCRSettingsViewModel
import settings.domain.SettingsViewModel
import settings.domain.TranslationSettingsViewModel

/**
 * Dependency injection module for application settings.
 *
 * Settings architecture:
 * - Repository: Handles loading/saving settings to JSON file
 * - ViewModel: Manages settings UI state and validation
 * - Model: Data class representing settings structure
 *
 * DI Scoping:
 * - Repository as singleton: Settings should be shared across app
 * - ViewModel as factory: New instance each time settings screen is opened
 */
val settingsModule = module {
  // Singleton repository - settings are shared application state
  single<SettingsRepository> {
    SettingsRepositoryImpl(settingsFilePath = "settings.json")
  }

  // Factory ViewModel - new instance each time settings screen is opened
  // This ensures fresh state and proper cleanup
  factory {
    SettingsViewModel(settingsRepository = get())
  }

  factory {
    TranslationSettingsViewModel(
      settingsRepository = get(),
      translationServiceProvider = get()
    )
  }

  factory {
    OCRSettingsViewModel(
      settingsRepository = get(),
      ocrServiceProvider = get()
    )
  }
}
