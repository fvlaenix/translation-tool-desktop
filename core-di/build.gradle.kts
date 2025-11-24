plugins {
  kotlin("jvm")
}

dependencies {
  implementation(project(":core-foundation"))

  // Data modules (for DI module aggregation)
  implementation(project(":data-text"))
  implementation(project(":data-images"))
  implementation(project(":service-ocr"))
  implementation(project(":service-translation"))

  // Feature modules (for DI module aggregation)
  implementation(project(":feature-settings"))
  implementation(project(":feature-fonts"))
  implementation(project(":feature-projects"))
  implementation(project(":feature-translator:advanced"))
  implementation(project(":feature-translator:simple"))
  implementation(project(":feature-translator:project:ocr"))
  implementation(project(":feature-translator:project:translation"))
  implementation(project(":feature-translator:project:edit"))

  // Koin DI
  implementation("io.insert-koin:koin-core:3.5.0")
  implementation("io.insert-koin:koin-compose:1.1.0")
}
