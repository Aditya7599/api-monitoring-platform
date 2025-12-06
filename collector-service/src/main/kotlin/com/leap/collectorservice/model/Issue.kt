package com.leap.collectorservice.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document

/**
 * Issue - A problem that needs to be resolved.
 * 
 * Uses OPTIMISTIC LOCKING with @Version:
 * - Each update increments the version number
 * - If two people try to update the same version, one fails
 * - This prevents "lost updates" when multiple users edit same issue
 * 
 * Saved to: metadatadb database, "issues" collection
 */
@Document(collection = "issues")
data class Issue(
    @Id
    val id: String? = null,
    
    val title: String,                    // Issue title
    val description: String,              // Detailed description
    val severity: String = "MEDIUM",      // LOW, MEDIUM, HIGH, CRITICAL
    val serviceName: String? = null,      // Which service reported this issue
    val status: String = "OPEN",          // OPEN, IN_PROGRESS, RESOLVED
    val createdAt: String,                // When issue was created
    
    val resolved: Boolean = false,        // Has it been fixed?
    val resolvedAt: String? = null,       // When it was resolved
    val resolvedBy: String? = null,       // Who resolved it
    
    /**
     * @Version enables OPTIMISTIC LOCKING:
     * 
     * How it works:
     * 1. You read issue with version=1
     * 2. Someone else updates it → version becomes 2
     * 3. You try to update with version=1 → FAILS!
     * 4. You must re-read and try again
     * 
     * This prevents accidentally overwriting someone else's changes.
     */
    @Version
    val version: Long? = null
)

