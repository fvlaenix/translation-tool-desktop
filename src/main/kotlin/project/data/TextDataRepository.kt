package project.data

import bean.WorkData

interface TextDataRepository {
  suspend fun loadWorkData(project: Project, textType: TextType): Result<WorkData?>
  suspend fun saveWorkData(project: Project, textType: TextType, workData: WorkData): Result<Unit>
  suspend fun updateTranslation(
    project: Project,
    textType: TextType,
    imageIndex: Int,
    blockIndex: Int,
    text: String
  ): Result<Unit>

  suspend fun hasWorkData(project: Project, textType: TextType): Result<Boolean>
}

enum class TextType(val fileName: String) {
  UNTRANSLATED("untranslated-text.json"),
  TRANSLATED("translated-text.json")
}