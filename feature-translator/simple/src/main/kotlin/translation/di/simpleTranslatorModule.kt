package translation.di

import org.koin.dsl.module
import translation.domain.SimpleTranslatorViewModel

val simpleTranslatorModule = module {
  factory {
    SimpleTranslatorViewModel(
      ocrRepository = get(),
      translationRepository = get()
    )
  }
}
