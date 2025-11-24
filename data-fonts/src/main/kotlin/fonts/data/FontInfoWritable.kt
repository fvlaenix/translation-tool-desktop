package fonts.data

import kotlinx.serialization.Serializable
import java.awt.Font
import java.nio.file.Path

@Serializable
data class FontInfoWritable(
  val name: String,
  val path: String
) {
  fun withFont(font: Font): FontInfo = FontInfo(name = name, path = Path.of(path), font = font)
}

data class FontInfo(
  val name: String,
  val path: Path,
  val font: Font
) {
  fun toWritable(): FontInfoWritable = FontInfoWritable(name = name, path = path.toString())
}