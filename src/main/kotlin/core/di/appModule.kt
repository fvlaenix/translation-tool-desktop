package core.di

import fonts.di.fontModule
import org.koin.dsl.module
import project.di.projectModule
import settings.di.settingsModule

val appModule = module {
  includes(
    coreModule,
    settingsModule,
    fontModule,
    projectModule
  )
}