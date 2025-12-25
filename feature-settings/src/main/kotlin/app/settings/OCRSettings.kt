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
import core.navigation.NavigationController
import core.navigation.NavigationDestination
import org.koin.compose.koinInject
import settings.data.OCRMode
import settings.domain.OCRSettingsViewModel

@Composable
fun OCRSettings(navigationController: NavigationController) {
  val viewModel: OCRSettingsViewModel = koinInject()

  val currentSettings by viewModel.currentSettings
  val validationErrors by viewModel.validationErrors
  val isLoading by viewModel.isLoading
  val isSaving by viewModel.isSaving
  val error by viewModel.error
  val saveSuccess by viewModel.saveSuccess
  val isTesting by viewModel.isTesting
  val testResult by viewModel.testResult

  var localMode by remember { mutableStateOf(currentSettings.ocrMode) }
  var localGrpcApiKey by remember { mutableStateOf(currentSettings.ocrGrpc.apiKey) }
  var localDirectCredentialsPath by remember { mutableStateOf(currentSettings.ocrDirect.credentialsPath) }
  var localDirectTimeout by remember { mutableStateOf(currentSettings.ocrDirect.timeoutSeconds.toString()) }

  LaunchedEffect(currentSettings) {
    localMode = currentSettings.ocrMode
    localGrpcApiKey = currentSettings.ocrGrpc.apiKey
    localDirectCredentialsPath = currentSettings.ocrDirect.credentialsPath
    localDirectTimeout = currentSettings.ocrDirect.timeoutSeconds.toString()
  }

  LaunchedEffect(saveSuccess) {
    if (saveSuccess) {
      navigationController.navigateTo(NavigationDestination.MainMenu)
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("OCR Settings") },
        navigationIcon = {
          IconButton(onClick = { navigationController.navigateTo(NavigationDestination.MainMenu) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Menu")
          }
        }
      )
    },
    bottomBar = {
      BottomAppBar {
        Row(modifier = Modifier.padding(8.dp)) {
          Button(
            onClick = { viewModel.saveSettings() },
            enabled = !isLoading && !isSaving && !isTesting,
            modifier = Modifier.padding(end = 8.dp)
          ) {
            if (isSaving) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp))
            } else {
              Text("Save")
            }
          }

          Button(
            onClick = { viewModel.testConnection() },
            enabled = !isLoading && !isSaving && !isTesting,
            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)
          ) {
            if (isTesting) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp))
            } else {
              Text("Test Connection")
            }
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
        TabRow(selectedTabIndex = if (localMode == OCRMode.GRPC) 0 else 1) {
          Tab(
            selected = localMode == OCRMode.GRPC,
            onClick = {
              localMode = OCRMode.GRPC
              viewModel.updateMode(OCRMode.GRPC)
            },
            text = { Text("gRPC") }
          )
          Tab(
            selected = localMode == OCRMode.DIRECT,
            onClick = {
              localMode = OCRMode.DIRECT
              viewModel.updateMode(OCRMode.DIRECT)
            },
            text = { Text("Direct") }
          )
        }

        if (localMode == OCRMode.GRPC) {
          Column(modifier = Modifier.padding(top = 12.dp)) {
            Text(
              text = "gRPC mode connects to an external OCR service.",
              modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(modifier = Modifier.padding(top = 8.dp)) {
              Text("API Key: ")
              TextField(
                singleLine = true,
                value = localGrpcApiKey,
                onValueChange = {
                  localGrpcApiKey = it
                  viewModel.updateGrpcApiKey(it)
                },
                visualTransformation = PasswordVisualTransformation(),
                isError = validationErrors.containsKey("grpcApiKey"),
                placeholder = { Text("Leave empty to use main API key") }
              )
            }
            if (validationErrors.containsKey("grpcApiKey")) {
              Text(
                text = validationErrors["grpcApiKey"] ?: "",
                color = MaterialTheme.colors.error,
                modifier = Modifier.padding(top = 4.dp)
              )
            }
          }
        }

        if (localMode == OCRMode.DIRECT) {
          Column(modifier = Modifier.padding(top = 12.dp)) {
            Text(
              text = "Direct mode uses Google Cloud Vision API directly.",
              modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(modifier = Modifier.padding(top = 8.dp)) {
              Text("Credentials Path: ")
              TextField(
                singleLine = true,
                value = localDirectCredentialsPath,
                onValueChange = {
                  localDirectCredentialsPath = it
                  viewModel.updateDirectCredentialsPath(it)
                },
                isError = validationErrors.containsKey("directCredentialsPath"),
                placeholder = { Text("/path/to/credentials.json") }
              )
            }
            if (validationErrors.containsKey("directCredentialsPath")) {
              Text(
                text = validationErrors["directCredentialsPath"] ?: "",
                color = MaterialTheme.colors.error,
                modifier = Modifier.padding(top = 4.dp)
              )
            }

            Row(modifier = Modifier.padding(top = 8.dp)) {
              Text("Timeout (sec): ")
              TextField(
                singleLine = true,
                value = localDirectTimeout,
                onValueChange = {
                  localDirectTimeout = it
                  val timeout = it.toIntOrNull() ?: 0
                  viewModel.updateDirectTimeoutSeconds(timeout)
                },
                isError = validationErrors.containsKey("directTimeout")
              )
            }
            if (validationErrors.containsKey("directTimeout")) {
              Text(
                text = validationErrors["directTimeout"] ?: "",
                color = MaterialTheme.colors.error,
                modifier = Modifier.padding(top = 4.dp)
              )
            }
          }
        }

        validationErrors.forEach { (key, message) ->
          if (key !in listOf("grpcApiKey", "directCredentialsPath", "directTimeout")) {
            Text(
              text = message,
              color = MaterialTheme.colors.error,
              modifier = Modifier.padding(top = 4.dp)
            )
          }
        }

        error?.let { errorMessage ->
          Text(
            text = errorMessage,
            color = MaterialTheme.colors.error,
            modifier = Modifier.padding(top = 8.dp)
          )
        }

        testResult?.let { result ->
          Text(
            text = result,
            color = if (result.startsWith("Connection test successful")) {
              MaterialTheme.colors.primary
            } else {
              MaterialTheme.colors.error
            },
            modifier = Modifier.padding(top = 8.dp)
          )
        }
      }
    }
  }
}
