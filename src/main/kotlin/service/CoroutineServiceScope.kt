package service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext

object CoroutineServiceScope {
  @OptIn(DelicateCoroutinesApi::class)
  val context = newFixedThreadPoolContext(
    4,
    "CoroutineServiceScope"
  )

  val scope = CoroutineScope(context)
}