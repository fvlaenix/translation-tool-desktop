package settings.di

import org.koin.dsl.module
import settings.data.SettingsRepository
import settings.data.SettingsRepositoryImpl
import settings.domain.SettingsViewModel

val settingsModule = module {

  single<SettingsRepository> {
    SettingsRepositoryImpl(settingsFilePath = "settings.json")
  }

  factory {
    SettingsViewModel(settingsRepository = get())
  }
}