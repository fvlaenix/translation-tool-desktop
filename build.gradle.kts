import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.google.protobuf.gradle.id
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
  kotlin("jvm")
  id("com.github.johnrengelman.shadow") version "8.1.1"
  id("com.google.protobuf") version "0.9.4"
  id("org.jetbrains.compose")
  id("org.jetbrains.kotlin.plugin.compose")
  kotlin("plugin.serialization") version "2.0.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
  maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
  google()
}

kotlin {
  val versions = listOf("macos-x64", "macos-arm64", "windows-x64", "windows-arm64", "linux-x64", "linux-arm64")

  val version = "0.7.70"

  sourceSets {
    dependencies {
      for (versiona in versions) {
        implementation("org.jetbrains.skiko:skiko-awt-runtime-$versiona:$version")
      }
    }
  }
}

dependencies {
  // Note, if you develop a library, you should use compose.desktop.common.
  // compose.desktop.currentOs should be used in launcher-sourceSet
  // (in a separate module for demo project and in testMain).
  // With compose.desktop.common you will also lose @Preview functionality
  implementation(compose.desktop.currentOs)

  // grpc
  implementation("io.grpc:grpc-kotlin-stub:1.4.0")
  implementation("com.google.protobuf:protobuf-java:3.16.3")
  implementation("com.google.protobuf:protobuf-kotlin:3.24.4")
  runtimeOnly("io.grpc:grpc-netty-shaded:1.59.0")
  implementation("io.grpc:grpc-protobuf:1.59.0")
  implementation("io.grpc:grpc-stub:1.59.0")
  compileOnly("org.apache.tomcat:annotations-api:6.0.53")
  protobuf(files("discord-bots-rpc/proxy-request.proto", "discord-bots-rpc/ocr-request.proto", "discord-bots-rpc/gpt-request.proto", "discord-bots-rpc/is-alive.proto", "discord-bots-rpc/image.proto"))

  // coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

  // json
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
}

compose.desktop {
  application {
    mainClass = "MainKt"

    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = "translation-tool-desktop"
      packageVersion = "1.0.0"
    }
  }
}

task<JavaExec>("runServer") {
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("MainKt")
}

fun createJarTaskByJavaExec(name: String, resultName: String) = tasks.create<ShadowJar>("${name}Jar") {
  mergeServiceFiles()
  group = "shadow"
  description = "Run server $name"

  from(sourceSets.main.get().output)
  from(project.configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
  exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
  configurations = listOf(project.configurations.runtimeClasspath.get())

  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  archiveFileName.set("${resultName}.jar")
  manifest {
    attributes["Main-Class"] = (tasks.findByName(name) as JavaExec).mainClass.get()
  }
}.apply task@ { tasks.named("jar") { dependsOn(this@task) } }

createJarTaskByJavaExec("runServer", "translation-tool-0.1-beta")

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:3.24.4"
  }
  plugins {
    id("grpc") {
      artifact = "io.grpc:protoc-gen-grpc-java:1.59.0"
    }
    create("grpckt") {
      artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.0:jdk8@jar"
    }
  }
  generateProtoTasks {
    all().forEach {
      it.plugins {
        id("grpc")
        id("grpckt")
      }
      it.builtins {
        create("kotlin")
      }
    }
  }
}