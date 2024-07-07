package app.main

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.AppStateEnum

@Composable
@Preview
fun MainMenu(mutableState: MutableState<AppStateEnum>) {
  Column(
    modifier = Modifier.padding(16.dp),
  ) {
    Text("Simple Translator is designed for simple translations where you just need to paste the text, recognise it, and translate it")
    Button(onClick = { mutableState.value = AppStateEnum.SIMPLE_VERSION }) {
      Text("Simple Translator")
    }
    Button(onClick = { mutableState.value = AppStateEnum.ADVANCED_VERSION }) {
      Text("Advanced Translator")
    }
  }
}