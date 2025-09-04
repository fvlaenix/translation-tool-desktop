package core.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.CoroutineContext

/**
 * Coroutine scope that cancels previous jobs when launching new ones.
 */
class PreemptiveCoroutineScope(private val coroutineScope: CoroutineScope) {
  private var currentJob: Job? = null
  private val lock = ReentrantLock()

  /**
   * Launches a new coroutine, canceling the previous one if it exists.
   */
  fun launch(coroutineContext: CoroutineContext, block: suspend CoroutineScope.() -> Unit) {
    lock.withLock {
      currentJob?.cancel()
      currentJob = coroutineScope.launch(coroutineContext) {
        block()
      }
    }
  }
}