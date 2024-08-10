package utils

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import bean.LoadingJsonException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import java.awt.Font
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

class FontService(private val path: Path) {
  private val fontLock = Mutex()
  private var loaded: Boolean = false
  private val fonts: MutableMap<String, FontInfo> = mutableMapOf()
  private val fontsList: SnapshotStateList<FontInfo> = mutableStateListOf()

  suspend fun load() {
    fontLock.withLock {
      fonts.clear()
      fontsList.clear()
      val fontsInfos = try {
        withContext(Dispatchers.IO) {
          JSON.decodeFromString<List<FontInfoWritable>>(path.readText())
        }
      } catch (_: SerializationException) {
        // TODO
        emptyList<FontInfoWritable>()
      } catch (_: IllegalArgumentException) {
        // TODO
        emptyList<FontInfoWritable>()
      } catch (_: IOException) {
        emptyList<FontInfoWritable>()
      }
      val listFonts: List<FontInfo> = coroutineScope {
        fontsInfos
          .map {
            async(Dispatchers.IO) {
              it.withFont(Font.createFont(0, Path.of(it.path).toFile()))
            }
          }
          .map { it.await() }
      }
      fonts.putAll(listFonts.associateBy { it.name })
      fontsList.addAll(listFonts)

      loaded = true
    }
  }

  private fun checkLoaded() {
    check(loaded)
  }

  suspend fun save() {
    fontLock.withLock {
      val fontsList = fonts.values
        .map { FontInfoWritable(name = it.name, path = it.path.toString()) }
      withContext(Dispatchers.IO) {
        path.writeText(JSON.encodeToString(fontsList))
      }
    }
  }

  fun getFontNotNull(name: String, size: Float): Font = runBlocking {
    fontLock.withLock {
      fonts[name]?.font?.deriveFont(size)
        ?: throw LoadingJsonException("Can't find font: $name. List of fonts: ${fonts.keys.toList()}")
    }
  }

  suspend fun add(name: String, path: Path) {
    val fontInfo = withContext(Dispatchers.IO) {
      val font = Font.createFont(0, path.toFile())
      FontInfo(name = name, path = path, font = font)
    }
    fontLock.withLock {
      fonts[name] = fontInfo
      fontsList.add(fontInfo)
    }
    save()
  }

  fun getMutableState(): SnapshotStateList<FontInfo> {
    return runBlocking {
      fontLock.withLock {
        checkLoaded()
        fontsList
      }
    }
  }

  fun isFontsAdded(): Boolean {
    return runBlocking {
      fontLock.withLock {
        checkLoaded()
        fonts.isNotEmpty()
      }
    }
  }

  companion object {
    private val path = Path.of("fonts.json")

    // TODO Dumb singleton, replace it
    private val DEFAULT = FontService(path)

    fun getInstance(): FontService = DEFAULT
  }

  @Serializable
  data class FontInfoWritable(
    val name: String,
    val path: String
  ) {
    fun withFont(font: Font): FontInfo = FontInfo(name = name, path = Path.of(path), font = font)
  }

  data class FontInfo(
    val name: String,
    val path: Path,
    val font: Font
  )
}