package translation.data

import java.awt.image.BufferedImage

/**
 * Repository interface for optical character recognition operations.
 */
interface OCRRepository {
  /**
   * Processes single image and extracts text content.
   */
  suspend fun processImage(image: BufferedImage): Result<String>

  /**
   * Processes multiple images in batch and extracts text from each.
   */
  suspend fun processBatchImages(images: List<BufferedImage>): Result<List<String>>

  /**
   * Extracts text with bounding box information from image.
   */
  suspend fun getBoxedOCR(image: BufferedImage): Result<List<OCRBoxData>>
}