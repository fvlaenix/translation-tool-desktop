package project.domain

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import core.base.BaseViewModel
import kotlinx.coroutines.launch
import project.data.ProjectInfo
import project.data.ProjectRepository

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

  fun refreshProjects() {
    viewModelScope.launch {
      _isRefreshing.value = true
      loadProjects()
      _isRefreshing.value = false
    }
  }

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