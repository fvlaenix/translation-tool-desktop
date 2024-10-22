package app.block

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.utils.NumberField
import app.utils.SearchableExpandedDropDownMenu
import bean.BeanColor
import bean.BlockSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import utils.FontService
import utils.Text2ImageUtils
import java.awt.image.BufferedImage
import kotlin.math.max
import kotlin.math.min

@Composable
fun BlockSettingsPanelWithPreview(settings: MutableState<BlockSettings>, imageState: MutableState<BufferedImage?>) {
  val coroutineScope = rememberCoroutineScope()

  Row {
    Column(modifier = Modifier.fillMaxWidth(0.5f)) {
      SimpleLoadedImageDisplayer<Unit>(image = imageState)
    }
    Column(modifier = Modifier.fillMaxWidth()) {
      BlockSettingsPanel(settings)
    }
    LaunchedEffect(settings.value) {
      imageState.value = null
      coroutineScope.launch(Dispatchers.IO) {
        val image = Text2ImageUtils.createSample(500, 500, settings.value)
        imageState.value = image
      }
    }
  }
}

@Composable
fun BlockSettingsPanel(settings: MutableState<BlockSettings>) {
  Column {
    FontBlockSettingsPanel(settings)
    FontSizePanel(settings)
    FontColor(settings)
    OutlineColor(settings)
    OutlineSize(settings)
    BackgroundColor(settings)
    BorderSize(settings)
  }
}

@Composable
private fun FontBlockSettingsPanel(settings: MutableState<BlockSettings>) {
  val fontService = FontService.getInstance()
  val fonts = fontService.getMutableState()

  val expanded = remember { mutableStateOf(false) }
  val scrollState = rememberScrollState()

  fun changeFont(fontName: String) {
    settings.value = settings.value.copy(fontName = fontName)
  }

  Row {
    Text("Font: ")

    SearchableExpandedDropDownMenu(
      listOfItems = fonts,
      dropdownItem = { name -> Text(name.name) },
      textFromItem = { font -> font.name },
      defaultItem = { },
      onSearchTextFieldClicked = { },
      onDropDownItemSelected = { changeFont(it.name) }
    )

    LaunchedEffect(expanded.value) {
      if (expanded.value) {
        scrollState.scrollTo(scrollState.maxValue)
      }
    }
  }

}

@Composable
private fun FontSizePanel(settings: MutableState<BlockSettings>) {
  Row {
    SizeOfSomething(
      name = "Font Size",
      defaultValue = 12,
      getter = { settings.value.fontSize },
      setter = { settings.value = settings.value.copy(fontSize = it) }
    )
  }
}

@Composable
private fun FontColor(settings: MutableState<BlockSettings>) {
  Row {
    Text("Font Color: ")
    BeanColor(
      getter = { settings.value.fontColor },
      setter = { settings.value = settings.value.copy(fontColor = it) })
  }
}

@Composable
private fun OutlineColor(settings: MutableState<BlockSettings>) {
  Row {
    Text("Outline Color: ")
    BeanColor(
      getter = { settings.value.outlineColor },
      setter = { settings.value = settings.value.copy(outlineColor = it) }
    )
  }
}

@Composable
private fun OutlineSize(settings: MutableState<BlockSettings>) {
  // TODO make double
  Row {
    SizeOfSomething(
      name = "Outline Size",
      defaultValue = 5,
      getter = { settings.value.outlineSize.toInt() },
      setter = { settings.value = settings.value.copy(outlineSize = it.toDouble()) }
    )
  }
}

@Composable
private fun BackgroundColor(settings: MutableState<BlockSettings>) {
  Row {
    Text("Background Color: ")
    BeanColor(
      getter = { settings.value.backgroundColor },
      setter = { settings.value = settings.value.copy(backgroundColor = it) }
    )
  }
}

@Composable
private fun BorderSize(settings: MutableState<BlockSettings>) {
  Row {
    SizeOfSomething(
      name = "Border Size",
      defaultValue = 3,
      getter = { settings.value.border },
      setter = { settings.value = settings.value.copy(border = it) }
    )
  }
}

@Composable
private fun SizeOfSomething(name: String?, defaultValue: Int, getter: () -> Int, setter: (Int) -> Unit) {
  val size = remember { mutableStateOf(getter()) }

  NumberField(
    name = name,
    number = size,
    setter = setter,
    convertNumber = { it ?: defaultValue }
  )
}

@Composable
private fun BeanColor(getter: () -> BeanColor, setter: (BeanColor) -> Unit) {
  val r = remember { mutableStateOf(getter().r) }
  val g = remember { mutableStateOf(getter().g) }
  val b = remember { mutableStateOf(getter().b) }
  val a = remember { mutableStateOf(getter().a) }

  Row(modifier = Modifier.fillMaxWidth()) {
    ColorComponentController("Red", r) { setter.invoke(BeanColor(r.value, g.value, b.value, a.value)) }
    ColorComponentController("Green", g) { setter.invoke(BeanColor(r.value, g.value, b.value, a.value)) }
    ColorComponentController("Blue", b) { setter.invoke(BeanColor(r.value, g.value, b.value, a.value)) }
    ColorComponentController("Alpha", a) { setter.invoke(BeanColor(r.value, g.value, b.value, a.value)) }
    Box(
      modifier = Modifier
        .size(50.dp)
        .background(Color(r.value, g.value, b.value, a.value))
    )
  }
}

@Composable
private fun ColorComponentController(name: String, color: MutableState<Int>, setter: (Int) -> Unit) {
  fun convertColor(it: Int?): Int {
    return max(0, min(255, it ?: 0))
  }
  NumberField(
    name = name,
    number = color,
    setter = setter,
    convertNumber = { convertColor(it) }
  )
}