package com.leap.collectorservice.auth

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

/**
 * UserRepository - Database operations for users.
 * 
 * Uses metadataMongoTemplate to store users in metadatadb.
 */
@Repository
class UserRepository(
    @Qualifier("metadataMongoTemplate")
    private val mongoTemplate: MongoTemplate
) {
    
    /**
     * Save a new user.
     */
    fun save(user: User): User {
        return mongoTemplate.save(user)
    }
    
    /**
     * Find user by username.
     */
    fun findByUsername(username: String): User? {
        val query = Query(Criteria.where("username").`is`(username))
        return mongoTemplate.findOne(query, User::class.java)
    }
    
    /**
     * Check if username exists.
     */
    fun existsByUsername(username: String): Boolean {
        return findByUsername(username) != null
    }
    
    /**
     * Find user by ID.
     */
    fun findById(id: String): User? {
        return mongoTemplate.findById(id, User::class.java)
    }
    
    /**
     * Delete user by username.
     */
    fun deleteByUsername(username: String) {
        val query = Query(Criteria.where("username").`is`(username))
        mongoTemplate.remove(query, User::class.java)
    }
}




