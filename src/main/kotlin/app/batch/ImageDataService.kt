package app.batch

import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import project.data.ImageDataRepository
import project.data.ImageType
import project.data.Project
import service.CoroutineServiceScope
import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedQueue

@Deprecated(message = "use repositories instead ImageDataRepository")
class ImageDataService private constructor(val project: Project, folderName: String) : ImagesService, KoinComponent {
  private val imageDataRepository: ImageDataRepository by inject()
  private val loaded = CompletableDeferred<Unit>()

  val workDataPath: Path = project.path.resolve(folderName)
  private val images: ConcurrentLinkedQueue<ImagePathInfo> = ConcurrentLinkedQueue<ImagePathInfo>()

  private val imageType = when (folderName) {
    UNTRANSLATED -> ImageType.UNTRANSLATED
    CLEANED -> ImageType.CLEANED
    EDITED -> ImageType.EDITED
    else -> throw IllegalArgumentException("Unknown folder name: $folderName")
  }

  init {
    CoroutineServiceScope.scope.launch {
      try {
        val loadedImages = imageDataRepository.loadImages(project, imageType).getOrElse { emptyList() }
        images.addAll(loadedImages)
      } finally {
        loaded.complete(Unit)
      }
    }
  }

  suspend fun waitUntilLoaded() = loaded.await()

  override fun clear() {
    runBlocking {
      images.clear()
      imageDataRepository.clearImages(project, imageType)
    }
  }

  override fun add(image: ImagePathInfo) {
    images.add(image)
    runBlocking {
      imageDataRepository.addImage(project, imageType, image)
    }
  }

  fun addAll(list: List<ImagePathInfo>) {
    images.addAll(list)
    runBlocking {
      imageDataRepository.saveImages(project, imageType, images.toList())
    }
  }

  override fun get(): ConcurrentLinkedQueue<ImagePathInfo> = images

  override suspend fun saveIfRequired() {
    withContext(Dispatchers.IO) {
      imageDataRepository.saveImages(project, imageType, images.toList())
    }
  }

  companion object {
    const val UNTRANSLATED = "untranslated"
    const val CLEANED = "cleaned"
    const val EDITED = "edited"

    private val PROJECTS: MutableMap<Pair<Project, String>, ImageDataService> = mutableMapOf()

    fun getInstance(project: Project, folderName: String): ImageDataService =
      PROJECTS.getOrPut(project to folderName) { ImageDataService(project, folderName) }
  }
}