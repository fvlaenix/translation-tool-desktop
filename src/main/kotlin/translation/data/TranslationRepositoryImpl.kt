package translation.data

import core.base.Repository
import core.utils.ProtobufUtils

class TranslationRepositoryImpl : TranslationRepository, Repository {

  override suspend fun translateText(text: String): Result<String> = safeCall {
    ProtobufUtils.getTranslation(text)
  }

  override suspend fun translateBatch(texts: List<String>): Result<List<String>> = safeCall {
    ProtobufUtils.getTranslation(texts)
  }

  override suspend fun translateWithContext(text: String, context: String?): Result<String> = safeCall {
    // For now, just translate the text. Context support can be added later if needed
    ProtobufUtils.getTranslation(text)
  }
}