package com.leap.clientservice.config

import com.leap.clientservice.interceptor.ApiTrackingInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val apiTrackingInterceptor: ApiTrackingInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        // Register interceptor for all requests
        registry.addInterceptor(apiTrackingInterceptor)
            .addPathPatterns("/**")
    }
}



