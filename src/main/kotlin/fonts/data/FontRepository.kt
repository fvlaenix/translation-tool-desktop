package fonts.data

import java.nio.file.Path

/**
 * Repository interface for managing font data including loading, saving and retrieval.
 */
interface FontRepository {
  /**
   * Loads fonts from storage.
   */
  suspend fun loadFonts(): Result<Unit>

  /**
   * Adds new font with name and path.
   */
  suspend fun addFont(name: String, path: Path): Result<Unit>

  /**
   * Retrieves font info by name.
   */
  suspend fun getFont(name: String): Result<FontInfo?>

  /**
   * Gets all available fonts list.
   */
  suspend fun getAllFonts(): Result<List<FontInfo>>

  /**
   * Saves fonts to storage.
   */
  suspend fun saveFonts(): Result<Unit>

  /**
   * Checks if fonts are available.
   */
  suspend fun isFontsAdded(): Result<Boolean>

  /**
   * Gets default font name.
   */
  suspend fun getDefaultFont(): Result<String>
}