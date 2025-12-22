pluginManagement {
  repositories {
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://maven.fvlaenix.com/repository/maven-public/")
    google()
    gradlePluginPortal()
    mavenCentral()
  }

  plugins {
    kotlin("jvm").version(extra["kotlin.version"] as String)
    id("org.jetbrains.compose").version(extra["compose.version"] as String)
    id("org.jetbrains.kotlin.plugin.compose").version(extra["kotlin.version"] as String)
  }
}

rootProject.name = "translation-tool-desktop"

// Core modules
include(":core-foundation")
include(":core-ui")
include(":core-di")

// Domain
include(":domain-translation")

// Data modules
include(":data-settings")
include(":data-fonts")
include(":data-projects")
include(":data-images")
include(":data-text")

// Service modules
include(":service-remote")
include(":service-ocr")
include(":service-translation")

// Feature modules
include(":feature-main-menu")
include(":feature-settings")
include(":feature-fonts")
include(":feature-projects")

// Translator features
include(":feature-translator:simple")
include(":feature-translator:advanced")
include(":feature-translator:project:common")
include(":feature-translator:project:images")
include(":feature-translator:project:ocr")
include(":feature-translator:project:translation")
include(":feature-translator:project:edit")

// App modules
include(":app-shell")
include(":app-desktop")
