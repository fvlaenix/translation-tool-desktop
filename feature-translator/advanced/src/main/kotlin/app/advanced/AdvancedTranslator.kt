package app.advanced

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.TopBar
import app.advanced.domain.ImageWithBoxesViewModel
import app.advanced.domain.TranslationStepViewModel
import app.advanced.steps.TranslationStep
import core.navigation.NavigationController
import org.koin.compose.koinInject
import java.awt.image.BufferedImage

/**
 * Advanced translation workflow with multi-step process: image with text boxes → translation step.
 */
@Composable
fun AdvancedTranslator(navigationController: NavigationController) {
  val currentSize = remember { mutableStateOf(IntSize.Zero) }
  val translationInfos = remember { mutableStateOf<List<TranslationInfo>>(emptyList()) }
  val isEnabled = remember { mutableStateOf(false) }
  val currentState = remember { mutableStateOf(AdvancedTranslatorState.INITIAL_IMAGE) }

  val imageWithBoxesViewModel: ImageWithBoxesViewModel = koinInject()
  val translationStepViewModel: TranslationStepViewModel = koinInject()

  val imageWithBoxesState by imageWithBoxesViewModel.uiState

  LaunchedEffect(imageWithBoxesState.image) {
    isEnabled.value = imageWithBoxesState.image != null
  }

  TopBar(
    navigationController, "Advanced Translator",
    bottomBar = {
      BottomAppBar {
        Row {
          Button(
            onClick = {
              if (currentState.value.ordinal > 0) {
                currentState.value = AdvancedTranslatorState.entries[currentState.value.ordinal - 1]
                isEnabled.value = imageWithBoxesState.image != null
              }
            },
            enabled = currentState.value.ordinal > 0
          ) {
            Text("Previous")
          }

          Button(
            onClick = {
              if (currentState.value.ordinal < AdvancedTranslatorState.entries.size - 1) {
                currentState.value = AdvancedTranslatorState.entries[currentState.value.ordinal + 1]
                isEnabled.value = false

                // Prepare translation infos when moving to translation step
                val fullBufferedImage = imageWithBoxesState.image?.toAwtImage()
                if (fullBufferedImage != null) {
                  val infos = if (imageWithBoxesState.boxes.isEmpty()) {
                    listOf(TranslationInfo(fullBufferedImage))
                  } else {
                    imageWithBoxesState.boxes.map { boxData ->
                      val subImage = fullBufferedImage.getSubimage(
                        boxData.x.toInt(),
                        boxData.y.toInt(),
                        boxData.width.toInt(),
                        boxData.height.toInt()
                      )
                      TranslationInfo(subImage)
                    }
                  }
                  translationInfos.value = infos
                }
              }
            },
            enabled = currentState.value.ordinal < AdvancedTranslatorState.entries.size - 1 && isEnabled.value
          ) {
            Text("Next")
          }
        }
      }
    }) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .onSizeChanged { size -> currentSize.value = size }
        .padding(16.dp)
    ) {
      AnimatedContent(
        targetState = currentState.value,
        transitionSpec = {
          if (targetState.ordinal > initialState.ordinal) {
            slideInHorizontally { height -> height } togetherWith slideOutHorizontally { height -> -height }
          } else {
            slideInHorizontally { height -> -height } togetherWith slideOutHorizontally { height -> height }
          }.using(
            SizeTransform(clip = false)
          )
        },
      ) { targetState ->
        when (targetState) {
          AdvancedTranslatorState.INITIAL_IMAGE -> {
            ImageWithBoxes(
              viewModel = imageWithBoxesViewModel,
              onStateChange = { /* boxes state is managed by ViewModel */ }
            )
          }

          AdvancedTranslatorState.TRANSLATION_STEP -> {
            TranslationStep(
              viewModel = translationStepViewModel,
              parentSize = currentSize,
              translationInfos = translationInfos
            )
          }
        }
      }
    }
  }
}

/**
 * Workflow states for advanced translator: initial_image, translation_step.
 */
enum class AdvancedTranslatorState {
  INITIAL_IMAGE, TRANSLATION_STEP
}

/**
 * Data class holding sub-image with OCR and translation text for advanced workflow.
 */
data class TranslationInfo(
  val subImage: BufferedImage,
  val ocr: String = "",
  val translation: String = ""
)