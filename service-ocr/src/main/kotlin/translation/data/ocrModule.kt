package translation.data

import org.koin.dsl.module

val ocrModule = module {
  single<OCRRepository> { OCRRepositoryImpl() }
}
