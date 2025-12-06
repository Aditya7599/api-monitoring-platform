package com.leap.collectorservice.auth

import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/**
 * DefaultAdminInitializer - Creates a default admin user on startup.
 * 
 * Only runs in "dev" profile.
 * Creates admin user if not already present.
 * 
 * Default credentials:
 *   Username: admin
 *   Password: admin123
 */
@Component
@Profile("dev")
class DefaultAdminInitializer(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : CommandLineRunner {
    
    override fun run(vararg args: String?) {
        val adminUsername = "admin"
        val adminPassword = "admin123"
        
        // Check if admin exists
        if (userRepository.existsByUsername(adminUsername)) {
            println("ℹ️ Admin user already exists, skipping creation")
            return
        }
        
        // Create admin user
        val admin = User(
            username = adminUsername,
            passwordHash = passwordEncoder.encode(adminPassword),
            roles = listOf("ROLE_USER", "ROLE_ADMIN")
        )
        
        userRepository.save(admin)
        
        println("✅ Default admin user created")
        println("   Username: $adminUsername")
        println("   Password: $adminPassword")
        println("   Roles: ROLE_USER, ROLE_ADMIN")
        println("   ⚠️ Change password in production!")
    }
}




