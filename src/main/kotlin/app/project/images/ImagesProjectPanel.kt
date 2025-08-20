package app.project.images

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.batch.ImageDataCreator
import app.editor.EditCreator
import app.ocr.OCRCreator
import app.translation.TranslationCreator
import core.navigation.NavigationController
import core.utils.AnimatedContentUtils.horizontalSpec
import project.data.Project
import project.domain.ProjectPanelViewModel

@Composable
fun ImagesProjectPanel(navigationController: NavigationController, project: Project, viewModel: ProjectPanelViewModel) {
  val projectState = remember { mutableStateOf(ImageProjectPanelState.MAIN_MENU) }

  AnimatedContent(
    targetState = projectState.value,
    modifier = Modifier.fillMaxSize(),
    transitionSpec = horizontalSpec<ImageProjectPanelState>()
  ) { targetState ->
    when (targetState) {
      ImageProjectPanelState.MAIN_MENU -> ImagesProjectPanelMenu(projectState, project, viewModel)
      ImageProjectPanelState.UNTRANSLATED_IMAGES_CREATOR -> ImageDataCreator(
        navigationController,
        projectState,
        project
      )

      ImageProjectPanelState.OCR_CREATOR -> OCRCreator(navigationController, project)
      ImageProjectPanelState.TRANSLATION_CREATOR -> TranslationCreator(navigationController, project)
      ImageProjectPanelState.CLEANED_IMAGES_CREATOR -> ImageDataCreator(navigationController, projectState, project)
      ImageProjectPanelState.EDIT_CREATOR -> EditCreator(navigationController, project)
    }
  }
}

@Composable
fun ImagesProjectPanelMenu(
  projectState: MutableState<ImageProjectPanelState>,
  project: Project,
  viewModel: ProjectPanelViewModel
) {
  val projectSections by viewModel.projectSections
  val isLoading by viewModel.isLoading

  Column {
    Text(project.name, style = MaterialTheme.typography.h3)

    if (isLoading) {
      CircularProgressIndicator()
      Text("Loading project status...")
    }

    projectSections.forEach { section ->
      Row(modifier = Modifier.padding(vertical = 4.dp)) {
        when (section.id) {
          "untranslated_images" -> {
            Button(
              onClick = { projectState.value = ImageProjectPanelState.UNTRANSLATED_IMAGES_CREATOR },
              enabled = section.isEnabled
            ) {
              Text("Add untranslated pictures")
            }
            Text(section.description, modifier = Modifier.padding(start = 8.dp))
          }

          "ocr" -> {
            Button(
              onClick = { projectState.value = ImageProjectPanelState.OCR_CREATOR },
              enabled = section.isEnabled
            ) {
              Text("Try OCR untranslated pictures")
            }
            Text(section.description, modifier = Modifier.padding(start = 8.dp))
            if (!section.isEnabled && section.isRequired) {
              Text("Untranslated images should be added first", color = MaterialTheme.colors.error)
            }
          }

          "translation" -> {
            Button(
              onClick = { projectState.value = ImageProjectPanelState.TRANSLATION_CREATOR },
              enabled = section.isEnabled
            ) {
              Text("Translate OCR")
            }
            Text(section.description, modifier = Modifier.padding(start = 8.dp))
            if (!section.isEnabled && section.isRequired) {
              Text("OCR should be done first", color = MaterialTheme.colors.error)
            }
          }

          "cleaned_images" -> {
            Button(
              onClick = { projectState.value = ImageProjectPanelState.CLEANED_IMAGES_CREATOR },
              enabled = section.isEnabled
            ) {
              Text("Add cleaned pictures")
            }
            Text(section.description, modifier = Modifier.padding(start = 8.dp))
          }

          "final_edit" -> {
            Button(
              onClick = { projectState.value = ImageProjectPanelState.EDIT_CREATOR },
              enabled = section.isEnabled
            ) {
              Text("Add translation to cleaned pictures")
            }
            Text(section.description, modifier = Modifier.padding(start = 8.dp))
            if (!section.isEnabled && section.isRequired) {
              Text("Cleaned images and translation should be added first", color = MaterialTheme.colors.error)
            }
          }
        }
      }
    }
  }
}

enum class ImageProjectPanelState {
  MAIN_MENU, UNTRANSLATED_IMAGES_CREATOR, OCR_CREATOR, TRANSLATION_CREATOR, CLEANED_IMAGES_CREATOR, EDIT_CREATOR
}