package translation.data

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import com.fvlaenix.text.ModelInfo
import com.fvlaenix.text.OpenAIAPIServiceImpl
import com.fvlaenix.text.OpenAIModelProvider
import com.fvlaenix.text.OpenRouterModelProvider
import core.base.Repository
import settings.data.TranslationDirectSettings
import settings.data.TranslationModelProvider
import java.util.logging.Logger
import kotlin.time.Duration.Companion.seconds

private val LOG = Logger.getLogger(DirectTranslationRepository::class.simpleName)

class DirectTranslationRepository(
  private val settings: TranslationDirectSettings
) : TranslationRepository, Repository {

  private val textModelService: OpenAIAPIServiceImpl by lazy {
    createTextModelService()
  }

  private val prompt: String by lazy {
    DirectTranslationRepository::class.java.getResource("/translation-prompt.txt")?.readText()
      ?: "Translate the text and keep line breaks. Output only the translation."
  }

  override suspend fun translateText(text: String): Result<String> = safeCall {
    ensureValidSettings()
    translateSingle(text)
  }

  override suspend fun translateBatch(texts: List<String>): Result<List<String>> = safeCall {
    ensureValidSettings()
    texts.map { translateSingle(it) }
  }

  override suspend fun translateWithContext(text: String, context: String?): Result<String> = safeCall {
    translateText(text).getOrThrow()
  }

  private fun createTextModelService(): OpenAIAPIServiceImpl {
    val (modelInfo, hostUrl) = resolveModelInfo()

    val openAi = OpenAI(
      token = settings.apiKey,
      logging = LoggingConfig(LogLevel.None),
      timeout = Timeout(socket = settings.timeoutSeconds.seconds),
      host = OpenAIHost(hostUrl)
    )

    return OpenAIAPIServiceImpl(
      openAI = openAi,
      modelInfo = modelInfo
    )
  }

  private fun resolveModelInfo(): Pair<ModelInfo, String> {
    return when (settings.provider) {
      TranslationModelProvider.OPENAI -> {
        val modelInfo = OpenAIModelProvider.models[settings.modelName]
          ?: run {
            LOG.warning("Unknown OpenAI model '${settings.modelName}', falling back to '${OpenAIModelProvider.GPT_4O.name}'.")
            OpenAIModelProvider.GPT_4O
          }
        val hostUrl = settings.apiBaseUrl.ifBlank { OpenAIModelProvider.HOST_URL }
        modelInfo to hostUrl
      }

      TranslationModelProvider.OPENROUTER -> {
        val modelInfo = OpenRouterModelProvider.models[settings.modelName]
          ?: run {
            LOG.warning("Unknown OpenRouter model '${settings.modelName}', falling back to '${OpenRouterModelProvider.OPENAI_GPT_4O.name}'.")
            OpenRouterModelProvider.OPENAI_GPT_4O
          }
        val hostUrl = settings.apiBaseUrl.ifBlank { OpenRouterModelProvider.HOST_URL }
        modelInfo to hostUrl
      }

      TranslationModelProvider.OTHER -> {
        val modelInfo = ModelInfo(
          name = settings.modelName,
          maxTokenCount = OpenAIModelProvider.GPT_4O.maxTokenCount,
          supportsSystem = true
        )
        val hostUrl = settings.apiBaseUrl.ifBlank { OpenAIModelProvider.HOST_URL }
        modelInfo to hostUrl
      }
    }
  }

  private suspend fun translateSingle(text: String): String {
    val response = textModelService.sendRequest(prompt, text)
    return response.trim()
  }

  private fun ensureValidSettings() {
    if (settings.apiKey.isBlank()) {
      throw IllegalStateException("Direct translation requires a non-empty API key.")
    }
    if (settings.timeoutSeconds <= 0) {
      throw IllegalStateException("Timeout must be greater than 0.")
    }
    if (settings.provider == TranslationModelProvider.OTHER && settings.apiBaseUrl.isBlank()) {
      throw IllegalStateException("API base URL is required for Other provider.")
    }
  }
}
