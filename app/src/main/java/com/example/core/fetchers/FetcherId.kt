package com.example.core.fetchers

/**
 * Identifier for modular fetchers, allowing users to enable/disable specific probes.
 */
enum class FetcherId(val defaultEnabled: Boolean) {
    PING(true),
    HOSTNAME(true),
    TTL(false),
    MAC_VENDOR(false),
    PORTS(false)
}
