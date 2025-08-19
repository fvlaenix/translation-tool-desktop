package bean

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.awt.Font

@Serializable
data class BlockSettings(
  val fontName: String,
  val fontSize: Int = 10,
  val fontColor: BeanColor = BeanColor.BLACK,
  val alignment: Alignment = Alignment.CENTER,
  val outlineColor: BeanColor = BeanColor.WHITE,
  val outlineSize: Double = 5.0,
  val backgroundColor: BeanColor = BeanColor.TRANSPARENT,
  val border: Int = 5
) {
  @Transient
  private var _font: Font? = null

  // This will be set externally when the font is resolved
  @Transient
  var font: Font
    get() = _font ?: Font("Arial", Font.PLAIN, fontSize) // fallback font
    set(value) {
      _font = value
    }

  companion object {
    fun createWithFont(
      fontName: String,
      font: Font,
      fontSize: Int = 10,
      fontColor: BeanColor = BeanColor.BLACK,
      alignment: Alignment = Alignment.CENTER,
      outlineColor: BeanColor = BeanColor.WHITE,
      outlineSize: Double = 5.0,
      backgroundColor: BeanColor = BeanColor.TRANSPARENT,
      border: Int = 5
    ): BlockSettings {
      return BlockSettings(
        fontName = fontName,
        fontSize = fontSize,
        fontColor = fontColor,
        alignment = alignment,
        outlineColor = outlineColor,
        outlineSize = outlineSize,
        backgroundColor = backgroundColor,
        border = border
      ).apply {
        this.font = font.deriveFont(fontSize.toFloat())
      }
    }
  }
}