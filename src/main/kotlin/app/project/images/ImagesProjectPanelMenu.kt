package app.project.images

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import app.AppStateEnum
import project.Project

@Composable
fun ImagesProjectPanelMenu(
  appState: MutableState<AppStateEnum>,
  projectState: MutableState<ImageProjectPanelState>,
  project: Project
) {
  Column {
    Text(project.name, style = MaterialTheme.typography.h3)

    Button(onClick = { projectState.value = ImageProjectPanelState.UNTRANSLATED_IMAGES_CREATOR }) { Text("Add untranslated pictures") }
    // TODO lock if no untranslated pictures
    Button(onClick = { projectState.value = ImageProjectPanelState.OCR_CREATOR }) { Text("Try OCR untranslated pictures") }
    // TODO lock if no OCR
    Button(onClick = { projectState.value = ImageProjectPanelState.TRANSLATION_CREATOR }) { Text("Translate OCR") }

    Button(onClick = { projectState.value = ImageProjectPanelState.CLEANED_IMAGES_CREATOR }) { Text("Add cleaned pictures") }
    // TODO lock if no translate and cleaned pictures
    Button(onClick = { projectState.value = ImageProjectPanelState.EDIT_CREATOR }) { Text("Add translation to cleaned pictures") }
  }
}