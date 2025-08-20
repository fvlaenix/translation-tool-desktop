package app.batch

import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import project.data.ImageDataRepository
import java.util.concurrent.ConcurrentLinkedQueue

class BatchService private constructor() : ImagesService, KoinComponent {
  private val imageDataRepository: ImageDataRepository by inject()

  override fun clear() {
    runBlocking {
      imageDataRepository.clearBatch()
    }
  }

  override fun add(image: ImagePathInfo) {
    runBlocking {
      imageDataRepository.addToBatch(image)
    }
  }

  fun addAll(list: List<ImagePathInfo>) {
    runBlocking {
      imageDataRepository.addAllToBatch(list)
    }
  }

  override fun get(): ConcurrentLinkedQueue<ImagePathInfo> {
    return runBlocking {
      val images = imageDataRepository.getBatchImages().getOrElse { emptyList() }
      ConcurrentLinkedQueue(images)
    }
  }

  override suspend fun saveIfRequired() {
    // Batch operations are in-memory, no need to save
  }

  companion object {
    private val DEFAULT = BatchService()

    fun getInstance(): BatchService = DEFAULT
  }
}