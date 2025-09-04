package fonts.data

import bean.LoadingJsonException
import java.awt.Font

/**
 * Utility for font operations including retrieval and validation.
 */
object FontUtils {
  /**
   * Retrieves a font by name with specified size, throwing exception if not found.
   */
  fun getFontNotNull(fonts: List<FontInfo>, name: String, size: Float): Font {
    val fontInfo = fonts.find { it.name == name }
      ?: throw LoadingJsonException("Can't find font: $name. List of fonts: ${fonts.map { it.name }}")
    return fontInfo.font.deriveFont(size)
  }

  /**
   * Validates if file is a valid font file.
   */
  fun validateFontFile(path: java.nio.file.Path): Boolean {
    return try {
      Font.createFont(0, path.toFile())
      true
    } catch (e: Exception) {
      false
    }
  }
}