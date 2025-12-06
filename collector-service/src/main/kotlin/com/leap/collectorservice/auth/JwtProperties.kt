package com.leap.collectorservice.auth

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * JWT configuration properties loaded from application.yml
 */
@Component
@ConfigurationProperties(prefix = "jwt")
class JwtProperties {
    lateinit var secret: String
    lateinit var issuer: String
    var expiresInSec: Long = 3600
}




