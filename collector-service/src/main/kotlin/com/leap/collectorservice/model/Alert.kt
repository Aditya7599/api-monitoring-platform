package com.leap.collectorservice.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

/**
 * Alert - Created when something unusual happens.
 * 
 * Alerts are triggered when:
 *   - Latency > 500ms (slow response)
 *   - Status code 5xx (server error)
 *   - Rate limit was hit
 * 
 * Saved to: metadatadb database, "alerts" collection
 */
@Document(collection = "alerts")
data class Alert(
    @Id
    val id: String? = null,
    
    val type: String,           // "HIGH_LATENCY", "SERVER_ERROR", "RATE_LIMIT"
    val message: String,        // Human-readable description
    val serviceName: String,    // Which service triggered this
    val endpoint: String,       // Which endpoint caused it
    val timestamp: String,      // When alert was created
    val value: String           // The actual value (e.g., "750ms")
)




