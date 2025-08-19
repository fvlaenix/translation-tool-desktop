package core.di

import org.koin.dsl.module
import settings.di.settingsModule

val appModule = module {
  includes(
    coreModule,
    settingsModule
  )
}