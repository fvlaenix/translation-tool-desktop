plugins {
  kotlin("jvm")
  id("org.jetbrains.compose")
  id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
  implementation(project(":core-foundation"))
  implementation(project(":core-ui"))
  implementation(project(":domain-translation"))
  implementation(project(":data-settings"))
  implementation(project(":data-fonts"))
  implementation(project(":data-images"))
  implementation(project(":data-projects"))
  implementation(project(":data-text"))
  implementation(project(":service-ocr"))
  implementation(project(":service-translation"))
  implementation(project(":feature-projects"))

  // Compose
  implementation(compose.desktop.common)
  implementation(compose.material3)

  // Koin
  implementation("io.insert-koin:koin-core:3.5.0")
  implementation("io.insert-koin:koin-compose:1.1.0")
}
