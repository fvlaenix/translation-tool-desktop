plugins {
  kotlin("jvm")
}

dependencies {
  implementation(project(":core-foundation"))
  implementation(project(":domain-translation"))
  implementation(project(":data-projects"))

  // Coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

  // Koin
  implementation("io.insert-koin:koin-core:3.5.0")
}
