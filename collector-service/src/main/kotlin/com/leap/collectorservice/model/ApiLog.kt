package com.leap.collectorservice.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

/**
 * ApiLog - Stores information about each API request.
 * 
 * Saved to: logsdb database, "logs" collection
 * 
 * @Document tells MongoDB this is a document (like a table row).
 */
@Document(collection = "logs")
data class ApiLog(
    @Id
    val id: String? = null,           // MongoDB auto-generates this
    
    val endpoint: String,              // URL path ("/test", "/api/users")
    val method: String,                // HTTP method (GET, POST, etc.)
    val timestamp: String,             // When request happened
    val requestSize: Long,             // Request body size in bytes
    val responseSize: Long,            // Response body size in bytes
    val statusCode: Int,               // HTTP status (200, 404, 500)
    val latencyMs: Long,               // How long it took (milliseconds)
    val serviceName: String,           // Which service sent this log
    val rateLimitHit: Boolean          // Was rate limit exceeded?
)




