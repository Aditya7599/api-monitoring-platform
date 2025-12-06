package com.leap.collectorservice.auth

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*

/**
 * AuthController - JWT authentication endpoints.
 * 
 * Endpoints:
 *   POST /auth/register - Create new user
 *   POST /auth/login    - Login and get JWT token
 */
@RestController
@RequestMapping("/auth")
class AuthController(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder
) {
    
    /**
     * POST /auth/register
     * 
     * Register a new user.
     * 
     * Body: { "username": "john", "password": "secret123" }
     * 
     * Returns 201 with user ID on success.
     * Returns 409 if username already exists.
     */
    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<Any> {
        // Validate input
        if (request.username.isBlank() || request.password.isBlank()) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "Username and password are required"))
        }
        
        if (request.password.length < 6) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "Password must be at least 6 characters"))
        }
        
        // Check if username exists
        if (userRepository.existsByUsername(request.username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("error" to "Username already exists"))
        }
        
        // Create user with hashed password
        val user = User(
            username = request.username,
            passwordHash = passwordEncoder.encode(request.password),
            roles = listOf("ROLE_USER")
        )
        
        val saved = userRepository.save(user)
        
        println("👤 User registered: ${saved.username} (ID: ${saved.id})")
        
        return ResponseEntity.status(HttpStatus.CREATED).body(
            RegisterResponse(
                id = saved.id!!,
                username = saved.username
            )
        )
    }
    
    /**
     * POST /auth/fix-admin (DEV ONLY)
     * 
     * Delete and recreate admin user with correct ROLE_ADMIN.
     * PUBLIC endpoint for development purposes.
     */
    @PostMapping("/fix-admin")
    fun fixAdmin(): ResponseEntity<Any> {
        // Delete existing admin user
        userRepository.deleteByUsername("admin")
        println("🗑️ Deleted existing admin user")
        
        // Create new admin with correct roles
        val admin = User(
            username = "admin",
            passwordHash = passwordEncoder.encode("admin123"),
            roles = listOf("ROLE_USER", "ROLE_ADMIN")
        )
        val saved = userRepository.save(admin)
        
        println("✅ Created new admin user with roles: ${saved.roles}")
        
        return ResponseEntity.ok(mapOf(
            "message" to "Admin user recreated with ROLE_ADMIN",
            "username" to saved.username,
            "roles" to saved.roles
        ))
    }
    
    /**
     * POST /auth/login
     * 
     * Authenticate user and return JWT token.
     * 
     * Body: { "username": "john", "password": "secret123" }
     * 
     * Returns: { "accessToken": "eyJ...", "tokenType": "Bearer", "expiresIn": 3600 }
     */
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<Any> {
        // Validate input
        if (request.username.isBlank() || request.password.isBlank()) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "Username and password are required"))
        }
        
        // Find user
        val user = userRepository.findByUsername(request.username)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Invalid username or password"))
        
        // Verify password
        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Invalid username or password"))
        }
        
        // Generate JWT token
        val token = jwtService.generateToken(user.username, user.roles)
        
        println("🔑 User logged in: ${user.username}")
        
        return ResponseEntity.ok(
            AuthResponse(
                accessToken = token,
                tokenType = "Bearer",
                expiresIn = jwtService.getExpiresInSec()
            )
        )
    }
}




