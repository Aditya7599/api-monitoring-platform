package com.leap.collectorservice.controller

import com.leap.collectorservice.repository.LogRepository
import com.leap.collectorservice.repository.AlertRepository
import com.leap.collectorservice.repository.IssueRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * StatsController - Dashboard statistics and analytics endpoints
 */
@RestController
@RequestMapping("/stats")
class StatsController(
    private val logRepository: LogRepository,
    private val alertRepository: AlertRepository,
    private val issueRepository: IssueRepository
) {
    
    /**
     * GET /stats/dashboard
     * Returns all dashboard statistics in one call
     */
    @GetMapping("/dashboard")
    fun getDashboardStats(): ResponseEntity<DashboardStats> {
        val totalLogCount = logRepository.countAll() // Actual total count from DB
        val logs = logRepository.findRecent(1000)    // Recent logs for stats calculation
        val alerts = alertRepository.findRecent(100)
        val issues = issueRepository.findAll()
        
        // Calculate stats
        val slowApiCount = logs.count { it.latencyMs > 500 }
        val brokenApiCount = logs.count { it.statusCode >= 500 }
        val rateLimitHits = logs.count { it.rateLimitHit }
        val avgLatency = if (logs.isNotEmpty()) logs.map { it.latencyMs }.average() else 0.0
        
        // Top 5 slow endpoints
        val topSlowEndpoints = logs
            .groupBy { it.endpoint }
            .mapValues { (_, logs) -> logs.map { it.latencyMs }.average() }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { EndpointLatency(it.key, it.value) }
        
        // Error rate by endpoint
        val errorRateByEndpoint = logs
            .groupBy { it.endpoint }
            .mapValues { (_, endpointLogs) ->
                val total = endpointLogs.size
                val errors = endpointLogs.count { it.statusCode >= 400 }
                if (total > 0) (errors.toDouble() / total * 100) else 0.0
            }
            .entries
            .sortedByDescending { it.value }
            .take(10)
            .map { EndpointErrorRate(it.key, it.value) }
        
        // Alerts by type
        val alertsByType = alerts.groupBy { it.type }.mapValues { it.value.size }
        
        // Issues stats
        val openIssues = issues.count { !it.resolved }
        val resolvedIssues = issues.count { it.resolved }
        
        val stats = DashboardStats(
            totalLogs = totalLogCount.toInt(), // Use actual DB count
            slowApiCount = slowApiCount,
            brokenApiCount = brokenApiCount,
            rateLimitHits = rateLimitHits,
            avgLatencyMs = avgLatency,
            topSlowEndpoints = topSlowEndpoints,
            errorRateByEndpoint = errorRateByEndpoint,
            alertsByType = alertsByType,
            openIssues = openIssues,
            resolvedIssues = resolvedIssues
        )
        
        return ResponseEntity.ok(stats)
    }
}

data class DashboardStats(
    val totalLogs: Int,
    val slowApiCount: Int,
    val brokenApiCount: Int,
    val rateLimitHits: Int,
    val avgLatencyMs: Double,
    val topSlowEndpoints: List<EndpointLatency>,
    val errorRateByEndpoint: List<EndpointErrorRate>,
    val alertsByType: Map<String, Int>,
    val openIssues: Int,
    val resolvedIssues: Int
)

data class EndpointLatency(
    val endpoint: String,
    val avgLatencyMs: Double
)

data class EndpointErrorRate(
    val endpoint: String,
    val errorRate: Double
)

