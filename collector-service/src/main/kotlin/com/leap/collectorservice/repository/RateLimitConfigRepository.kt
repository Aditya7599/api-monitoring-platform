package com.leap.collectorservice.repository

import com.leap.collectorservice.model.RateLimitConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

/**
 * RateLimitConfigRepository - Database operations for rate limit configurations.
 * 
 * Uses metadataMongoTemplate → connects to metadatadb database.
 * Stores rate limiter config overrides as required by the assignment.
 */
@Repository
class RateLimitConfigRepository(
    @Qualifier("metadataMongoTemplate")
    private val mongoTemplate: MongoTemplate
) {
    
    /**
     * Save or update a rate limit config.
     */
    fun save(config: RateLimitConfig): RateLimitConfig {
        return mongoTemplate.save(config)
    }
    
    /**
     * Find config by service name.
     */
    fun findByServiceName(serviceName: String): RateLimitConfig? {
        val query = Query(Criteria.where("serviceName").`is`(serviceName))
        return mongoTemplate.findOne(query, RateLimitConfig::class.java)
    }
    
    /**
     * Get all rate limit configs.
     */
    fun findAll(): List<RateLimitConfig> {
        return mongoTemplate.findAll(RateLimitConfig::class.java)
    }
    
    /**
     * Get all enabled configs.
     */
    fun findAllEnabled(): List<RateLimitConfig> {
        val query = Query(Criteria.where("enabled").`is`(true))
        return mongoTemplate.find(query, RateLimitConfig::class.java)
    }
    
    /**
     * Delete a config by service name.
     */
    fun deleteByServiceName(serviceName: String) {
        val query = Query(Criteria.where("serviceName").`is`(serviceName))
        mongoTemplate.remove(query, RateLimitConfig::class.java)
    }
}

