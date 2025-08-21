package app.editor.di

import app.editor.domain.EditCreatorStepViewModel
import org.koin.dsl.module

val editorModule = module {
  factory { EditCreatorStepViewModel() }
}