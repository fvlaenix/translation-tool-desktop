package app.batch

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import project.ImagesProjectData
import project.Project
import service.CoroutineServiceScope
import utils.SortedImagesUtils.sortedByName
import java.util.concurrent.ConcurrentLinkedQueue
import javax.imageio.ImageIO
import kotlin.io.path.createDirectories
import kotlin.io.path.nameWithoutExtension

// TODO remove project, obsolete
class BatchService private constructor(val project: Project?) : ImagesService {
  private val imagesProjectData: ImagesProjectData? = project?.data as ImagesProjectData?
  private val loaded = CompletableDeferred<Unit>()

  init {
    if (imagesProjectData != null) {
      CoroutineServiceScope.scope.launch {
        try {
          val project = project!!
          val path = project.path
          val uneditedImagesPath = path.resolve(imagesProjectData.uneditedImagesFolderName)

          val images = (uneditedImagesPath.toFile().listFiles() ?: emptyArray()).map { it.toPath() }.sortedByName()
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
    } else {
      loaded.complete(Unit)
    }
  }

  suspend fun waitUntilLoaded() = loaded.await()

  private val mutableList: ConcurrentLinkedQueue<ImagePathInfo> = ConcurrentLinkedQueue()

  override fun clear() {
    mutableList.clear()
  }

  override fun add(image: ImagePathInfo) {
    mutableList.add(image)
  }

  fun addAll(list: List<ImagePathInfo>) {
    mutableList.addAll(list)
  }

  override fun get(): ConcurrentLinkedQueue<ImagePathInfo> = mutableList

  override suspend fun saveIfRequired() {
    if (project != null) {
      imagesProjectData!!
      withContext(Dispatchers.IO) {
        val folder = project.path.resolve(imagesProjectData.uneditedImagesFolderName)
        folder.createDirectories()
        val list = get()
        list.forEachIndexed { index, imagePathInfo ->
          val image = imagePathInfo.image
          val path = folder.resolve("${(index + 1).toString().padStart(list.size.toString().length + 1, '0')}.png")
          ImageIO.write(image, "PNG", path.toFile())
        }
      }
    }
  }

  companion object {
    private val DEFAULT = BatchService(null)
    private val PROJECTS: MutableMap<Project, BatchService> = mutableMapOf()

    fun getInstance(): BatchService = DEFAULT

    @Deprecated(message = "Use ImageDatService instead")
    fun getInstance(project: Project): BatchService = PROJECTS.getOrPut(project) { BatchService(project) }
  }
}