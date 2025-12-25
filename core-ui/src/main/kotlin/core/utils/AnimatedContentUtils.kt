package core.utils

import androidx.compose.animation.*
import core.navigation.NavigationDestination

/**
 * Utility for creating animated content transitions with navigation-aware direction.
 */
object AnimatedContentUtils {
  /**
   * Creates horizontal slide transitions based on navigation destination order.
   */
  fun <T : NavigationDestination> horizontalSpec(): AnimatedContentTransitionScope<T>.() -> ContentTransform = {
    val targetOrder = getNavigationOrder(targetState as NavigationDestination)
    val initialOrder = getNavigationOrder(initialState as NavigationDestination)

    if (targetOrder > initialOrder) {
      slideInHorizontally { height -> height } togetherWith slideOutHorizontally { height -> -height }
    } else {
      slideInHorizontally { height -> -height } togetherWith slideOutHorizontally { height -> height }
    }.using(
      SizeTransform(clip = false)
    )
  }

  private fun getNavigationOrder(destination: NavigationDestination): Int {
    return when (destination) {
      NavigationDestination.MainMenu -> 0
      NavigationDestination.SimpleTranslator -> 1
      NavigationDestination.AdvancedTranslator -> 2
      NavigationDestination.BatchCreator -> 3
      NavigationDestination.OCRCreator -> 4
      NavigationDestination.LoadOCRCreator -> 5
      NavigationDestination.TranslationCreator -> 6
      NavigationDestination.EditCreator -> 7
      NavigationDestination.NewProject -> 8
      NavigationDestination.Project -> 9
      NavigationDestination.Settings -> 10
      NavigationDestination.FontSettings -> 11
      NavigationDestination.TranslationSettings -> 12
      NavigationDestination.OCRSettings -> 13
    }
  }
}
