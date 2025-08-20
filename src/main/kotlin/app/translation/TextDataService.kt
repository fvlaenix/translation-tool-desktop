// Replace app/translation/TextDataService.kt with this adapter version:

package app.translation

import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import project.data.Project
import project.data.TextDataRepository
import project.data.TextType
import service.CoroutineServiceScope
import translation.data.WorkData
import java.nio.file.Path

@Deprecated(message = "use repositories instead TextDataRepository")
class TextDataService private constructor(val project: Project, language: String) : KoinComponent {
  private val textDataRepository: TextDataRepository by inject()
  private val loaded = CompletableDeferred<Unit>()

  val workDataPath: Path = project.path.resolve("$language-text.json")

  private val textType = when (language) {
    UNTRANSLATED -> TextType.UNTRANSLATED
    TRANSLATED -> TextType.TRANSLATED
    else -> throw IllegalArgumentException("Unknown language: $language")
  }

  init {
    CoroutineServiceScope.scope.launch {
      try {
        // Load work data from repository
        _workData = textDataRepository.loadWorkData(project, textType).getOrNull()
      } finally {
        loaded.complete(Unit)
      }
    }
  }

  suspend fun waitUntilLoaded() = loaded.await()

  private var _workData: WorkData? = null

  var workData: WorkData?
    get() = _workData
    set(value) {
      _workData = value
      if (value != null) {
        runBlocking {
          textDataRepository.saveWorkData(project, textType, value)
        }
      }
    }

  suspend fun save() {
    val currentWorkData = _workData
    if (currentWorkData != null) {
      withContext(Dispatchers.IO) {
        textDataRepository.saveWorkData(project, textType, currentWorkData).getOrThrow()
      }
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