plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  id("org.jetbrains.compose")
  id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
  implementation(project(":core-foundation"))
  implementation(project(":domain-translation"))

  // Compose
  implementation(compose.runtime)

  // Coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

  // JSON serialization
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

  // Koin
  implementation("io.insert-koin:koin-core:3.5.0")
}
