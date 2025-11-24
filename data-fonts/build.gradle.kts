plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
}

dependencies {
  implementation(project(":core-foundation"))
  implementation(project(":domain-translation"))

  // Coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

  // JSON serialization
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

  // Koin
  implementation("io.insert-koin:koin-core:3.5.0")
}
