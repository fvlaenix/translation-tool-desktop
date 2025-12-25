package translation.data

import androidx.compose.ui.unit.IntSize
import com.fvlaenix.image.protobuf.image
import com.fvlaenix.ocr.protobuf.ocrImageRequest
import com.fvlaenix.proxy.protobuf.ProxyServiceGrpcKt
import com.google.protobuf.kotlin.toByteString
import core.base.Repository
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import settings.data.SettingsRepository
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.*
import javax.imageio.ImageIO

private val AUTHORIZATION_KEY: Metadata.Key<String> = Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER)

class OCRRepositoryImpl(
  private val settingsRepository: SettingsRepository
) : OCRRepository, Repository {

  override suspend fun processImage(image: BufferedImage): Result<String> = safeCall {
    val settings = settingsRepository.loadSettings().getOrElse { settings.data.SettingsModel.DEFAULT }
    val apiKey = settings.ocrGrpc.apiKey.ifBlank { settings.apiKey }

    getDataFromChannel(settings.proxyServiceHostname, settings.proxyServicePort, apiKey) { proxyStub ->
      val metadata = Metadata()
      metadata.put(AUTHORIZATION_KEY, apiKey)
      val response = proxyStub.ocrImage(image.toOcrImageRequest(), metadata)
      if (response.hasError()) {
        response.error
      } else {
        response.rectangles.rectanglesList.joinToString("\n") { it.text }
      }
    }
  }

  override suspend fun processBatchImages(images: List<BufferedImage>): Result<List<String>> = safeCall {
    val settings = settingsRepository.loadSettings().getOrElse { settings.data.SettingsModel.DEFAULT }
    val apiKey = settings.ocrGrpc.apiKey.ifBlank { settings.apiKey }

    images.map { image ->
      getDataFromChannel(settings.proxyServiceHostname, settings.proxyServicePort, apiKey) { proxyStub ->
        val metadata = Metadata()
        metadata.put(AUTHORIZATION_KEY, apiKey)
        val response = proxyStub.ocrImage(image.toOcrImageRequest(), metadata)
        if (response.hasError()) {
          response.error
        } else {
          response.rectangles.rectanglesList.joinToString("\n") { it.text }
        }
      }
    }
  }

  override suspend fun getBoxedOCR(image: BufferedImage): Result<List<OCRBoxData>> = safeCall {
    val settings = settingsRepository.loadSettings().getOrElse { settings.data.SettingsModel.DEFAULT }
    val apiKey = settings.ocrGrpc.apiKey.ifBlank { settings.apiKey }

    getDataFromChannel(settings.proxyServiceHostname, settings.proxyServicePort, apiKey) { proxyStub ->
      val metadata = Metadata()
      metadata.put(AUTHORIZATION_KEY, apiKey)
      val response = proxyStub.ocrImage(image.toOcrImageRequest(), metadata)

      if (response.hasError()) {
        throw Exception("Error occurred while processing image: ${response.error}")
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
  }

  private fun BufferedImage.toOcrImageRequest() = ocrImageRequest {
    this.image = image {
      this.fileName = "image.png"
      val outputStream = ByteArrayOutputStream()
      ImageIO.write(this@toOcrImageRequest, "PNG", outputStream)
      this.content = outputStream.toByteArray().toByteString()
    }
  }

  private suspend fun <T> getDataFromChannel(
    hostname: String,
    port: Int,
    apiKey: String,
    body: suspend (ProxyServiceGrpcKt.ProxyServiceCoroutineStub) -> T
  ): T {
    val channel = ManagedChannelBuilder.forAddress(hostname, port)
      .usePlaintext()
      .maxInboundMessageSize(50 * 1024 * 1024)
      .build()
    try {
      val proxyService = ProxyServiceGrpcKt.ProxyServiceCoroutineStub(channel)
      return body(proxyService)
    } finally {
      channel.shutdown()
    }
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
}