package core

import core.di.appModule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

/**
 * Application lifecycle manager. Handles dependency injection initialization and cleanup.
 */
class MangaTranslationApplication {

  /**
   * Initializes dependency injection container with all required modules.
   */
  fun initialize() {
    try {
      startKoin {
        modules(appModule)
      }
    } catch (e: Exception) {
      println("Failed to initialize dependency injection: ${e.message}")
      throw e
    }
  }

  /**
   * Cleanly shuts down dependency injection container.
   */
  fun shutdown() {
    try {
      stopKoin()
    } catch (e: Exception) {
      println("Error during DI shutdown: ${e.message}")
    }
  }
}