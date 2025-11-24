package project.domain

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import project.data.Project
import project.data.ProjectInfo

/**
 * Manages the currently selected project state across the application.
 */
class ProjectSelectionState {

  private val _selectedProjectInfo = mutableStateOf<ProjectInfo?>(null)
  val selectedProjectInfo: State<ProjectInfo?> = _selectedProjectInfo

  private val _selectedProject = mutableStateOf<Project?>(null)
  val selectedProject: State<Project?> = _selectedProject

  /**
   * Selects a project with optional project data.
   */
  fun selectProject(projectInfo: ProjectInfo, project: Project? = null) {
    _selectedProjectInfo.value = projectInfo
    _selectedProject.value = project
  }

  /**
   * Sets the full project data for the currently selected project.
   */
  fun setSelectedProject(project: Project) {
    _selectedProject.value = project
  }

  /**
   * Clears the current project selection.
   */
  fun clearSelection() {
    _selectedProjectInfo.value = null
    _selectedProject.value = null
  }

  /**
   * Checks if a project is currently selected.
   */
  fun hasSelection(): Boolean {
    return _selectedProjectInfo.value != null
  }
}