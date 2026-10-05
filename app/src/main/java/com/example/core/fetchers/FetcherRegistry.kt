package com.example.core.fetchers

/**
 * Registry holding all available fetchers for the scanner.
 */
object FetcherRegistry {

    fun getFetchers(
        enabledIds: Set<FetcherId>,
        portsToScan: List<Int> = listOf(22, 80, 443, 8080),
        portTimeoutMs: Int = 300
    ): List<HostFetcher> {
        val list = mutableListOf<HostFetcher>()
        if (FetcherId.PING in enabledIds) list.add(PingFetcher())
        if (FetcherId.HOSTNAME in enabledIds) list.add(HostnameFetcher())
        if (FetcherId.TTL in enabledIds) list.add(TtlFetcher())
        if (FetcherId.MAC_VENDOR in enabledIds) list.add(MacFetcher())
        if (FetcherId.PORTS in enabledIds) list.add(PortFetcher(portsToScan, portTimeoutMs))
        return list
    }
}
