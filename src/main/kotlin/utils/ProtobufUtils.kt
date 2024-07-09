package utils

import app.settings.SettingsState
import com.fvlaenix.ocr.protobuf.OcrImageRequest
import com.fvlaenix.ocr.protobuf.OcrServiceGrpcKt
import com.fvlaenix.ocr.protobuf.ocrImageRequest
import com.fvlaenix.translation.protobuf.TranslationServiceGrpcKt
import com.fvlaenix.translation.protobuf.translationRequest
import com.google.protobuf.kotlin.toByteString
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object ProtobufUtils {
  private fun BufferedImage.getImageRequest(): OcrImageRequest = ocrImageRequest {
    this.image = com.fvlaenix.image.protobuf.image {
      this.fileName = "image.png"
      val outputStream = ByteArrayOutputStream()
      ImageIO.write(this@getImageRequest, "PNG", outputStream)
      this.content = outputStream.toByteArray().toByteString()
    }
  }

  fun getOCR(image: BufferedImage): String {
    return runBlocking {
      val ocrChannel = ManagedChannelBuilder.forAddress(SettingsState.DEFAULT.ocrServiceHostname, 50051)
        .usePlaintext()
        .maxInboundMessageSize(50 * 1024 * 1024)
        .build()
      try {
        val ocrChannelService = OcrServiceGrpcKt.OcrServiceCoroutineStub(ocrChannel)
        val response = ocrChannelService.ocrImage(image.getImageRequest())
        if (response.hasError()) {
          response.error
        } else {
          response.rectangles.rectanglesList.joinToString("\n") { it.text }
        }
      } finally {
        ocrChannel.shutdown()
      }
    }
  }

  fun getTranslation(text: String): String {
    return runBlocking {
      val gptChannel = ManagedChannelBuilder.forAddress(SettingsState.DEFAULT.translatorServiceHostname, 50052)
        .usePlaintext()
        .maxInboundMessageSize(50 * 1024 * 1024)
        .build()
      try {
        val gptChannelService = TranslationServiceGrpcKt.TranslationServiceCoroutineStub(gptChannel)
        val response = gptChannelService.translation(translationRequest { this.text = text })
        if (response.hasError()) {
          throw IllegalStateException()
        } else {
          response.text
        }
      } finally {
        gptChannel.shutdown()
      }
    }
  }
}