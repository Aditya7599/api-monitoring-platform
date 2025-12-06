package com.leap.collectorservice.controller

import com.leap.collectorservice.model.RateLimitConfig
import com.leap.collectorservice.repository.RateLimitConfigRepository
import com.leap.collectorservice.service.RateLimiterService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.Instant

/**
 * RateLimitConfigController - Manage rate limit configuration overrides.
 * 
 * Allows runtime configuration of rate limits stored in metadatadb.
 * These override the YAML configuration.
 */
@RestController
@RequestMapping("/ratelimit")
class RateLimitConfigController(
    private val rateLimitConfigRepository: RateLimitConfigRepository,
    private val rateLimiterService: RateLimiterService
) {
    
    /**
     * GET /ratelimit/configs
     * Get all rate limit configurations.
     */
    @GetMapping("/configs")
    fun getAllConfigs(): ResponseEntity<List<RateLimitConfig>> {
        val configs = rateLimitConfigRepository.findAll()
        return ResponseEntity.ok(configs)
    }
    
    /**
     * GET /ratelimit/configs/{serviceName}
     * Get rate limit config for a specific service.
     */
    @GetMapping("/configs/{serviceName}")
    fun getConfig(@PathVariable serviceName: String): ResponseEntity<Any> {
        val config = rateLimitConfigRepository.findByServiceName(serviceName)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "No config found for service: $serviceName"))
        return ResponseEntity.ok(config)
    }
    
    /**
     * POST /ratelimit/configs
     * Create or update a rate limit config.
     * Admin only.
     * 
     * Body: { "serviceName": "orders-service", "limit": 120 }
     */
    @PostMapping("/configs")
    @PreAuthorize("hasRole('ADMIN')")
    fun createOrUpdateConfig(@RequestBody request: RateLimitConfigRequest): ResponseEntity<RateLimitConfig> {
        val now = Instant.now().toString()
        
        // Check if config exists
        val existing = rateLimitConfigRepository.findByServiceName(request.serviceName)
        
        val config = if (existing != null) {
            // Update existing
            existing.copy(
                limit = request.limit,
                enabled = request.enabled ?: true,
                updatedAt = now,
                updatedBy = request.updatedBy
            )
        } else {
            // Create new
            RateLimitConfig(
                serviceName = request.serviceName,
                limit = request.limit,
                enabled = request.enabled ?: true,
                createdAt = now,
                updatedBy = request.updatedBy
            )
        }
        
        val saved = rateLimitConfigRepository.save(config)
        
        // Refresh the rate limiter bucket for this service
        rateLimiterService.refreshBucket(request.serviceName)
        
        println("📊 Rate limit config ${if (existing != null) "updated" else "created"}: ${saved.serviceName} = ${saved.limit} req/sec")
        
        return ResponseEntity.status(if (existing != null) HttpStatus.OK else HttpStatus.CREATED).body(saved)
    }
    
    /**
     * DELETE /ratelimit/configs/{serviceName}
     * Delete a rate limit config (reverts to YAML default).
     * Admin only.
     */
    @DeleteMapping("/configs/{serviceName}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteConfig(@PathVariable serviceName: String): ResponseEntity<Any> {
        val existing = rateLimitConfigRepository.findByServiceName(serviceName)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "No config found for service: $serviceName"))
        
        rateLimitConfigRepository.deleteByServiceName(serviceName)
        rateLimiterService.refreshBucket(serviceName)
        
        println("🗑️ Rate limit config deleted for: $serviceName")
        
        return ResponseEntity.ok(mapOf("message" to "Config deleted, service will use YAML default"))
    }
    
    /**
     * GET /ratelimit/status/{serviceName}
     * Get current rate limit status for a service.
     */
    @GetMapping("/status/{serviceName}")
    fun getStatus(@PathVariable serviceName: String): ResponseEntity<Map<String, Any>> {
        val tokens = rateLimiterService.getAvailableTokens(serviceName)
        val isLimited = rateLimiterService.isRateLimited(serviceName)
        
        return ResponseEntity.ok(mapOf(
            "serviceName" to serviceName,
            "availableTokens" to tokens,
            "isRateLimited" to isLimited
        ))
    }
}

data class RateLimitConfigRequest(
    val serviceName: String,
    val limit: Int,
    val enabled: Boolean? = true,
    val updatedBy: String? = null
)

