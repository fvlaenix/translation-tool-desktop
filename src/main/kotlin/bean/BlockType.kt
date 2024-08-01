package bean

import kotlinx.serialization.Serializable

@Serializable
sealed class BlockType {
  @Serializable
  data class Rectangle(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
  ): BlockType()

  @Serializable
  data class Oval(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
  ): BlockType()
}