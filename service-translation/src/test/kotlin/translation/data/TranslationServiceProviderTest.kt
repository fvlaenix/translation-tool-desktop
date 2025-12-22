package translation.data

import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import settings.data.SettingsModel
import settings.data.SettingsRepository
import settings.data.TranslationMode

class TranslationServiceProviderTest {

  @Test
  fun `refresh switches repository based on mode`() {
    val grpcRepo = StubTranslationRepository()
    val directRepo = StubTranslationRepository()
    val provider = TranslationServiceProvider(
      grpcRepository = grpcRepo,
      settingsRepository = StubSettingsRepository(SettingsModel.DEFAULT),
      directFactory = { directRepo }
    )

    provider.refresh(SettingsModel.DEFAULT.copy(translationMode = TranslationMode.DIRECT))
    assertSame(directRepo, provider.get())

    provider.refresh(SettingsModel.DEFAULT.copy(translationMode = TranslationMode.GRPC))
    assertSame(grpcRepo, provider.get())
  }
}

private class StubTranslationRepository : TranslationRepository {
  override suspend fun translateText(text: String): Result<String> = Result.success(text)

  override suspend fun translateBatch(texts: List<String>): Result<List<String>> = Result.success(texts)

  override suspend fun translateWithContext(text: String, context: String?): Result<String> = Result.success(text)
}

private class StubSettingsRepository(
  private var settings: SettingsModel
) : SettingsRepository {
  override suspend fun loadSettings(): Result<SettingsModel> = Result.success(settings)

  override suspend fun saveSettings(settings: SettingsModel): Result<Unit> {
    this.settings = settings
    return Result.success(Unit)
  }

  override suspend fun updateSetting(key: String, value: Any): Result<Unit> {
    return Result.failure(UnsupportedOperationException("Not supported in tests"))
  }
}
