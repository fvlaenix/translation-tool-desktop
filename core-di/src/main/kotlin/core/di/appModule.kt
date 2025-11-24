package core.di

import app.advanced.di.advancedModule
import app.editor.di.editorModule
import fonts.di.fontModule
import org.koin.dsl.module
import project.data.imageDataModule
import project.di.projectModule
import settings.di.settingsModule
import translation.data.ocrModule
import translation.data.textDataModule
import translation.data.translationModule
import translation.di.ocrCreatorModule
import translation.di.simpleTranslatorModule
import translation.di.translationCreatorModule

/**
 * Main application module that combines all feature modules.
 *
 * Dependency Injection (DI) provides:
 * - Automatic dependency resolution and injection
 * - Easy testing (can replace real implementations with mocks)
 * - Cleaner code (no manual dependency construction)
 * - Better separation of concerns
 *
 * Module organization:
 * - coreModule: Navigation, error handling, base services
 * - settingsModule: Application settings management
 * - fontModule: Font loading and management
 * - projectModule: Project creation and management
 * - ocrModule: OCR service repository
 * - translationModule: Translation service repository
 * - textDataModule: Work data and text data repositories
 * - imageDataModule: Image data repository
 * - ocrCreatorModule: OCR creator view model
 * - translationCreatorModule: Translation creator view model
 * - simpleTranslatorModule: Simple translator view model
 * - advancedModule: Advanced translator components
 * - editorModule: Editor components
 */
val appModule = module {
  includes(
    coreModule,
    settingsModule,
    fontModule,
    projectModule,
    ocrModule,
    translationModule,
    textDataModule,
    imageDataModule,
    ocrCreatorModule,
    translationCreatorModule,
    simpleTranslatorModule,
    advancedModule,
    editorModule
  )
}