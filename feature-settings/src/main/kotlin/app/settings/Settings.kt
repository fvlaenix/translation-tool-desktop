package app.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import core.navigation.NavigationController
import core.navigation.NavigationDestination
import org.koin.compose.koinInject
import settings.domain.SettingsViewModel

@Composable
fun Settings(navigationController: NavigationController) {
  val viewModel: SettingsViewModel = koinInject()

  val currentSettings by viewModel.currentSettings
  val validationErrors by viewModel.validationErrors
  val isLoading by viewModel.isLoading
  val isSaving by viewModel.isSaving
  val error by viewModel.error
  val saveSuccess by viewModel.saveSuccess

  var localHostname by remember { mutableStateOf(currentSettings.proxyServiceHostname) }
  var localPort by remember { mutableStateOf(currentSettings.proxyServicePort.toString()) }
  var localApiKey by remember { mutableStateOf(currentSettings.apiKey) }

  // Update local state when settings change
  LaunchedEffect(currentSettings) {
    localHostname = currentSettings.proxyServiceHostname
    localPort = currentSettings.proxyServicePort.toString()
    localApiKey = currentSettings.apiKey
  }

  // Navigate after successful save
  LaunchedEffect(saveSuccess) {
    if (saveSuccess) {
      navigationController.navigateTo(NavigationDestination.MainMenu)
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Settings") },
        navigationIcon = {
          IconButton(onClick = { navigationController.navigateTo(NavigationDestination.MainMenu) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Menu")
          }
        }
      )
    },
    bottomBar = {
      BottomAppBar {
        Button(
          onClick = {
            viewModel.saveSettings()
          },
          enabled = !isLoading && !isSaving
        ) {
          if (isSaving) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp))
          } else {
            Text("Save")
          }
        }
      }
    }
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      if (isLoading) {
        CircularProgressIndicator()
        Text("Loading settings...")
      } else {
        Row(modifier = Modifier.padding(top = 8.dp)) {
          Text("Hostname: ")
          TextField(
            singleLine = true,
            value = localHostname,
            onValueChange = {
              localHostname = it
              viewModel.updateHostname(it)
            },
            isError = validationErrors.containsKey("hostname")
          )
          Text("Port: ")
          TextField(
            singleLine = true,
            value = localPort,
            onValueChange = {
              localPort = it
              val portInt = it.toIntOrNull() ?: 443
              viewModel.updatePort(portInt)
            },
            isError = validationErrors.containsKey("port")
          )
        }

        Row(modifier = Modifier.padding(top = 8.dp)) {
          Text("API key: ")
          TextField(
            singleLine = true,
            value = localApiKey,
            onValueChange = {
              localApiKey = it
              viewModel.updateApiKey(it)
            },
            isError = validationErrors.containsKey("apiKey")
          )
        }

        // Show validation errors
        validationErrors.forEach { (field, message) ->
          Text(
            text = message,
            color = MaterialTheme.colors.error,
            modifier = Modifier.padding(top = 4.dp)
          )
        }

        // Show general errors
        error?.let { errorMessage ->
          Text(
            text = errorMessage,
            color = MaterialTheme.colors.error,
            modifier = Modifier.padding(top = 8.dp)
          )
        }
      }
    }
  }
}