plugins {
  kotlin("jvm")
  id("org.jetbrains.compose")
  id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
  implementation(project(":core-foundation"))
  implementation(project(":domain-translation"))

  // Compose
  implementation(compose.desktop.common)
  implementation(compose.material3)
  implementation(compose.animation)
  implementation(compose.ui)
  implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")

  // Coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")

  // Testing
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
  testImplementation("org.jetbrains.compose.ui:ui-test-junit4:1.6.10")
}

tasks.test {
  useJUnitPlatform()
}
