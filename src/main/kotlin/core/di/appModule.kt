package core.di

import fonts.di.fontModule
import org.koin.dsl.module
import settings.di.settingsModule

val appModule = module {
  includes(
    coreModule,
    settingsModule,
    fontModule
  )
}