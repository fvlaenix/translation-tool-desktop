package core.base

interface Repository {
  suspend fun <T> safeCall(action: suspend () -> T): Result<T> {
    return try {
      Result.success(action())
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}