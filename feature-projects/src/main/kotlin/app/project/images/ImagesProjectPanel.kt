package app.project.images

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.batch.ImageDataCreator
import app.editor.EditCreator
import app.ocr.OCRCreator
import app.project.rememberProjectWorkflowState
import app.translation.TranslationCreator
import core.navigation.NavigationController
import project.data.Project
import project.domain.ProjectPanelViewModel
import translation.data.ImageProjectPanelState

@Composable
fun ImagesProjectPanel(navigationController: NavigationController, project: Project, viewModel: ProjectPanelViewModel) {
  val projectState = remember { mutableStateOf(ImageProjectPanelState.MAIN_MENU) }

  AnimatedContent(
    targetState = projectState.value,
    modifier = Modifier.fillMaxSize()
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
  val workflowState = rememberProjectWorkflowState(project)
  val isLoading by viewModel.isLoading

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    ProjectHeaderSection(
      projectName = project.name,
      isLoading = isLoading || workflowState.isLoading
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Step 1: Add Images
    ProjectWorkflowSection(
      title = "1. Image Preparation",
      description = "Add images to your project"
    ) {
      ProjectWorkflowButton(
        text = "Add Untranslated Pictures",
        description = "Import original images for processing",
        enabled = true,
        statusText = workflowState.getAddImagesStatusText(),
        isReady = true,
        onClick = { projectState.value = ImageProjectPanelState.UNTRANSLATED_IMAGES_CREATOR }
      )

      ProjectWorkflowButton(
        text = "Add Cleaned Pictures",
        description = "Import cleaned/edited versions (optional for some workflows)",
        enabled = true,
        statusText = workflowState.getCleanedImagesStatusText(),
        isReady = true,
        onClick = { projectState.value = ImageProjectPanelState.CLEANED_IMAGES_CREATOR }
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Step 2: OCR Processing
    ProjectWorkflowSection(
      title = "2. Text Extraction",
      description = "Extract text from images using OCR"
    ) {
      ProjectWorkflowButton(
        text = "Try OCR Untranslated Pictures",
        description = "Extract Japanese/source text from images",
        enabled = workflowState.canRunOCR(),
        statusText = workflowState.getOCRStatusText(),
        isReady = workflowState.canRunOCR() && !workflowState.hasOCRData,
        onClick = { projectState.value = ImageProjectPanelState.OCR_CREATOR }
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Step 3: Translation
    ProjectWorkflowSection(
      title = "3. Translation",
      description = "Translate extracted text"
    ) {
      ProjectWorkflowButton(
        text = "Translate OCR",
        description = "Translate extracted text to target language",
        enabled = workflowState.canRunTranslation(),
        statusText = workflowState.getTranslationStatusText(),
        isReady = workflowState.canRunTranslation() && !workflowState.hasTranslationData,
        onClick = { projectState.value = ImageProjectPanelState.TRANSLATION_CREATOR }
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Step 4: Final Output
    ProjectWorkflowSection(
      title = "4. Final Output",
      description = "Generate final translated images"
    ) {
      ProjectWorkflowButton(
        text = "Add Translation to Cleaned Pictures",
        description = "Apply translated text to cleaned images",
        enabled = workflowState.canRunEdit(),
        statusText = workflowState.getEditStatusText(),
        isReady = workflowState.canRunEdit(),
        onClick = { projectState.value = ImageProjectPanelState.EDIT_CREATOR }
      )
    }
  }
}

@Composable
private fun ProjectHeaderSection(
  projectName: String,
  isLoading: Boolean
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    elevation = 6.dp,
    shape = RoundedCornerShape(12.dp),
    backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f)
  ) {
    Column(
      modifier = Modifier.padding(20.dp)
    ) {
      Text(
        text = "Project: $projectName",
        style = MaterialTheme.typography.h4,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colors.primary
      )

      if (isLoading) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(top = 8.dp)
        ) {
          CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Loading project status...",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
          )
        }
      } else {
        Text(
          text = "Follow the workflow steps below to process your images",
          style = MaterialTheme.typography.body1,
          color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
          modifier = Modifier.padding(top = 4.dp)
        )
      }
    }
  }
}

@Composable
private fun ProjectWorkflowSection(
  title: String,
  description: String,
  content: @Composable ColumnScope.() -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    elevation = 4.dp,
    shape = RoundedCornerShape(8.dp)
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.h6,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colors.primary
      )
      Text(
        text = description,
        style = MaterialTheme.typography.caption,
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
        modifier = Modifier.padding(bottom = 12.dp)
      )
      content()
    }
  }
}

@Composable
private fun ProjectWorkflowButton(
  text: String,
  description: String,
  enabled: Boolean,
  statusText: String,
  isReady: Boolean,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    elevation = if (enabled) 2.dp else 0.dp,
    backgroundColor = when {
      !enabled -> MaterialTheme.colors.surface.copy(alpha = 0.5f)
      isReady -> MaterialTheme.colors.surface
      else -> MaterialTheme.colors.surface
    }
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(
          backgroundColor = if (isReady) MaterialTheme.colors.primary else MaterialTheme.colors.secondary,
          disabledBackgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
        )
      ) {
        Text(text)
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1.2f)) {
        Text(
          text = description,
          style = MaterialTheme.typography.body2,
          color = if (enabled) MaterialTheme.colors.onSurface else MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(top = 2.dp)
        ) {
          Icon(
            imageVector = if (isReady) Icons.Default.Check else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isReady) Color.Green else Color(0xFFFF9800),
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = statusText,
            style = MaterialTheme.typography.caption,
            color = if (isReady) Color.Green else Color(0xFFFF9800),
            fontSize = 11.sp
          )
        }
      }
    }
  }
}