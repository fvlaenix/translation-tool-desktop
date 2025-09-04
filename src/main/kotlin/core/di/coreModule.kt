package core.di

import core.error.ErrorHandler
import core.navigation.NavigationController
import org.koin.dsl.module

/**
 * Core dependency injection module providing navigation and error handling services.
 */
val coreModule = module {

  // Navigation
  single { NavigationController() }

  // Error handling
  single { ErrorHandler() }
}