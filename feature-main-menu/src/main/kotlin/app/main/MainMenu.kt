package app.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import app.TopBar
import app.project.ProjectListPanel
import core.navigation.NavigationController
import core.navigation.NavigationDestination
import fonts.data.FontRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Main navigation menu with buttons for different translation workflows and project management.
 */
@Composable
fun MainMenu(navigationController: NavigationController) {
  val fontRepository: FontRepository = koinInject()
  val workflowState = rememberWorkflowState()
  val scope = rememberCoroutineScope()

  var isFontsAdded by remember { mutableStateOf(false) }

  // Check fonts availability
  LaunchedEffect(Unit) {
    scope.launch {
      fontRepository.isFontsAdded()
        .onSuccess { hasFont -> isFontsAdded = hasFont }
        .onFailure { isFontsAdded = false }
    }
  }

  TopBar(navigationController, "Main Menu", true) {
    Row(
      modifier = Modifier.padding(16.dp)
    ) {
      Column(modifier = Modifier.fillMaxWidth(0.5f)) {
        Column(modifier = Modifier.fillMaxWidth(0.9f)) {
          // Quick Tools Section
          WorkflowSection(
            title = "Quick Tools",
            description = "Single-image workflows"
          ) {
            WorkflowButton(
              text = "Simple Translator",
              description = "OCR + Translation in one step",
              enabled = isFontsAdded,
              statusText = if (isFontsAdded) "Ready" else "Add fonts first",
              isReady = isFontsAdded,
              onClick = { navigationController.navigateTo(NavigationDestination.SimpleTranslator) }
            )

            WorkflowButton(
              text = "Advanced Translator",
              description = "Manual text box selection",
              enabled = isFontsAdded,
              statusText = if (isFontsAdded) "Ready" else "Add fonts first",
              isReady = isFontsAdded,
              onClick = { navigationController.navigateTo(NavigationDestination.AdvancedTranslator) }
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Batch Workflow Section
          WorkflowSection(
            title = "Batch Workflow",
            description = "Multi-step process for multiple images"
          ) {
            WorkflowButton(
              text = "1. Batch Creator",
              description = "Add images to process",
              enabled = true,
              statusText = if (workflowState.hasImages) "Images added" else "Ready to add images",
              isReady = true,
              onClick = { navigationController.navigateTo(NavigationDestination.BatchCreator) }
            )

            WorkflowButton(
              text = "2. OCR Creator",
              description = "Extract text from images",
              enabled = workflowState.canRunOCR() && isFontsAdded,
              statusText = when {
                !isFontsAdded -> "Add fonts first"
                else -> workflowState.getOCRStatusText()
              },
              isReady = workflowState.canRunOCR() && isFontsAdded,
              onClick = { navigationController.navigateTo(NavigationDestination.OCRCreator) }
            )

            WorkflowButton(
              text = "3. Translation Creator",
              description = "Translate extracted text",
              enabled = workflowState.canRunTranslation(),
              statusText = workflowState.getTranslationStatusText(),
              isReady = workflowState.canRunTranslation(),
              onClick = { navigationController.navigateTo(NavigationDestination.TranslationCreator) }
            )

            WorkflowButton(
              text = "4. Edit Creator",
              description = "Add translated text to images",
              enabled = workflowState.canRunEdit(),
              statusText = workflowState.getEditStatusText(),
              isReady = workflowState.canRunEdit(),
              onClick = { navigationController.navigateTo(NavigationDestination.EditCreator) }
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Utility Section
          WorkflowSection(
            title = "Utilities",
            description = "Additional tools"
          ) {
            WorkflowButton(
              text = "Load OCR",
              description = "Load existing OCR data",
              enabled = true,
              statusText = "Ready",
              isReady = true,
              onClick = { navigationController.navigateTo(NavigationDestination.LoadOCRCreator) }
            )
          }
        }
      }

      Column {
        ProjectListPanel(navigationController)
      }
    }
  }
}

@Composable
private fun WorkflowSection(
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
        fontWeight = FontWeight.Bold
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
private fun WorkflowButton(
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

      Column(modifier = Modifier.weight(1f)) {
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