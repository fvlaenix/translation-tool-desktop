package app.project.images

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import app.AppStateEnum
import app.batch.ImageDataCreator
import app.batch.ImageDataService
import app.editor.EditCreator
import app.ocr.OCRCreator
import app.translation.TextDataService
import app.translation.TranslationCreator
import kotlinx.coroutines.launch
import project.ImagesProjectData
import project.Project
import utils.AnimatedContentUtils.horizontalSpec

@Composable
fun ImagesProjectPanel(state: MutableState<AppStateEnum>, project: Project) {
  val projectState = remember { mutableStateOf(ImageProjectPanelState.MAIN_MENU) }
  val imagesProjectData = project.data as ImagesProjectData

  AnimatedContent(
    targetState = projectState.value,
    modifier = Modifier.fillMaxSize(),
    transitionSpec = horizontalSpec<ImageProjectPanelState>()
  ) { targetState ->
    when (targetState) {
      ImageProjectPanelState.MAIN_MENU -> ImagesProjectPanelMenu(state, projectState, project)
      ImageProjectPanelState.UNTRANSLATED_IMAGES_CREATOR -> ImageDataCreator(state, projectState, project)
      ImageProjectPanelState.OCR_CREATOR -> OCRCreator(state, project)
      ImageProjectPanelState.TRANSLATION_CREATOR -> TranslationCreator(state, project)
      ImageProjectPanelState.CLEANED_IMAGES_CREATOR -> ImageDataCreator(state, projectState, project)
      ImageProjectPanelState.EDIT_CREATOR -> EditCreator(state, project)
    }
  }
}

enum class ImageProjectPanelState {
  MAIN_MENU, UNTRANSLATED_IMAGES_CREATOR, OCR_CREATOR, TRANSLATION_CREATOR, CLEANED_IMAGES_CREATOR, EDIT_CREATOR
}