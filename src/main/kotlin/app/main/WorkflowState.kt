package app.main

import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import project.data.ImageDataRepository
import project.data.TextDataRepository
import project.data.TextType
import project.domain.ProjectSelectionState
import translation.data.WorkDataRepository

/**
 * Manages workflow state and validation for proper step sequencing.
 */
@Composable
fun rememberWorkflowState(): WorkflowState {
  val imageDataRepository: ImageDataRepository = koinInject()
  val workDataRepository: WorkDataRepository = koinInject()
  val textDataRepository: TextDataRepository = koinInject()
  val projectSelectionState: ProjectSelectionState = koinInject()

  val state = remember { WorkflowState() }
  val scope = rememberCoroutineScope()

  LaunchedEffect(Unit) {
    scope.launch {
      state.refreshState(imageDataRepository, workDataRepository, textDataRepository, projectSelectionState)
    }
  }

  // Refresh when project selection changes
  LaunchedEffect(projectSelectionState.selectedProject) {
    scope.launch {
      state.refreshState(imageDataRepository, workDataRepository, textDataRepository, projectSelectionState)
    }
  }

  return state
}

class WorkflowState {
  var hasImages by mutableStateOf(false)
    private set
  var hasOCRData by mutableStateOf(false)
    private set
  var hasTranslationData by mutableStateOf(false)
    private set
  var hasCleanedImages by mutableStateOf(false)
    private set
  var isLoading by mutableStateOf(true)
    private set

  suspend fun refreshState(
    imageDataRepository: ImageDataRepository,
    workDataRepository: WorkDataRepository,
    textDataRepository: TextDataRepository,
    projectSelectionState: ProjectSelectionState
  ) {
    isLoading = true

    try {
      val currentProject = projectSelectionState.selectedProject

      if (currentProject == null) {
        // App-level workflow
        hasImages = imageDataRepository.getBatchImages()
          .getOrElse { emptyList() }
          .isNotEmpty()

        hasOCRData = workDataRepository.getWorkData()
          .getOrNull()
          ?.imagesData
          ?.isNotEmpty() == true

        hasTranslationData = hasOCRData // In app workflow, translation is part of the same work data
        hasCleanedImages = hasImages // Cleaned images same as batch images in app workflow
      } else {
        // Project-based workflow
        hasImages = imageDataRepository.loadImages(currentProject.value!!, project.data.ImageType.UNTRANSLATED)
          .getOrElse { emptyList() }
          .isNotEmpty()

        hasOCRData = textDataRepository.hasWorkData(currentProject.value!!, TextType.UNTRANSLATED)
          .getOrElse { false }

        hasTranslationData = textDataRepository.hasWorkData(currentProject.value!!, TextType.TRANSLATED)
          .getOrElse { false }

        hasCleanedImages = imageDataRepository.loadImages(currentProject.value!!, project.data.ImageType.CLEANED)
          .getOrElse { emptyList() }
          .isNotEmpty()
      }
    } catch (e: Exception) {
      // Reset to safe state on error
      hasImages = false
      hasOCRData = false
      hasTranslationData = false
      hasCleanedImages = false
    } finally {
      isLoading = false
    }
  }

  fun canRunOCR(): Boolean = hasImages
  fun canRunTranslation(): Boolean = hasOCRData
  fun canRunEdit(): Boolean = hasTranslationData && hasCleanedImages

  fun getOCRStatusText(): String = when {
    !hasImages -> "Add images first"
    else -> "Ready"
  }

  fun getTranslationStatusText(): String = when {
    !hasOCRData -> "Run OCR first"
    else -> "Ready"
  }

  fun getEditStatusText(): String = when {
    !hasTranslationData -> "Run translation first"
    !hasCleanedImages -> "Add cleaned images first"
    else -> "Ready"
  }
}