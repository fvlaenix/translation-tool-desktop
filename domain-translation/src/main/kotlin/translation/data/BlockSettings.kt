package translation.data

import bean.Alignment
import bean.BeanColor
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.awt.Font

/**
 * Contains styling settings for text blocks including font, colors, and alignment.
 */
@Serializable
data class BlockSettings(
  val fontName: String,
  val fontSize: Int = 10,
  val fontColor: BeanColor = BeanColor.Companion.BLACK,
  val alignment: Alignment = Alignment.CENTER,
  val outlineColor: BeanColor = BeanColor.Companion.WHITE,
  val outlineSize: Double = 5.0,
  val backgroundColor: BeanColor = BeanColor.Companion.TRANSPARENT,
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
    /**
     * Creates block settings with a pre-resolved font instance.
     */
    fun createWithFont(
      fontName: String,
      font: Font,
      fontSize: Int = 10,
      fontColor: BeanColor = BeanColor.Companion.BLACK,
      alignment: Alignment = Alignment.CENTER,
      outlineColor: BeanColor = BeanColor.Companion.WHITE,
      outlineSize: Double = 5.0,
      backgroundColor: BeanColor = BeanColor.Companion.TRANSPARENT,
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