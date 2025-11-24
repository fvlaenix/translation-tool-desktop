package project.data

import org.koin.dsl.module

val imageDataModule = module {
  single<ImageDataRepository> { ImageDataRepositoryImpl() }
}
