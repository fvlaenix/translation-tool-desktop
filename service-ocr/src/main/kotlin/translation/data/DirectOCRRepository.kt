package translation.data

import androidx.compose.ui.unit.IntSize
import com.fvlaenix.ocr.OCRUtils
import core.base.Repository
import settings.data.OCRDirectSettings
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.*
import javax.imageio.ImageIO

class DirectOCRRepository(
  private val settings: OCRDirectSettings
) : OCRRepository, Repository {

  init {
    configureClient()
  }

  private fun configureClient() {
    if (settings.credentialsPath.isBlank()) {
      throw IllegalStateException("Direct OCR requires a non-empty credentials path.")
    }
    OCRUtils.configureWithCredentialsPath(settings.credentialsPath)
  }

  override suspend fun processImage(image: BufferedImage): Result<String> = safeCall {
    ensureValidSettings()
    val imageBytes = bufferedImageToBytes(image)
    val response = OCRUtils.ocrBytesArrayToText(imageBytes)

    if (response.hasError()) {
      throw Exception("OCR error: ${response.error}")
    }

    response.rectangles.rectanglesList.joinToString("\n") { it.text }
  }

  override suspend fun processBatchImages(images: List<BufferedImage>): Result<List<String>> = safeCall {
    ensureValidSettings()
    images.map { image ->
      val imageBytes = bufferedImageToBytes(image)
      val response = OCRUtils.ocrBytesArrayToText(imageBytes)

      if (response.hasError()) {
        throw Exception("OCR error: ${response.error}")
      }

      response.rectangles.rectanglesList.joinToString("\n") { it.text }
    }
  }

  override suspend fun getBoxedOCR(image: BufferedImage): Result<List<OCRBoxData>> = safeCall {
    ensureValidSettings()
    val imageBytes = bufferedImageToBytes(image)
    val response = OCRUtils.ocrBytesArrayToText(imageBytes)

    if (response.hasError()) {
      throw Exception("OCR error: ${response.error}")
    }

    val rectangles = response.rectangles.rectanglesList
    val imageSize = IntSize(image.width, image.height)

    val boxes = rectangles.map { rectangle ->
      OCRBoxData(
        id = generateUniqueId(),
        box = BlockPosition(
          x = rectangle.x.toDouble(),
          y = rectangle.y.toDouble(),
          width = rectangle.width.toDouble(),
          height = rectangle.height.toDouble(),
          shape = BlockPosition.Shape.Rectangle
        ).clampToImageBounds(imageSize),
        text = rectangle.text
      )
    }

    ensureUniqueIds(boxes)
  }

  private fun bufferedImageToBytes(image: BufferedImage): ByteArray {
    val outputStream = ByteArrayOutputStream()
    ImageIO.write(image, "PNG", outputStream)
    return outputStream.toByteArray()
  }

  private fun generateUniqueId(): String = UUID.randomUUID().toString()

  private fun ensureUniqueIds(boxes: List<OCRBoxData>): List<OCRBoxData> {
    val ids = boxes.map { it.id }
    val duplicates = ids.groupingBy { it }.eachCount().filter { it.value > 1 }.keys

    if (duplicates.isEmpty()) {
      return boxes
    }

    val usedIds = mutableSetOf<String>()
    return boxes.map { box ->
      if (box.id in duplicates || box.id in usedIds) {
        var newId = generateUniqueId()
        while (newId in usedIds || newId in ids) {
          newId = generateUniqueId()
        }
        usedIds.add(newId)
        box.copy(id = newId)
      } else {
        usedIds.add(box.id)
        box
      }
    }
  }

  private fun ensureValidSettings() {
    if (settings.credentialsPath.isBlank()) {
      throw IllegalStateException("Direct OCR requires a non-empty credentials path.")
    }
    if (settings.timeoutSeconds <= 0) {
      throw IllegalStateException("Timeout must be greater than 0.")
    }
  }
}
