package core.error

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Modern Snackbar-style error display component
 */
@Composable
fun ErrorDisplay(
  errorHandler: ErrorHandler,
  modifier: Modifier = Modifier
) {
  val currentError by errorHandler.currentError

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.BottomCenter
  ) {
    AnimatedVisibility(
      visible = currentError != null,
      enter = slideInVertically(
        initialOffsetY = { it },
        animationSpec = tween(300)
      ) + fadeIn(animationSpec = tween(300)),
      exit = slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(300)
      ) + fadeOut(animationSpec = tween(300))
    ) {
      currentError?.let { error ->
        ErrorSnackbar(
          errorMessage = error,
          onDismiss = { errorHandler.dismissError() }
        )
      }
    }
  }
}

@Composable
private fun ErrorSnackbar(
  errorMessage: ErrorMessage,
  onDismiss: () -> Unit
) {
  val (backgroundColor, contentColor, icon) = when (errorMessage.type) {
    ErrorType.ERROR -> Triple(
      Color(0xFFD32F2F),
      Color.White,
      Icons.Default.Warning // TODO Replace with Error
    )

    ErrorType.WARNING -> Triple(
      Color(0xFFFF9800),
      Color.White,
      Icons.Default.Warning
    )

    ErrorType.INFO -> Triple(
      Color(0xFF1976D2),
      Color.White,
      Icons.Default.Info
    )

    ErrorType.SUCCESS -> Triple(
      Color(0xFF388E3C),
      Color.White,
      Icons.Default.CheckCircle
    )
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
      .shadow(8.dp, RoundedCornerShape(8.dp)),
    shape = RoundedCornerShape(8.dp),
    backgroundColor = backgroundColor,
    elevation = 0.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = contentColor,
        modifier = Modifier.size(24.dp)
      )

      Text(
        text = errorMessage.message,
        color = contentColor,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.weight(1f)
      )

      Icon(
        imageVector = Icons.Default.Close,
        contentDescription = "Dismiss",
        tint = contentColor.copy(alpha = 0.7f),
        modifier = Modifier
          .size(20.dp)
          .clickable { onDismiss() }
      )
    }
  }
}

/**
 * Overlay component to be used at the top level of the app
 */
@Composable
fun ErrorOverlay(
  errorHandler: ErrorHandler,
  content: @Composable () -> Unit
) {
  Box(modifier = Modifier.fillMaxSize()) {
    content()

    ErrorDisplay(
      errorHandler = errorHandler,
      modifier = Modifier.fillMaxSize()
    )
  }
}