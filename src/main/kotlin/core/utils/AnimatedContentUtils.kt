package core.utils

import androidx.compose.animation.*

object AnimatedContentUtils {
  fun <T : Enum<T>> horizontalSpec(): AnimatedContentTransitionScope<T>.() -> ContentTransform = {
    if (targetState.ordinal > initialState.ordinal) {
      slideInHorizontally { height -> height } togetherWith slideOutHorizontally { height -> -height }
    } else {
      slideInHorizontally { height -> -height } togetherWith slideOutHorizontally { height -> height }
    }.using(
      SizeTransform(clip = false)
    )
  }
}