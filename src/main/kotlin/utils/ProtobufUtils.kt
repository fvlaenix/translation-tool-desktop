package utils

import app.settings.SettingsState
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
    this.image = com.fvlaenix.image.protobuf.image {
      this.fileName = "image.png"
      val outputStream = ByteArrayOutputStream()
      ImageIO.write(this@getImageRequest, "PNG", outputStream)
      this.content = outputStream.toByteArray().toByteString()
    }
  }

  private fun getStringFromChannel(body: suspend (ProxyServiceGrpcKt.ProxyServiceCoroutineStub) -> String): String {
    return runBlocking {
      val channel = ManagedChannelBuilder.forAddress(SettingsState.DEFAULT.proxyServiceHostname, SettingsState.DEFAULT.proxyServicePort)
        .usePlaintext()
        .maxInboundMessageSize(50 * 1024 * 1024)
        .build()
      try {
        val proxyService = ProxyServiceGrpcKt.ProxyServiceCoroutineStub(channel)
        return@runBlocking body(proxyService)
      } catch (e: Exception) {
        e.message!!
      } finally {
        channel.shutdown()
      }
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