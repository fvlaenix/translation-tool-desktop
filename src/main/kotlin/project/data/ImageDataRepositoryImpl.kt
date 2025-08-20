package project.data

import app.batch.ImagePathInfo
import core.base.Repository
import core.utils.SortedImagesUtils.sortedByName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.imageio.ImageIO
import kotlin.io.path.createDirectories
import kotlin.io.path.nameWithoutExtension

class ImageDataRepositoryImpl : ImageDataRepository, Repository {

  override suspend fun loadImages(project: Project, imageType: ImageType): Result<List<ImagePathInfo>> = safeCall {
    withContext(Dispatchers.IO) {
      val workDataPath = project.path.resolve(imageType.folderName)
      val files = workDataPath.toFile().listFiles() ?: emptyArray()

      files.map { it.toPath() }
        .sortedByName()
        .map { imagePath ->
          ImagePathInfo(
            ImageIO.read(imagePath.toFile()),
            imagePath.nameWithoutExtension
          )
        }
    }
  }

  override suspend fun saveImages(project: Project, imageType: ImageType, images: List<ImagePathInfo>): Result<Unit> =
    safeCall {
      withContext(Dispatchers.IO) {
        val workDataPath = project.path.resolve(imageType.folderName)
        workDataPath.createDirectories()

        images.forEachIndexed { index, imagePathInfo ->
          val image = imagePathInfo.image
          val fileName = "${(index + 1).toString().padStart(images.size.toString().length + 1, '0')}.png"
          val path = workDataPath.resolve(fileName)
          ImageIO.write(image, "PNG", path.toFile())
        }
      }
    }

  override suspend fun addImage(project: Project, imageType: ImageType, image: ImagePathInfo): Result<Unit> = safeCall {
    val currentImages = loadImages(project, imageType).getOrElse { emptyList() }.toMutableList()
    currentImages.add(image)
    saveImages(project, imageType, currentImages).getOrThrow()
  }

  override suspend fun clearImages(project: Project, imageType: ImageType): Result<Unit> = safeCall {
    withContext(Dispatchers.IO) {
      val workDataPath = project.path.resolve(imageType.folderName)
      workDataPath.toFile().listFiles()?.forEach { it.delete() }
    }
  }

  override suspend fun getImageCount(project: Project, imageType: ImageType): Result<Int> = safeCall {
    loadImages(project, imageType).getOrElse { emptyList() }.size
  }

  private val batchImages = mutableListOf<ImagePathInfo>()
  private val batchMutex = Mutex()

  override suspend fun addToBatch(image: ImagePathInfo): Result<Unit> = safeCall {
    batchMutex.withLock {
      batchImages.add(image)
    }
  }

  override suspend fun getBatchImages(): Result<List<ImagePathInfo>> = safeCall {
    batchMutex.withLock {
      batchImages.toList()
    }
  }

  override suspend fun clearBatch(): Result<Unit> = safeCall {
    batchMutex.withLock {
      batchImages.clear()
    }
  }

  override suspend fun addAllToBatch(images: List<ImagePathInfo>): Result<Unit> = safeCall {
    batchMutex.withLock {
      batchImages.addAll(images)
    }
  }
}