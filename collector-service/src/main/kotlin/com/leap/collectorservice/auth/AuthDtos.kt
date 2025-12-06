package com.leap.collectorservice.auth

/**
 * DTOs for authentication endpoints.
 */

/**
 * Request body for POST /auth/register
 */
data class RegisterRequest(
    val username: String,
    val password: String
)

/**
 * Request body for POST /auth/login
 */
data class LoginRequest(
    val username: String,
    val password: String
)

/**
 * Response for successful login
 */
data class AuthResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long
)

/**
 * Response for successful registration
 */
data class RegisterResponse(
    val id: String,
    val username: String,
    val message: String = "User registered successfully"
)




