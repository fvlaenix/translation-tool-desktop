package core.utils

import java.awt.image.BufferedImage
import java.awt.image.ColorModel

/**
 * Utility functions for image processing operations.
 */
object ImageUtils {
  /**
   * Creates a deep copy of this BufferedImage.
   */
  fun BufferedImage.deepCopy(): BufferedImage {
    val cm: ColorModel = this.colorModel
    val isAlphaPremultiplied: Boolean = cm.isAlphaPremultiplied
    val raster = this.copyData(null)
    return BufferedImage(cm, raster, isAlphaPremultiplied, null)
  }
}