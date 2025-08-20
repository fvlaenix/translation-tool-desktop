package project.domain

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import core.base.BaseViewModel
import kotlinx.coroutines.launch
import project.data.ProjectRepository
import java.nio.file.Path
import kotlin.io.path.exists

class NewProjectViewModel(
  private val projectRepository: ProjectRepository,
  private val projectSelectionState: ProjectSelectionState
) : BaseViewModel() {

  private val _projectName = mutableStateOf("")
  val projectName: State<String> = _projectName

  private val _basePath = mutableStateOf("")
  val basePath: State<String> = _basePath

  private val _validationErrors = mutableStateOf<Map<String, String>>(emptyMap())
  val validationErrors: State<Map<String, String>> = _validationErrors

  private val _isCreating = mutableStateOf(false)
  val isCreating: State<Boolean> = _isCreating

  private val _creationSuccess = mutableStateOf(false)
  val creationSuccess: State<Boolean> = _creationSuccess

  val projectFolderName: String
    get() = _projectName.value.replace(Regex("[^a-zA-Z0-9]"), "-").lowercase()

  val fullProjectPath: String
    get() = if (_basePath.value.isNotEmpty() && _projectName.value.isNotEmpty()) {
      Path.of(_basePath.value, projectFolderName).toString()
    } else ""

  fun setProjectName(name: String) {
    _projectName.value = name
    validateInputs()
  }

  fun setBasePath(path: String) {
    _basePath.value = path
    validateInputs()
  }

  fun validateInputs() {
    val errors = mutableMapOf<String, String>()

    // Validate project name
    if (_projectName.value.isBlank()) {
      errors["name"] = "Project name cannot be empty"
    }

    // Validate base path
    if (_basePath.value.isBlank()) {
      errors["path"] = "Base path cannot be empty"
    } else {
      val basePath = Path.of(_basePath.value)
      if (!basePath.exists()) {
        errors["path"] = "Base path does not exist"
      } else if (_projectName.value.isNotEmpty()) {
        val projectPath = basePath.resolve(projectFolderName)
        if (projectPath.exists()) {
          errors["path"] = "Project folder already exists"
        }
      }
    }

    _validationErrors.value = errors
  }

  fun isValid(): Boolean {
    validateInputs()
    return _validationErrors.value.isEmpty()
  }

  fun createProject() {
    if (!isValid()) {
      setError("Please fix validation errors before creating project")
      return
    }

    viewModelScope.launch {
      _isCreating.value = true
      clearError()

      projectRepository.createProject(_projectName.value, fullProjectPath)
        .onSuccess { projectInfo ->
          _creationSuccess.value = true
          projectSelectionState.selectProject(projectInfo)

          // Load the full project data
          projectRepository.getProject(projectInfo)
            .onSuccess { project ->
              projectSelectionState.setSelectedProject(project)
            }
        }
        .onFailure { exception ->
          setError("Failed to create project: ${exception.message}")
        }

      _isCreating.value = false
    }
  }

  fun resetForm() {
    _projectName.value = ""
    _basePath.value = ""
    _validationErrors.value = emptyMap()
    _creationSuccess.value = false
    clearError()
  }
}