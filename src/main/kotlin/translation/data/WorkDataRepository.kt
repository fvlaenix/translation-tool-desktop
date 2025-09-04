package translation.data

/**
 * Repository interface for managing work data persistence and retrieval.
 */
interface WorkDataRepository {
  /** Retrieves current work data if available */
  suspend fun getWorkData(): Result<WorkData?>

  /** Stores work data */
  suspend fun setWorkData(workData: WorkData): Result<Unit>

  /** Clears all stored work data */
  suspend fun clearWorkData(): Result<Unit>

  /** Checks if work data exists */
  suspend fun hasWorkData(): Result<Boolean>
}