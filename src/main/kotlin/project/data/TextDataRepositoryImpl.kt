package project.data

import core.base.Repository
import core.utils.JSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import translation.data.WorkData
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Text data repository implementation. Manages work data loading, saving and translation text operations.
 */
class TextDataRepositoryImpl : TextDataRepository, Repository {

  override suspend fun loadWorkData(project: Project, textType: TextType): Result<WorkData?> = safeCall {
    withContext(Dispatchers.IO) {
      val workDataPath = project.path.resolve(textType.fileName)

      if (!workDataPath.exists()) {
        null
      } else {
        JSON.decodeFromString<WorkData>(workDataPath.readText())
      }
    }
  }

  override suspend fun saveWorkData(project: Project, textType: TextType, workData: WorkData): Result<Unit> = safeCall {
    withContext(Dispatchers.IO) {
      val workDataPath = project.path.resolve(textType.fileName)
      workDataPath.writeText(JSON.encodeToString(workData))
    }
  }

  override suspend fun updateTranslation(
    project: Project,
    textType: TextType,
    imageIndex: Int,
    blockIndex: Int,
    text: String
  ): Result<Unit> = safeCall {
    val currentWorkData = loadWorkData(project, textType).getOrNull()
      ?: throw IllegalStateException("WorkData not found for project ${project.name}")

    val updatedImagesData = currentWorkData.imagesData.toMutableList()
    val imageData = updatedImagesData[imageIndex]
    val updatedBlockData = imageData.blockData.toMutableList()
    val blockData = updatedBlockData[blockIndex]

    updatedBlockData[blockIndex] = blockData.copy(text = text)
    updatedImagesData[imageIndex] = imageData.copy(blockData = updatedBlockData)

    val updatedWorkData = currentWorkData.copy(imagesData = updatedImagesData)
    saveWorkData(project, textType, updatedWorkData).getOrThrow()
  }

  override suspend fun hasWorkData(project: Project, textType: TextType): Result<Boolean> = safeCall {
    project.path.resolve(textType.fileName).exists()
  }

  override suspend fun updateTranslationText(
    project: Project,
    textType: TextType,
    imageIndex: Int,
    blockIndex: Int,
    translatedText: String
  ): Result<Unit> = safeCall {
    val currentWorkData = loadWorkData(project, textType).getOrNull()
      ?: throw IllegalStateException("WorkData not found for project ${project.name}")

    val updatedImagesData = currentWorkData.imagesData.toMutableList()
    val imageData = updatedImagesData[imageIndex]
    val updatedBlockData = imageData.blockData.toMutableList()
    val blockData = updatedBlockData[blockIndex]

    updatedBlockData[blockIndex] = blockData.copy(text = translatedText)
    updatedImagesData[imageIndex] = imageData.copy(blockData = updatedBlockData)

    val updatedWorkData = currentWorkData.copy(imagesData = updatedImagesData)
    saveWorkData(project, textType, updatedWorkData).getOrThrow()
  }

  override suspend fun getUntranslatedTexts(project: Project): Result<List<String>> = safeCall {
    val workData = loadWorkData(project, TextType.UNTRANSLATED).getOrNull()
      ?: throw IllegalStateException("No untranslated work data found")

    workData.imagesData.flatMap { imageData ->
      imageData.blockData.map { blockData -> blockData.text }
    }
  }

  override suspend fun setTranslatedTexts(
    project: Project,
    imageIndex: Int,
    translatedTexts: List<String>
  ): Result<Unit> = safeCall {
    val untranslatedWorkData = loadWorkData(project, TextType.UNTRANSLATED).getOrNull()
      ?: throw IllegalStateException("No untranslated work data found")

    val translatedWorkData = loadWorkData(project, TextType.TRANSLATED).getOrNull() ?: run {
      // Create new translated work data based on untranslated
      untranslatedWorkData.copy(
        imagesData = untranslatedWorkData.imagesData.map { imageData ->
          imageData.copy(
            blockData = imageData.blockData.map { blockData ->
              blockData.copy(text = "")
            }
          )
        }
      )
    }

    val updatedImagesData = translatedWorkData.imagesData.toMutableList()
    val imageData = updatedImagesData[imageIndex]
    val updatedBlockData = imageData.blockData.toMutableList()

    translatedTexts.forEachIndexed { index, translatedText ->
      if (index < updatedBlockData.size) {
        updatedBlockData[index] = updatedBlockData[index].copy(text = translatedText)
      }
    }

    updatedImagesData[imageIndex] = imageData.copy(blockData = updatedBlockData)
    val updatedWorkData = translatedWorkData.copy(imagesData = updatedImagesData)

    saveWorkData(project, TextType.TRANSLATED, updatedWorkData).getOrThrow()
  }

  override suspend fun createTranslatedWorkData(
    project: Project,
    baseWorkData: WorkData
  ): Result<WorkData> = safeCall {
    val translatedWorkData = baseWorkData.copy(
      imagesData = baseWorkData.imagesData.map { imageData ->
        imageData.copy(
          blockData = imageData.blockData.map { blockData ->
            blockData.copy(text = "")
          }
        )
      }
    )

    saveWorkData(project, TextType.TRANSLATED, translatedWorkData).getOrThrow()
    translatedWorkData
  }

  override suspend fun getWorkDataPath(project: Project, textType: TextType): Result<Path> = safeCall {
    project.path.resolve(textType.fileName)
  }
}