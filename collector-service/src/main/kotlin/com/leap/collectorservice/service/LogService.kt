package com.leap.collectorservice.service

import com.leap.collectorservice.model.Alert
import com.leap.collectorservice.model.ApiLog
import com.leap.collectorservice.repository.AlertRepository
import com.leap.collectorservice.repository.LogRepository
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * LogService - Business logic for processing logs and creating alerts.
 * 
 * When a log comes in:
 * 1. Save it to logsdb
 * 2. Check if we need to create alerts
 * 3. If yes, save alerts to metadatadb
 */
@Service
class LogService(
    private val logRepository: LogRepository,
    private val alertRepository: AlertRepository
) {
    
    // Alert if latency exceeds this (milliseconds)
    private val latencyThreshold = 500L
    
    /**
     * Process an incoming log.
     */
    fun processLog(log: ApiLog): ApiLog {
        // Save the log
        val savedLog = logRepository.save(log)
        
        // Check for alert conditions
        createAlertsIfNeeded(log)
        
        return savedLog
    }
    
    /**
     * Check conditions and create alerts if needed.
     */
    private fun createAlertsIfNeeded(log: ApiLog) {
        val now = Instant.now().toString()
        
        // ALERT 1: High latency (> 500ms)
        if (log.latencyMs > latencyThreshold) {
            alertRepository.save(Alert(
                type = "HIGH_LATENCY",
                message = "Response took ${log.latencyMs}ms (threshold: ${latencyThreshold}ms)",
                serviceName = log.serviceName,
                endpoint = log.endpoint,
                timestamp = now,
                value = "${log.latencyMs}ms"
            ))
            println("⚠️ Alert: High latency ${log.latencyMs}ms on ${log.endpoint}")
        }
        
        // ALERT 2: Server error (5xx status)
        if (log.statusCode.toString().startsWith("5")) {
            alertRepository.save(Alert(
                type = "SERVER_ERROR",
                message = "Server returned ${log.statusCode}",
                serviceName = log.serviceName,
                endpoint = log.endpoint,
                timestamp = now,
                value = "HTTP ${log.statusCode}"
            ))
            println("🔴 Alert: Server error ${log.statusCode} on ${log.endpoint}")
        }
        
        // ALERT 3: Rate limit hit
        if (log.rateLimitHit) {
            alertRepository.save(Alert(
                type = "RATE_LIMIT",
                message = "Rate limit exceeded",
                serviceName = log.serviceName,
                endpoint = log.endpoint,
                timestamp = now,
                value = "limit hit"
            ))
            println("🚦 Alert: Rate limit hit on ${log.endpoint}")
        }
    }
    
    /**
     * Get recent logs.
     */
    fun getRecentLogs(limit: Int = 100): List<ApiLog> {
        return logRepository.findRecent(limit)
    }
    
    /**
     * Get recent alerts.
     */
    fun getRecentAlerts(limit: Int = 100): List<Alert> {
        return alertRepository.findRecent(limit)
    }
}




