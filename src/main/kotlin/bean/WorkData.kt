package bean

import kotlinx.serialization.Serializable

@Serializable
data class WorkData(
  val version: Int,
  val author: String,
  val imagesData: List<ImageData>
)