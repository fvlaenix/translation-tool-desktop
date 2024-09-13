package app.ocr

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.AppStateEnum
import app.TopBar
import app.advanced.BoxOnImageData
import app.batch.BatchService
import app.utils.SimpleLoadedImageDisplayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import utils.ProtobufUtils
import java.awt.image.BufferedImage

@Composable
fun OCRCreator(state: MutableState<AppStateEnum>) {
  val coroutineScope = rememberCoroutineScope()
  var lockedByTask by remember { mutableStateOf(false) }

  val imageSize = remember { mutableStateOf(IntSize.Zero) }

  val images = BatchService.getInstance().get().toList()
  var index by remember { mutableStateOf(-1) }
  val imagesBoxes = remember { mutableStateListOf<List<OCRBoxData>>() }

  var currentImage = remember { mutableStateOf<BufferedImage?>(null) }
  val boxes = remember { mutableStateListOf<OCRBoxData>() }
  var selectedBox by remember { mutableStateOf<BoxOnImageData?>(null) }

  fun setIndex(newIndex: Int) {
    if (newIndex == -1) {
      index = -1
    } else {
      // save result
      if (index >= 0) {
        while (imagesBoxes.size <= index) {
          imagesBoxes.add(listOf())
        }
        imagesBoxes[index] = boxes
      }

      // clean
      selectedBox = null
      currentImage.value = null
      boxes.clear()

      // set new index
      index = newIndex

      // show new
      boxes.addAll(imagesBoxes.getOrElse(index) { listOf() })
      currentImage.value = images[index].image
    }
  }

  TopBar(state, "OCR Creator",
    bottomBar = {
      BottomAppBar {
        Row {
          Button(onClick = { setIndex(index - 1) }, enabled = index != 0 && !lockedByTask) { Text("Previous") }
          Button(onClick = { setIndex(index + 1) }, enabled = index != images.size && !lockedByTask) { Text("Next") }
          Button(onClick = { TODO() }, enabled = !lockedByTask) { Text("Done") }
        }
      }
    }
  ) {
    if (index == -1) {
      Text("Click next if you want to continue")
    } else {
      Row(
        modifier = Modifier
      ) {
        Column(modifier = Modifier.fillMaxWidth(0.8f).onSizeChanged { imageSize.value = it }) {
          SimpleLoadedImageDisplayer(currentImage, boxes)
        }
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
          Row(modifier = Modifier.fillMaxWidth()) {
            Button(
              onClick = {
                lockedByTask = true
                coroutineScope.launch(Dispatchers.IO) {
                  val currentOcrBoxes = ProtobufUtils.getBoxedOCR(currentImage.value!!, imageSize)
                  boxes.clear()
                  boxes.addAll(currentOcrBoxes)
                  lockedByTask = false
                }
              },
              enabled = !lockedByTask,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Try OCR")
            }
          }
          boxes.forEachIndexed { index, box ->
            Row(modifier = Modifier.fillMaxWidth().height((imageSize.value.height / 5).dp)) {
              TextField(
                value = box.text,
                modifier = Modifier.fillMaxSize(0.9f).padding(10.dp),
                onValueChange = { boxes[index] = box.copy(text = it) }
              )
            }
          }
        }
      }
    }
  }
}