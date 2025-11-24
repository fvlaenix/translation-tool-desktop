package fonts.di

import fonts.data.FontRepository
import fonts.data.FontRepositoryImpl
import fonts.domain.FontResolver
import fonts.domain.FontViewModel
import org.koin.dsl.module

/**
 * Koin module for font-related dependencies including repository and resolver.
 */
val fontModule = module {

  single<FontRepository> {
    FontRepositoryImpl(fontsFilePath = "fonts.json")
  }

  factory {
    FontViewModel(fontRepository = get())
  }

  single {
    FontResolver()
  }
}