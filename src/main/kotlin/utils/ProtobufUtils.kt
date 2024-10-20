package utils

import app.ocr.OCRBoxData
import app.settings.SettingsState
import bean.BlockPosition
import com.fvlaenix.image.protobuf.image
import com.fvlaenix.ocr.protobuf.OcrImageRequest
import com.fvlaenix.ocr.protobuf.ocrImageRequest
import com.fvlaenix.proxy.protobuf.ProxyServiceGrpcKt
import com.fvlaenix.translation.protobuf.translationRequest
import com.google.protobuf.kotlin.toByteString
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

private val AUTHORIZATION_KEY: Metadata.Key<String> = Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER)

object ProtobufUtils {
  private fun BufferedImage.getImageRequest(): OcrImageRequest = ocrImageRequest {
    this.image = image {
      this.fileName = "image.png"
      val outputStream = ByteArrayOutputStream()
      ImageIO.write(this@getImageRequest, "PNG", outputStream)
      this.content = outputStream.toByteArray().toByteString()
    }
  }

  private fun <T> getDataFromChannel(body: suspend (ProxyServiceGrpcKt.ProxyServiceCoroutineStub) -> T): T {
    return runBlocking {
      val channel = ManagedChannelBuilder.forAddress(
        SettingsState.DEFAULT.proxyServiceHostname,
        SettingsState.DEFAULT.proxyServicePort
      )
        .usePlaintext()
        .maxInboundMessageSize(50 * 1024 * 1024)
        .build()
      try {
        val proxyService = ProxyServiceGrpcKt.ProxyServiceCoroutineStub(channel)
        return@runBlocking body(proxyService)
      } finally {
        channel.shutdown()
      }
    }
  }

  private fun getStringFromChannel(body: suspend (ProxyServiceGrpcKt.ProxyServiceCoroutineStub) -> String): String {
    return try {
      getDataFromChannel(body)
    } catch (e: Exception) {
      return e.message.toString()
    }
  }

  fun getOCR(image: BufferedImage): String {
    return getStringFromChannel { proxyStub ->
      val metadata = Metadata()
      metadata.put(AUTHORIZATION_KEY, SettingsState.DEFAULT.apiKey)
      val response = proxyStub.ocrImage(image.getImageRequest(), metadata)
      if (response.hasError()) {
        response.error
      } else {
        response.rectangles.rectanglesList.joinToString("\n") { it.text }
      }
    }
  }

  fun getBoxedOCR(image: BufferedImage): List<OCRBoxData> {
    return getDataFromChannel { proxyStub ->
      val metadata = Metadata()
      metadata.put(AUTHORIZATION_KEY, SettingsState.DEFAULT.apiKey)
      val response = proxyStub.ocrImage(image.getImageRequest(), metadata)

      if (response.hasError()) {
        throw Exception("Error occurred while processing image: ${response.error}")
      }
      val rectangles = response.rectangles.rectanglesList

      rectangles.map { rectangle ->
        OCRBoxData(
          box = BlockPosition(
            x = rectangle.x.toDouble(),
            y = rectangle.y.toDouble(),
            width = rectangle.width.toDouble(),
            height = rectangle.height.toDouble(),
            shape = BlockPosition.Shape.Rectangle
          ),
          text = rectangle.text
        )
      }
    }
  }

  fun getTranslation(text: String): String {
    return getStringFromChannel { proxyStub ->
      val metadata = Metadata()
      metadata.put(AUTHORIZATION_KEY, SettingsState.DEFAULT.apiKey)
      val response = proxyStub.translation(translationRequest { this.text = text }, metadata)
      if (response.hasError()) {
        response.error
      } else {
        response.text
      }
    }
  }
}