package com.example.core.models

/**
 * Representation of a scanned host status, matching the classic Angry IP Scanner indicators:
 * - ALIVE: Host responded to ping/TCP probe
 * - DEAD: No response within timeout
 * - UNKNOWN: Probe not yet attempted or status undetermined
 */
enum class HostStatus {
    ALIVE,
    DEAD,
    UNKNOWN
}
