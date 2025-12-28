package app.editor.di

import app.editor.domain.EditCreatorStepViewModel
import org.koin.dsl.module
import project.data.Project
import translation.domain.EditCreatorViewModel

val editorModule = module {
  factory { EditCreatorStepViewModel(fontResolver = get()) }

  factory { (project: Project?) ->
    EditCreatorViewModel(
      imageDataRepository = get(),
      textDataRepository = get(),
      fontResolver = get()
    )
  }
}