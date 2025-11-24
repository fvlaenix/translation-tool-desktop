plugins {
  kotlin("jvm")
  id("org.jetbrains.compose")
  id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
  implementation(project(":core-foundation"))
  implementation(project(":core-ui"))
  implementation(project(":domain-translation"))
  implementation(project(":data-projects"))
  implementation(project(":data-images"))
  implementation(project(":data-text"))
  implementation(project(":data-fonts"))

  // Translator project submodules
  implementation(project(":feature-translator:project:images"))
  implementation(project(":feature-translator:project:ocr"))
  implementation(project(":feature-translator:project:translation"))
  implementation(project(":feature-translator:project:edit"))

  // Compose
  implementation(compose.desktop.common)
  implementation(compose.material3)

  // File chooser
  implementation("io.github.vinceglb:filekit-compose:0.8.3")

  // Koin
  implementation("io.insert-koin:koin-core:3.5.0")
  implementation("io.insert-koin:koin-compose:1.1.0")
}
