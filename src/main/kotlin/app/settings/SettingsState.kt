package app.settings

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

@Serializable
data class SettingsState(
  val ocrServiceHostname: String = "localhost",
  val translatorServiceHostname: String = "localhost"
) {
  companion object {
    private const val path = "settings.json"

    var DEFAULT = load()

    fun save(ocrServiceHostname: String, translatorServiceHostname: String) {
      DEFAULT = SettingsState(ocrServiceHostname, translatorServiceHostname)
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