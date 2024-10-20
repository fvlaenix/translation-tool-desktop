package app.batch

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import project.Project
import service.CoroutineServiceScope
import utils.SortedImagesUtils.sortedByName
import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedQueue
import javax.imageio.ImageIO
import kotlin.io.path.createDirectories
import kotlin.io.path.nameWithoutExtension

class ImageDataService private constructor(val project: Project, folderName: String) : ImagesService {
  private val loaded = CompletableDeferred<Unit>()

  val workDataPath: Path = project.path.resolve(folderName)
  private val images: ConcurrentLinkedQueue<ImagePathInfo> = ConcurrentLinkedQueue<ImagePathInfo>()

  init {
    CoroutineServiceScope.scope.launch {
      try {
        val images = (workDataPath.toFile().listFiles() ?: emptyArray()).map { it.toPath() }.sortedByName()
        images.forEach { imagePath ->
          val imagePathInfo = ImagePathInfo(
            ImageIO.read(imagePath.toFile()),
            imagePath.nameWithoutExtension
          )
          add(imagePathInfo)
        }
      } finally {
        loaded.complete(Unit)
      }
    }
  }

  // TODO call it
  suspend fun waitUntilLoaded() = loaded.await()

  override fun clear() {
    images.clear()
  }

  override fun add(image: ImagePathInfo) {
    images.add(image)
  }

  fun addAll(list: List<ImagePathInfo>) {
    images.addAll(list)
  }

  override fun get(): ConcurrentLinkedQueue<ImagePathInfo> = images

  override suspend fun saveIfRequired() {
    withContext(Dispatchers.IO) {
      workDataPath.createDirectories()
      val list = get()
      list.forEachIndexed { index, imagePathInfo ->
        val image = imagePathInfo.image
        val path = workDataPath.resolve("${(index + 1).toString().padStart(list.size.toString().length + 1, '0')}.png")
        ImageIO.write(image, "PNG", path.toFile())
      }
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