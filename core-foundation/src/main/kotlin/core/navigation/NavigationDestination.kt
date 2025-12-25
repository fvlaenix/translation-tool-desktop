package core.navigation

/**
 * Sealed class representing all possible navigation destinations
 */
sealed class NavigationDestination {

  // Main sections
  object MainMenu : NavigationDestination()

  // Translation tools
  object SimpleTranslator : NavigationDestination()
  object AdvancedTranslator : NavigationDestination()

  // Workflow tools
  object BatchCreator : NavigationDestination()
  object OCRCreator : NavigationDestination()
  object LoadOCRCreator : NavigationDestination()
  object TranslationCreator : NavigationDestination()
  object EditCreator : NavigationDestination()

  // Project management
  object NewProject : NavigationDestination()
  object Project : NavigationDestination()

  // Settings
  object Settings : NavigationDestination()
  object FontSettings : NavigationDestination()
  object TranslationSettings : NavigationDestination()
  object OCRSettings : NavigationDestination()
}
