package translation.domain

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import core.base.BaseViewModel
import kotlinx.coroutines.launch
import project.data.Project
import project.data.TextDataRepository
import project.data.TextType
import translation.data.TranslationRepository
import translation.data.WorkData
import translation.data.WorkDataRepository

data class TranslationPair(
  val originalText: String,
  val translatedText: String,
  val imageIndex: Int,
  val blockIndex: Int
)

class TranslationCreatorViewModel(
  private val translationRepository: TranslationRepository,
  private val workDataRepository: WorkDataRepository,
  private val textDataRepository: TextDataRepository
) : BaseViewModel() {

  private val _translationPairs = mutableStateOf<List<TranslationPair>>(emptyList())
  val translationPairs: State<List<TranslationPair>> = _translationPairs

  private val _isTranslating = mutableStateOf(false)
  val isTranslating: State<Boolean> = _isTranslating

  private val _currentProject = mutableStateOf<Project?>(null)
  val currentProject: State<Project?> = _currentProject

  fun loadTranslationData(project: Project?) {
    _currentProject.value = project

    viewModelScope.launch {
      setLoading(true)
      clearError()

      try {
        val workData = if (project == null) {
          // App-level: get from WorkDataRepository
          workDataRepository.getWorkData().getOrNull()
        } else {
          // Project-level: get from TextDataRepository
          textDataRepository.loadWorkData(project, TextType.UNTRANSLATED).getOrNull()
        }

        if (workData == null) {
          setError("No OCR data found. Please run OCR first.")
          return@launch
        }

        // Create translation pairs from the work data
        val pairs = mutableListOf<TranslationPair>()
        workData.imagesData.forEachIndexed { imageIndex, imageData ->
          imageData.blockData.forEachIndexed { blockIndex, blockData ->
            pairs.add(
              TranslationPair(
                originalText = blockData.text,
                translatedText = "", // Will be filled by translation
                imageIndex = imageIndex,
                blockIndex = blockIndex
              )
            )
          }
        }

        _translationPairs.value = pairs

        // Load existing translations if available
        loadExistingTranslations(project)

      } catch (e: Exception) {
        setError("Failed to load translation data: ${e.message}")
      }

      setLoading(false)
    }
  }

  private suspend fun loadExistingTranslations(project: Project?) {
    try {
      val translatedWorkData = if (project == null) {
        // For app-level, we don't have separate translated storage yet
        null
      } else {
        textDataRepository.loadWorkData(project, TextType.TRANSLATED).getOrNull()
      }

      if (translatedWorkData != null) {
        val updatedPairs = _translationPairs.value.toMutableList()
        translatedWorkData.imagesData.forEachIndexed { imageIndex, imageData ->
          imageData.blockData.forEachIndexed { blockIndex, blockData ->
            val pairIndex = updatedPairs.indexOfFirst {
              it.imageIndex == imageIndex && it.blockIndex == blockIndex
            }
            if (pairIndex >= 0) {
              updatedPairs[pairIndex] = updatedPairs[pairIndex].copy(
                translatedText = blockData.text
              )
            }
          }
        }
        _translationPairs.value = updatedPairs
      }
    } catch (e: Exception) {
      // Ignore errors when loading existing translations
    }
  }

  fun translateAll() {
    viewModelScope.launch {
      _isTranslating.value = true
      clearError()

      try {
        val textsToTranslate = _translationPairs.value.map { it.originalText }

        translationRepository.translateBatch(textsToTranslate)
          .onSuccess { translations ->
            val updatedPairs = _translationPairs.value.toMutableList()
            translations.forEachIndexed { index, translation ->
              if (index < updatedPairs.size) {
                updatedPairs[index] = updatedPairs[index].copy(
                  translatedText = translation
                )
              }
            }
            _translationPairs.value = updatedPairs
          }
          .onFailure { exception ->
            setError("Batch translation failed: ${exception.message}")
          }

      } catch (e: Exception) {
        setError("Translation failed: ${e.message}")
      }

      _isTranslating.value = false
    }
  }

  fun translateSingle(index: Int) {
    val pair = _translationPairs.value.getOrNull(index) ?: return

    viewModelScope.launch {
      _isTranslating.value = true
      clearError()

      translationRepository.translateText(pair.originalText)
        .onSuccess { translation ->
          val updatedPairs = _translationPairs.value.toMutableList()
          updatedPairs[index] = pair.copy(translatedText = translation)
          _translationPairs.value = updatedPairs
        }
        .onFailure { exception ->
          setError("Translation failed: ${exception.message}")
        }

      _isTranslating.value = false
    }
  }

  fun updateTranslation(index: Int, translatedText: String) {
    val updatedPairs = _translationPairs.value.toMutableList()
    if (index in updatedPairs.indices) {
      updatedPairs[index] = updatedPairs[index].copy(translatedText = translatedText)
      _translationPairs.value = updatedPairs
    }
  }

  fun saveTranslations() {
    viewModelScope.launch {
      setLoading(true)
      clearError()

      try {
        val project = _currentProject.value

        if (project == null) {
          // App-level: update the work data in WorkDataRepository
          val currentWorkData = workDataRepository.getWorkData().getOrNull()
            ?: throw IllegalStateException("No work data found")

          val updatedWorkData = updateWorkDataWithTranslations(currentWorkData)
          workDataRepository.setWorkData(updatedWorkData).getOrThrow()

        } else {
          // Project-level: save to TextDataRepository
          val untranslatedWorkData = textDataRepository.loadWorkData(project, TextType.UNTRANSLATED).getOrNull()
            ?: throw IllegalStateException("No untranslated work data found")

          val translatedWorkData = updateWorkDataWithTranslations(untranslatedWorkData)
          textDataRepository.saveWorkData(project, TextType.TRANSLATED, translatedWorkData).getOrThrow()
        }

      } catch (e: Exception) {
        setError("Failed to save translations: ${e.message}")
      }

      setLoading(false)
    }
  }

  private fun updateWorkDataWithTranslations(baseWorkData: WorkData): WorkData {
    val updatedImagesData = baseWorkData.imagesData.toMutableList()

    _translationPairs.value.forEach { pair ->
      if (pair.imageIndex < updatedImagesData.size) {
        val imageData = updatedImagesData[pair.imageIndex]
        val updatedBlockData = imageData.blockData.toMutableList()

        if (pair.blockIndex < updatedBlockData.size) {
          updatedBlockData[pair.blockIndex] = updatedBlockData[pair.blockIndex].copy(
            text = pair.translatedText
          )
          updatedImagesData[pair.imageIndex] = imageData.copy(blockData = updatedBlockData)
        }
      }
    }

    return baseWorkData.copy(imagesData = updatedImagesData)
  }
}