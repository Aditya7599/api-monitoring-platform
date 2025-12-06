package com.leap.collectorservice.service

import java.util.concurrent.atomic.AtomicLong

/**
 * TokenBucket - Implements the Token Bucket rate limiting algorithm.
 * 
 * HOW TOKEN BUCKET WORKS:
 * 
 * Imagine a bucket that holds tokens:
 * 1. The bucket has a maximum capacity (e.g., 100 tokens)
 * 2. Tokens are added at a fixed rate (e.g., 10 tokens per second)
 * 3. Each request "consumes" one token
 * 4. If no tokens available, request is denied
 * 5. Tokens don't overflow - bucket never exceeds capacity
 * 
 * WHY TOKEN BUCKET?
 * - Allows short bursts (up to capacity)
 * - Smooths out traffic over time
 * - Simple and efficient
 * 
 * Example:
 *   capacity = 100, refillTokens = 10, refillIntervalMs = 1000
 *   → Bucket holds max 100 tokens
 *   → Every 1000ms (1 second), 10 tokens are added
 *   → Allows bursts of 100 requests, then ~10 requests/second sustained
 * 
 * @param capacity Maximum tokens the bucket can hold
 * @param refillTokens How many tokens to add each refill period
 * @param refillIntervalMs How often to refill (in milliseconds)
 */
class TokenBucket(
    private val capacity: Long,
    private val refillTokens: Long,
    private val refillIntervalMs: Long
) {
    // Current number of tokens (thread-safe)
    private val tokens = AtomicLong(capacity)
    
    // Last time we refilled tokens
    private val lastRefillTime = AtomicLong(System.currentTimeMillis())
    
    /**
     * Try to consume one token.
     * 
     * @return true if token was available (request allowed)
     *         false if no tokens (request denied)
     */
    fun tryConsume(): Boolean {
        // First, refill tokens based on elapsed time
        refill()
        
        // Try to take a token
        while (true) {
            val currentTokens = tokens.get()
            
            // No tokens available - deny request
            if (currentTokens <= 0) {
                println("   🔴 TokenBucket: OUT OF TOKENS (0/$capacity)")
                return false
            }
            
            // Try to decrement tokens atomically
            // compareAndSet ensures thread safety
            if (tokens.compareAndSet(currentTokens, currentTokens - 1)) {
                val remaining = currentTokens - 1
                if (remaining <= 2) {
                    println("   ⚠️ TokenBucket: LOW TOKENS ($remaining/$capacity)")
                }
                return true
            }
            // If compareAndSet failed, another thread modified tokens
            // Loop and try again
        }
    }
    
    /**
     * Refill tokens based on time elapsed since last refill.
     * 
     * This is called automatically by tryConsume().
     */
    private fun refill() {
        val now = System.currentTimeMillis()
        val lastRefill = lastRefillTime.get()
        val elapsed = now - lastRefill
        
        // Calculate how many refill periods have passed
        val periods = elapsed / refillIntervalMs
        
        if (periods > 0) {
            // Calculate tokens to add
            val tokensToAdd = periods * refillTokens
            
            // Try to update last refill time
            if (lastRefillTime.compareAndSet(lastRefill, lastRefill + (periods * refillIntervalMs))) {
                // Add tokens (but don't exceed capacity)
                while (true) {
                    val currentTokens = tokens.get()
                    val newTokens = minOf(capacity, currentTokens + tokensToAdd)
                    
                    if (tokens.compareAndSet(currentTokens, newTokens)) {
                        break
                    }
                }
            }
        }
    }
    
    /**
     * Get current token count (for monitoring/debugging).
     */
    fun getAvailableTokens(): Long {
        refill()
        return tokens.get()
    }
}

