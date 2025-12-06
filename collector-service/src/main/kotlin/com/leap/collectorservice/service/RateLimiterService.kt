package com.leap.collectorservice.service

import com.leap.collectorservice.config.RateLimitProperties
import com.leap.collectorservice.repository.RateLimitConfigRepository
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * RateLimiterService - Manages rate limiting for different services.
 * 
 * Rate limits are loaded from:
 * 1. Database (metadatadb.rate_limit_configs) - takes priority
 * 2. application.yml config - fallback
 * 
 * This allows runtime configuration of rate limits without restart.
 */
@Service
class RateLimiterService(
    private val rateLimitProperties: RateLimitProperties,
    private val rateLimitConfigRepository: RateLimitConfigRepository
) {
    
    // Print loaded config on startup
    init {
        println("🚀 RateLimiterService initialized")
        println("   Default limit (YAML): ${rateLimitProperties.default}")
        println("   Service limits (YAML): ${rateLimitProperties.map}")
        
        // Load database overrides
        val dbConfigs = rateLimitConfigRepository.findAllEnabled()
        if (dbConfigs.isNotEmpty()) {
            println("   Database overrides: ${dbConfigs.map { "${it.serviceName}=${it.limit}" }}")
        }
    }
    
    /**
     * Map of serviceName → TokenBucket
     */
    private val buckets = ConcurrentHashMap<String, TokenBucket>()
    
    // Token bucket settings - refill every second
    private val refillIntervalMs = 1000L
    
    /**
     * Check if a request from the given service is allowed.
     */
    fun allowRequest(serviceName: String): Boolean {
        val bucket = getOrCreateBucket(serviceName)
        val allowed = bucket.tryConsume()
        val remaining = bucket.getAvailableTokens()
        
        if (allowed) {
            println("✅ RATE LIMIT: $serviceName - ALLOWED (tokens remaining: $remaining)")
        } else {
            println("🚫 RATE LIMIT: $serviceName - DENIED (tokens: $remaining)")
        }
        
        return allowed
    }
    
    /**
     * Get existing bucket or create a new one lazily.
     */
    private fun getOrCreateBucket(serviceName: String): TokenBucket {
        return buckets.computeIfAbsent(serviceName) { name ->
            val limit = getLimitForService(name)
            println("🪣 Creating TokenBucket for '$name' with limit=$limit tokens/sec")
            
            TokenBucket(
                capacity = limit,
                refillTokens = limit,
                refillIntervalMs = refillIntervalMs
            )
        }
    }
    
    /**
     * Get the rate limit for a specific service.
     * 
     * Priority:
     * 1. Database config (metadatadb.rate_limit_configs)
     * 2. YAML service-specific config
     * 3. YAML default config
     */
    private fun getLimitForService(serviceName: String): Long {
        // First check database for runtime override
        val dbConfig = rateLimitConfigRepository.findByServiceName(serviceName)
        if (dbConfig != null && dbConfig.enabled) {
            println("   📊 Using DB config for '$serviceName': ${dbConfig.limit} req/sec")
            return dbConfig.limit.toLong()
        }
        
        // Then check YAML service-specific limit
        val yamlLimit = rateLimitProperties.map[serviceName]
        if (yamlLimit != null) {
            println("   📄 Using YAML config for '$serviceName': $yamlLimit req/sec")
            return yamlLimit.toLong()
        }
        
        // Fall back to default
        println("   ⚙️ Using default config for '$serviceName': ${rateLimitProperties.default} req/sec")
        return rateLimitProperties.default.toLong()
    }
    
    /**
     * Refresh bucket for a service (call after DB config change).
     */
    fun refreshBucket(serviceName: String) {
        buckets.remove(serviceName)
        println("🔄 Refreshed rate limit bucket for '$serviceName'")
    }
    
    /**
     * Get available tokens for a service.
     */
    fun getAvailableTokens(serviceName: String): Long {
        return buckets[serviceName]?.getAvailableTokens() ?: getLimitForService(serviceName)
    }
    
    /**
     * Check if a service is currently rate limited.
     */
    fun isRateLimited(serviceName: String): Boolean {
        val bucket = buckets[serviceName] ?: return false
        return bucket.getAvailableTokens() <= 0
    }
}
