package core.di

import app.advanced.di.advancedModule
import app.editor.di.editorModule
import fonts.di.fontModule
import org.koin.dsl.module
import project.di.projectModule
import settings.di.settingsModule
import translation.di.translationModule

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
 * - translationModule: OCR and translation services
 */
val appModule = module {
  includes(
    coreModule,
    settingsModule,
    fontModule,
    projectModule,
    translationModule,
    advancedModule,
    editorModule
  )
}