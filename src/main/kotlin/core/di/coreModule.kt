package core.di

import core.error.ErrorHandler
import core.navigation.NavigationController
import org.koin.dsl.module

val coreModule = module {

  // Navigation
  single { NavigationController() }

  // Error handling
  single { ErrorHandler() }
}