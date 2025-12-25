plugins {
  kotlin("jvm")
}

dependencies {
  implementation(project(":core-foundation"))
  implementation(project(":domain-translation"))
  implementation(project(":data-settings"))
  implementation(project(":service-remote"))

  // Compose UI (for IntSize)
  implementation("androidx.compose.ui:ui:1.7.3")

  // OCR library (includes Google Cloud Vision transitively)
  implementation("com.github.fvlaenix:ocr-image-to-text:1.0.3")

  // gRPC
  implementation("io.grpc:grpc-kotlin-stub:1.4.0")
  implementation("io.grpc:grpc-protobuf:1.59.0")
  implementation("io.grpc:grpc-stub:1.59.0")
  runtimeOnly("io.grpc:grpc-netty-shaded:1.59.0")

  // Protobuf
  implementation("com.google.protobuf:protobuf-java:3.16.3")
  implementation("com.google.protobuf:protobuf-kotlin:3.24.4")

  // Coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

  // Koin
  implementation("io.insert-koin:koin-core:3.5.0")
}
