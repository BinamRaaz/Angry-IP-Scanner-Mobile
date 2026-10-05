package com.example.core.networking

/**
 * Representation of current active network environment.
 */
data class LocalNetworkState(
    val isConnected: Boolean = false,
    val isWifi: Boolean = false,
    val networkName: String? = null,
    val localIp: String? = null,
    val gatewayIp: String? = null,
    val subnetMask: String? = null,
    val suggestedRangeStart: String? = null,
    val suggestedRangeEnd: String? = null,
    val cidrNotation: String? = null
)

/**
 * Interface providing local device network details without tightly coupling to Android context.
 */
interface NetworkInfoProvider {
    fun getCurrentNetworkState(): LocalNetworkState
}
