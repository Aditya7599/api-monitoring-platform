package com.leap.collectorservice.controller

import com.leap.collectorservice.model.ApiLog
import com.leap.collectorservice.service.LogService
import com.leap.collectorservice.service.RateLimiterService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LogController - Endpoint for log ingestion from services.
 * 
 * Endpoint:
 *   POST /collect/log  - Receive log from client-service (public)
 * 
 * Note: GET /logs and GET /alerts are in CollectorRestController (protected)
 */
@RestController
class LogController(
    private val logService: LogService,
    private val rateLimiterService: RateLimiterService
) {
    
    /**
     * POST /collect/log
     * 
     * Receives a log from client-service.
     * Saves to logsdb and creates alerts if needed.
     * 
     * If rate limit is exceeded:
     * - Still saves the log with rateLimitHit=true
     * - Returns 429 status
     */
    @PostMapping("/collect/log")
    fun collectLog(@RequestBody log: ApiLog): ResponseEntity<Any> {
        // Check rate limit BEFORE saving
        val rateLimitExceeded = !rateLimiterService.allowRequest(log.serviceName)
        
        // If rate limit exceeded, update the log to reflect this
        val logToSave = if (rateLimitExceeded) {
            println("🚫 Rate limit exceeded for ${log.serviceName} - saving with rateLimitHit=true")
            log.copy(rateLimitHit = true)
        } else {
            log
        }
        
        println("📥 Log received: ${logToSave.method} ${logToSave.endpoint} [${logToSave.statusCode}] ${logToSave.latencyMs}ms rateLimitHit=${logToSave.rateLimitHit}")
        
        // Always save the log (so rateLimitHit=true gets stored in MongoDB)
        val saved = logService.processLog(logToSave)
        
        // Return 429 if rate limit was exceeded, 200 otherwise
        return if (rateLimitExceeded) {
            ResponseEntity.status(429).body(saved)
        } else {
            ResponseEntity.ok(saved)
        }
    }
}

