package app.project

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import core.navigation.NavigationController
import core.navigation.NavigationDestination
import io.github.vinceglb.filekit.core.FileKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import project.domain.NewProjectViewModel

@Composable
fun NewProjectPanel(navigationController: NavigationController) {
  val viewModel: NewProjectViewModel = koinInject()
  val scope = rememberCoroutineScope()

  val projectName by viewModel.projectName
  val basePath by viewModel.basePath
  val validationErrors by viewModel.validationErrors
  val isCreating by viewModel.isCreating
  val creationSuccess by viewModel.creationSuccess
  val error by viewModel.error

  // Navigate to project panel on successful creation
  LaunchedEffect(creationSuccess) {
    if (creationSuccess) {
      navigationController.navigateTo(NavigationDestination.Project)
      viewModel.resetForm()
    }
  }

  Column(modifier = Modifier.padding(16.dp)) {
    Row {
      Text("Path: ")
      TextField(
        value = basePath,
        onValueChange = { viewModel.setBasePath(it) },
        label = { Text("Project base path") },
        singleLine = true,
        isError = validationErrors.containsKey("path"),
        enabled = !isCreating
      )
      Button(
        onClick = {
          scope.launch(Dispatchers.IO) {
            val files = FileKit.pickDirectory("Directory where project created")
            files?.file?.absolutePath?.let { path ->
              viewModel.setBasePath(path)
            }
          }
        },
        enabled = !isCreating
      ) {
        Text("Select")
      }
    }

    validationErrors["path"]?.let { errorMessage ->
      Text(
        text = errorMessage,
        color = MaterialTheme.colors.error,
        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
      )
    }

    Row {
      Text("Project name: ")
      TextField(
        value = projectName,
        onValueChange = { viewModel.setProjectName(it) },
        label = { Text("Project name") },
        isError = validationErrors.containsKey("name"),
        enabled = !isCreating
      )
    }

    validationErrors["name"]?.let { errorMessage ->
      Text(
        text = errorMessage,
        color = MaterialTheme.colors.error,
        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
      )
    }

    Row {
      if (!viewModel.isValid()) {
        Text("Project parent folder should exist and project folder should not exist")
      } else {
        Text("Project folder will be created in ${viewModel.fullProjectPath}")
      }
    }

    if (isCreating) {
      Row {
        CircularProgressIndicator()
        Text("Creating project...")
      }
    }

    error?.let { errorMessage ->
      Text(
        text = errorMessage,
        color = MaterialTheme.colors.error,
        modifier = Modifier.padding(top = 8.dp)
      )
    }

    Row {
      Button(
        onClick = { viewModel.createProject() },
        enabled = viewModel.isValid() && !isCreating
      ) {
        Text("Create project")
      }

      Button(
        onClick = {
          viewModel.resetForm()
          navigationController.navigateTo(NavigationDestination.MainMenu)
        },
        enabled = !isCreating
      ) {
        Text("Cancel")
      }
    }
  }
}