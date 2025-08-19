package fonts.data

import java.nio.file.Path

interface FontRepository {
  suspend fun loadFonts(): Result<Unit>
  suspend fun addFont(name: String, path: Path): Result<Unit>
  suspend fun getFont(name: String): Result<FontInfo?>
  suspend fun getAllFonts(): Result<List<FontInfo>>
  suspend fun saveFonts(): Result<Unit>
  suspend fun isFontsAdded(): Result<Boolean>
  suspend fun getDefaultFont(): Result<String>
}