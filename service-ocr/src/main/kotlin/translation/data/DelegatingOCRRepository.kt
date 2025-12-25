package translation.data

import java.awt.image.BufferedImage

class DelegatingOCRRepository(
  private val provider: OCRServiceProvider
) : OCRRepository {

  override suspend fun processImage(image: BufferedImage): Result<String> {
    return provider.get().processImage(image)
  }

  override suspend fun processBatchImages(images: List<BufferedImage>): Result<List<String>> {
    return provider.get().processBatchImages(images)
  }

  override suspend fun getBoxedOCR(image: BufferedImage): Result<List<OCRBoxData>> {
    return provider.get().getBoxedOCR(image)
  }
}
