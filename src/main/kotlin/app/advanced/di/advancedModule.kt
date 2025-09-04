package app.advanced.di

import app.advanced.domain.ImageWithBoxesViewModel
import app.advanced.domain.TranslationStepViewModel
import org.koin.dsl.module

/**
 * Dependency injection module for advanced translation workflow components.
 */
val advancedModule = module {
  factory { ImageWithBoxesViewModel() }
  factory { TranslationStepViewModel() }
}