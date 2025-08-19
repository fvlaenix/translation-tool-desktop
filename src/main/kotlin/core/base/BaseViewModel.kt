package core.base

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.swing.Swing

abstract class BaseViewModel {
  private val viewModelJob = SupervisorJob()
  protected val viewModelScope = CoroutineScope(Dispatchers.Swing + viewModelJob)

  private val _isLoading = mutableStateOf(false)
  val isLoading: State<Boolean> = _isLoading

  private val _error = mutableStateOf<String?>(null)
  val error: State<String?> = _error

  protected fun setLoading(loading: Boolean) {
    _isLoading.value = loading
  }

  protected fun setError(error: String?) {
    _error.value = error
  }

  protected fun clearError() {
    _error.value = null
  }

  open fun onCleared() {
    viewModelScope.cancel()
  }
}