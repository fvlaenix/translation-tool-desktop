package core.navigation

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import app.AppStateEnum

/**
 * Navigation controller that manages app navigation state
 * Wraps the existing AppStateEnum system for better structure
 */
class NavigationController {

  private val _currentDestination = mutableStateOf<NavigationDestination>(NavigationDestination.MainMenu)
  val currentDestination: State<NavigationDestination> = _currentDestination

  private val _currentAppState = mutableStateOf(AppStateEnum.MAIN_MENU)
  val currentAppState: State<AppStateEnum> = _currentAppState

  /**
   * Navigate to a specific destination
   */
  fun navigateTo(destination: NavigationDestination) {
    _currentDestination.value = destination
    _currentAppState.value = destination.appState
  }

  /**
   * Navigate back to main menu
   */
  fun navigateToMainMenu() {
    navigateTo(NavigationDestination.MainMenu)
  }

  /**
   * Get current destination
   */
  fun getCurrentDestination(): NavigationDestination {
    return _currentDestination.value
  }

  /**
   * Check if currently at main menu
   */
  fun isAtMainMenu(): Boolean {
    return _currentDestination.value is NavigationDestination.MainMenu
  }

  /**
   * Legacy support - navigate using AppStateEnum
   * This allows gradual migration from the old system
   */
  fun navigateToAppState(appState: AppStateEnum) {
    val destination = NavigationDestination.fromAppState(appState)
    navigateTo(destination)
  }

  /**
   * Validate navigation transition
   * For now, all transitions are allowed
   */
  private fun isValidTransition(from: NavigationDestination, to: NavigationDestination): Boolean {
    return true // All transitions allowed for simplicity
  }
}