package com.leap.collectorservice.repository

import com.leap.collectorservice.model.Alert
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

/**
 * AlertRepository - Database operations for alerts.
 * 
 * Uses metadataMongoTemplate → connects to metadatadb database.
 */
@Repository
class AlertRepository(
    @Qualifier("metadataMongoTemplate")  // Use the metadata database
    private val mongoTemplate: MongoTemplate
) {
    
    /**
     * Save an alert to the database.
     */
    fun save(alert: Alert): Alert {
        return mongoTemplate.save(alert)
    }
    
    /**
     * Get recent alerts, sorted newest first.
     */
    fun findRecent(limit: Int = 100): List<Alert> {
        val query = Query()
            .with(Sort.by(Sort.Direction.DESC, "timestamp"))
            .limit(limit)
        return mongoTemplate.find(query, Alert::class.java)
    }
}




