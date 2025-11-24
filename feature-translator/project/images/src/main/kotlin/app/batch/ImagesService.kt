package app.batch

@Deprecated("Make something else")
interface ImagesService {
  suspend fun add(image: ImagePathInfo)
  suspend fun clear()
  suspend fun get(): List<ImagePathInfo>
  suspend fun saveIfRequired()
}