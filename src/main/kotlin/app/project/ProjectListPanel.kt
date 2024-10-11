package app.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.AppStateEnum
import project.ProjectsService
import utils.KotlinUtils.applyIf
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ProjectListPanel(state: MutableState<AppStateEnum>) {
  val projectsService = remember { ProjectsService.getInstance() }
  val fullSize = remember { mutableStateOf(IntSize.Zero) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .onSizeChanged { fullSize.value = it }
  ) {
    Row(modifier = Modifier.fillMaxWidth()) {
      Button(onClick = {
        state.value = AppStateEnum.NEW_PROJECT
      }) {
        Text("Create new project")
      }
    }

    projectsService.getProjects().forEach { project ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colors.primary)
          .pointerInput(Unit) {
            detectTapGestures {
              projectsService.selectedProjectInfo = project
              state.value = AppStateEnum.PROJECT
            }
          }
      ) {
        ProjectPreviewPanel(project)
      }
    }
  }
}

@Composable
private fun ProjectPreviewPanel(baseProjectData: ProjectsService.ProjectInfoData) {
  Column(
    modifier = Modifier
      .padding(10.dp)
      .applyIf(!baseProjectData.exists) { it.background(Color.Gray) }
  ) {
    Text(
      text = baseProjectData.name,
      style = MaterialTheme.typography.h6
    )

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val localDateTime = LocalDateTime.ofInstant(baseProjectData.lastTimeChange.toInstant(), ZoneId.systemDefault())
    val formattedTime = localDateTime.format(formatter)

    Text(
      text = "Last changed: $formattedTime",
      style = MaterialTheme.typography.body2
    )
  }
}