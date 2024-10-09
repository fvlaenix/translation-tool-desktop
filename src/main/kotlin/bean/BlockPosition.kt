package bean

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

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
    data object Rectangle: Shape
    @Serializable
    data object Oval: Shape
  }
}