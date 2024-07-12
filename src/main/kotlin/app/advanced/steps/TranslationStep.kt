package app.advanced.steps

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import utils.ProtobufUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Composable
fun TranslationStep(parentSize: MutableState<IntSize>, translationInfos: MutableState<List<TranslationInfo>>) {
  val scope = rememberCoroutineScope()

  Column(
    modifier = Modifier
      .verticalScroll(rememberScrollState())
      .fillMaxSize()
  ) {
    translationInfos.value.forEach { info ->
      val localSize = remember { mutableStateOf(IntSize.Zero) }
      var localOcrText by remember { mutableStateOf(info.ocr) }
      var localTranslationText by remember { mutableStateOf(info.translation) }

      val outputStream = ByteArrayOutputStream()
      ImageIO.write(info.subImage, "png", outputStream)
      val byteArray = outputStream.toByteArray()
      val image = loadImageBitmap(ByteArrayInputStream(byteArray))
      Row(
        modifier = Modifier
          .border(1.dp, Color.Black, CutCornerShape(4.dp))
          .height(parentSize.value.height.dp / 4)
          .fillMaxWidth()
          .onSizeChanged { localSize.value = it }
      ) {
        // Image
        Column(
          modifier = Modifier
            .padding(4.dp)
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
            .padding(4.dp)
            .size(width = localSize.value.width.dp / 3, height = localSize.value.height.dp)
        ) {
          Button(onClick = {
            scope.launch(Dispatchers.IO) {
              val localBufferedImage = info.subImage
              val previous = if (localOcrText.isBlank()) "" else localOcrText + "\n\n"
              localOcrText = previous + ProtobufUtils.getOCR(localBufferedImage)
            }
          }) {
            Text("Try OCR")
          }

          TextField(
            value = localOcrText,
            onValueChange = { localOcrText = it },
            modifier = Modifier
              .fillMaxSize()
          )
        }
        // Translate
        Column(
          modifier = Modifier
            .padding(4.dp)
            .size(width = localSize.value.width.dp / 3, height = localSize.value.height.dp)
        ) {
          Button(onClick = {
            scope.launch(Dispatchers.IO) {
              val previous = if (localTranslationText.isBlank()) "" else localTranslationText + "\n\n"
              localTranslationText = previous + ProtobufUtils.getTranslation(localOcrText)
            }
          }) {
            Text("Try Translate")
          }
          TextField(
            value = localTranslationText,
            onValueChange = { localTranslationText = it },
            modifier = Modifier
              .fillMaxSize()
          )
        }
      }
    }
  }
}