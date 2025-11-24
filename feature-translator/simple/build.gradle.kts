plugins {
  kotlin("jvm")
  id("org.jetbrains.compose")
  id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
  implementation(project(":core-foundation"))
  implementation(project(":core-ui"))
  implementation(project(":domain-translation"))
  implementation(project(":service-ocr"))
  implementation(project(":service-translation"))

  // Compose
  implementation(compose.desktop.common)
  implementation(compose.material3)

  // Koin
  implementation("io.insert-koin:koin-core:3.5.0")
  implementation("io.insert-koin:koin-compose:1.1.0")
}
