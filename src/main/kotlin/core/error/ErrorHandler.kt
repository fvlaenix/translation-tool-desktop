package core.error

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Centralized error handling with modern UI notifications and console logging
 */
class ErrorHandler {

  private val _currentError = mutableStateOf<ErrorMessage?>(null)
  val currentError: State<ErrorMessage?> = _currentError

  private val _errorHistory = mutableStateOf<List<ErrorMessage>>(emptyList())
  val errorHistory: State<List<ErrorMessage>> = _errorHistory

  private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  /**
   * Show an error with automatic dismissal
   */
  fun showError(
    message: String,
    throwable: Throwable? = null,
    type: ErrorType = ErrorType.ERROR,
    duration: Long = 4000L
  ) {
    val errorMessage = ErrorMessage(
      message = message,
      throwable = throwable,
      type = type,
      timestamp = LocalDateTime.now()
    )

    // Log to console
    logToConsole(errorMessage)

    // Add to history
    addToHistory(errorMessage)

    // Show in UI
    _currentError.value = errorMessage

    // Auto-dismiss after duration
    scope.launch {
      delay(duration)
      if (_currentError.value == errorMessage) {
        _currentError.value = null
      }
    }
  }

  /**
   * Show a success message
   */
  fun showSuccess(message: String, duration: Long = 3000L) {
    showError(message, type = ErrorType.SUCCESS, duration = duration)
  }

  /**
   * Show a warning message
   */
  fun showWarning(message: String, duration: Long = 4000L) {
    showError(message, type = ErrorType.WARNING, duration = duration)
  }

  /**
   * Show an info message
   */
  fun showInfo(message: String, duration: Long = 3000L) {
    showError(message, type = ErrorType.INFO, duration = duration)
  }

  /**
   * Dismiss current error manually
   */
  fun dismissError() {
    _currentError.value = null
  }

  /**
   * Clear error history
   */
  fun clearHistory() {
    _errorHistory.value = emptyList()
  }

  private fun logToConsole(errorMessage: ErrorMessage) {
    val timestamp = errorMessage.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    val prefix = when (errorMessage.type) {
      ErrorType.ERROR -> "ERROR"
      ErrorType.WARNING -> "WARN"
      ErrorType.INFO -> "INFO"
      ErrorType.SUCCESS -> "SUCCESS"
    }

    println("[$timestamp] $prefix: ${errorMessage.message}")

    errorMessage.throwable?.let { throwable ->
      println("Exception details:")
      throwable.printStackTrace()
    }
  }

  private fun addToHistory(errorMessage: ErrorMessage) {
    val currentHistory = _errorHistory.value.toMutableList()
    currentHistory.add(0, errorMessage) // Add to beginning

    // Keep only last 50 errors
    if (currentHistory.size > 50) {
      currentHistory.removeAt(currentHistory.size - 1)
    }

    _errorHistory.value = currentHistory
  }

  fun cleanup() {
    scope.cancel()
  }
}

/**
 * Error message data class
 */
data class ErrorMessage(
  val message: String,
  val throwable: Throwable? = null,
  val type: ErrorType = ErrorType.ERROR,
  val timestamp: LocalDateTime
)

/**
 * Types of error messages
 */
enum class ErrorType {
  ERROR,
  WARNING,
  INFO,
  SUCCESS
}