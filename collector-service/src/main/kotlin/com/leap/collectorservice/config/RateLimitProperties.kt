package com.leap.collectorservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "ratelimits")
open class RateLimitProperties {
    // default limit when service-specific entry not present
    open var default: Int = 100

    // service-specific overrides, populated from YAML map
    open var map: MutableMap<String, Int> = mutableMapOf()

    // helper: return limit for a service name (or default if missing)
    open fun getLimitFor(serviceName: String?): Int {
        if (serviceName == null) return default
        return map[serviceName] ?: default
    }
}
