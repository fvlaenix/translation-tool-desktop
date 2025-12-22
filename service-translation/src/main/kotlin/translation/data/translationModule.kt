package translation.data

import org.koin.dsl.module
import settings.data.SettingsRepository

val translationModule = module {
  single { TranslationRepositoryImpl() }
  single {
    TranslationServiceProvider(
      grpcRepository = get<TranslationRepositoryImpl>(),
      settingsRepository = get<SettingsRepository>(),
      directFactory = { directSettings -> DirectTranslationRepository(directSettings) }
    )
  }
  single<TranslationRepository> { DelegatingTranslationRepository(get()) }
}
