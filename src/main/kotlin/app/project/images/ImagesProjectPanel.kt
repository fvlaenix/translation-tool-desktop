package app.project.images

import androidx.compose.foundation.layout.Row
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import app.AppStateEnum
import project.BaseProjectData
import project.ImagesProjectData

@Composable
fun ImagesProjectPanel(state: MutableState<AppStateEnum>, baseProjectData: BaseProjectData) {
  val imagesProjectData = baseProjectData.data as ImagesProjectData

  Row {
    Text(baseProjectData.name, style = MaterialTheme.typography.h3)
  }
}