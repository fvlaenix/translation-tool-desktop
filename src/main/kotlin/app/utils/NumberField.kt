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
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun NumberField(
  name: String?,
  number: MutableState<Int>,
  setter: (Int) -> Unit,
  convertNumber: (Int?) -> Int = { it ?: 0 }
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .height(40.dp)
      .padding(4.dp)
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
      value = number.value.toString(),
      onValueChange = { newValue ->
        val convertedColor = convertNumber(newValue.toIntOrNull())
        number.value = convertedColor
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