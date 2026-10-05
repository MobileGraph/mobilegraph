package io.mobilegraph.a2a.client

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.mobilegraph.a2a.models.SecurityScheme

/**
 * Interface for providing authentication credentials to A2A requests.
 *
 * Implementations handle the specific mechanics of injecting
 * authentication headers/tokens based on the security schemes
 * advertised in the remote agent's Agent Card.
 *
 * Credentials are never logged or exposed in task payloads.
 */
interface A2AAuthProvider {
    /**
     * Applies authentication to an outgoing HTTP request.
     *
     * @param request The Ktor HTTP request builder to add auth to.
     * @param securitySchemes The security schemes from the remote Agent Card.
     */
    suspend fun authenticate(
        request: HttpRequestBuilder,
        securitySchemes: Map<String, SecurityScheme>,
    )
}

/**
 * No-op authentication provider for agents that do not require authentication.
 */
class NoAuthProvider : A2AAuthProvider {
    override suspend fun authenticate(
        request: HttpRequestBuilder,
        securitySchemes: Map<String, SecurityScheme>,
    ) {
        // No authentication needed
    }
}

/**
 * Bearer token authentication provider.
 *
 * Injects an `Authorization: Bearer <token>` header into requests.
 *
 * @property tokenProvider A suspend function that returns the current token.
 *   Using a function (rather than a static string) allows token refresh.
 */
class BearerTokenAuthProvider(
    private val tokenProvider: suspend () -> String,
) : A2AAuthProvider {
    /**
     * Convenience constructor for a static token.
     */
    constructor(token: String) : this({ token })

    override suspend fun authenticate(
        request: HttpRequestBuilder,
        securitySchemes: Map<String, SecurityScheme>,
    ) {
        val token = tokenProvider()
        request.header("Authorization", "Bearer $token")
    }
}

/**
 * API key authentication provider.
 *
 * Injects an API key into the request as a header, query parameter,
 * or cookie based on the security scheme's `in` specification.
 *
 * @property key The API key value.
 * @property headerName The header name to use (defaults to the scheme's `name` or "X-API-Key").
 */
class ApiKeyAuthProvider(
    private val key: String,
    private val headerName: String? = null,
) : A2AAuthProvider {
    override suspend fun authenticate(
        request: HttpRequestBuilder,
        securitySchemes: Map<String, SecurityScheme>,
    ) {
        // Find the API key scheme
        val apiKeyScheme = securitySchemes.values.firstOrNull { it.type == "apiKey" }
        val name = headerName ?: apiKeyScheme?.name ?: "X-API-Key"
        val location = apiKeyScheme?.location ?: "header"

        when (location) {
            "header" -> request.header(name, key)
            "query" -> request.url.parameters.append(name, key)
            // Cookie auth is not common and would require cookie jar support
        }
    }
}
