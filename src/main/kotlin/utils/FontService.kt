package utils

import bean.LoadingJsonException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.Font
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readText
import kotlin.io.path.writeText

class FontService(private val path: Path) {
  private val fontsPaths: MutableMap<String, Path> = mutableMapOf()
  private val fonts: MutableMap<String, Font> = mutableMapOf()

  init {
    load()
  }

  fun load() {
    fontsPaths.clear()
    fontsPaths.putAll(Json.decodeFromString<Map<String, String>>(path.readText()).mapValues { Path.of(it.value) })
    fonts.clear()
    fonts.putAll(fontsPaths.mapValues { Font.createFont(0, it.value.toFile()) })
  }

  fun save() {
    path.writeText(
      Json { prettyPrint = true }.encodeToString(fontsPaths.mapValues { (_, path) -> path.absolutePathString() })
    )
  }

  fun getFontNotNull(name: String, size: Float): Font = fonts[name]?.deriveFont(size) ?: throw LoadingJsonException("Can't find font: $name. List of fonts: ${fonts.keys.toList()}")

  fun add(name: String, path: Path) {
    fontsPaths[name] = path
    fonts[name] = Font.createFont(0, path.toFile())
    save()
  }

  companion object {
    private val path = Path.of("fonts.json")

    // TODO Dumb singleton, replace it
    private val DEFAULT = FontService(path)

    fun getInstance(): FontService = DEFAULT
  }
}