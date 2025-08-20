package app.block

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.utils.ChipSelector
import app.utils.NumberField
import app.utils.SearchableExpandedDropDownMenu
import bean.Alignment
import bean.BeanColor
import core.utils.Text2ImageUtils
import fonts.domain.FontResolver
import fonts.domain.FontViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import translation.data.BlockSettings
import java.awt.image.BufferedImage
import kotlin.math.max
import kotlin.math.min

@Composable
fun BlockSettingsPanelWithPreview(settings: MutableState<BlockSettings>, imageState: MutableState<BufferedImage?>) {
  val coroutineScope = rememberCoroutineScope()
  val fontResolver: FontResolver = koinInject()

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
        // Resolve font before creating sample
        val resolvedSettings = fontResolver.resolveFont(settings.value)
        val image = Text2ImageUtils.createSample(500, 500, resolvedSettings)
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
    Alignment(settings)
  }
}

@Composable
private fun FontBlockSettingsPanel(settings: MutableState<BlockSettings>) {
  val fontViewModel: FontViewModel = koinInject()
  val availableFonts by fontViewModel.availableFonts
  val isLoading by fontViewModel.isLoading

  // Load fonts when this composable is first displayed
  LaunchedEffect(Unit) {
    fontViewModel.loadFonts()
  }

  val expanded = remember { mutableStateOf(false) }
  val scrollState = rememberScrollState()

  fun changeFont(fontName: String) {
    settings.value = settings.value.copy(fontName = fontName)
  }

  Row {
    Text("Font: ")

    if (isLoading) {
      CircularProgressIndicator(modifier = Modifier.size(24.dp))
    } else {
      SearchableExpandedDropDownMenu(
        listOfItems = availableFonts,
        dropdownItem = { fontInfo -> Text(fontInfo.name) },
        textFromItem = { fontInfo -> fontInfo.name },
        defaultItem = { },
        onSearchTextFieldClicked = { },
        onDropDownItemSelected = { changeFont(it.name) }
      )
    }

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
private fun Alignment(settings: MutableState<BlockSettings>) {
  Row {
    val chipState =
      ChipSelector.rememberChipSelectorState(Alignment.entries.map { it.name }, listOf(settings.value.alignment.name)) {
        settings.value = settings.value.copy(alignment = Alignment.valueOf(it))
      }
    ChipSelector.ChipsSelector(chipState, modifier = Modifier.fillMaxWidth())
  }
}

@Composable
private fun SizeOfSomething(name: String?, defaultValue: Int, getter: () -> Int, setter: (Int) -> Unit) {
  val size = getter()

  NumberField(
    name = name,
    number = size,
    setter = setter,
    convertNumber = { it ?: defaultValue }
  )
}

@Composable
private fun BeanColor(getter: () -> BeanColor, setter: (BeanColor) -> Unit) {
  val r = getter().r
  val g = getter().g
  val b = getter().b
  val a = getter().a

  Row(modifier = Modifier.fillMaxWidth()) {
    ColorComponentController("Red", r) { setter.invoke(BeanColor(it, g, b, a)) }
    ColorComponentController("Green", g) { setter.invoke(BeanColor(r, it, b, a)) }
    ColorComponentController("Blue", b) { setter.invoke(BeanColor(r, g, it, a)) }
    ColorComponentController("Alpha", a) { setter.invoke(BeanColor(r, g, b, it)) }
    Box(
      modifier = Modifier
        .size(50.dp)
        .background(Color(r, g, b, a))
    )
  }
}

@Composable
private fun ColorComponentController(name: String, color: Int, setter: (Int) -> Unit) {
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