package app.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import core.navigation.NavigationController
import core.navigation.NavigationDestination
import core.utils.KotlinUtils.applyIf
import org.koin.compose.koinInject
import project.data.ProjectInfo
import project.domain.ProjectListViewModel
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ProjectListPanel(navigationController: NavigationController) {
  val viewModel: ProjectListViewModel = koinInject()

  val projects by viewModel.projects
  val isLoading by viewModel.isLoading
  val isRefreshing by viewModel.isRefreshing
  val error by viewModel.error

  val fullSize = remember { mutableStateOf(IntSize.Zero) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .onSizeChanged { fullSize.value = it }
  ) {
    Row(modifier = Modifier.fillMaxWidth()) {
      Button(onClick = {
        navigationController.navigateTo(NavigationDestination.NewProject)
      }) {
        Text("Create new project")
      }

      Button(
        onClick = { viewModel.refreshProjects() },
        enabled = !isLoading && !isRefreshing
      ) {
        if (isRefreshing) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp))
        } else {
          Text("Refresh")
        }
      }
    }

    if (isLoading) {
      Row(modifier = Modifier.fillMaxWidth()) {
        CircularProgressIndicator()
        Text("Loading projects...")
      }
    }

    error?.let { errorMessage ->
      Text(
        text = errorMessage,
        color = MaterialTheme.colors.error,
        modifier = Modifier.padding(8.dp)
      )
    }

    projects.forEach { project ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colors.primary)
          .pointerInput(Unit) {
            detectTapGestures {
              viewModel.selectProject(project)
              navigationController.navigateTo(NavigationDestination.Project)
            }
          }
      ) {
        ProjectPreviewPanel(project)
      }
    }
  }
}

@Composable
private fun ProjectPreviewPanel(projectInfo: ProjectInfo) {
  Column(
    modifier = Modifier
      .padding(10.dp)
      .applyIf(!projectInfo.exists) { it.background(Color.Gray) }
  ) {
    Text(
      text = projectInfo.name,
      style = MaterialTheme.typography.h6
    )

    val formattedTime = if (projectInfo.lastTimeChange == null) {
      "????-??-?? ??:??:??"
    } else {
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
      val localDateTime = LocalDateTime.ofInstant(projectInfo.lastTimeChange!!.toInstant(), ZoneId.systemDefault())
      localDateTime.format(formatter)
    }

    Text(
      text = "Last changed: $formattedTime",
      style = MaterialTheme.typography.body2
    )
  }
}