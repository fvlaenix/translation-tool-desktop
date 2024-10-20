package app.batch

import java.util.concurrent.ConcurrentLinkedQueue

class BatchService private constructor() : ImagesService {
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

  override suspend fun saveIfRequired() {}

  companion object {
    private val DEFAULT = BatchService()

    fun getInstance(): BatchService = DEFAULT
  }
}