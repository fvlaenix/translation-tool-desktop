package project.data

import bean.WorkData
import core.base.Repository
import core.utils.JSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

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
}