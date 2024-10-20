package utils

import bean.WorkData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

class CurrentWorkService {
  var currentWork: WorkData? = null

  fun save(name: String) {
    Path.of("$name.json").writeText(Json { prettyPrint = true }.encodeToString(currentWork))
  }

  fun load(name: String) {
    currentWork = Json { prettyPrint = true }.decodeFromString(Path.of("$name.json").readText())
  }

  companion object {
    private val INSTANCE = CurrentWorkService()

    fun getInstance(): CurrentWorkService = INSTANCE
  }
}