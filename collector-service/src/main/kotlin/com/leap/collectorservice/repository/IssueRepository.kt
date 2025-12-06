package com.leap.collectorservice.repository

import com.leap.collectorservice.model.Issue
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

/**
 * IssueRepository - Database operations for issues.
 * 
 * Uses metadataMongoTemplate → connects to metadatadb database.
 * 
 * Issues have @Version for optimistic locking - when you save(),
 * MongoDB checks if version matches. If not, it throws an exception.
 */
@Repository
class IssueRepository(
    @Qualifier("metadataMongoTemplate")
    private val mongoTemplate: MongoTemplate
) {
    
    /**
     * Save an issue. If updating, version must match or it fails!
     */
    fun save(issue: Issue): Issue {
        return mongoTemplate.save(issue)
    }
    
    /**
     * Find issue by ID.
     */
    fun findById(id: String): Issue? {
        return mongoTemplate.findById(id, Issue::class.java)
    }
    
    /**
     * Get all issues, sorted by creation date.
     */
    fun findAll(): List<Issue> {
        val query = Query()
            .with(Sort.by(Sort.Direction.DESC, "createdAt"))
        return mongoTemplate.find(query, Issue::class.java)
    }
    
    /**
     * Get only unresolved issues.
     */
    fun findUnresolved(): List<Issue> {
        val query = Query(Criteria.where("resolved").`is`(false))
            .with(Sort.by(Sort.Direction.DESC, "createdAt"))
        return mongoTemplate.find(query, Issue::class.java)
    }
}




