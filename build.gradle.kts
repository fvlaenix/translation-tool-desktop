plugins {
  kotlin("jvm") version "2.1.20" apply false
  kotlin("plugin.serialization") version "2.1.20" apply false
  id("org.jetbrains.compose") version "1.7.3" apply false
  id("org.jetbrains.kotlin.plugin.compose") version "2.1.20" apply false
  id("com.google.protobuf") version "0.9.4" apply false
}

group = "org.example"
version = "1.0-SNAPSHOT"

allprojects {
  repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://maven.fvlaenix.com/repository/maven-public")
    google()
  }
}

subprojects {
  apply(plugin = "org.jetbrains.kotlin.jvm")

  configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
    jvmToolchain(21)
  }
}
