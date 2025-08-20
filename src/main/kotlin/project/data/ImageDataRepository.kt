package project.data

import app.batch.ImagePathInfo
import java.nio.file.Path

interface ImageDataRepository {
  suspend fun loadImages(project: Project, imageType: ImageType): Result<List<ImagePathInfo>>
  suspend fun saveImages(project: Project, imageType: ImageType, images: List<ImagePathInfo>): Result<Unit>
  suspend fun addImage(project: Project, imageType: ImageType, image: ImagePathInfo): Result<Unit>
  suspend fun clearImages(project: Project, imageType: ImageType): Result<Unit>
  suspend fun getImageCount(project: Project, imageType: ImageType): Result<Int>

  suspend fun addToBatch(image: ImagePathInfo): Result<Unit>
  suspend fun getBatchImages(): Result<List<ImagePathInfo>>
  suspend fun clearBatch(): Result<Unit>
  suspend fun addAllToBatch(images: List<ImagePathInfo>): Result<Unit>

  suspend fun getWorkDataPath(project: Project, imageType: ImageType): Result<Path>
}

enum class ImageType(val folderName: String) {
  UNTRANSLATED("untranslated"),
  CLEANED("cleaned"),
  EDITED("edited")
}