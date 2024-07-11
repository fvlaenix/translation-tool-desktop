package app.advanced

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.AppStateEnum
import app.TopBar
import app.advanced.steps.TranslationStep
import java.awt.image.BufferedImage
import kotlin.math.max

@Composable
fun AdvancedTranslator(mutableState: MutableState<AppStateEnum>) {
  val currentSize = remember { mutableStateOf(IntSize.Zero) }

  val imageBuffered = remember { mutableStateOf<ImageBitmap?>(null) }
  val boxes = remember { mutableStateOf<List<BoxOnImageData>>(emptyList()) }
  val translationInfos = remember { mutableStateOf<List<TranslationInfo>>(emptyList()) }
  val isEnabled = remember { mutableStateOf(false) }
  val imageSize = remember { mutableStateOf(IntSize.Zero) }

  val currentState = remember { mutableStateOf(AdvancedTranslatorState.INITIAL_IMAGE) }

  TopBar(mutableState, "Advanced Translator",
    bottomBar = {
      BottomAppBar {
        Row {
          Button(
            onClick = {
              currentState.value = AdvancedTranslatorState.entries[currentState.value.ordinal - 1]
              isEnabled.value = false
            },
            enabled = currentState.value.ordinal > 0
          ) {
            Text("Previous")
          }
          Button(
            onClick = {
              currentState.value = AdvancedTranslatorState.entries[currentState.value.ordinal + 1]
              isEnabled.value = false

              val fullBufferedImage = imageBuffered.value!!.toAwtImage()

              val scale = max(fullBufferedImage.width / imageSize.value.width.toDouble(), fullBufferedImage.height / imageSize.value.height.toDouble())

              translationInfos.value = boxes.value.map { boxData ->
                fun Int.scale(): Int = (this * scale).toInt()
                val subImage = fullBufferedImage.getSubimage(boxData.x.scale(), boxData.y.scale(), boxData.sizeX.value.scale(), boxData.sizeY.value.scale())
                TranslationInfo(subImage)
              }
              if (translationInfos.value.isEmpty()) {
                translationInfos.value = listOf(TranslationInfo(fullBufferedImage))
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
        .fillMaxSize().onSizeChanged { size -> currentSize.value = size }
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
          AdvancedTranslatorState.INITIAL_IMAGE -> ImageWithBoxes(imageBuffered, boxes, isEnabled, imageSize)
          AdvancedTranslatorState.TRANSLATION_STEP -> TranslationStep(currentSize, translationInfos)
        }
      }
    }
  }
}

enum class AdvancedTranslatorState {
  INITIAL_IMAGE, TRANSLATION_STEP
}

data class TranslationInfo(
  val subImage: BufferedImage,
  val ocr: String = "",
  val translation: String = ""
)