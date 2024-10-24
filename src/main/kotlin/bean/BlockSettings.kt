package bean

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import utils.FontService
import java.awt.Font

@Serializable
data class BlockSettings(
  val fontName: String,
  val fontSize: Int = 10,
  val fontColor: BeanColor = BeanColor.BLACK,
  val outlineColor: BeanColor = BeanColor.WHITE,
  val outlineSize: Double = 5.0,
  val backgroundColor: BeanColor = BeanColor.TRANSPARENT,
  val border: Int = 5
) {
  @Transient
  val font: Font = FontService.getInstance().getFontNotNull(fontName, fontSize.toFloat())
}