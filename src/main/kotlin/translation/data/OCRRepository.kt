package translation.data

import app.ocr.OCRBoxData
import java.awt.image.BufferedImage

interface OCRRepository {
  suspend fun processImage(image: BufferedImage): Result<String>
  suspend fun processBatchImages(images: List<BufferedImage>): Result<List<String>>
  suspend fun getBoxedOCR(image: BufferedImage): Result<List<OCRBoxData>>
}