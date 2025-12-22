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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.fvlaenix.text.OpenAIModelProvider
import com.fvlaenix.text.OpenRouterModelProvider
import core.navigation.NavigationController
import core.navigation.NavigationDestination
import org.koin.compose.koinInject
import settings.data.TranslationMode
import settings.data.TranslationModelProvider
import settings.domain.TranslationSettingsViewModel

@Composable
fun TranslationSettings(navigationController: NavigationController) {
  val viewModel: TranslationSettingsViewModel = koinInject()

  val currentSettings by viewModel.currentSettings
  val validationErrors by viewModel.validationErrors
  val isLoading by viewModel.isLoading
  val isSaving by viewModel.isSaving
  val error by viewModel.error
  val saveSuccess by viewModel.saveSuccess

  var localMode by remember { mutableStateOf(currentSettings.translationMode) }
  var localApiKey by remember { mutableStateOf(currentSettings.translationDirect.apiKey) }
  var localProvider by remember { mutableStateOf(currentSettings.translationDirect.provider) }
  var localModelName by remember { mutableStateOf(currentSettings.translationDirect.modelName) }
  var localApiBaseUrl by remember { mutableStateOf(currentSettings.translationDirect.apiBaseUrl) }
  var localTimeout by remember { mutableStateOf(currentSettings.translationDirect.timeoutSeconds.toString()) }
  var modelMenuExpanded by remember { mutableStateOf(false) }

  LaunchedEffect(currentSettings) {
    localMode = currentSettings.translationMode
    localApiKey = currentSettings.translationDirect.apiKey
    localProvider = currentSettings.translationDirect.provider
    localModelName = currentSettings.translationDirect.modelName
    localApiBaseUrl = currentSettings.translationDirect.apiBaseUrl
    localTimeout = currentSettings.translationDirect.timeoutSeconds.toString()
  }

  LaunchedEffect(saveSuccess) {
    if (saveSuccess) {
      navigationController.navigateTo(NavigationDestination.MainMenu)
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Translation Settings") },
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
          onClick = { viewModel.saveSettings() },
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
        TabRow(selectedTabIndex = if (localMode == TranslationMode.GRPC) 0 else 1) {
          Tab(
            selected = localMode == TranslationMode.GRPC,
            onClick = {
              localMode = TranslationMode.GRPC
              viewModel.updateMode(TranslationMode.GRPC)
            },
            text = { Text("gRPC") }
          )
          Tab(
            selected = localMode == TranslationMode.DIRECT,
            onClick = {
              localMode = TranslationMode.DIRECT
              viewModel.updateMode(TranslationMode.DIRECT)
            },
            text = { Text("Direct") }
          )
        }

        if (localMode == TranslationMode.GRPC) {
          Text(
            text = "Legacy gRPC mode. No settings are required here.",
            modifier = Modifier.padding(top = 12.dp)
          )
        }

        if (localMode == TranslationMode.DIRECT) {
          TabRow(
            selectedTabIndex = when (localProvider) {
              TranslationModelProvider.OPENAI -> 0
              TranslationModelProvider.OPENROUTER -> 1
              TranslationModelProvider.OTHER -> 2
            },
            modifier = Modifier.padding(top = 12.dp)
          ) {
            Tab(
              selected = localProvider == TranslationModelProvider.OPENAI,
              onClick = {
                localProvider = TranslationModelProvider.OPENAI
                viewModel.updateDirectProvider(TranslationModelProvider.OPENAI)
              },
              text = { Text("OpenAI") }
            )
            Tab(
              selected = localProvider == TranslationModelProvider.OPENROUTER,
              onClick = {
                localProvider = TranslationModelProvider.OPENROUTER
                viewModel.updateDirectProvider(TranslationModelProvider.OPENROUTER)
              },
              text = { Text("OpenRouter") }
            )
            Tab(
              selected = localProvider == TranslationModelProvider.OTHER,
              onClick = {
                localProvider = TranslationModelProvider.OTHER
                viewModel.updateDirectProvider(TranslationModelProvider.OTHER)
              },
              text = { Text("Other") }
            )
          }

          Row(modifier = Modifier.padding(top = 12.dp)) {
            Text("Model: ")
            if (localProvider == TranslationModelProvider.OTHER) {
              TextField(
                singleLine = true,
                value = localModelName,
                onValueChange = {
                  localModelName = it
                  viewModel.updateDirectModelName(it)
                },
                isError = validationErrors.containsKey("directModelName")
              )
            } else {
              Column {
                TextField(
                  singleLine = true,
                  value = localModelName,
                  onValueChange = { },
                  readOnly = true,
                  modifier = Modifier
                    .padding(end = 8.dp),
                  isError = validationErrors.containsKey("directModelName"),
                  trailingIcon = {
                    IconButton(onClick = { modelMenuExpanded = true }) {
                      Text("▼")
                    }
                  }
                )
                DropdownMenu(
                  expanded = modelMenuExpanded,
                  onDismissRequest = { modelMenuExpanded = false }
                ) {
                  val models = when (localProvider) {
                    TranslationModelProvider.OPENAI -> OpenAIModelProvider.models.keys.toList()
                    TranslationModelProvider.OPENROUTER -> OpenRouterModelProvider.models.keys.toList()
                    TranslationModelProvider.OTHER -> emptyList()
                  }
                  models.forEach { model ->
                    DropdownMenuItem(
                      onClick = {
                        localModelName = model
                        modelMenuExpanded = false
                        viewModel.updateDirectModelName(model)
                      }
                    ) {
                      Text(model)
                    }
                  }
                }
              }
            }
          }

          Row(modifier = Modifier.padding(top = 8.dp)) {
            Text("API key: ")
            TextField(
              singleLine = true,
              value = localApiKey,
              onValueChange = {
                localApiKey = it
                viewModel.updateDirectApiKey(it)
              },
              visualTransformation = PasswordVisualTransformation(),
              isError = validationErrors.containsKey("directApiKey")
            )
          }

          Row(modifier = Modifier.padding(top = 8.dp)) {
            Text("API base URL: ")
            TextField(
              singleLine = true,
              value = localApiBaseUrl,
              onValueChange = {
                localApiBaseUrl = it
                viewModel.updateDirectApiBaseUrl(it)
              },
              isError = validationErrors.containsKey("directApiBaseUrl")
            )
          }

          Row(modifier = Modifier.padding(top = 8.dp)) {
            Text("Timeout (sec): ")
            TextField(
              singleLine = true,
              value = localTimeout,
              onValueChange = {
                localTimeout = it
                val timeout = it.toIntOrNull() ?: 0
                viewModel.updateDirectTimeoutSeconds(timeout)
              },
              isError = validationErrors.containsKey("directTimeout")
            )
          }
        }

        validationErrors.forEach { (_, message) ->
          Text(
            text = message,
            color = MaterialTheme.colors.error,
            modifier = Modifier.padding(top = 4.dp)
          )
        }

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
