package translation.data

interface TranslationRepository {
  suspend fun translateText(text: String): Result<String>
  suspend fun translateBatch(texts: List<String>): Result<List<String>>
  suspend fun translateWithContext(text: String, context: String? = null): Result<String>
}