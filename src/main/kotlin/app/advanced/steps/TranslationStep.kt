package app.advanced.steps

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
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
import app.advanced.domain.TranslationStepViewModel
import app.common.ProcessingButton
import org.koin.compose.koinInject
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Composable
fun TranslationStep(
  viewModel: TranslationStepViewModel = koinInject(),
  parentSize: MutableState<IntSize>,
  translationInfos: MutableState<List<TranslationInfo>>
) {
  val uiState by viewModel.uiState

  LaunchedEffect(parentSize.value) {
    viewModel.updateParentSize(parentSize.value)
  }

  LaunchedEffect(translationInfos.value) {
    viewModel.setTranslationInfos(translationInfos.value)
  }

  LaunchedEffect(uiState.translationInfos) {
    translationInfos.value = uiState.translationInfos
  }

  Column(
    modifier = Modifier
      .verticalScroll(rememberScrollState())
      .fillMaxSize()
  ) {
    uiState.translationInfos.forEachIndexed { index, info ->
      TranslationInfoItem(
        index = index,
        info = info,
        parentHeight = uiState.parentSize.height,
        isProcessing = viewModel.isProcessingInfo(index),
        processingType = uiState.processingType,
        onOCRProcess = { viewModel.processOCRForInfo(index) },
        onTranslate = { viewModel.translateInfo(index) },
        onOcrTextChange = { viewModel.setOcrTextDirectly(index, it) },
        onTranslationTextChange = { viewModel.setTranslationTextDirectly(index, it) }
      )
    }
  }
}

@Composable
private fun TranslationInfoItem(
  index: Int,
  info: TranslationInfo,
  parentHeight: Int,
  isProcessing: Boolean,
  processingType: ProcessingType?,
  onOCRProcess: () -> Unit,
  onTranslate: () -> Unit,
  onOcrTextChange: (String) -> Unit,
  onTranslationTextChange: (String) -> Unit
) {
  val localSize = remember { mutableStateOf(IntSize.Zero) }

  Row(
    modifier = Modifier
      .border(1.dp, Color.Black, CutCornerShape(16.dp))
      .height(parentHeight.dp / 4)
      .fillMaxWidth()
      .onSizeChanged { localSize.value = it }
  ) {
    ImagePreviewColumn(
      info = info,
      width = localSize.value.width / 3,
      height = localSize.value.height
    )

    OCRInputColumn(
      info = info,
      width = localSize.value.width / 3,
      height = localSize.value.height,
      isProcessing = isProcessing,
      processingType = processingType,
      onOCRProcess = onOCRProcess,
      onTextChange = onOcrTextChange
    )

    TranslationInputColumn(
      info = info,
      width = localSize.value.width / 3,
      height = localSize.value.height,
      isProcessing = isProcessing,
      processingType = processingType,
      onTranslate = onTranslate,
      onTextChange = onTranslationTextChange
    )
  }
}

@Composable
private fun ImagePreviewColumn(
  info: TranslationInfo,
  width: Int,
  height: Int
) {
  val outputStream = ByteArrayOutputStream()
  ImageIO.write(info.subImage, "png", outputStream)
  val byteArray = outputStream.toByteArray()
  val image = loadImageBitmap(java.io.ByteArrayInputStream(byteArray))

  Column(
    modifier = Modifier
      .padding(16.dp)
      .size(width = width.dp, height = height.dp)
  ) {
    androidx.compose.foundation.Image(
      bitmap = image,
      contentDescription = null,
      modifier = Modifier.fillMaxSize()
    )
  }
}

@Composable
private fun OCRInputColumn(
  info: TranslationInfo,
  width: Int,
  height: Int,
  isProcessing: Boolean,
  processingType: ProcessingType?,
  onOCRProcess: () -> Unit,
  onTextChange: (String) -> Unit
) {
  Column(
    modifier = Modifier
      .padding(16.dp)
      .size(width = width.dp, height = height.dp)
  ) {
    ProcessingButton(
      text = "Try OCR",
      isProcessing = isProcessing && processingType == ProcessingType.OCR,
      enabled = !isProcessing,
      onClick = onOCRProcess
    )

    TextField(
      value = info.ocr,
      onValueChange = onTextChange,
      modifier = Modifier.fillMaxSize(),
      enabled = !isProcessing
    )
  }
}

@Composable
private fun TranslationInputColumn(
  info: TranslationInfo,
  width: Int,
  height: Int,
  isProcessing: Boolean,
  processingType: ProcessingType?,
  onTranslate: () -> Unit,
  onTextChange: (String) -> Unit
) {
  Column(
    modifier = Modifier
      .padding(16.dp)
      .size(width = width.dp, height = height.dp)
  ) {
    ProcessingButton(
      text = "Try Translate",
      isProcessing = isProcessing && processingType == ProcessingType.TRANSLATION,
      enabled = !isProcessing && info.ocr.isNotBlank(),
      onClick = onTranslate
    )

    TextField(
      value = info.translation,
      onValueChange = onTextChange,
      modifier = Modifier.fillMaxSize(),
      enabled = !isProcessing
    )
  }
}