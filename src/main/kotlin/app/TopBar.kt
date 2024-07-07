package app

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState

@Composable
fun TopBar(
  mutableState: MutableState<AppStateEnum>,
  text: String,
  goToMenu: Boolean = true,
  body: @Composable (PaddingValues) -> Unit
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(text) },
        navigationIcon = if (goToMenu) { {
          IconButton(onClick = { mutableState.value = AppStateEnum.MAIN_MENU }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Menu")
          } } } else null
      )
    },
    content = body
  )
}