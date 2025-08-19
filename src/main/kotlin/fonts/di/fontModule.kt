package fonts.di

import fonts.data.FontRepository
import fonts.data.FontRepositoryImpl
import fonts.domain.FontResolver
import fonts.domain.FontViewModel
import org.koin.dsl.module

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