package app.project

import androidx.compose.runtime.*
import fonts.data.FontRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import project.data.*

/**
 * Manages workflow state and validation for project-based workflows.
 */
@Composable
fun rememberProjectWorkflowState(project: Project): ProjectWorkflowState {
  val imageDataRepository: ImageDataRepository = koinInject()
  val textDataRepository: TextDataRepository = koinInject()
  val fontRepository: FontRepository = koinInject()

  val state = remember { ProjectWorkflowState() }
  val scope = rememberCoroutineScope()

  LaunchedEffect(project) {
    scope.launch {
      state.refreshState(project, imageDataRepository, textDataRepository, fontRepository)
    }
  }

  return state
}

class ProjectWorkflowState {
  var hasUntranslatedImages by mutableStateOf(false)
    private set
  var hasOCRData by mutableStateOf(false)
    private set
  var hasTranslationData by mutableStateOf(false)
    private set
  var hasCleanedImages by mutableStateOf(false)
    private set
  var hasFonts by mutableStateOf(false)
    private set
  var isLoading by mutableStateOf(true)
    private set

  // Counts for display
  var untranslatedImageCount by mutableStateOf(0)
    private set
  var cleanedImageCount by mutableStateOf(0)
    private set

  suspend fun refreshState(
    project: Project,
    imageDataRepository: ImageDataRepository,
    textDataRepository: TextDataRepository,
    fontRepository: FontRepository
  ) {
    isLoading = true

    try {
      // Check fonts
      hasFonts = fontRepository.isFontsAdded().getOrElse { false }

      // Check untranslated images
      val untranslatedImages = imageDataRepository.loadImages(project, ImageType.UNTRANSLATED).getOrElse { emptyList() }
      hasUntranslatedImages = untranslatedImages.isNotEmpty()
      untranslatedImageCount = untranslatedImages.size

      // Check OCR data - actually try to load it to ensure it's valid
      hasOCRData = textDataRepository.loadWorkData(project, TextType.UNTRANSLATED)
        .getOrNull()?.imagesData?.isNotEmpty() == true

      // Check translation data - actually try to load it to ensure it's valid
      hasTranslationData = textDataRepository.loadWorkData(project, TextType.TRANSLATED)
        .getOrNull()?.imagesData?.isNotEmpty() == true

      // Check cleaned images
      val cleanedImages = imageDataRepository.loadImages(project, ImageType.CLEANED).getOrElse { emptyList() }
      hasCleanedImages = cleanedImages.isNotEmpty()
      cleanedImageCount = cleanedImages.size

    } catch (e: Exception) {
      // Reset to safe state on error
      hasUntranslatedImages = false
      hasOCRData = false
      hasTranslationData = false
      hasCleanedImages = false
      hasFonts = false
      untranslatedImageCount = 0
      cleanedImageCount = 0
    } finally {
      isLoading = false
    }
  }

  fun canRunOCR(): Boolean = hasUntranslatedImages && hasFonts
  fun canRunTranslation(): Boolean = hasOCRData
  fun canRunEdit(): Boolean = hasTranslationData && hasCleanedImages

  fun getAddImagesStatusText(): String = when {
    hasUntranslatedImages -> "$untranslatedImageCount images added"
    else -> "Ready to add images"
  }

  fun getOCRStatusText(): String = when {
    !hasUntranslatedImages -> "Add untranslated images first"
    !hasFonts -> "Add fonts first"
    hasOCRData -> "OCR completed"
    else -> "Ready"
  }

  fun getTranslationStatusText(): String = when {
    !hasOCRData -> "Run OCR first"
    hasTranslationData -> "Translation completed"
    else -> "Ready"
  }

  fun getCleanedImagesStatusText(): String = when {
    hasCleanedImages -> "$cleanedImageCount cleaned images added"
    else -> "Ready to add cleaned images"
  }

  fun getEditStatusText(): String = when {
    !hasTranslationData -> "Run translation first"
    !hasCleanedImages -> "Add cleaned images first"
    else -> "Ready"
  }
}