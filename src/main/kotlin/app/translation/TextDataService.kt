package app.translation

import bean.WorkData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import project.ImagesProjectData
import project.Project
import service.CoroutineServiceScope
import utils.JSON
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

class TextDataService private constructor(val project: Project, val language: String) {
  private var imagesProjectData: ImagesProjectData = project.data as ImagesProjectData
  private val loaded = CompletableDeferred<Unit>()

  // TODO remove postfixTextDataFileName obsolete
  val workDataPath: Path = project.path.resolve(language + imagesProjectData.postfixTextDataFileName)

  init {
    CoroutineServiceScope.scope.launch {
      try {
        if (workDataPath.exists()) {
          workData = JSON.decodeFromString(workDataPath.readText())
        }
      } finally {
        loaded.complete(Unit)
      }
    }
  }

  // TODO use it
  suspend fun waitUntilLoaded() = loaded.await()

  var workData: WorkData? = null

  // TODO use it
  suspend fun save() {
    // TODO fix NPE
    val workData = workData!!
    withContext(Dispatchers.IO) {
      workDataPath.writeText(JSON.encodeToString(workData))
    }
  }

  companion object {
    const val UNTRANSLATED = "untranslated"
    const val TRANSLATED = "translated"

    private val PROJECTS: MutableMap<Pair<Project, String>, TextDataService> = mutableMapOf()

    fun getInstance(project: Project, language: String): TextDataService =
      PROJECTS.getOrPut(project to language) { TextDataService(project, language) }
  }
}