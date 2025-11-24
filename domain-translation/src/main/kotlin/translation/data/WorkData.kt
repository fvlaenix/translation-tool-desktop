package translation.data

import kotlinx.serialization.Serializable

/**
 * Contains complete work data with version, author and image collection.
 */
@Serializable
data class WorkData(
  val version: Int,
  val author: String,
  val imagesData: List<ImageData>
)