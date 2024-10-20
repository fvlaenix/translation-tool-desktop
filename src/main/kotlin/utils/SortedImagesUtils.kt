package utils

import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

object SortedImagesUtils {
  fun Collection<Path>.sortedByName(): List<Path> {
    check(all { it.nameWithoutExtension.toIntOrNull() != null }) { "Should be integer in name: ${this.joinToString { it.nameWithoutExtension }}" }
    return this.sortedBy { it.nameWithoutExtension.toInt() }
  }
}