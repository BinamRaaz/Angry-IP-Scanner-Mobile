package com.example.core.networking

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.example.core.utilities.IpUtils
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Concrete implementation of [NetworkInfoProvider] that inspects device connectivity,
 * Wi-Fi SSID, IP configuration, gateway, and subnet mask via Android system services.
 */
class AndroidNetworkInfoProvider(
    private val context: Context
) : NetworkInfoProvider {

    override fun getCurrentNetworkState(): LocalNetworkState {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return fallbackNetworkInterfaceState()

            val activeNetwork = connectivityManager.activeNetwork
                ?: return fallbackNetworkInterfaceState()

            val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
            val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            val isEthernet = caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true

            val linkProperties = connectivityManager.getLinkProperties(activeNetwork)
            var localIp: String? = null
            var prefixLength: Int? = null

            linkProperties?.linkAddresses?.forEach { linkAddr ->
                if (linkAddr.address is Inet4Address && !linkAddr.address.isLoopbackAddress) {
                    localIp = linkAddr.address.hostAddress
                    prefixLength = linkAddr.prefixLength
                }
            }

            var gatewayIp: String? = null
            linkProperties?.routes?.forEach { route ->
                if (route.isDefaultRoute && route.gateway is Inet4Address) {
                    gatewayIp = route.gateway?.hostAddress
                }
            }

            var ssid: String? = null
            if (isWifi) {
                try {
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    val rawSsid = wifiManager?.connectionInfo?.ssid?.trim('"')
                    if (!rawSsid.isNullOrBlank() && rawSsid != "<unknown ssid>") {
                        ssid = rawSsid
                    } else {
                        ssid = "Wi-Fi Network"
                    }
                } catch (_: Exception) {
                    ssid = "Wi-Fi Network"
                }
            } else if (isEthernet) {
                ssid = "Ethernet"
            }

            if (localIp != null) {
                val prefix = prefixLength ?: 24
                val subnetMask = IpUtils.prefixLengthToMask(prefix)
                val baseLong = (IpUtils.ipv4ToLong(localIp!!) and (0xFFFFFFFFL shl (32 - prefix))) and 0xFFFFFFFFL
                val baseIp = IpUtils.longToIpv4(baseLong)
                val cidr = "$baseIp/$prefix"
                val range = IpUtils.parseCidr(cidr)

                return LocalNetworkState(
                    isConnected = true,
                    isWifi = isWifi,
                    networkName = ssid ?: if (isWifi) "Wi-Fi" else "Local Network",
                    localIp = localIp,
                    gatewayIp = gatewayIp,
                    subnetMask = subnetMask,
                    suggestedRangeStart = range?.first,
                    suggestedRangeEnd = range?.second,
                    cidrNotation = cidr
                )
            }
        } catch (_: Exception) {
        }

        return fallbackNetworkInterfaceState()
    }

    private fun fallbackNetworkInterfaceState(): LocalNetworkState {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return LocalNetworkState(isConnected = false)
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                for (interfaceAddress in intf.interfaceAddresses) {
                    val addr = interfaceAddress.address
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val localIp = addr.hostAddress ?: continue
                        val prefix = interfaceAddress.networkPrefixLength.toInt().coerceIn(8, 30)
                        val subnetMask = IpUtils.prefixLengthToMask(prefix)
                        val baseLong = (IpUtils.ipv4ToLong(localIp) and (0xFFFFFFFFL shl (32 - prefix))) and 0xFFFFFFFFL
                        val baseIp = IpUtils.longToIpv4(baseLong)
                        val cidr = "$baseIp/$prefix"
                        val range = IpUtils.parseCidr(cidr)

                        val isWifi = intf.name.startsWith("wlan")

                        return LocalNetworkState(
                            isConnected = true,
                            isWifi = isWifi,
                            networkName = if (isWifi) "Wi-Fi (${intf.name})" else intf.name,
                            localIp = localIp,
                            gatewayIp = null,
                            subnetMask = subnetMask,
                            suggestedRangeStart = range?.first,
                            suggestedRangeEnd = range?.second,
                            cidrNotation = cidr
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }
        return LocalNetworkState(isConnected = false)
    }
}
