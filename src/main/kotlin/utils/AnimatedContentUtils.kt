package utils

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

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