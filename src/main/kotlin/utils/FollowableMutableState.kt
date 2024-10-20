package utils

import androidx.compose.runtime.MutableState

class FollowableMutableState<T>(private val delegated: MutableState<T>) : MutableState<T> {
  private val followers = mutableListOf<(T, T) -> Unit>()
  override var value: T
    get() = delegated.value
    set(value) {
      val before = delegated.value
      delegated.value = value
      followers.forEach { it(before, value) }
    }

  override fun component1(): T = value

  override fun component2(): (T) -> Unit {
    return { value = it }
  }

  fun follow(block: (T, T) -> Unit) {
    followers.add(block)
  }
}