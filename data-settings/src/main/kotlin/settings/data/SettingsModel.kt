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
data class SettingsModel(
  val proxyServiceHostname: String = "localhost",
  val proxyServicePort: Int = 443,
  val apiKey: String = "TEST_API_KEY",
  val translationMode: TranslationMode = TranslationMode.GRPC,
  val translationGrpc: TranslationGrpcSettings = TranslationGrpcSettings(),
  val translationDirect: TranslationDirectSettings = TranslationDirectSettings()
) {
  companion object {
    val DEFAULT = SettingsModel()
  }
}
