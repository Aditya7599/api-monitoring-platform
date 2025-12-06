package com.leap.collectorservice.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * JwtAuthenticationFilter - Validates JWT token on each request.
 * 
 * How it works:
 * 1. Extract "Authorization: Bearer <token>" header
 * 2. Validate the JWT token
 * 3. If valid, set Authentication in SecurityContext
 * 4. Continue filter chain
 */
@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Get Authorization header
        val authHeader = request.getHeader("Authorization")
        
        // Check if it's a Bearer token
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7) // Remove "Bearer " prefix
            
            // Validate token
            val claims = jwtService.validateTokenAndGetClaims(token)
            
            if (claims != null) {
                val username = claims.subject
                
                @Suppress("UNCHECKED_CAST")
                val roles = claims["roles"] as? List<String> ?: emptyList()
                
                // Convert roles to Spring Security authorities
                val authorities = roles.map { SimpleGrantedAuthority(it) }
                
                // Create authentication token
                val authentication = UsernamePasswordAuthenticationToken(
                    username,    // principal (username)
                    null,        // credentials (not needed after auth)
                    authorities  // granted authorities (roles)
                )
                
                // Set in security context
                SecurityContextHolder.getContext().authentication = authentication
                
                // Also set as request attribute for easy access
                request.setAttribute("username", username)
                request.setAttribute("roles", roles)
                
                println("✅ JWT authenticated: $username with roles $roles")
            }
        }
        
        // Continue filter chain
        filterChain.doFilter(request, response)
    }
}




