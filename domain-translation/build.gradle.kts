plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  id("org.jetbrains.compose")
  id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
  // Compose (for @Immutable annotations and IntSize)
  implementation(compose.desktop.common)
  implementation(compose.runtime)

  // JSON serialization
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
}
