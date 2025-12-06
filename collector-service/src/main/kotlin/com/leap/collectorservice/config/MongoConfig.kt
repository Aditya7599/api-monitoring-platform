package com.leap.collectorservice.config

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.MongoTransactionManager
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory

/**
 * MongoConfig - Sets up connections to TWO MongoDB databases.
 * 
 * We have two databases on the same MongoDB server:
 *   1. logsdb - for API logs (high volume)
 *   2. metadatadb - for alerts and issues (low volume)
 * 
 * Each database gets its own MongoTemplate bean.
 */
@Configuration
class MongoConfig {
    
    // Read values from application.yml
    @Value("\${mongodb.logs.uri}")
    private lateinit var logsUri: String
    
    @Value("\${mongodb.logs.database}")
    private lateinit var logsDatabase: String
    
    @Value("\${mongodb.metadata.uri}")
    private lateinit var metadataUri: String
    
    @Value("\${mongodb.metadata.database}")
    private lateinit var metadataDatabase: String
    
    // ========== LOGS DATABASE (logsdb) ==========
    
    /**
     * MongoClient for logs database.
     * MongoClient = connection to MongoDB server.
     */
    @Bean(name = ["logsMongoClient"])
    fun logsMongoClient(): MongoClient {
        return MongoClients.create(logsUri)
    }
    
    /**
     * MongoTemplate for logs database.
     * MongoTemplate = what we use to read/write data.
     * 
     * @Primary = this is the default if no specific template is requested.
     */
    @Bean(name = ["logsMongoTemplate"])
    @Primary
    fun logsMongoTemplate(): MongoTemplate {
        return MongoTemplate(logsMongoClient(), logsDatabase)
    }
    
    // ========== METADATA DATABASE (metadatadb) ==========
    
    @Bean(name = ["metadataMongoClient"])
    fun metadataMongoClient(): MongoClient {
        return MongoClients.create(metadataUri)
    }
    
    /**
     * MongoTemplate for metadata database.
     * Use @Qualifier("metadataMongoTemplate") when injecting.
     */
    @Bean(name = ["metadataMongoTemplate"])
    fun metadataMongoTemplate(): MongoTemplate {
        return MongoTemplate(metadataMongoClient(), metadataDatabase)
    }
    
    // ========== TRANSACTION MANAGERS ==========
    // Required for safe concurrent writes and rollback support
    
    /**
     * MongoTransactionManager for logs database.
     * Enables @Transactional support for logsdb operations.
     * 
     * @Primary = default transaction manager if none specified.
     */
    @Bean(name = ["logsTransactionManager"])
    @Primary
    fun logsTransactionManager(): MongoTransactionManager {
        val factory: MongoDatabaseFactory = SimpleMongoClientDatabaseFactory(logsMongoClient(), logsDatabase)
        return MongoTransactionManager(factory)
    }
    
    /**
     * MongoTransactionManager for metadata database.
     * Enables @Transactional support for metadatadb operations.
     * 
     * Use @Transactional("metadataTransactionManager") when needed.
     */
    @Bean(name = ["metadataTransactionManager"])
    fun metadataTransactionManager(): MongoTransactionManager {
        val factory: MongoDatabaseFactory = SimpleMongoClientDatabaseFactory(metadataMongoClient(), metadataDatabase)
        return MongoTransactionManager(factory)
    }
}




