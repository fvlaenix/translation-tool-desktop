package translation.data

import app.ocr.OCRBoxData
import core.base.Repository
import core.utils.ProtobufUtils
import java.awt.image.BufferedImage

class OCRRepositoryImpl : OCRRepository, Repository {

  override suspend fun processImage(image: BufferedImage): Result<String> = safeCall {
    ProtobufUtils.getOCR(image)
  }

  override suspend fun processBatchImages(images: List<BufferedImage>): Result<List<String>> = safeCall {
    images.map { image ->
      ProtobufUtils.getOCR(image)
    }
  }

  override suspend fun getBoxedOCR(image: BufferedImage): Result<List<OCRBoxData>> = safeCall {
    ProtobufUtils.getBoxedOCR(image)
  }
}