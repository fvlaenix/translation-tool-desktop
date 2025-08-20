package core.base

/**
 * Base interface for all repositories in the application.
 *
 * Repositories are responsible for:
 * - Abstracting data access (files, network, memory, etc.)
 * - Providing clean, domain-focused API to ViewModels
 * - Handling data source switching (local vs remote)
 * - Managing data caching and synchronization
 * - Converting between external data formats and domain models
 *
 * The Repository pattern provides:
 * - Separation between business logic (ViewModels) and data access
 * - Testability (can mock repositories for testing)
 * - Flexibility to change data sources without affecting business logic
 * - Centralized data access logic
 */
interface Repository {
  /**
   * Safe wrapper for operations that might throw exceptions.
   * Converts exceptions to Result types for consistent error handling.
   *
   * Usage example:
   * ```
   * override suspend fun loadData(): Result<Data> = safeCall {
   *     // Code that might throw exception
   *     loadDataFromFile()
   * }
   * ```
   */
  suspend fun <T> safeCall(action: suspend () -> T): Result<T> {
    return try {
      Result.success(action())
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}