package app.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.math.sign

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NumberField(
  name: String?,
  number: Int,
  setter: (Int) -> Unit,
  convertNumber: (Int?) -> Int = { it ?: 0 }
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .height(40.dp)
      .padding(4.dp)
      .onPointerEvent(PointerEventType.Scroll) { event ->
        val delta = event.changes.first().scrollDelta.y.roundToInt().sign
        if (delta != 0) {
          event.changes.first().consume()
          setter(number - delta)
        }
      }
  ) {
    if (name != null) {
      Text(
        "$name: ",
        modifier = Modifier
          .background(Color.LightGray)
          .padding(4.dp)
          .border(1.dp, Color.Gray)
      )
    }

    BasicTextField(
      value = number.toString(),
      onValueChange = { newValue ->
        val convertedColor = convertNumber(newValue.toIntOrNull())
        setter(convertedColor)
      },
      keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
      singleLine = true,
      modifier = Modifier
        .width(50.dp)
        .background(Color.LightGray)
        .padding(4.dp)
        .border(1.dp, Color.Gray)
        .height(40.dp)
    )
  }
}