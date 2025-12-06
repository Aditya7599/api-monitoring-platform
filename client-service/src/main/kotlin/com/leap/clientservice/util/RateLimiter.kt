package com.leap.clientservice.util

import com.leap.clientservice.config.RateLimitProperties
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * RateLimiter - Tracks if we exceed the configured requests per second.
 * 
 * IMPORTANT: This does NOT block requests. It only tells us
 * if the limit was exceeded so we can log it.
 * 
 * How it works:
 * 1. Track which "second" we're in (timestamp / 1000)
 * 2. Count requests in that second
 * 3. When a new second starts, reset counter
 * 4. If count > limit, return true (limit exceeded)
 * 
 * Configuration (application.yml):
 *   monitoring:
 *     rateLimit:
 *       limit: 100  # requests per second
 */
@Component
class RateLimiter(
    private val rateLimitProperties: RateLimitProperties
) {
    
    // Counter for current second (AtomicInteger = thread-safe)
    private val count = AtomicInteger(0)
    
    // Which second we're counting (AtomicLong = thread-safe)
    private val currentSecond = AtomicLong(0)
    
    /**
     * Log the configured limit on startup
     */
    @PostConstruct
    fun init() {
        println("⚡ RateLimiter initialized")
        println("   Service: ${rateLimitProperties.service}")
        println("   Limit: ${rateLimitProperties.limit} requests/second")
    }
    
    /**
     * Check if rate limit is exceeded.
     * Call this for every request.
     * 
     * @return true if over configured limit this second
     */
    fun isLimitExceeded(): Boolean {
        // Get current time in seconds
        val nowSecond = System.currentTimeMillis() / 1000
        val lastSecond = currentSecond.get()
        
        // New second? Reset the counter
        if (nowSecond != lastSecond) {
            if (currentSecond.compareAndSet(lastSecond, nowSecond)) {
                count.set(1)
                return false
            }
        }
        
        // Increment and check if over limit (from config!)
        val exceeded = count.incrementAndGet() > rateLimitProperties.limit
        
        if (exceeded) {
            println("🚦 Rate limit exceeded! (${count.get()}/${rateLimitProperties.limit})")
        }
        
        return exceeded
    }
    
    /**
     * Get current request count for this second
     */
    fun getCurrentCount(): Int = count.get()
    
    /**
     * Get the configured limit
     */
    fun getLimit(): Int = rateLimitProperties.limit
}
