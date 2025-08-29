package core.utils

import androidx.compose.ui.unit.IntSize
import app.ocr.OCRBoxData
import com.fvlaenix.image.protobuf.image
import com.fvlaenix.ocr.protobuf.OcrImageRequest
import com.fvlaenix.ocr.protobuf.ocrImageRequest
import com.fvlaenix.proxy.protobuf.ProxyServiceGrpcKt
import com.fvlaenix.translation.protobuf.translationBlock
import com.fvlaenix.translation.protobuf.translationFile
import com.fvlaenix.translation.protobuf.translationFilesRequest
import com.fvlaenix.translation.protobuf.translationRequest
import com.google.protobuf.kotlin.toByteString
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import settings.data.SettingsRepository
import translation.data.BlockPosition
import translation.data.clampToImageBounds
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

private val AUTHORIZATION_KEY: Metadata.Key<String> = Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER)

object ProtobufUtils : KoinComponent {
  private val settingsRepository: SettingsRepository by inject()

  private fun getSettings() = runBlocking {
    settingsRepository.loadSettings().getOrElse {
      settings.data.SettingsModel.DEFAULT
    }
  }

  private fun BufferedImage.getImageRequest(): OcrImageRequest = ocrImageRequest {
    this.image = image {
      this.fileName = "image.png"
      val outputStream = ByteArrayOutputStream()
      ImageIO.write(this@getImageRequest, "PNG", outputStream)
      this.content = outputStream.toByteArray().toByteString()
    }
  }

  private suspend fun <T> getDataFromChannel(body: suspend (ProxyServiceGrpcKt.ProxyServiceCoroutineStub) -> T): T {
    val settings = getSettings()
    val channel = ManagedChannelBuilder.forAddress(
      settings.proxyServiceHostname,
      settings.proxyServicePort
    )
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

  private suspend fun getStringFromChannel(body: suspend (ProxyServiceGrpcKt.ProxyServiceCoroutineStub) -> String): String {
    return try {
      getDataFromChannel(body)
    } catch (e: Exception) {
      return e.message.toString()
    }
  }

  suspend fun getOCR(image: BufferedImage): String {
    return getStringFromChannel { proxyStub ->
      val settings = getSettings()
      val metadata = Metadata()
      metadata.put(AUTHORIZATION_KEY, settings.apiKey)
      val response = proxyStub.ocrImage(image.getImageRequest(), metadata)
      if (response.hasError()) {
        response.error
      } else {
        response.rectangles.rectanglesList.joinToString("\n") { it.text }
      }
    }
  }

  suspend fun getBoxedOCR(image: BufferedImage): List<OCRBoxData> {
    return getDataFromChannel { proxyStub ->
      val settings = getSettings()
      val metadata = Metadata()
      metadata.put(AUTHORIZATION_KEY, settings.apiKey)
      val response = proxyStub.ocrImage(image.getImageRequest(), metadata)

      if (response.hasError()) {
        throw Exception("Error occurred while processing image: ${response.error}")
      }
      val rectangles = response.rectangles.rectanglesList
      val imageSize = IntSize(image.width, image.height)

      rectangles.map { rectangle ->
        OCRBoxData(
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
    }
  }

  suspend fun getTranslation(text: String): String {
    return getStringFromChannel { proxyStub ->
      val settings = getSettings()
      val metadata = Metadata()
      metadata.put(AUTHORIZATION_KEY, settings.apiKey)
      val response = proxyStub.translation(translationRequest { this.text = text }, metadata)
      if (response.hasError()) {
        response.error
      } else {
        response.text
      }
    }
  }

  suspend fun getTranslation(text: List<String>): List<String> {
    return getDataFromChannel { proxyStub ->
      val settings = getSettings()
      val metadata = Metadata()
      metadata.put(AUTHORIZATION_KEY, settings.apiKey)
      val request = translationFilesRequest {
        this.requests.add(translationFile {
          this.fileName = "translation.txt"
          this.blocks.addAll(text.map { translationBlock { this.text = it } })
        })
      }
      val response = proxyStub.translationFile(request, metadata)
      response.responseList[0].blocksList.map { it.translation }
    }
  }
}