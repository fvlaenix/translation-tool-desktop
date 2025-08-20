package core.navigation

import app.AppStateEnum

/**
 * Sealed class representing all possible navigation destinations
 * Wraps the existing AppStateEnum for better type safety
 */
sealed class NavigationDestination(val appState: AppStateEnum) {

  // Main sections
  object MainMenu : NavigationDestination(AppStateEnum.MAIN_MENU)

  // Translation tools
  object SimpleTranslator : NavigationDestination(AppStateEnum.SIMPLE_VERSION)
  object AdvancedTranslator : NavigationDestination(AppStateEnum.ADVANCED_VERSION)

  // Workflow tools
  object BatchCreator : NavigationDestination(AppStateEnum.BATCH_CREATOR)
  object OCRCreator : NavigationDestination(AppStateEnum.OCR_CREATOR)
  object LoadOCRCreator : NavigationDestination(AppStateEnum.LOAD_OCR_CREATOR)
  object TranslationCreator : NavigationDestination(AppStateEnum.TRANSLATION_CREATOR)
  object EditCreator : NavigationDestination(AppStateEnum.EDIT_CREATOR)

  // Project management
  object NewProject : NavigationDestination(AppStateEnum.NEW_PROJECT)
  object Project : NavigationDestination(AppStateEnum.PROJECT)

  // Settings
  object Settings : NavigationDestination(AppStateEnum.SETTINGS)
  object FontSettings : NavigationDestination(AppStateEnum.FONT_SETTINGS)

  companion object {
    fun fromAppState(appState: AppStateEnum): NavigationDestination {
      return when (appState) {
        AppStateEnum.MAIN_MENU -> MainMenu
        AppStateEnum.SIMPLE_VERSION -> SimpleTranslator
        AppStateEnum.ADVANCED_VERSION -> AdvancedTranslator
        AppStateEnum.BATCH_CREATOR -> BatchCreator
        AppStateEnum.OCR_CREATOR -> OCRCreator
        AppStateEnum.LOAD_OCR_CREATOR -> LoadOCRCreator
        AppStateEnum.TRANSLATION_CREATOR -> TranslationCreator
        AppStateEnum.EDIT_CREATOR -> EditCreator
        AppStateEnum.NEW_PROJECT -> NewProject
        AppStateEnum.PROJECT -> Project
        AppStateEnum.SETTINGS -> Settings
        AppStateEnum.FONT_SETTINGS -> FontSettings
      }
    }
  }
}