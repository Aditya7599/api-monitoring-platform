package com.leap.collectorservice.auth

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * User - Represents a user in the system.
 * 
 * Stored in metadatadb database, "users" collection.
 * Password is stored as BCrypt hash (never plain text).
 */
@Document(collection = "users")
data class User(
    @Id
    val id: String? = null,
    
    @Indexed(unique = true)
    val username: String,
    
    val passwordHash: String,
    
    val roles: List<String> = listOf("ROLE_USER"),
    
    val createdAt: Instant = Instant.now()
)




