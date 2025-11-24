package project.data

import translation.data.WorkData
import java.nio.file.Path

/**
 * Repository interface for managing text data in projects including translations.
 */
interface TextDataRepository {
  /**
   * Loads work data from project by text type.
   */
  suspend fun loadWorkData(project: Project, textType: TextType): Result<WorkData?>

  /**
   * Saves work data to project by text type.
   */
  suspend fun saveWorkData(project: Project, textType: TextType, workData: WorkData): Result<Unit>

  /**
   * Updates translation text for specific block.
   */
  suspend fun updateTranslation(
    project: Project,
    textType: TextType,
    imageIndex: Int,
    blockIndex: Int,
    text: String
  ): Result<Unit>

  /**
   * Checks if work data exists for project type.
   */
  suspend fun hasWorkData(project: Project, textType: TextType): Result<Boolean>

  /**
   * Updates translated text for specific block.
   */
  suspend fun updateTranslationText(
    project: Project,
    textType: TextType,
    imageIndex: Int,
    blockIndex: Int,
    translatedText: String
  ): Result<Unit>

  /**
   * Gets all untranslated text strings.
   */
  suspend fun getUntranslatedTexts(project: Project): Result<List<String>>

  /**
   * Sets translated texts for specific image.
   */
  suspend fun setTranslatedTexts(
    project: Project,
    imageIndex: Int,
    translatedTexts: List<String>
  ): Result<Unit>

  /**
   * Creates translated work data from base data.
   */
  suspend fun createTranslatedWorkData(
    project: Project,
    baseWorkData: WorkData
  ): Result<WorkData>

  /**
   * Gets work data file path for project type.
   */
  suspend fun getWorkDataPath(project: Project, textType: TextType): Result<Path>
}

/**
 * Types of text data with corresponding file names.
 */
enum class TextType(val fileName: String) {
  UNTRANSLATED("untranslated-text.json"),
  TRANSLATED("translated-text.json")
}