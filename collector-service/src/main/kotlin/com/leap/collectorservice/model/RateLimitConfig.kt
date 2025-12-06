package com.leap.collectorservice.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

/**
 * RateLimitConfig - Stores rate limiter configuration overrides in metadatadb.
 * 
 * This allows runtime configuration of rate limits per service
 * without restarting the application.
 * 
 * Saved to: metadatadb database, "rate_limit_configs" collection
 */
@Document(collection = "rate_limit_configs")
data class RateLimitConfig(
    @Id
    val id: String? = null,
    
    /** Service name (e.g., "client-service", "orders-service") */
    val serviceName: String,
    
    /** Maximum requests per second allowed */
    val limit: Int,
    
    /** Whether this config is active */
    val enabled: Boolean = true,
    
    /** When this config was created */
    val createdAt: String,
    
    /** When this config was last updated */
    val updatedAt: String? = null,
    
    /** Who created/updated this config */
    val updatedBy: String? = null
)

