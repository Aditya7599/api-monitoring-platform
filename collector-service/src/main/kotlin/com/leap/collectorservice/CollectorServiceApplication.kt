package com.leap.collectorservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import com.leap.collectorservice.config.RateLimitProperties
import com.leap.collectorservice.auth.JwtProperties

@SpringBootApplication
@EnableConfigurationProperties(RateLimitProperties::class, JwtProperties::class)
open class CollectorServiceApplication

fun main(args: Array<String>) {
    runApplication<CollectorServiceApplication>(*args)
}




