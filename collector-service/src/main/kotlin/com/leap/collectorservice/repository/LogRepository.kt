package com.leap.collectorservice.repository

import com.leap.collectorservice.model.ApiLog
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

/**
 * LogRepository - Database operations for API logs.
 * 
 * Uses logsMongoTemplate → connects to logsdb database.
 */
@Repository
class LogRepository(
    @Qualifier("logsMongoTemplate")  // Use the logs database
    private val mongoTemplate: MongoTemplate
) {
    
    /**
     * Save a log to the database.
     */
    fun save(log: ApiLog): ApiLog {
        return mongoTemplate.save(log)
    }
    
    /**
     * Get recent logs, sorted newest first.
     */
    fun findRecent(limit: Int = 100): List<ApiLog> {
        val query = Query()
            .with(Sort.by(Sort.Direction.DESC, "timestamp"))
            .limit(limit)
        return mongoTemplate.find(query, ApiLog::class.java)
    }
    
    /**
     * Find logs with optional filters.
     * 
     * @param serviceName Filter by service name (exact match)
     * @param endpoint Filter by endpoint path (exact match)
     * @param statusCode Filter by HTTP status code
     * @param rateLimitHit Filter by rate limit hit flag
     * @param limit Maximum number of results (default 100)
     * @return List of matching ApiLog objects, sorted by timestamp desc
     */
    fun findWithFilters(
        serviceName: String? = null,
        endpoint: String? = null,
        statusCode: Int? = null,
        rateLimitHit: Boolean? = null,
        limit: Int = 100
    ): List<ApiLog> {
        val query = Query()
        
        // Build criteria based on provided filters
        val criteriaList = mutableListOf<Criteria>()
        
        serviceName?.let {
            criteriaList.add(Criteria.where("serviceName").`is`(it))
        }
        
        endpoint?.let {
            criteriaList.add(Criteria.where("endpoint").`is`(it))
        }
        
        statusCode?.let {
            criteriaList.add(Criteria.where("statusCode").`is`(it))
        }
        
        rateLimitHit?.let {
            criteriaList.add(Criteria.where("rateLimitHit").`is`(it))
        }
        
        // Combine all criteria with AND
        if (criteriaList.isNotEmpty()) {
            query.addCriteria(Criteria().andOperator(*criteriaList.toTypedArray()))
        }
        
        // Sort by timestamp descending, limit results
        query.with(Sort.by(Sort.Direction.DESC, "timestamp"))
        query.limit(limit)
        
        return mongoTemplate.find(query, ApiLog::class.java)
    }
    
    /**
     * Count total logs in database.
     */
    fun countAll(): Long {
        return mongoTemplate.count(Query(), ApiLog::class.java)
    }
}

