package com.leap.collectorservice.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

/**
 * JwtService - Handles JWT token generation and validation.
 * 
 * Uses HS256 algorithm with secret from application.yml.
 */
@Service
class JwtService(
    private val jwtProperties: JwtProperties
) {
    
    // Generate signing key from secret
    private val signingKey: SecretKey by lazy {
        val secretBytes = jwtProperties.secret.toByteArray()
        // Ensure key is at least 256 bits for HS256
        if (secretBytes.size < 32) {
            Keys.hmacShaKeyFor(jwtProperties.secret.padEnd(32, '0').toByteArray())
        } else {
            Keys.hmacShaKeyFor(secretBytes)
        }
    }
    
    init {
        println("🔐 JwtService initialized")
        println("   Issuer: ${jwtProperties.issuer}")
        println("   Expires in: ${jwtProperties.expiresInSec} seconds")
    }
    
    /**
     * Generate a JWT token for a user.
     * 
     * @param username The username (stored as subject)
     * @param roles List of roles (stored as claim)
     * @return Signed JWT token string
     */
    fun generateToken(username: String, roles: List<String>): String {
        val now = Date()
        val expiry = Date(now.time + jwtProperties.expiresInSec * 1000)
        
        return Jwts.builder()
            .setSubject(username)
            .setIssuer(jwtProperties.issuer)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .claim("roles", roles)
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact()
    }
    
    /**
     * Validate a JWT token and extract claims.
     * 
     * @param token The JWT token string
     * @return Claims if valid, null if invalid/expired
     */
    fun validateTokenAndGetClaims(token: String): Claims? {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .body
        } catch (e: Exception) {
            println("⚠️ JWT validation failed: ${e.message}")
            null
        }
    }
    
    /**
     * Extract username from token.
     */
    fun getUsername(token: String): String? {
        return validateTokenAndGetClaims(token)?.subject
    }
    
    /**
     * Extract roles from token.
     */
    @Suppress("UNCHECKED_CAST")
    fun getRoles(token: String): List<String> {
        val claims = validateTokenAndGetClaims(token) ?: return emptyList()
        return claims["roles"] as? List<String> ?: emptyList()
    }
    
    /**
     * Check if token is valid (not expired, correct signature).
     */
    fun isTokenValid(token: String): Boolean {
        return validateTokenAndGetClaims(token) != null
    }
    
    /**
     * Get expiry time in seconds.
     */
    fun getExpiresInSec(): Long = jwtProperties.expiresInSec
}




