package app.project

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.AppStateEnum
import io.github.vinceglb.filekit.core.FileKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import project.ImagesProjectData
import project.BaseProjectData
import project.ProjectsService
import utils.JSON
import java.nio.file.Path
import kotlin.io.path.*

@Composable
fun NewProjectPanel(state: MutableState<AppStateEnum>) {
  val scope = rememberCoroutineScope()

  var pathString by remember { mutableStateOf("") }
  var projectName by remember { mutableStateOf("") }
  val projectFolderName = projectName.replace(Regex("[^a-zA-Z0-9]"), "-").lowercase()

  var isDoneUnlocked by remember { mutableStateOf(false) }

  LaunchedEffect(pathString, projectFolderName) {
    val path = Path.of(pathString)
    isDoneUnlocked = path.exists() && path.resolve(projectFolderName).notExists()
  }

  Column(modifier = Modifier.padding(16.dp)) {
    Row {
      Text("Path: ")
      TextField(
        value = pathString,
        onValueChange = { pathString = it },
        label = { Text("Project base path") },
        singleLine = true
      )
      Button(
        onClick = {
          scope.launch(Dispatchers.IO) {
            val files = FileKit.pickDirectory("Directory where project created")
            pathString = files?.file?.absolutePath ?: return@launch
          }
        }
      ) {
        Text("Select")
      }
    }
    Row {
      Text("Project name: ")
      TextField(
        value = projectName,
        onValueChange = { projectName = it },
        label = { Text("Project name") },
      )
    }
    Row {
      if (!isDoneUnlocked) {
        Text("Project parent folder should exists and project folder should not exists")
      } else {
        Text("Project folder will be created in ${Path.of(pathString, projectFolderName).absolutePathString()}")
      }
    }
    Row {
      Button(
        onClick = {
          val projectPath = Path.of(pathString, projectFolderName)
          projectPath.createDirectories()
          val projectInfo = ProjectsService.ProjectInfoData(projectName, projectPath.absolutePathString())
          val imagesProjectData = ImagesProjectData(projectName)
          val projectData = BaseProjectData(projectName, imagesProjectData)
          runBlocking {
            // TODO make indicator
            ProjectsService.getInstance().add(projectInfo)
          }
          val projectFile = projectPath.resolve("project.json")
          projectFile.writeText(JSON.encodeToString<BaseProjectData>(projectData))
          ProjectsService.getInstance().selectedProjectInfo = projectInfo
          state.value = AppStateEnum.PROJECT
        },
        enabled = isDoneUnlocked
      ) {
        Text("Create project")
      }
    }
  }
}