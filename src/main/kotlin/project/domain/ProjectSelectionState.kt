package project.domain

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import project.data.Project
import project.data.ProjectInfo

class ProjectSelectionState {

  private val _selectedProjectInfo = mutableStateOf<ProjectInfo?>(null)
  val selectedProjectInfo: State<ProjectInfo?> = _selectedProjectInfo

  private val _selectedProject = mutableStateOf<Project?>(null)
  val selectedProject: State<Project?> = _selectedProject

  fun selectProject(projectInfo: ProjectInfo, project: Project? = null) {
    _selectedProjectInfo.value = projectInfo
    _selectedProject.value = project
  }

  fun setSelectedProject(project: Project) {
    _selectedProject.value = project
  }

  fun clearSelection() {
    _selectedProjectInfo.value = null
    _selectedProject.value = null
  }

  fun hasSelection(): Boolean {
    return _selectedProjectInfo.value != null
  }
}