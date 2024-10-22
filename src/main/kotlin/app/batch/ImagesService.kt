package app.batch

import java.util.concurrent.ConcurrentLinkedQueue

interface ImagesService {
  fun add(image: ImagePathInfo)
  fun clear()
  fun get(): ConcurrentLinkedQueue<ImagePathInfo>
  suspend fun saveIfRequired()
}