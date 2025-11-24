package core.base

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import core.error.ErrorHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.swing.Swing
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Base class for all ViewModels in the application.
 *
 * ViewModels are responsible for:
 * - Managing UI state and business logic
 * - Communicating with repositories to fetch/save data
 * - Handling user interactions and converting them to business operations
 * - Providing reactive state to UI components
 *
 * This base class provides common functionality like:
 * - Coroutine scope management for async operations
 * - Common loading/error state management
 * - Integration with centralized error handling
 * - Automatic cleanup when ViewModel is destroyed
 */
abstract class BaseViewModel : KoinComponent {
  // Coroutine scope tied to ViewModel lifecycle - automatically cancels when ViewModel is destroyed
  private val viewModelJob = SupervisorJob()
  protected val viewModelScope = CoroutineScope(Dispatchers.Swing + viewModelJob)

  // Inject centralized error handler for consistent error display across the app
  private val errorHandler: ErrorHandler by inject()

  // Common UI state that most ViewModels need
  private val _isLoading = mutableStateOf(false)
  val isLoading: State<Boolean> = _isLoading

  private val _error = mutableStateOf<String?>(null)
  val error: State<String?> = _error

  /**
   * Set loading state - typically used when starting/ending async operations
   */
  protected fun setLoading(loading: Boolean) {
    _isLoading.value = loading
  }

  /**
   * Set error state and also display in centralized error handler
   */
  protected fun setError(error: String?) {
    _error.value = error
    if (error != null) {
      errorHandler.showError(error)
    }
  }

  /**
   * Set error with exception details for better debugging
   */
  protected fun setError(error: String, throwable: Throwable) {
    _error.value = error
    errorHandler.showError(error, throwable)
  }

  /**
   * Convenience methods for different types of messages
   */
  protected fun showSuccess(message: String) {
    errorHandler.showSuccess(message)
  }

  protected fun showWarning(message: String) {
    errorHandler.showWarning(message)
  }

  protected fun showInfo(message: String) {
    errorHandler.showInfo(message)
  }

  protected fun clearError() {
    _error.value = null
  }

  /**
   * Called when ViewModel is no longer needed - cleans up coroutines
   */
  open fun onCleared() {
    viewModelScope.cancel()
  }
}