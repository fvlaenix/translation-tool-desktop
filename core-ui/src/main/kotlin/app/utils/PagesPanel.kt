package app.utils

import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import app.TopBar
import core.navigation.NavigationController

@Composable
fun <T> PagesPanel(
  name: String,
  navigationController: NavigationController,
  dataExtractor: suspend () -> List<T>,
  startWindow: @Composable () -> Unit = { Text("Click next if you want to continue") },
  stepWindow: @Composable (MutableState<Int>, MutableState<T?>) -> Unit,
  finalWindow: @Composable (SnapshotStateList<T>) -> Unit,
) {
  val jobCounter = remember { mutableIntStateOf(0) }

  var index by remember { mutableIntStateOf(-1) }
  var searchIndex by remember { mutableIntStateOf(1) }

  val data = remember { mutableStateListOf<T>() }
  val currentItem = remember { mutableStateOf<T?>(null) }
  val isLoading = remember { mutableStateOf(false) }

  // Load data with LaunchedEffect instead of direct call
  LaunchedEffect(Unit) {
    isLoading.value = true
    try {
      val extractedData = dataExtractor()
      data.clear()
      data.addAll(extractedData)
    } catch (e: Exception) {
      // Handle error appropriately
      println("Error loading data: ${e.message}")
    } finally {
      isLoading.value = false
    }
  }

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

  fun isWorkInProgress(): Boolean = jobCounter.value > 0

  TopBar(
    navigationController, name,
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
    if (isLoading.value) {
      CircularProgressIndicator()
    } else {
      when (index) {
        -1 -> startWindow()
        data.size -> finalWindow(data)
        else -> stepWindow(jobCounter, currentItem)
      }
    }
  }
}