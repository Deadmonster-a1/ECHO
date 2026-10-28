package com.spotify.clone.core.cosmos

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Replicates Spotify's internal Cosmos microservice router (com.spotify.cosmos).
 * In Spotify, UI layers do not invoke repositories or network engines directly;
 * instead, they dispatch declarative requests across the Cosmos in-process bus using
 * custom `sp://` and `hm://` URI protocols.
 */
data class CosmosRequest(
    val method: String = "GET",
    val uri: String,
    val body: Any? = null,
    val headers: Map<String, String> = emptyMap()
)

data class CosmosResponse<T>(
    val statusCode: Int,
    val data: T?,
    val error: String? = null
)

typealias CosmosHandler = suspend (CosmosRequest) -> CosmosResponse<*>

object CosmosRouter {
    private val routes = mutableMapOf<String, CosmosHandler>()

    /**
     * Registers a microservice handler for a specific `sp://` route authority and path.
     * Example: `register("player/play") { ... }`
     */
    fun register(path: String, handler: CosmosHandler) {
        val cleanPath = path.removePrefix("sp://").trim('/')
        routes[cleanPath] = handler
    }

    /**
     * Dispatches a Cosmos request across the internal service bus.
     */
    suspend fun <T> resolve(request: CosmosRequest): CosmosResponse<T> = withContext(Dispatchers.IO) {
        val parsedUri = Uri.parse(request.uri)
        val routeKey = "${parsedUri.authority}${parsedUri.path}".trim('/')

        val handler = routes[routeKey]
            ?: return@withContext CosmosResponse(
                statusCode = 404,
                data = null,
                error = "Cosmos 404: No handler registered for ${request.uri}"
            )

        try {
            @Suppress("UNCHECKED_CAST")
            handler(request) as CosmosResponse<T>
        } catch (e: Exception) {
            CosmosResponse(
                statusCode = 500,
                data = null,
                error = "Cosmos 500: ${e.message}"
            )
        }
    }
}
