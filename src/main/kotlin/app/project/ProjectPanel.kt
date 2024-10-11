package app.project

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import app.AppStateEnum
import app.project.images.ImagesProjectPanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import project.BaseProjectData
import project.ImagesProjectData
import project.ProjectsService
import utils.JSON
import kotlin.io.path.readText

@Composable
fun ProjectPanel(state: MutableState<AppStateEnum>) {
  val mutableSelectedProjectData: MutableState<BaseProjectData?> = remember { mutableStateOf(null) }

  val scope = rememberCoroutineScope()

  LaunchedEffect(Unit) {
    scope.launch(Dispatchers.IO) {
      val currentProjectInfo = ProjectsService.getInstance().selectedProjectInfo ?: TODO()
      if (!currentProjectInfo.exists) {
        TODO()
      }
      val path = currentProjectInfo.path
      val projectFile = path.resolve("project.json")
      try {
        val project = JSON.decodeFromString<BaseProjectData>(projectFile.readText())
        mutableSelectedProjectData.value = project
      } catch (_: Exception) {
        TODO()
      }
    }
  }

  val selectedProjectData = mutableSelectedProjectData.value
  if (selectedProjectData == null) {
    LoadingProjectPanel()
  } else {
    ProjectPanel(state, selectedProjectData)
  }
}

@Composable
private fun ProjectPanel(state: MutableState<AppStateEnum>, baseProjectData: BaseProjectData) {
  when (baseProjectData.data) {
    is ImagesProjectData -> {
      ImagesProjectPanel(state, baseProjectData)
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