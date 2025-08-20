package translation.data

interface WorkDataRepository {
  suspend fun getWorkData(): Result<WorkData?>
  suspend fun setWorkData(workData: WorkData): Result<Unit>
  suspend fun clearWorkData(): Result<Unit>
  suspend fun hasWorkData(): Result<Boolean>
}