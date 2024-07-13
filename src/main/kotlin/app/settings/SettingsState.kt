package app.settings

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

@Serializable
data class SettingsState(
  val proxyServiceHostname: String = "localhost",
  val apiKey: String = "TEST_API_KEY"
) {
  companion object {
    private const val path = "settings.json"

    var DEFAULT = load()

    fun save(proxyServiceHostname: String, apiKey: String) {
      DEFAULT = SettingsState(proxyServiceHostname, apiKey)
      Path(path).writeText(
        Json { prettyPrint = true }.encodeToString(DEFAULT)
      )
    }

    private fun load(): SettingsState {
      val value = runCatching {
        val text = Path(path).readText()
        Json.decodeFromString<SettingsState>(text)
      }.getOrNull()
      return value ?: SettingsState()
    }
  }
}