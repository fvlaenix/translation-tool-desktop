package settings.data

import kotlinx.serialization.Serializable

@Serializable
enum class TranslationMode {
  GRPC,
  DIRECT
}

@Serializable
enum class TranslationModelProvider {
  OPENAI,
  OPENROUTER,
  OTHER
}

@Serializable
enum class OCRMode {
  GRPC,
  DIRECT
}

@Serializable
data class TranslationDirectSettings(
  val apiKey: String = "",
  val provider: TranslationModelProvider = TranslationModelProvider.OPENAI,
  val modelName: String = "gpt-4o",
  val apiBaseUrl: String = "",
  val timeoutSeconds: Int = 120
)

@Serializable
data class TranslationGrpcSettings(
  val placeholder: String = ""
)

@Serializable
data class OCRGrpcSettings(
  val apiKey: String = ""
)

@Serializable
data class OCRDirectSettings(
  val credentialsPath: String = "",
  val timeoutSeconds: Int = 120
)

@Serializable
data class SettingsModel(
  val proxyServiceHostname: String = "localhost",
  val proxyServicePort: Int = 443,
  val apiKey: String = "TEST_API_KEY",
  val translationMode: TranslationMode = TranslationMode.GRPC,
  val translationGrpc: TranslationGrpcSettings = TranslationGrpcSettings(),
  val translationDirect: TranslationDirectSettings = TranslationDirectSettings(),
  val ocrMode: OCRMode = OCRMode.GRPC,
  val ocrGrpc: OCRGrpcSettings = OCRGrpcSettings(),
  val ocrDirect: OCRDirectSettings = OCRDirectSettings()
) {
  companion object {
    val DEFAULT = SettingsModel()
  }
}
