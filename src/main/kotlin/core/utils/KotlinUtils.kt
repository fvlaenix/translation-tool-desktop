package core.utils

/**
 * Kotlin language utilities and extension functions.
 */
object KotlinUtils {
  /**
   * Conditional apply: applies transform only if condition is true.
   */
  fun <T> T.applyIf(condition: Boolean, block: (T) -> T): T =
    if (condition) block(this)
    else this
}