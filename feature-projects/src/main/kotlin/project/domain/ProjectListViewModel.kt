package project.domain

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import core.base.BaseViewModel
import kotlinx.coroutines.launch
import project.data.ProjectInfo
import project.data.ProjectRepository

/**
 * View model for managing project list display and selection.
 */
class ProjectListViewModel(
  private val projectRepository: ProjectRepository,
  private val projectSelectionState: ProjectSelectionState
) : BaseViewModel() {

  private val _projects = mutableStateOf<List<ProjectInfo>>(emptyList())
  val projects: State<List<ProjectInfo>> = _projects

  private val _isRefreshing = mutableStateOf(false)
  val isRefreshing: State<Boolean> = _isRefreshing

  init {
    loadProjects()
  }

  /**
   * Loads all available projects from the repository.
   */
  fun loadProjects() {
    viewModelScope.launch {
      setLoading(true)
      clearError()

      projectRepository.loadProjects()
        .onSuccess { projectList ->
          _projects.value = projectList
        }
        .onFailure { exception ->
          setError("Failed to load projects: ${exception.message}")
        }

      setLoading(false)
    }
  }

  /**
   * Selects a project and loads its full data.
   */
  fun selectProject(projectInfo: ProjectInfo) {
    viewModelScope.launch {
      projectSelectionState.selectProject(projectInfo)

      // Load the full project data
      projectRepository.getProject(projectInfo)
        .onSuccess { project ->
          projectSelectionState.setSelectedProject(project)
        }
        .onFailure { exception ->
          setError("Failed to load project: ${exception.message}")
        }
    }
  }

  /**
   * Refreshes the project list by reloading from repository.
   */
  fun refreshProjects() {
    viewModelScope.launch {
      _isRefreshing.value = true
      loadProjects()
      _isRefreshing.value = false
    }
  }

  /**
   * Deletes a project and updates the local list and selection state.
   */
  fun deleteProject(projectInfo: ProjectInfo) {
    viewModelScope.launch {
      projectRepository.deleteProject(projectInfo)
        .onSuccess {
          // Remove from local list
          _projects.value = _projects.value.filter { it.stringPath != projectInfo.stringPath }

          // Clear selection if this project was selected
          if (projectSelectionState.selectedProjectInfo.value?.stringPath == projectInfo.stringPath) {
            projectSelectionState.clearSelection()
          }
        }
        .onFailure { exception ->
          setError("Failed to delete project: ${exception.message}")
        }
    }
  }
}