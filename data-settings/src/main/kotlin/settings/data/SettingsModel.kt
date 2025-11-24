package settings.data

import kotlinx.serialization.Serializable

@Serializable
data class SettingsModel(
  val proxyServiceHostname: String = "localhost",
  val proxyServicePort: Int = 443,
  val apiKey: String = "TEST_API_KEY"
) {
  companion object {
    val DEFAULT = SettingsModel()
  }
}