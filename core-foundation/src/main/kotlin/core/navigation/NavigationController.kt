package core.navigation

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

/**
 * Navigation controller that manages app navigation state.
 *
 * This centralized navigation approach provides:
 * - Single source of truth for current screen/destination
 * - Type-safe navigation using sealed classes
 * - Reactive navigation state for UI components
 * - Easy navigation validation and history management
 *
 * Replaces the old approach of passing navigation state through multiple components.
 * ViewModels and UI components can inject this controller to trigger navigation.
 */
class NavigationController {

  private val _currentDestination = mutableStateOf<NavigationDestination>(NavigationDestination.MainMenu)
  val currentDestination: State<NavigationDestination> = _currentDestination

  /**
   * Navigate to a specific destination.
   */
  fun navigateTo(destination: NavigationDestination) {
    _currentDestination.value = destination
  }

  /**
   * Quick navigation back to main menu - most common navigation action
   */
  fun navigateToMainMenu() {
    navigateTo(NavigationDestination.MainMenu)
  }

  /**
   * Get current destination for navigation logic
   */
  fun getCurrentDestination(): NavigationDestination {
    return _currentDestination.value
  }

  /**
   * Check if currently at main menu (useful for back button logic)
   */
  fun isAtMainMenu(): Boolean {
    return _currentDestination.value is NavigationDestination.MainMenu
  }

  /**
   * Validate navigation transition (future enhancement)
   * For now, all transitions are allowed for simplicity
   */
  private fun isValidTransition(from: NavigationDestination, to: NavigationDestination): Boolean {
    return true // All transitions allowed for simplicity
  }
}