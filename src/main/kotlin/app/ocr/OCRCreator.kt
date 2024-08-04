package app.ocr

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import app.AppStateEnum
import app.TopBar
import app.batch.BatchService

@Composable
fun OCRCreator(state: MutableState<AppStateEnum>) {
  val parentSize = remember { mutableStateOf(IntSize.Zero) }

  val images = BatchService.getInstance().get()
  var index by remember { mutableStateOf(0) }
  val ocrService = OCRService()
  ocrService.initOfNotInitialized(images)

  TopBar(state, "OCR Creator",
    bottomBar = {
      BottomAppBar {
        Row {
          Button(onClick = { index-- }, enabled = index != 0) { Text("Previous") }
          Button(onClick = { index++ }, enabled = index != images.size) { Text("Next") }
          Button(onClick = { TODO() }) { Text("Done") }
        }
      }
    }
  ) {
    Row(
      modifier = Modifier
        .onSizeChanged { size -> parentSize.value = size }
    ) {
      Column {

      }
      Column {

      }
    }
  }
}