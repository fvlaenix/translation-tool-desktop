package settings.data

import core.utils.JSON
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SettingsModelTest {

  @Test
  fun `default settings serialize and deserialize`() {
    val encoded = JSON.encodeToString(SettingsModel.DEFAULT)
    val decoded = JSON.decodeFromString<SettingsModel>(encoded)

    assertEquals(SettingsModel.DEFAULT, decoded)
  }
}
