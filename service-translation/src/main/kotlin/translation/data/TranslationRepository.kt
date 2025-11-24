package translation.data

/**
 * Repository interface for text translation operations.
 */
interface TranslationRepository {
  /**
   * Translates a single text string.
   */
  suspend fun translateText(text: String): Result<String>

  /**
   * Translates multiple text strings in batch.
   */
  suspend fun translateBatch(texts: List<String>): Result<List<String>>

  /**
   * Translates text with optional context for better accuracy.
   */
  suspend fun translateWithContext(text: String, context: String? = null): Result<String>
}