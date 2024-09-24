package bean

import kotlinx.serialization.Serializable

@Serializable
sealed class BlockType {
  abstract val x: Int
  abstract val y: Int
  abstract val width: Int
  abstract val height: Int

  @Serializable
  data class Rectangle(
    override val x: Int,
    override val y: Int,
    override val width: Int,
    override val height: Int,
  ): BlockType()

  @Serializable
  data class Oval(
    override val x: Int,
    override val y: Int,
    override val width: Int,
    override val height: Int,
  ): BlockType()
}