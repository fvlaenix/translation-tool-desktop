package core.utils

import kotlinx.serialization.json.Json

/**
 * Shared JSON configuration instance with pretty printing enabled.
 */
val JSON = Json { prettyPrint = true }