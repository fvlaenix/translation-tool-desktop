// Create new file: src/main/kotlin/translation/di/translationModule.kt

package translation.di

import org.koin.dsl.module
import project.data.Project
import translation.data.*
import translation.domain.*

val translationModule = module {

  // App-level repositories (singletons)
  single<OCRRepository> {
    OCRRepositoryImpl()
  }

  single<TranslationRepository> {
    TranslationRepositoryImpl()
  }

  single<WorkDataRepository> {
    WorkDataRepositoryImpl()
  }

  // ViewModels (factories for new instances per usage)
  factory { (project: Project?) ->
    OCRCreatorViewModel(
      ocrRepository = get(),
      workDataRepository = get(),
      imageDataRepository = get(),
      textDataRepository = get()
    )
  }

  factory { (project: Project?) ->
    TranslationCreatorViewModel(
      translationRepository = get(),
      workDataRepository = get(),
      textDataRepository = get()
    )
  }

  factory { (project: Project?) ->
    EditCreatorViewModel(
      imageDataRepository = get(),
      textDataRepository = get(),
      fontResolver = get()
    )
  }

  factory {
    SimpleTranslatorViewModel(
      ocrRepository = get(),
      translationRepository = get()
    )
  }

  factory {
    AdvancedTranslatorViewModel(
      ocrRepository = get(),
      translationRepository = get()
    )
  }
}