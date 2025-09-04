package app.batch

import java.awt.image.BufferedImage

/**
 * Holds image data with its associated name for batch processing.
 */
data class ImagePathInfo(val image: BufferedImage, val name: String)