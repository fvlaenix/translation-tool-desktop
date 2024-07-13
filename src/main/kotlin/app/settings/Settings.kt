package app.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.AppStateEnum

@Composable
fun Settings(mutableState: MutableState<AppStateEnum>) {
  var ocrServiceHostname by remember { mutableStateOf(SettingsState.DEFAULT.proxyServiceHostname)}
  var port by remember { mutableStateOf(SettingsState.DEFAULT.proxyServicePort) }
  var apiKey by remember { mutableStateOf(SettingsState.DEFAULT.apiKey) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Settings") },
        navigationIcon = {
          IconButton(onClick = { mutableState.value = AppStateEnum.MAIN_MENU }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Menu")
          } }
      )
    },
    bottomBar = {
      BottomAppBar {
        Button(onClick = {
          SettingsState.save(ocrServiceHostname, port, apiKey)
          mutableState.value = AppStateEnum.MAIN_MENU
        }) {
          Text("Save")
        }
      }
    }
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      Row(modifier = Modifier.padding(top = 8.dp)) {
        Text("Hostname: ")
        TextField(
          singleLine = true,
          value = ocrServiceHostname,
          onValueChange = { ocrServiceHostname = it }
        )
        Text("Port: ")
        TextField(
          singleLine = true,
          value = port.toString(),
          onValueChange = { port = it.toIntOrNull() ?: 443 }
        )
      }
      Row(modifier = Modifier.padding(top = 8.dp)) {
        Text("API key: ")
        TextField(
          singleLine = true,
          value = apiKey,
          onValueChange = { apiKey = it}
        )
      }
    }
  }
}