package app.utils

import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import app.AppStateEnum
import app.TopBar
import java.util.concurrent.atomic.AtomicInteger

@Composable
fun <T> PagesPanel(
  name: String,
  state: MutableState<AppStateEnum>,
  dataExtractor: () -> List<T>,
  startWindow: @Composable () -> Unit = { Text("Click next if you want to continue") },
  stepWindow: @Composable (AtomicInteger, MutableState<T?>) -> Unit,
  finalWindow: @Composable (SnapshotStateList<T>) -> Unit,
) {
  val jobCounter = remember { AtomicInteger(0) }

  var index by remember { mutableIntStateOf(-1) }
  var searchIndex by remember { mutableIntStateOf(1) }

  val data = remember { mutableStateListOf<T>().apply { addAll(dataExtractor()) } }

  val currentItem = remember { mutableStateOf<T?>(null) }

  fun setIndex(newIndex: Int) {
    // save result
    if (index in data.indices) {
      data[index] = currentItem.value!!
    }

    // clean
    currentItem.value = null

    // set new index
    index = newIndex

    // show new
    if (index !in data.indices) return
    currentItem.value = data[index]
  }

  fun isWorkInProgress(): Boolean = jobCounter.get() > 0

  TopBar(state, name,
    bottomBar = {
      Row {
        Button(onClick = { setIndex(index - 1) }, enabled = index > 0 && !isWorkInProgress()) { Text("Previous") }
        Button(
          onClick = { setIndex(index + 1) },
          enabled = index + 1 < data.size && !isWorkInProgress()
        ) { Text("Next") }
        Button(onClick = { setIndex(data.size) }, enabled = index != data.size && !isWorkInProgress()) { Text("Done") }
        NumberField("Page Number", searchIndex, { searchIndex = it })
        Button(
          onClick = { setIndex(searchIndex) },
          enabled = searchIndex in data.indices && !isWorkInProgress()
        ) { Text("Go") }
      }
    }
  ) {
    when (index) {
      -1 -> startWindow()
      data.size -> finalWindow(data)
      else -> stepWindow(jobCounter, currentItem)
    }
  }
}