package com.example.core.fetchers

/**
 * Lightweight IEEE OUI database for matching MAC addresses to hardware vendors.
 */
object VendorLookup {
    private val OUI_MAP = mapOf(
        "00:50:56" to "VMware",
        "00:0C:29" to "VMware",
        "08:00:27" to "VirtualBox",
        "B8:27:EB" to "Raspberry Pi",
        "DC:A6:32" to "Raspberry Pi",
        "E4:5F:01" to "Raspberry Pi",
        "00:1A:11" to "Google",
        "F4:F5:E8" to "TP-Link",
        "50:C7:BF" to "TP-Link",
        "00:04:4B" to "NVIDIA",
        "FC:FB:FB" to "Cisco",
        "00:15:5D" to "Microsoft",
        "00:1E:67" to "Intel",
        "00:1B:21" to "Intel",
        "3C:22:FB" to "Apple",
        "F0:18:98" to "Apple",
        "AC:DE:48" to "Apple",
        "D4:6A:91" to "Amazon",
        "74:C2:46" to "Amazon",
        "00:11:32" to "Synology",
        "18:E8:29" to "Ubiquiti",
        "24:5A:4C" to "Ubiquiti",
        "78:8A:20" to "Ubiquiti"
    )

    fun lookup(mac: String): String? {
        val cleanMac = mac.trim().uppercase().replace("-", ":")
        if (cleanMac.length < 8) return null
        val prefix = cleanMac.substring(0, 8)
        return OUI_MAP[prefix]
    }
}
