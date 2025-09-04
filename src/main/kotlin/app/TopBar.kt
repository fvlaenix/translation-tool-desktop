package app

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.Composable
import core.navigation.NavigationController
import core.navigation.NavigationDestination

/**
 * Reusable top app bar composable with navigation, title, and action buttons.
 */
@Composable
fun TopBar(
  navigationController: NavigationController,
  text: String,
  isMainMenu: Boolean = false,
  bottomBar: @Composable () -> Unit = {},
  body: @Composable (PaddingValues) -> Unit
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(text) },
        navigationIcon = if (!isMainMenu) {
          {
            IconButton(onClick = { navigationController.navigateToMainMenu() }) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Menu")
            }
          }
        } else null,
        actions = {
          if (isMainMenu) {
            IconButton(onClick = { navigationController.navigateTo(NavigationDestination.FontSettings) }) {
              Text("F")
            }
            IconButton(onClick = { navigationController.navigateTo(NavigationDestination.Settings) }) {
              Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Settings")
            }
          }
        }
      )
    },
    bottomBar = bottomBar,
    content = body
  )
}