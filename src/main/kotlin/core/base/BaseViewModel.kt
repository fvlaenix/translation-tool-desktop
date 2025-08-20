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

abstract class BaseViewModel : KoinComponent {
  private val viewModelJob = SupervisorJob()
  protected val viewModelScope = CoroutineScope(Dispatchers.Swing + viewModelJob)

  // Inject ErrorHandler for centralized error handling
  private val errorHandler: ErrorHandler by inject()

  private val _isLoading = mutableStateOf(false)
  val isLoading: State<Boolean> = _isLoading

  private val _error = mutableStateOf<String?>(null)
  val error: State<String?> = _error

  protected fun setLoading(loading: Boolean) {
    _isLoading.value = loading
  }

  protected fun setError(error: String?) {
    _error.value = error
    if (error != null) {
      // Also show in centralized error handler
      errorHandler.showError(error)
    }
  }

  protected fun setError(error: String, throwable: Throwable) {
    _error.value = error
    errorHandler.showError(error, throwable)
  }

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

  open fun onCleared() {
    viewModelScope.cancel()
  }
}