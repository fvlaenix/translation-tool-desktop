package app.advanced.steps

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.advanced.TranslationInfo
import app.advanced.domain.ProcessingType
import org.koin.compose.koinInject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Composable
fun TranslationStep(
  viewModel: app.advanced.domain.TranslationStepViewModel = koinInject(),
  parentSize: MutableState<IntSize>,
  translationInfos: MutableState<List<TranslationInfo>>
) {
  val uiState by viewModel.uiState

  // Sync with parent state
  LaunchedEffect(parentSize.value) {
    viewModel.updateParentSize(parentSize.value)
  }

  LaunchedEffect(translationInfos.value) {
    viewModel.setTranslationInfos(translationInfos.value)
  }

  // Update parent state when our state changes
  LaunchedEffect(uiState.translationInfos) {
    translationInfos.value = uiState.translationInfos
  }

  Column(
    modifier = Modifier
      .verticalScroll(rememberScrollState())
      .fillMaxSize()
  ) {
    uiState.translationInfos.forEachIndexed { index, info ->
      val localSize = remember { mutableStateOf(IntSize.Zero) }
      val isProcessingThisInfo = viewModel.isProcessingInfo(index)

      val outputStream = ByteArrayOutputStream()
      ImageIO.write(info.subImage, "png", outputStream)
      val byteArray = outputStream.toByteArray()
      val image = loadImageBitmap(ByteArrayInputStream(byteArray))

      Row(
        modifier = Modifier
          .border(1.dp, Color.Black, CutCornerShape(16.dp))
          .height(uiState.parentSize.height.dp / 4)
          .fillMaxWidth()
          .onSizeChanged { localSize.value = it }
      ) {
        // Image
        Column(
          modifier = Modifier
            .padding(16.dp)
            .size(width = localSize.value.width.dp / 3, height = localSize.value.height.dp)
        ) {
          Image(
            bitmap = image,
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
          )
        }

        // OCR
        Column(
          modifier = Modifier
            .padding(16.dp)
            .size(width = localSize.value.width.dp / 3, height = localSize.value.height.dp)
        ) {
          Button(
            onClick = { viewModel.processOCRForInfo(index) },
            enabled = !isProcessingThisInfo
          ) {
            if (isProcessingThisInfo && uiState.processingType == ProcessingType.OCR) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp))
            } else {
              Text("Try OCR")
            }
          }

          TextField(
            value = info.ocr,
            onValueChange = { viewModel.setOcrTextDirectly(index, it) },
            modifier = Modifier.fillMaxSize(),
            enabled = !isProcessingThisInfo
          )
        }

        // Translate
        Column(
          modifier = Modifier
            .padding(16.dp)
            .size(width = localSize.value.width.dp / 3, height = localSize.value.height.dp)
        ) {
          Button(
            onClick = { viewModel.translateInfo(index) },
            enabled = !isProcessingThisInfo && info.ocr.isNotBlank()
          ) {
            if (isProcessingThisInfo && uiState.processingType == ProcessingType.TRANSLATION) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp))
            } else {
              Text("Try Translate")
            }
          }

          TextField(
            value = info.translation,
            onValueChange = { viewModel.setTranslationTextDirectly(index, it) },
            modifier = Modifier.fillMaxSize(),
            enabled = !isProcessingThisInfo
          )
        }
      }
    }
  }
}