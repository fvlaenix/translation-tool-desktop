plugins {
  kotlin("jvm")
}

dependencies {
  implementation(project(":core-foundation"))
  implementation(project(":domain-translation"))
  implementation(project(":data-settings"))
  implementation(project(":service-remote"))

  implementation("com.github.fvlaenix:ai-services:1.0.4")

  // Koin
  implementation("io.insert-koin:koin-core:3.5.0")

  // Testing
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

tasks.test {
  useJUnitPlatform()
}
