package com.leap.clientservice.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * TestController - Simple controller to test our interceptor.
 * 
 * @RestController = @Controller + @ResponseBody
 * This means methods return data directly (not view names).
 */
@RestController
class TestController {
    
    /**
     * GET /test - Returns "OK"
     * 
     * Use this to test the API tracking:
     *   curl http://localhost:8080/test
     * 
     * Each call will:
     * 1. Be intercepted by ApiTrackingInterceptor
     * 2. Generate a log
     * 3. Send log to http://localhost:8081/collect/log
     */
    @GetMapping("/test")
    fun test(): String {
        return "OK"
    }
}




