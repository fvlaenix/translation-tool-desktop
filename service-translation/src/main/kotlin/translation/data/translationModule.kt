package translation.data

import org.koin.dsl.module

val translationModule = module {
  single<TranslationRepository> { TranslationRepositoryImpl() }
}