package translation.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@Immutable
data class BlockPosition(
  val x: Double,
  val y: Double,
  val width: Double,
  val height: Double,
  val shape: Shape
) {
  @Serializable
  sealed interface Shape {
    @Serializable
    data object Rectangle : Shape

    @Serializable
    data object Oval : Shape
  }

  @Transient
  var heavyChangeListener: HeavyChangeListener? = null

  interface HeavyChangeListener {
    fun onChange()
  }
}