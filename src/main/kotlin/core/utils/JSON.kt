package core.utils

import kotlinx.serialization.json.Json

/**
 * Shared JSON configuration instance with pretty printing enabled.
 */
internal val JSON = Json { prettyPrint = true }