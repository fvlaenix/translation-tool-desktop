package translation.di

import org.koin.dsl.module
import project.data.Project
import translation.domain.TranslationCreatorViewModel

val translationCreatorModule = module {
  factory { (project: Project?) ->
    TranslationCreatorViewModel(
      translationRepository = get(),
      workDataRepository = get(),
      textDataRepository = get()
    )
  }
}
