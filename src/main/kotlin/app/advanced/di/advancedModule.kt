package app.advanced.di

import app.advanced.domain.ImageWithBoxesViewModel
import app.advanced.domain.TranslationStepViewModel
import org.koin.dsl.module

val advancedModule = module {
  factory { ImageWithBoxesViewModel() }
  factory { TranslationStepViewModel() }
}