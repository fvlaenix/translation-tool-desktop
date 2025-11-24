package project.data

/**
 * Repository interface for project management operations.
 */
interface ProjectRepository {
  /**
   * Loads all saved project information.
   */
  suspend fun loadProjects(): Result<List<ProjectInfo>>

  /**
   * Saves project information to storage.
   */
  suspend fun saveProject(project: ProjectInfo): Result<Unit>

  /**
   * Creates a new project with given name and path.
   */
  suspend fun createProject(name: String, path: String): Result<ProjectInfo>

  /**
   * Loads complete project data from project info.
   */
  suspend fun getProject(projectInfo: ProjectInfo): Result<Project>

  /**
   * Deletes a project and its data.
   */
  suspend fun deleteProject(projectInfo: ProjectInfo): Result<Unit>

  /**
   * Validates if project path and name are valid.
   */
  suspend fun validateProjectPath(path: String, projectName: String): Result<Boolean>
}