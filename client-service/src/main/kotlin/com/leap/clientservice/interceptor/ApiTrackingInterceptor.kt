package com.leap.clientservice.interceptor

import com.leap.clientservice.model.ApiLog
import com.leap.clientservice.util.RateLimiter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.servlet.HandlerInterceptor
import java.time.Instant

/**
 * ApiTrackingInterceptor - Captures data about every HTTP request.
 * 
 * This interceptor:
 * 1. BEFORE request: records start time, checks rate limit
 * 2. AFTER request: collects all data, sends to collector-service
 * 
 * HandlerInterceptor has 3 methods:
 * - preHandle: runs BEFORE the controller
 * - postHandle: runs AFTER controller but BEFORE response is sent
 * - afterCompletion: runs AFTER everything is done
 */
@Component
class ApiTrackingInterceptor(
    private val rateLimiter: RateLimiter,
    private val rateLimitProperties: com.leap.clientservice.config.RateLimitProperties
) : HandlerInterceptor {
    
    // WebClient to send logs to collector-service (async/non-blocking)
    private val webClient = WebClient.builder()
        .baseUrl("http://localhost:8081")
        .build()
    
    // Service name from config (not hardcoded)
    private val serviceName: String
        get() = rateLimitProperties.service
    
    /**
     * STEP 1: Called BEFORE the controller handles the request.
     * 
     * We save the start time and rate limit status here.
     */
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        // Save start time to calculate latency later
        request.setAttribute("startTime", System.currentTimeMillis())
        
        // Check rate limit and save result
        request.setAttribute("rateLimitHit", rateLimiter.isLimitExceeded())
        
        // Return true = continue processing the request
        // Return false = block the request (we never do this)
        return true
    }
    
    /**
     * STEP 2: Called AFTER everything is complete.
     * 
     * This is where we collect all the data and send the log.
     */
    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        // Get the start time we saved
        val startTime = request.getAttribute("startTime") as? Long ?: return
        
        // Calculate how long the request took
        val latencyMs = System.currentTimeMillis() - startTime
        
        // Get rate limit status we saved
        val rateLimitHit = request.getAttribute("rateLimitHit") as? Boolean ?: false
        
        // Get request size (from Content-Length header)
        val requestSize = request.contentLengthLong.coerceAtLeast(0)
        
        // Get response size (from Content-Length header if set)
        val responseSize = response.getHeader("Content-Length")?.toLongOrNull() ?: 0L
        
        // Build the log object
        val log = ApiLog(
            endpoint = request.requestURI,
            method = request.method,
            timestamp = Instant.now().toString(),
            requestSize = requestSize,
            responseSize = responseSize.coerceAtLeast(0),
            statusCode = response.status,
            latencyMs = latencyMs,
            serviceName = serviceName,
            rateLimitHit = rateLimitHit
        )
        
        // Send log to collector-service (async - don't wait for response)
        sendLog(log)
    }
    
    /**
     * Send the log to collector-service asynchronously.
     * 
     * We use subscribe() to fire-and-forget. This means:
     * - We don't wait for the response
     * - We don't slow down the original request
     * - If it fails, we just print an error
     */
    private fun sendLog(log: ApiLog) {
        webClient.post()
            .uri("/collect/log")
            .bodyValue(log)
            .retrieve()
            .bodyToMono(String::class.java)
            .subscribe(
                { /* Success - do nothing */ },
                { error -> println("Failed to send log: ${error.message}") }
            )
    }
}




