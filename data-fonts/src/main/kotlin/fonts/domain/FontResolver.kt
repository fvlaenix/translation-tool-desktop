package fonts.domain

import fonts.data.FontRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import translation.data.BlockSettings
import java.awt.Font

class FontResolver : KoinComponent {
  private val fontRepository: FontRepository by inject()

  suspend fun resolveFont(settings: BlockSettings): BlockSettings {
    return fontRepository.getFont(settings.fontName)
      .fold(
        onSuccess = { fontInfo ->
          if (fontInfo != null) {
            settings.apply {
              font = fontInfo.font.deriveFont(settings.fontSize.toFloat())
            }
          } else {
            // Font not found, use fallback
            settings.apply {
              font = Font("Arial", Font.PLAIN, settings.fontSize)
            }
          }
        },
        onFailure = {
          // Error loading font, use fallback
          settings.apply {
            font = Font("Arial", Font.PLAIN, settings.fontSize)
          }
        }
      )
  }

  suspend fun createDefaultSettings(): BlockSettings {
    val defaultFontName = fontRepository.getDefaultFont().getOrElse { "Arial" }
    val defaultFont = fontRepository.getFont(defaultFontName)
      .getOrNull()?.font?.deriveFont(10f)
      ?: Font("Arial", Font.PLAIN, 10)

    return BlockSettings.createWithFont(
      fontName = defaultFontName,
      font = defaultFont
    )
  }
}