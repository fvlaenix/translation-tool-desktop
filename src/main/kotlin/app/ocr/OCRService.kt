package app.ocr

import app.batch.ImagePathInfo
import bean.BlockSettings
import bean.ImageData
import bean.WorkData
import kotlin.io.path.name

class OCRService {
  var workData: WorkData? = null

  fun initOfNotInitialized(images: Collection<ImagePathInfo>) {
    if (workData == null) {
      workData = WorkData(
        1,
        "author",
        images.mapIndexed { index, imagePathInfo: ImagePathInfo ->
          ImageData(
            image = imagePathInfo.path.name,
            imageName = imagePathInfo.path.name,
            index = index,
            blockData = emptyList(),
            settings = BlockSettings.DEFAULT
          )
        }
      )
    }
  }

  companion object {
    private val DEFAULT = OCRService()

    fun getInstance(): OCRService = DEFAULT
  }
}