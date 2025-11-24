package app.project

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.project.images.ImagesProjectPanel
import core.navigation.NavigationController
import core.navigation.NavigationDestination
import org.koin.compose.koinInject
import project.data.ImagesProjectData
import project.data.Project
import project.domain.ProjectPanelViewModel

@Composable
fun ProjectPanel(navigationController: NavigationController) {
  val viewModel: ProjectPanelViewModel = koinInject()

  val currentProject by viewModel.currentProject
  val isLoadingProject by viewModel.isLoadingProject
  val error by viewModel.error

  // Load project when this composable is displayed
  LaunchedEffect(Unit) {
    viewModel.loadProject()
  }

  when {
    isLoadingProject -> LoadingProjectPanel()
    error != null -> ErrorProjectPanel(error!!) { viewModel.refreshProject() }
    currentProject != null -> ProjectPanel(navigationController, currentProject!!, viewModel)
    else -> NoProjectSelectedPanel(navigationController)
  }
}

@Composable
private fun ProjectPanel(
  navigationController: NavigationController,
  project: Project,
  viewModel: ProjectPanelViewModel
) {
  when (project.data) {
    is ImagesProjectData -> {
      ImagesProjectPanel(navigationController, project, viewModel)
    }
  }
}

@Composable
private fun LoadingProjectPanel() {
  Column(modifier = Modifier.fillMaxSize()) {
    Text("Loading project...", style = MaterialTheme.typography.h1, modifier = Modifier.fillMaxWidth())
    CircularProgressIndicator(modifier = Modifier.fillMaxSize())
  }
}

@Composable
private fun ErrorProjectPanel(errorMessage: String, onRetry: () -> Unit) {
  Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
    Text(
      text = "Error loading project",
      style = MaterialTheme.typography.h4,
      color = MaterialTheme.colors.error
    )
    Text(
      text = errorMessage,
      style = MaterialTheme.typography.body1,
      modifier = Modifier.padding(top = 8.dp)
    )
    Button(
      onClick = onRetry,
      modifier = Modifier.padding(top = 16.dp)
    ) {
      Text("Retry")
    }
  }
}

@Composable
private fun NoProjectSelectedPanel(navigationController: NavigationController) {
  Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
    Text("No project selected", style = MaterialTheme.typography.h4)
    Button(
      onClick = { navigationController.navigateTo(NavigationDestination.MainMenu) },
      modifier = Modifier.padding(top = 16.dp)
    ) {
      Text("Go to Main Menu")
    }
  }
}