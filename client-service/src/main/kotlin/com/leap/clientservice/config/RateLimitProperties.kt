package com.leap.clientservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * RateLimitProperties - Reads rate limit config from application.yml
 * 
 * Binds to:
 *   monitoring:
 *     rateLimit:
 *       service: client-service
 *       limit: 100
 */
@Component
@ConfigurationProperties(prefix = "monitoring.rate-limit")  // Spring relaxed binding handles rateLimit → rate-limit
class RateLimitProperties {
    
    /** Service name for logging */
    var service: String = "client-service"
    
    /** Max requests per second (default: 100) */
    var limit: Int = 100
}

