package core.base

/**
 * Sealed class representing different UI states for async operations.
 */
sealed class UiState<out T> {
  /** Initial idle state before any operation */
  object Idle : UiState<Nothing>()

  /** Loading state during async operation */
  object Loading : UiState<Nothing>()

  /** Success state with result data */
  data class Success<T>(val data: T) : UiState<T>()

  /** Error state with message and optional throwable */
  data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>()
}