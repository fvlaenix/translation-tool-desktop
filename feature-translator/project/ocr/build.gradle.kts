plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  id("org.jetbrains.compose")
  id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
  implementation(project(":core-foundation"))
  implementation(project(":core-ui"))
  implementation(project(":domain-translation"))
  implementation(project(":data-images"))
  implementation(project(":data-text"))
  implementation(project(":data-fonts"))
  implementation(project(":data-projects"))
  implementation(project(":feature-fonts"))
  implementation(project(":service-ocr"))
  implementation(project(":feature-translator:project:common"))

  // JSON serialization
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

  // reorderable list
  implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")

  // Compose
  implementation(compose.desktop.common)
  implementation(compose.material3)

  // Koin
  implementation("io.insert-koin:koin-core:3.5.0")
  implementation("io.insert-koin:koin-compose:1.1.0")
}
