package translation.data

import org.koin.dsl.module
import settings.data.SettingsRepository

val ocrModule = module {
  single { OCRRepositoryImpl(get<SettingsRepository>()) }
  single {
    OCRServiceProvider(
      grpcRepository = get<OCRRepositoryImpl>(),
      settingsRepository = get<SettingsRepository>(),
      directFactory = { directSettings -> DirectOCRRepository(directSettings) }
    )
  }
  single<OCRRepository> { DelegatingOCRRepository(get()) }
}
