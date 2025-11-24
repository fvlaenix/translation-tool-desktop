package app.common

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun RequirementErrorText(
  message: String,
  modifier: Modifier = Modifier
) {
  Text(
    text = message,
    color = androidx.compose.material.MaterialTheme.colors.error,
    modifier = modifier
  )
}