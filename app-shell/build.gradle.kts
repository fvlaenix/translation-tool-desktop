plugins {
  kotlin("jvm")
  id("org.jetbrains.compose")
  id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
  implementation(project(":core-foundation"))
  implementation(project(":core-ui"))
  implementation(project(":core-di"))
  implementation(project(":data-projects"))
  implementation(project(":domain-translation"))
  implementation(project(":feature-main-menu"))
  implementation(project(":feature-settings"))
  implementation(project(":feature-fonts"))
  implementation(project(":feature-projects"))
  implementation(project(":feature-translator:simple"))
  implementation(project(":feature-translator:advanced"))
  implementation(project(":feature-translator:project:common"))
  implementation(project(":feature-translator:project:images"))
  implementation(project(":feature-translator:project:ocr"))
  implementation(project(":feature-translator:project:translation"))
  implementation(project(":feature-translator:project:edit"))

  // Compose
  implementation(compose.desktop.common)
  implementation(compose.material3)

  // Koin
  implementation("io.insert-koin:koin-core:3.5.0")
  implementation("io.insert-koin:koin-compose:1.1.0")
}
