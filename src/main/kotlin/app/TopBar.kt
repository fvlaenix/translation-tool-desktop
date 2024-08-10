package app

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState

@Composable
fun TopBar(
  mutableState: MutableState<AppStateEnum>,
  text: String,
  isMainMenu: Boolean = false,
  bottomBar: @Composable () -> Unit = {},
  body: @Composable (PaddingValues) -> Unit
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(text) },
        navigationIcon = if (!isMainMenu) { {
          IconButton(onClick = { mutableState.value = AppStateEnum.MAIN_MENU }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Menu")
          } } } else null,
        actions = { if (isMainMenu) {
          IconButton(onClick = { mutableState.value = AppStateEnum.FONT_SETTINGS }) {
            Text("F")
          }
          IconButton(onClick = { mutableState.value = AppStateEnum.SETTINGS }) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Settings")
          } }
        }
      )
    },
    bottomBar = bottomBar,
    content = body
  )
}