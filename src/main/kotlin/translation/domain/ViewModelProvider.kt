package translation.domain

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import project.data.Project

/**
 * Factory provider for translation workflow ViewModels using dependency injection.
 */
object ViewModelProvider : KoinComponent {

  /**
   * Creates OCR creator ViewModel with optional project context.
   */
  fun getOCRCreatorViewModel(project: Project? = null): OCRCreatorViewModel {
    return get { parametersOf(project) }
  }

  /**
   * Creates translation creator ViewModel with optional project context.
   */
  fun getTranslationCreatorViewModel(project: Project? = null): TranslationCreatorViewModel {
    return get { parametersOf(project) }
  }

  /**
   * Creates edit creator ViewModel with optional project context.
   */
  fun getEditCreatorViewModel(project: Project? = null): EditCreatorViewModel {
    return get { parametersOf(project) }
  }
}