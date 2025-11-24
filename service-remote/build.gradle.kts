plugins {
  kotlin("jvm")
  id("com.google.protobuf")
  id("org.jetbrains.compose")
  id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
  implementation(project(":core-foundation"))
  implementation(project(":domain-translation"))
  implementation(project(":data-settings"))

  // Compose
  implementation(compose.runtime)
  implementation(compose.desktop.common)

  // Coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

  // Koin
  implementation("io.insert-koin:koin-core:3.5.0")

  // gRPC
  implementation("io.grpc:grpc-kotlin-stub:1.4.0")
  implementation("com.google.protobuf:protobuf-java:4.28.2")
  implementation("com.google.protobuf:protobuf-kotlin:4.28.2")
  runtimeOnly("io.grpc:grpc-netty-shaded:1.59.0")
  implementation("io.grpc:grpc-protobuf:1.59.0")
  implementation("io.grpc:grpc-stub:1.59.0")
  compileOnly("org.apache.tomcat:annotations-api:6.0.53")

  // Protobuf files
  protobuf(
    files(
      "../discord-bots-rpc/proxy-request.proto",
      "../discord-bots-rpc/ocr-request.proto",
      "../discord-bots-rpc/gpt-request.proto",
      "../discord-bots-rpc/is-alive.proto",
      "../discord-bots-rpc/image.proto"
    )
  )
}

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:3.24.4"
  }
  plugins {
    create("grpc") {
      artifact = "io.grpc:protoc-gen-grpc-java:1.59.0"
    }
    create("grpckt") {
      artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.0:jdk8@jar"
    }
  }
  generateProtoTasks {
    all().forEach {
      it.plugins {
        create("grpc")
        create("grpckt")
      }
      it.builtins {
        create("kotlin")
      }
    }
  }
}
