package bean

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.awt.Color

/**
 * Serializable color wrapper with RGBA values and validation.
 */
@Serializable
data class BeanColor(val r: Int, val g: Int, val b: Int, val a: Int) {

  init {
    require(r in 0..255) { "r must be between 0 and 255" }
    require(g in 0..255) { "g must be between 0 and 255" }
    require(b in 0..255) { "b must be between 0 and 255" }
    require(a in 0..255) { "a must be between 0 and 255" }
  }

  private fun require(value: Boolean, lazyMessage: () -> String) {
    if (!value) throw LoadingJsonException(lazyMessage())
  }

  @Transient
  val color: Color = Color(r, g, b, a)

  companion object {
    val DEFAULT = BeanColor(0, 0, 0, 255)
    val BLACK = fromColor(Color.BLACK)
    val WHITE = fromColor(Color.WHITE)
    val TRANSPARENT = BeanColor(0, 0, 0, 0)

    /**
     * Creates a BeanColor from a Java AWT Color.
     */
    fun fromColor(color: Color) = BeanColor(color.red, color.green, color.blue, color.alpha)
  }
}