package project.di

import org.koin.dsl.module
import project.data.*
import project.domain.NewProjectViewModel
import project.domain.ProjectListViewModel
import project.domain.ProjectPanelViewModel
import project.domain.ProjectSelectionState

/**
 * Koin module for project-related dependencies including repositories and view models.
 */
val projectModule = module {

  // Repositories
  single<ProjectRepository> {
    ProjectRepositoryImpl(projectsFilePath = "projects.json")
  }

  single<ImageDataRepository> {
    ImageDataRepositoryImpl()
  }

  single<TextDataRepository> {
    TextDataRepositoryImpl()
  }

  single {
    ProjectFileManager()
  }

  // Domain layer
  single {
    ProjectSelectionState()
  }

  factory {
    ProjectListViewModel(
      projectRepository = get(),
      projectSelectionState = get()
    )
  }

  factory {
    NewProjectViewModel(
      projectRepository = get(),
      projectSelectionState = get()
    )
  }

  factory {
    ProjectPanelViewModel(
      projectRepository = get(),
      imageDataRepository = get(),
      textDataRepository = get(),
      projectSelectionState = get()
    )
  }
}