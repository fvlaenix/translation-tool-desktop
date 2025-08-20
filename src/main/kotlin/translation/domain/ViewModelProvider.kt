package translation.domain

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import project.data.Project

object ViewModelProvider : KoinComponent {

  fun getOCRCreatorViewModel(project: Project? = null): OCRCreatorViewModel {
    return get { parametersOf(project) }
  }

  fun getTranslationCreatorViewModel(project: Project? = null): TranslationCreatorViewModel {
    return get { parametersOf(project) }
  }

  fun getEditCreatorViewModel(project: Project? = null): EditCreatorViewModel {
    return get { parametersOf(project) }
  }
}