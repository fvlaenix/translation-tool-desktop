plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  id("org.jetbrains.compose")
  id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
  // Compose
  implementation(compose.desktop.common)
  implementation(compose.runtime)

  // Coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")

  // Koin
  implementation("io.insert-koin:koin-core:3.5.0")

  // JSON serialization
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

  // Testing
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

tasks.test {
  useJUnitPlatform()
}
