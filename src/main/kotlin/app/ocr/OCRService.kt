package app.ocr

import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import translation.data.WorkData
import translation.data.WorkDataRepository

@Deprecated(message = "use repository instead WorkDataRepository")
class OCRService private constructor() : KoinComponent {
  private val workDataRepository: WorkDataRepository by inject()

  var workData: WorkData?
    get() = runBlocking { workDataRepository.getWorkData().getOrNull() }
    set(value) = runBlocking {
      if (value != null) {
        workDataRepository.setWorkData(value)
      } else {
        workDataRepository.clearWorkData()
      }
    }

  companion object {
    private val DEFAULT = OCRService()

    fun getInstance(): OCRService = DEFAULT
  }
}