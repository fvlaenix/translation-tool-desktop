package translation.data

class DelegatingTranslationRepository(
  private val provider: TranslationServiceProvider
) : TranslationRepository {

  override suspend fun translateText(text: String): Result<String> {
    return provider.get().translateText(text)
  }

  override suspend fun translateBatch(texts: List<String>): Result<List<String>> {
    return provider.get().translateBatch(texts)
  }

  override suspend fun translateWithContext(text: String, context: String?): Result<String> {
    return provider.get().translateWithContext(text, context)
  }
}
