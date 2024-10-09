package app.batch

import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentLinkedQueue

class BatchService {

  private val mutableList: ConcurrentLinkedQueue<ImagePathInfo> = ConcurrentLinkedQueue()

  fun clear() {
    mutableList.clear()
  }

  fun add(image: ImagePathInfo) {
    mutableList.add(image)
  }

  fun addAll(list: List<ImagePathInfo>) {
    mutableList.addAll(list)
  }

  fun get(): ConcurrentLinkedQueue<ImagePathInfo> = mutableList

  companion object {
    private val DEFAULT = BatchService()

    fun getInstance(): BatchService = DEFAULT
  }
}