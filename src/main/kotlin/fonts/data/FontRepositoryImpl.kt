package fonts.data

import core.base.Repository
import core.utils.JSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import java.awt.Font
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Font repository implementation. Manages font loading, storage and persistence to json file.
 */
class FontRepositoryImpl(
  private val fontsFilePath: String
) : FontRepository, Repository {

  private val fontLock = Mutex()
  private val fonts: MutableMap<String, FontInfo> = mutableMapOf()
  private var loaded: Boolean = false

  /**
   * Loads fonts from json file and creates font objects.
   */
  override suspend fun loadFonts(): Result<Unit> = safeCall {
    fontLock.withLock {
      fonts.clear()
      val fontsInfos = try {
        withContext(Dispatchers.IO) {
          JSON.decodeFromString<List<FontInfoWritable>>(Path.of(fontsFilePath).readText())
        }
      } catch (_: SerializationException) {
        emptyList<FontInfoWritable>()
      } catch (_: IllegalArgumentException) {
        emptyList<FontInfoWritable>()
      } catch (_: IOException) {
        emptyList<FontInfoWritable>()
      }

      val listFonts: List<FontInfo> = withContext(Dispatchers.IO) {
        fontsInfos.mapNotNull { fontWritable ->
          try {
            fontWritable.withFont(Font.createFont(0, Path.of(fontWritable.path).toFile()))
          } catch (e: Exception) {
            println("Failed to load font ${fontWritable.name} from ${fontWritable.path}: ${e.message}")
            null
          }
        }
      }

      fonts.putAll(listFonts.associateBy { it.name })
      loaded = true
    }
  }

  /**
   * Adds a new font from file path to the repository.
   */
  override suspend fun addFont(name: String, path: Path): Result<Unit> = safeCall {
    val fontInfo = withContext(Dispatchers.IO) {
      val font = Font.createFont(0, path.toFile())
      FontInfo(name = name, path = path, font = font)
    }

    fontLock.withLock {
      fonts[name] = fontInfo
      loaded = true
    }

    saveFonts().getOrThrow()
  }

  /**
   * Retrieves font by name from the repository.
   */
  override suspend fun getFont(name: String): Result<FontInfo?> = safeCall {
    fontLock.withLock {
      checkLoaded()
      fonts[name]
    }
  }

  /**
   * Returns all loaded fonts from the repository.
   */
  override suspend fun getAllFonts(): Result<List<FontInfo>> = safeCall {
    fontLock.withLock {
      checkLoaded()
      fonts.values.toList()
    }
  }

  /**
   * Saves current fonts list to json file.
   */
  override suspend fun saveFonts(): Result<Unit> = safeCall {
    fontLock.withLock {
      val fontsList = fonts.values.map { it.toWritable() }
      withContext(Dispatchers.IO) {
        Path.of(fontsFilePath).writeText(JSON.encodeToString(fontsList))
      }
    }
  }

  /**
   * Checks if any fonts are loaded in the repository.
   */
  override suspend fun isFontsAdded(): Result<Boolean> = safeCall {
    fontLock.withLock {
      checkLoaded()
      fonts.isNotEmpty()
    }
  }

  /**
   * Returns the name of the first available font as default.
   */
  override suspend fun getDefaultFont(): Result<String> = safeCall {
    fontLock.withLock {
      checkLoaded()
      fonts.keys.firstOrNull() ?: throw IllegalStateException("No fonts available")
    }
  }

  private fun checkLoaded() {
    if (!loaded) {
      throw IllegalStateException("Fonts not loaded yet. Call loadFonts() first.")
    }
  }
}