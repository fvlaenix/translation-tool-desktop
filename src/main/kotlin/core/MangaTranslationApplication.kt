package core

import core.di.appModule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

class MangaTranslationApplication {

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

  fun shutdown() {
    try {
      stopKoin()
    } catch (e: Exception) {
      println("Error during DI shutdown: ${e.message}")
    }
  }
}