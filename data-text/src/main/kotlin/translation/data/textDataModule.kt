package translation.data

import org.koin.dsl.module
import project.data.TextDataRepository
import project.data.TextDataRepositoryImpl

val textDataModule = module {
  single<WorkDataRepository> { WorkDataRepositoryImpl() }
  single<TextDataRepository> { TextDataRepositoryImpl() }
}
