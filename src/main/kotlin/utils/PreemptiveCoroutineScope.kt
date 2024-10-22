package utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.CoroutineContext

class PreemptiveCoroutineScope(private val coroutineScope: CoroutineScope) {
  private var currentJob: Job? = null
  private val lock = ReentrantLock()

  fun launch(coroutineContext: CoroutineContext, block: suspend CoroutineScope.() -> Unit) {
    lock.withLock {
      currentJob?.cancel()
      currentJob = coroutineScope.launch(coroutineContext) {
        block()
      }
    }
  }
}