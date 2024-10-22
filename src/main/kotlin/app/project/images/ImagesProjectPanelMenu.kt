package app.project.images

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import app.AppStateEnum
import app.batch.ImageDataService
import app.translation.TextDataService
import kotlinx.coroutines.launch
import project.Project

@Composable
fun ImagesProjectPanelMenu(
  appState: MutableState<AppStateEnum>,
  projectState: MutableState<ImageProjectPanelState>,
  project: Project
) {
  val scope = rememberCoroutineScope()

  val untranslatedImagesDataService = remember { ImageDataService.getInstance(project, ImageDataService.UNTRANSLATED) }
  val untranslatedTextDataService = remember { TextDataService.getInstance(project, TextDataService.UNTRANSLATED) }
  val translatedTextDataService = remember { TextDataService.getInstance(project, TextDataService.TRANSLATED) }
  val cleanImagesDataService = remember { ImageDataService.getInstance(project, ImageDataService.CLEANED) }

  // TODO make them useful
  val untranslatedImagesDataServiceLoaded = remember { mutableStateOf(false) }
  val untranslatedTextDataServiceLoaded = remember { mutableStateOf(false) }
  val translatedTextDataServiceLoaded = remember { mutableStateOf(false) }
  val cleanImagesDataServiceLoaded = remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    scope.launch {
      untranslatedImagesDataService.waitUntilLoaded()
      untranslatedImagesDataServiceLoaded.value = true
    }
    scope.launch {
      untranslatedTextDataService.waitUntilLoaded()
      untranslatedTextDataServiceLoaded.value = true
    }
    scope.launch {
      translatedTextDataService.waitUntilLoaded()
      translatedTextDataServiceLoaded.value = true
    }
    scope.launch {
      cleanImagesDataService.waitUntilLoaded()
      cleanImagesDataServiceLoaded.value = true
    }
  }

  Column {
    Text(project.name, style = MaterialTheme.typography.h3)

    Row {
      if (!untranslatedImagesDataServiceLoaded.value) {
        CircularProgressIndicator()
      }
      Button(
        onClick = { projectState.value = ImageProjectPanelState.UNTRANSLATED_IMAGES_CREATOR },
        enabled = untranslatedImagesDataServiceLoaded.value
      ) { Text("Add untranslated pictures") }
    }

    Row {
      if (!untranslatedTextDataServiceLoaded.value) {
        CircularProgressIndicator()
      }
      Button(
        onClick = { projectState.value = ImageProjectPanelState.OCR_CREATOR },
        enabled = untranslatedTextDataServiceLoaded.value && untranslatedImagesDataService.get().isNotEmpty()
      ) { Text("Try OCR untranslated pictures") }
      if (untranslatedTextDataServiceLoaded.value && untranslatedImagesDataService.get().isEmpty()) {
        Text("Untranslated images should be added first")
      }
    }

    Row {
      if (!translatedTextDataServiceLoaded.value) {
        CircularProgressIndicator()
      }
      Button(
        onClick = { projectState.value = ImageProjectPanelState.TRANSLATION_CREATOR },
        enabled = translatedTextDataServiceLoaded.value && untranslatedTextDataService.workData != null
      ) { Text("Translate OCR") }
      if (translatedTextDataServiceLoaded.value && untranslatedTextDataService.workData == null) {
        Text("OCR should be done first")
      }
    }

    Row {
      if (!cleanImagesDataServiceLoaded.value) {
        CircularProgressIndicator()
      }
      Button(
        onClick = { projectState.value = ImageProjectPanelState.CLEANED_IMAGES_CREATOR },
        enabled = cleanImagesDataServiceLoaded.value
      ) { Text("Add cleaned pictures") }
    }

    Row {
      Button(
        onClick = { projectState.value = ImageProjectPanelState.EDIT_CREATOR },
        enabled = cleanImagesDataService.get().isNotEmpty() && translatedTextDataService.workData != null
      ) {
        Text("Add translation to cleaned pictures")
      }
      if (cleanImagesDataService.get().isEmpty() || translatedTextDataService.workData == null) {
        Text("Cleaned images and translation should be added first")
      }
    }
  }
}