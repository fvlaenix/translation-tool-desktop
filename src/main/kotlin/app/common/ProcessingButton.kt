package app.common

import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProcessingButton(
  text: String,
  isProcessing: Boolean,
  enabled: Boolean = true,
  onClick: () -> Unit
) {
  Button(
    onClick = onClick,
    enabled = enabled && !isProcessing
  ) {
    if (isProcessing) {
      CircularProgressIndicator(modifier = Modifier.size(16.dp))
    } else {
      Text(text)
    }
  }
}