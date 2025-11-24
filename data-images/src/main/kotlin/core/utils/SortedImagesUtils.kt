package core.utils

import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

/**
 * Utility for sorting image paths by numeric names.
 */
object SortedImagesUtils {
  /**
   * Sorts image paths by their numeric filename without extension.
   */
  fun Collection<Path>.sortedByName(): List<Path> {
    check(all { it.nameWithoutExtension.toIntOrNull() != null }) { "Should be integer in name: ${this.joinToString { it.nameWithoutExtension }}" }
    return this.sortedBy { it.nameWithoutExtension.toInt() }
  }
}