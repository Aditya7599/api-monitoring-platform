package com.leap.collectorservice.service

import com.leap.collectorservice.model.Issue
import com.leap.collectorservice.repository.IssueRepository
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * IssueService - Business logic for managing issues.
 * 
 * OPTIMISTIC LOCKING EXPLANATION:
 * 
 * Problem: Two users read the same issue, both try to update it.
 *          Without locking, the second update overwrites the first!
 * 
 * Solution: @Version field on Issue entity.
 *          - Every update increments version
 *          - If versions don't match, update fails
 *          - User must re-read and try again
 */
@Service
class IssueService(
    private val issueRepository: IssueRepository
) {
    
    /**
     * Create a new issue.
     */
    fun createIssue(title: String, description: String, severity: String): Issue {
        val issue = Issue(
            title = title,
            description = description,
            severity = severity,
            createdAt = Instant.now().toString()
        )
        return issueRepository.save(issue)
    }
    
    /**
     * Resolve an issue.
     * 
     * Uses optimistic locking - if someone else updated the issue
     * since we read it, this will throw OptimisticLockingFailureException.
     * 
     * @param issueId The issue to resolve
     * @param resolvedBy Who is resolving it
     * @throws NoSuchElementException if issue not found
     * @throws OptimisticLockingFailureException if concurrent modification
     */
    fun resolveIssue(issueId: String, resolvedBy: String): Issue {
        // Step 1: Find the issue
        val issue = issueRepository.findById(issueId)
            ?: throw NoSuchElementException("Issue not found: $issueId")
        
        // Step 2: Check if already resolved
        if (issue.resolved) {
            return issue  // Already done
        }
        
        // Step 3: Update with resolved=true
        // The version field is automatically checked!
        val resolved = issue.copy(
            resolved = true,
            resolvedAt = Instant.now().toString(),
            resolvedBy = resolvedBy
        )
        
        // Step 4: Save - throws if version mismatch
        return issueRepository.save(resolved)
    }
    
    /**
     * Get all issues.
     */
    fun getAllIssues(): List<Issue> {
        return issueRepository.findAll()
    }
    
    /**
     * Get unresolved issues only.
     */
    fun getUnresolvedIssues(): List<Issue> {
        return issueRepository.findUnresolved()
    }
    
    /**
     * Find issue by ID.
     */
    fun findById(id: String): Issue? {
        return issueRepository.findById(id)
    }
}




