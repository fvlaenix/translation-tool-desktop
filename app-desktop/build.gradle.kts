import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
  kotlin("jvm")
  id("org.jetbrains.compose")
  id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
  implementation(project(":data-projects"))
  implementation(project(":data-fonts"))
  implementation(project(":app-shell"))

  // Koin
  implementation("io.insert-koin:koin-core:3.5.0")

  // Compose Desktop
  implementation(compose.desktop.currentOs)

  // Skiko runtime for multiple platforms
  val skikoVersion = "0.9.24"
  val versions = listOf("macos-x64", "macos-arm64", "windows-x64", "windows-arm64", "linux-x64", "linux-arm64")
  versions.forEach { platform ->
    implementation("org.jetbrains.skiko:skiko-awt-runtime-$platform:$skikoVersion")
  }
}

compose.desktop {
  application {
    mainClass = "MainKt"

    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = "translation-tool-desktop"
      packageVersion = "1.0.0"

      linux {
        modules("jdk.security.auth")
      }
    }
  }
}
