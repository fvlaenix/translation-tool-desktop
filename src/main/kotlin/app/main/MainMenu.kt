package app.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.AppStateEnum
import app.TopBar
import app.project.ProjectListPanel
import utils.FontService

@Composable
fun MainMenu(mutableState: MutableState<AppStateEnum>) {
  TopBar(mutableState, "Main Menu", true) {
    Row(
      modifier = Modifier.padding(16.dp)
    ) {
      Column(modifier = Modifier.fillMaxWidth(0.5f)) {
        Button(onClick = { mutableState.value = AppStateEnum.SIMPLE_VERSION }) {
          Text("Simple Translator")
        }
        Button(onClick = { mutableState.value = AppStateEnum.ADVANCED_VERSION }) {
          Text("Advanced Translator")
        }
        Button(onClick = { mutableState.value = AppStateEnum.BATCH_CREATOR }) {
          Text("Batch Creator")
        }
        Button(
          onClick = { mutableState.value = AppStateEnum.OCR_CREATOR },
          enabled = FontService.getInstance().isFontsAdded()
        ) {
          Text("OCR Creator")
        }
        Button(
          onClick = { mutableState.value = AppStateEnum.LOAD_OCR_CREATOR }
        ) {
          Text("Load OCR")
        }
        Button(
          onClick = { mutableState.value = AppStateEnum.TRANSLATION_CREATOR }
        ) {
          Text("Translation Creator")
        }
        Button(
          onClick = { mutableState.value = AppStateEnum.EDIT_CREATOR }
        ) {
          Text("Edit creator")
        }
      }
      Column {
        ProjectListPanel(mutableState)
      }
    }
  }
}