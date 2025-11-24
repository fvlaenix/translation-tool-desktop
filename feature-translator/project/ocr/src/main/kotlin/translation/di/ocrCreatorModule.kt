package translation.di

import org.koin.dsl.module
import project.data.Project
import translation.domain.OCRCreatorViewModel

val ocrCreatorModule = module {
  factory { (project: Project?) ->
    OCRCreatorViewModel(
      ocrRepository = get(),
      workDataRepository = get(),
      imageDataRepository = get(),
      textDataRepository = get()
    )
  }
}
