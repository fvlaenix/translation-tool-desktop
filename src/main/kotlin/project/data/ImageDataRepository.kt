package project.data

import app.batch.ImagePathInfo
import java.nio.file.Path

/**
 * Repository interface for managing project image data and batch operations.
 */
interface ImageDataRepository {
  /**
   * Loads images from project by type.
   */
  suspend fun loadImages(project: Project, imageType: ImageType): Result<List<ImagePathInfo>>

  /**
   * Saves images to project by type.
   */
  suspend fun saveImages(project: Project, imageType: ImageType, images: List<ImagePathInfo>): Result<Unit>

  /**
   * Adds single image to project.
   */
  suspend fun addImage(project: Project, imageType: ImageType, image: ImagePathInfo): Result<Unit>

  /**
   * Clears all images from project type.
   */
  suspend fun clearImages(project: Project, imageType: ImageType): Result<Unit>

  /**
   * Gets image count for project type.
   */
  suspend fun getImageCount(project: Project, imageType: ImageType): Result<Int>

  /**
   * Adds image to batch processing.
   */
  suspend fun addToBatch(image: ImagePathInfo): Result<Unit>

  /**
   * Gets all batch images.
   */
  suspend fun getBatchImages(): Result<List<ImagePathInfo>>

  /**
   * Clears batch processing queue.
   */
  suspend fun clearBatch(): Result<Unit>

  /**
   * Adds multiple images to batch.
   */
  suspend fun addAllToBatch(images: List<ImagePathInfo>): Result<Unit>

  /**
   * Gets work data path for project type.
   */
  suspend fun getWorkDataPath(project: Project, imageType: ImageType): Result<Path>
}

/**
 * Types of images in the translation workflow with folder names.
 */
enum class ImageType(val folderName: String) {
  UNTRANSLATED("untranslated"),
  CLEANED("cleaned"),
  EDITED("edited")
}