package translation.data

import core.base.Repository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WorkDataRepositoryImpl : WorkDataRepository, Repository {
  private val mutex = Mutex()
  private var workData: WorkData? = null

  override suspend fun getWorkData(): Result<WorkData?> = safeCall {
    mutex.withLock {
      workData
    }
  }

  override suspend fun setWorkData(workData: WorkData): Result<Unit> = safeCall {
    mutex.withLock {
      this.workData = workData
    }
  }

  override suspend fun clearWorkData(): Result<Unit> = safeCall {
    mutex.withLock {
      workData = null
    }
  }

  override suspend fun hasWorkData(): Result<Boolean> = safeCall {
    mutex.withLock {
      workData != null
    }
  }
}