plugins {
  kotlin("jvm")
}

dependencies {
  implementation(project(":core-foundation"))
  implementation(project(":domain-translation"))
  implementation(project(":data-settings"))
  implementation(project(":service-remote"))

  // Koin
  implementation("io.insert-koin:koin-core:3.5.0")
}
