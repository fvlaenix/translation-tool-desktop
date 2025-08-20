package project.domain

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import core.base.BaseViewModel
import kotlinx.coroutines.launch
import project.data.*

class ProjectPanelViewModel(
  private val projectRepository: ProjectRepository,
  private val imageDataRepository: ImageDataRepository,
  private val textDataRepository: TextDataRepository,
  private val projectSelectionState: ProjectSelectionState
) : BaseViewModel() {

  private val _currentProject = mutableStateOf<Project?>(null)
  val currentProject: State<Project?> = _currentProject

  private val _projectSections = mutableStateOf<List<ProjectSection>>(emptyList())
  val projectSections: State<List<ProjectSection>> = _projectSections

  private val _isLoadingProject = mutableStateOf(false)
  val isLoadingProject: State<Boolean> = _isLoadingProject

  init {
    // Listen to project selection changes
    observeProjectSelection()
  }

  private fun observeProjectSelection() {
    // This would ideally be a proper observable, but for simplicity we'll check in loadProject
  }

  fun loadProject() {
    val selectedProjectInfo = projectSelectionState.selectedProjectInfo.value
    if (selectedProjectInfo == null) {
      setError("No project selected")
      return
    }

    viewModelScope.launch {
      _isLoadingProject.value = true
      clearError()

      projectRepository.getProject(selectedProjectInfo)
        .onSuccess { project ->
          _currentProject.value = project
          projectSelectionState.setSelectedProject(project)
          loadProjectSections(project)
        }
        .onFailure { exception ->
          setError("Failed to load project: ${exception.message}")
        }

      _isLoadingProject.value = false
    }
  }

  private suspend fun loadProjectSections(project: Project) {
    val sections = mutableListOf<ProjectSection>()

    try {
      when (project.data) {
        is ImagesProjectData -> {
          sections.addAll(loadImageProjectSections(project))
        }
      }
      _projectSections.value = sections
    } catch (e: Exception) {
      setError("Failed to load project sections: ${e.message}")
    }
  }

  private suspend fun loadImageProjectSections(project: Project): List<ProjectSection> {
    val sections = mutableListOf<ProjectSection>()

    // Untranslated images section
    val untranslatedCount = imageDataRepository.getImageCount(project, ImageType.UNTRANSLATED).getOrElse { 0 }
    sections.add(
      ProjectSection(
        id = "untranslated_images",
        title = "Untranslated Images",
        description = "$untranslatedCount images",
        isEnabled = true,
        isRequired = false
      )
    )

    // OCR section
    val hasUntranslatedText = textDataRepository.hasWorkData(project, TextType.UNTRANSLATED).getOrElse { false }
    sections.add(
      ProjectSection(
        id = "ocr",
        title = "OCR Processing",
        description = if (hasUntranslatedText) "Completed" else "Not started",
        isEnabled = untranslatedCount > 0,
        isRequired = untranslatedCount > 0
      )
    )

    // Translation section
    val hasTranslatedText = textDataRepository.hasWorkData(project, TextType.TRANSLATED).getOrElse { false }
    sections.add(
      ProjectSection(
        id = "translation",
        title = "Translation",
        description = if (hasTranslatedText) "Completed" else "Not started",
        isEnabled = hasUntranslatedText,
        isRequired = hasUntranslatedText
      )
    )

    // Cleaned images section
    val cleanedCount = imageDataRepository.getImageCount(project, ImageType.CLEANED).getOrElse { 0 }
    sections.add(
      ProjectSection(
        id = "cleaned_images",
        title = "Cleaned Images",
        description = "$cleanedCount images",
        isEnabled = true,
        isRequired = false
      )
    )

    // Final editing section
    sections.add(
      ProjectSection(
        id = "final_edit",
        title = "Final Editing",
        description = "Combine cleaned images with translations",
        isEnabled = cleanedCount > 0 && hasTranslatedText,
        isRequired = cleanedCount > 0 && hasTranslatedText
      )
    )

    return sections
  }

  fun refreshProject() {
    loadProject()
  }

  data class ProjectSection(
    val id: String,
    val title: String,
    val description: String,
    val isEnabled: Boolean,
    val isRequired: Boolean
  )
}