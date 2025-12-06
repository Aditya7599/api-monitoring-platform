package com.leap.collectorservice.controller

import com.leap.collectorservice.model.Issue
import com.leap.collectorservice.service.IssueService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * IssueController - Additional issue endpoints.
 * 
 * Note: Main issue endpoints (GET /issues, POST /issues/create, POST /issues/resolve)
 * are in CollectorRestController.
 * 
 * This controller provides supplementary endpoints:
 *   GET /issues/unresolved - Get only unresolved issues
 */
@RestController
@RequestMapping("/issues")
class IssueController(
    private val issueService: IssueService
) {
    
    /**
     * GET /issues/unresolved
     * 
     * Get only unresolved issues.
     * Requires JWT authentication.
     */
    @GetMapping("/unresolved")
    fun getUnresolvedIssues(): ResponseEntity<List<Issue>> {
        return ResponseEntity.ok(issueService.getUnresolvedIssues())
    }
}

