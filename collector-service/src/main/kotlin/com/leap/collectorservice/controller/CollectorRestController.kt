package com.leap.collectorservice.controller

import com.leap.collectorservice.model.Alert
import com.leap.collectorservice.model.ApiLog
import com.leap.collectorservice.model.Issue
import com.leap.collectorservice.repository.AlertRepository
import com.leap.collectorservice.repository.IssueRepository
import com.leap.collectorservice.repository.LogRepository
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.Instant

/**
 * CollectorRestController - Main REST API for collector-service.
 * 
 * Endpoints:
 *   GET  /logs           - Get logs with optional filters (JWT required)
 *   GET  /alerts         - Get all alerts (JWT required)
 *   GET  /issues         - Get all issues (JWT required)
 *   POST /issues/create  - Create new issue (public)
 *   POST /issues/resolve - Resolve issue (JWT + ADMIN required)
 */
@RestController
class CollectorRestController(
    private val logRepository: LogRepository,
    private val alertRepository: AlertRepository,
    private val issueRepository: IssueRepository
) {
    
    // ==================== LOGS ====================
    
    /**
     * GET /logs
     * 
     * Get API logs with optional filtering.
     * Requires JWT authentication.
     * 
     * Query parameters:
     *   - serviceName: Filter by service name
     *   - endpoint: Filter by endpoint path
     *   - statusCode: Filter by HTTP status code
     *   - rateLimitHit: Filter by rate limit flag (true/false)
     *   - limit: Max results (default 100)
     * 
     * Examples:
     *   GET /logs
     *   GET /logs?serviceName=client-service
     *   GET /logs?statusCode=500&limit=50
     *   GET /logs?rateLimitHit=true
     */
    @GetMapping("/logs")
    fun getLogs(
        @RequestParam(required = false) serviceName: String?,
        @RequestParam(required = false) endpoint: String?,
        @RequestParam(required = false) statusCode: Int?,
        @RequestParam(required = false) rateLimitHit: Boolean?,
        @RequestParam(defaultValue = "100") limit: Int
    ): ResponseEntity<List<ApiLog>> {
        
        println("📊 GET /logs - filters: serviceName=$serviceName, endpoint=$endpoint, statusCode=$statusCode, rateLimitHit=$rateLimitHit, limit=$limit")
        
        val logs = logRepository.findWithFilters(
            serviceName = serviceName,
            endpoint = endpoint,
            statusCode = statusCode,
            rateLimitHit = rateLimitHit,
            limit = limit
        )
        
        return ResponseEntity.ok(logs)
    }
    
    // ==================== ALERTS ====================
    
    /**
     * GET /alerts
     * 
     * Get all alerts sorted by timestamp descending.
     * Requires JWT authentication.
     * 
     * Query parameters:
     *   - limit: Max results (default 100)
     */
    @GetMapping("/alerts")
    fun getAlerts(
        @RequestParam(defaultValue = "100") limit: Int
    ): ResponseEntity<List<Alert>> {
        
        println("🚨 GET /alerts - limit=$limit")
        
        val alerts = alertRepository.findRecent(limit)
        return ResponseEntity.ok(alerts)
    }
    
    // ==================== ISSUES ====================
    
    /**
     * GET /issues
     * 
     * Get all issues sorted by createdAt descending.
     * Requires JWT authentication.
     */
    @GetMapping("/issues")
    fun getIssues(): ResponseEntity<List<Issue>> {
        
        println("📋 GET /issues")
        
        val issues = issueRepository.findAll()
        return ResponseEntity.ok(issues)
    }
    
    /**
     * POST /issues/create
     * 
     * Create a new issue. PUBLIC endpoint (no JWT required).
     * 
     * Body:
     * {
     *   "title": "Something is broken",
     *   "description": "Detailed description...",
     *   "serviceName": "client-service"
     * }
     * 
     * Returns: Created issue with status "OPEN"
     */
    @PostMapping("/issues/create")
    fun createIssue(@RequestBody request: CreateIssueRequest): ResponseEntity<Issue> {
        
        // Validate required fields
        if (request.title.isBlank()) {
            return ResponseEntity.badRequest().build()
        }
        
        val issue = Issue(
            title = request.title,
            description = request.description ?: "",
            serviceName = request.serviceName,
            severity = "MEDIUM",
            status = "OPEN",
            createdAt = Instant.now().toString(),
            resolved = false
        )
        
        val saved = issueRepository.save(issue)
        
        println("📝 Issue created: ${saved.title} (ID: ${saved.id})")
        
        return ResponseEntity.status(HttpStatus.CREATED).body(saved)
    }
    
    /**
     * POST /issues/resolve
     * 
     * Mark an issue as resolved. 
     * Requires JWT authentication AND ROLE_ADMIN.
     * 
     * Uses optimistic locking - if issue was modified since read,
     * returns 409 Conflict.
     * 
     * Body:
     * {
     *   "issueId": "abc123"
     * }
     */
    @PostMapping("/issues/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    fun resolveIssue(@RequestBody request: ResolveIssueRequest): ResponseEntity<Any> {
        
        println("🔧 Resolving issue: ${request.issueId}")
        
        return try {
            // Find the issue
            val issue = issueRepository.findById(request.issueId)
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(mapOf("error" to "Issue not found: ${request.issueId}"))
            
            // Check if already resolved
            if (issue.resolved) {
                return ResponseEntity.ok(issue)
            }
            
            // Update issue with optimistic locking
            val resolved = issue.copy(
                resolved = true,
                status = "RESOLVED",
                resolvedAt = Instant.now().toString()
            )
            
            val saved = issueRepository.save(resolved)
            
            println("✅ Issue resolved: ${saved.title}")
            
            ResponseEntity.ok(saved)
            
        } catch (e: OptimisticLockingFailureException) {
            println("⚠️ Optimistic lock conflict for issue: ${request.issueId}")
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf(
                    "error" to "Conflict",
                    "message" to "Issue was modified by another user. Please refresh and try again."
                ))
        }
    }
}

// ==================== REQUEST DTOs ====================

/**
 * Request body for POST /issues/create
 */
data class CreateIssueRequest(
    val title: String,
    val description: String? = null,
    val serviceName: String? = null
)

/**
 * Request body for POST /issues/resolve
 */
data class ResolveIssueRequest(
    val issueId: String
)




