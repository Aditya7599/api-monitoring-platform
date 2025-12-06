package com.leap.clientservice.model

/**
 * ApiLog - Data class that holds information about each API request.
 * 
 * A "data class" in Kotlin automatically gives us:
 * - Constructor with all fields
 * - toString(), equals(), hashCode()
 * - copy() method
 * 
 * This will be converted to JSON and sent to collector-service.
 */
data class ApiLog(
    // The URL path (e.g., "/test", "/api/users")
    val endpoint: String,
    
    // HTTP method (GET, POST, PUT, DELETE, etc.)
    val method: String,
    
    // When the request happened (ISO-8601 format)
    val timestamp: String,
    
    // Size of request body in bytes (0 if no body)
    val requestSize: Long,
    
    // Size of response body in bytes
    val responseSize: Long,
    
    // HTTP status code (200, 404, 500, etc.)
    val statusCode: Int,
    
    // How long the request took in milliseconds
    val latencyMs: Long,
    
    // Name of this service
    val serviceName: String,
    
    // True if rate limit (100 req/sec) was exceeded
    val rateLimitHit: Boolean
)




