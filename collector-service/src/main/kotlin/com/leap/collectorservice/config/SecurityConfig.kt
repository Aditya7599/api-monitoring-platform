package com.leap.collectorservice.config

import com.leap.collectorservice.auth.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val corsConfigurationSource: CorsConfigurationSource
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // Enable CORS
            .cors { it.configurationSource(corsConfigurationSource) }
            
            // Disable CSRF (stateless API using JWT)
            .csrf { it.disable() }

            // Stateless session (no cookies)
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }

            // Configure endpoint authorization
            .authorizeHttpRequests { auth ->

                // 1️⃣ PUBLIC ENDPOINTS
                auth
                    .requestMatchers("/auth/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/collect/log").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                    // 2️⃣ PROTECTED ENDPOINTS (JWT REQUIRED)
                    .requestMatchers("/logs/**").authenticated()
                    .requestMatchers("/alerts/**").authenticated()
                    .requestMatchers("/stats/**").authenticated()
                    .requestMatchers("/ratelimit/configs").authenticated()
                    .requestMatchers("/ratelimit/status/**").authenticated()
                    .requestMatchers("/issues/create").authenticated()
                    .requestMatchers("/issues/**").authenticated()

                    // 3️⃣ ROLE ADMIN ENDPOINT (ADMIN ONLY)
                    .requestMatchers(HttpMethod.POST, "/issues/resolve").hasRole("ADMIN")

                    // 4️⃣ EVERYTHING ELSE
                    .anyRequest().authenticated()
            }

            // Add JWT filter before username/password filter
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter::class.java
            )

            // Disable default login form / http basic
            .formLogin { it.disable() }
            .httpBasic { it.disable() }

        return http.build()
    }

    // BCrypt password encoder bean (required by controllers/services)
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
