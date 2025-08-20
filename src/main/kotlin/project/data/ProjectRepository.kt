package project.data

interface ProjectRepository {
  suspend fun loadProjects(): Result<List<ProjectInfo>>
  suspend fun saveProject(project: ProjectInfo): Result<Unit>
  suspend fun createProject(name: String, path: String): Result<ProjectInfo>
  suspend fun getProject(projectInfo: ProjectInfo): Result<Project>
  suspend fun deleteProject(projectInfo: ProjectInfo): Result<Unit>
  suspend fun validateProjectPath(path: String, projectName: String): Result<Boolean>
}