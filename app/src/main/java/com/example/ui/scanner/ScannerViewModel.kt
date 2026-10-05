package com.example.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.fetchers.FetcherId
import com.example.core.models.DiscoveredHost
import com.example.core.models.HostStatus
import com.example.core.models.ScanProgress
import com.example.core.models.ScanTarget
import com.example.core.networking.LocalNetworkState
import com.example.core.networking.NetworkConnectivityMonitor
import com.example.core.networking.NetworkInfoProvider
import com.example.core.preferences.PreferencesRepository
import com.example.core.preferences.ScannerPreferences
import com.example.core.scanner.DefaultScannerEngine
import com.example.core.scanner.ScannerEngine
import com.example.core.utilities.IpUtils
import com.example.core.utilities.PortUtils
import com.example.data.repository.ScanHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOption {
    IP_ASC,
    IP_DESC,
    PING_ASC,
    STATUS_ALIVE_FIRST,
    HOSTNAME_ASC
}

data class ScannerUiState(
    val startIp: String = "192.168.1.1",
    val endIp: String = "192.168.1.254",
    val cidrInput: String = "192.168.1.0/24",
    val isCidrMode: Boolean = false,
    val errorMessage: String? = null,
    val showOnlyAlive: Boolean = false,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.IP_ASC,
    val enabledFetchers: Set<FetcherId> = setOf(
        FetcherId.PING,
        FetcherId.HOSTNAME,
        FetcherId.TTL,
        FetcherId.MAC_VENDOR,
        FetcherId.PORTS
    ),
    val portsInput: String = "22, 80, 443, 8080",
    val portTimeoutMs: Int = 300,
    val hostComments: Map<String, String> = emptyMap()
)

class ScannerViewModel(
    private val scannerEngine: ScannerEngine = DefaultScannerEngine(),
    private val networkInfoProvider: NetworkInfoProvider? = null,
    private var scanHistoryRepository: ScanHistoryRepository? = null,
    private var preferencesRepository: PreferencesRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val _networkState = MutableStateFlow(
        networkInfoProvider?.getCurrentNetworkState() ?: LocalNetworkState()
    )
    val networkState: StateFlow<LocalNetworkState> = _networkState.asStateFlow()

    val progress: StateFlow<ScanProgress> = scannerEngine.progress

    private var scanStartTime = 0L
    private var currentPreferences = ScannerPreferences()

    val results: StateFlow<List<DiscoveredHost>> = combine(
        scannerEngine.results,
        _uiState
    ) { hostList, state ->
        var list = hostList.map { host ->
            val comment = state.hostComments[host.ip] ?: host.comment
            if (comment != host.comment) host.copy(comment = comment) else host
        }

        // Filter: Show only alive
        if (state.showOnlyAlive) {
            list = list.filter { it.status == HostStatus.ALIVE }
        }

        // Filter: Search query
        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.trim().lowercase()
            list = list.filter { host ->
                host.ip.contains(query) ||
                    (host.hostname?.lowercase()?.contains(query) == true) ||
                    (host.comment?.lowercase()?.contains(query) == true)
            }
        }

        // Sort
        when (state.sortOption) {
            SortOption.IP_ASC -> list.sortedBy { host ->
                try { IpUtils.ipv4ToLong(host.ip) } catch (_: Exception) { Long.MAX_VALUE }
            }
            SortOption.IP_DESC -> list.sortedByDescending { host ->
                try { IpUtils.ipv4ToLong(host.ip) } catch (_: Exception) { Long.MIN_VALUE }
            }
            SortOption.PING_ASC -> list.sortedWith(
                compareBy<DiscoveredHost> { it.status != HostStatus.ALIVE }
                    .thenBy { it.responseTimeMs ?: Long.MAX_VALUE }
            )
            SortOption.STATUS_ALIVE_FIRST -> list.sortedWith(
                compareBy<DiscoveredHost> { it.status != HostStatus.ALIVE }
                    .thenBy { try { IpUtils.ipv4ToLong(it.ip) } catch (_: Exception) { 0L } }
            )
            SortOption.HOSTNAME_ASC -> list.sortedWith(
                compareBy<DiscoveredHost> { it.hostname.isNullOrBlank() }
                    .thenBy { it.hostname ?: "" }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        networkInfoProvider?.let { provider ->
            refreshNetworkState(provider)
        }

        preferencesRepository?.let { repo ->
            setPreferencesRepository(repo)
        }

        // Monitor scan progress to auto-save to history on completion
        viewModelScope.launch {
            var wasScanning = false
            progress.collect { prog ->
                if (wasScanning && !prog.isScanning && prog.scannedCount > 0 && currentPreferences.autoSaveHistory) {
                    val duration = System.currentTimeMillis() - scanStartTime
                    val currentResults = scannerEngine.results.value
                    val currentState = _uiState.value
                    val finalHosts = currentResults.map { host ->
                        val comment = currentState.hostComments[host.ip] ?: host.comment
                        if (comment != host.comment) host.copy(comment = comment) else host
                    }
                    val (start, end, cidr) = if (currentState.isCidrMode) {
                        val parsed = IpUtils.parseCidr(currentState.cidrInput)
                        Triple(parsed?.first ?: currentState.startIp, parsed?.second ?: currentState.endIp, currentState.cidrInput)
                    } else {
                        Triple(currentState.startIp, currentState.endIp, null)
                    }
                    scanHistoryRepository?.saveScan(
                        startIp = start,
                        endIp = end,
                        cidr = cidr,
                        durationMs = duration.coerceAtLeast(100L),
                        hosts = finalHosts
                    )
                }
                wasScanning = prog.isScanning
            }
        }
    }

    fun setScanHistoryRepository(repository: ScanHistoryRepository) {
        this.scanHistoryRepository = repository
    }

    fun setPreferencesRepository(repository: PreferencesRepository) {
        this.preferencesRepository = repository
        viewModelScope.launch {
            repository.preferencesFlow.collect { prefs ->
                currentPreferences = prefs
                val enabled = mutableSetOf<FetcherId>(FetcherId.PING)
                if (prefs.resolveHostnames) enabled.add(FetcherId.HOSTNAME)
                if (prefs.fetchTtl) enabled.add(FetcherId.TTL)
                if (prefs.fetchMacVendor) enabled.add(FetcherId.MAC_VENDOR)
                if (prefs.scanPorts) enabled.add(FetcherId.PORTS)

                _uiState.value = _uiState.value.copy(
                    enabledFetchers = enabled,
                    portsInput = prefs.defaultPorts,
                    portTimeoutMs = prefs.portTimeoutMs,
                    showOnlyAlive = if (_uiState.value.showOnlyAlive) true else prefs.defaultShowAliveOnly
                )
            }
        }
    }

    fun observeConnectivity(monitor: NetworkConnectivityMonitor) {
        viewModelScope.launch {
            monitor.connectivityState.collect { netState ->
                _networkState.value = netState
                if (!netState.isConnected && progress.value.isScanning) {
                    stopScan()
                    _uiState.value = _uiState.value.copy(errorMessage = "Scan aborted: Network connection was lost.")
                }
            }
        }
    }

    fun onAppBackgrounded() {
        if (progress.value.isScanning) {
            stopScan()
            _uiState.value = _uiState.value.copy(errorMessage = "Scan stopped: App moved to background.")
        }
    }

    fun refreshNetworkState(provider: NetworkInfoProvider) {
        val net = provider.getCurrentNetworkState()
        _networkState.value = net
        if (net.isConnected && net.suggestedRangeStart != null && net.suggestedRangeEnd != null) {
            _uiState.value = _uiState.value.copy(
                startIp = net.suggestedRangeStart,
                endIp = net.suggestedRangeEnd,
                cidrInput = net.cidrNotation ?: _uiState.value.cidrInput
            )
        }
    }

    fun scanCurrentSubnet(provider: NetworkInfoProvider? = null) {
        if (provider != null) {
            refreshNetworkState(provider)
        }
        val net = _networkState.value
        if (net.isConnected && net.suggestedRangeStart != null && net.suggestedRangeEnd != null) {
            _uiState.value = _uiState.value.copy(
                startIp = net.suggestedRangeStart,
                endIp = net.suggestedRangeEnd,
                cidrInput = net.cidrNotation ?: _uiState.value.cidrInput,
                errorMessage = null
            )
            startScan()
        } else {
            startScan()
        }
    }

    fun loadTargetRange(startIp: String, endIp: String, cidr: String?, startScanNow: Boolean = true) {
        if (cidr != null) {
            _uiState.value = _uiState.value.copy(
                isCidrMode = true,
                cidrInput = cidr,
                startIp = startIp,
                endIp = endIp,
                errorMessage = null
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isCidrMode = false,
                startIp = startIp,
                endIp = endIp,
                errorMessage = null
            )
        }
        if (startScanNow) {
            startScan()
        }
    }

    fun onStartIpChanged(ip: String) {
        _uiState.value = _uiState.value.copy(startIp = ip, errorMessage = null)
    }

    fun onEndIpChanged(ip: String) {
        _uiState.value = _uiState.value.copy(endIp = ip, errorMessage = null)
    }

    fun onCidrChanged(cidr: String) {
        _uiState.value = _uiState.value.copy(cidrInput = cidr, errorMessage = null)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onSortOptionChanged(sortOption: SortOption) {
        _uiState.value = _uiState.value.copy(sortOption = sortOption)
    }

    fun onPortsInputChanged(ports: String) {
        _uiState.value = _uiState.value.copy(portsInput = ports)
    }

    fun onPortTimeoutChanged(timeoutMs: Int) {
        _uiState.value = _uiState.value.copy(portTimeoutMs = timeoutMs)
    }

    fun toggleCidrMode() {
        val newMode = !_uiState.value.isCidrMode
        if (newMode) {
            _uiState.value = _uiState.value.copy(isCidrMode = true, errorMessage = null)
        } else {
            val parsed = IpUtils.parseCidr(_uiState.value.cidrInput)
            if (parsed != null) {
                _uiState.value = _uiState.value.copy(
                    isCidrMode = false,
                    startIp = parsed.first,
                    endIp = parsed.second,
                    errorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(isCidrMode = false, errorMessage = null)
            }
        }
    }

    fun toggleShowOnlyAlive() {
        _uiState.value = _uiState.value.copy(showOnlyAlive = !_uiState.value.showOnlyAlive)
    }

    fun toggleFetcher(fetcherId: FetcherId) {
        val current = _uiState.value.enabledFetchers
        val updated = if (fetcherId in current) {
            if (fetcherId == FetcherId.PING) current else current - fetcherId
        } else {
            current + fetcherId
        }
        _uiState.value = _uiState.value.copy(enabledFetchers = updated)
    }

    fun startScan() {
        val state = _uiState.value
        val (start, end) = if (state.isCidrMode) {
            val parsed = IpUtils.parseCidr(state.cidrInput)
            if (parsed == null) {
                _uiState.value = state.copy(errorMessage = "Invalid CIDR notation (e.g. 192.168.1.0/24)")
                return
            }
            parsed
        } else {
            if (!IpUtils.isValidIpv4(state.startIp)) {
                _uiState.value = state.copy(errorMessage = "Invalid Start IP format")
                return
            }
            if (!IpUtils.isValidIpv4(state.endIp)) {
                _uiState.value = state.copy(errorMessage = "Invalid End IP format")
                return
            }
            if (IpUtils.ipv4ToLong(state.startIp) > IpUtils.ipv4ToLong(state.endIp)) {
                _uiState.value = state.copy(errorMessage = "Start IP must be less than or equal to End IP")
                return
            }
            Pair(state.startIp, state.endIp)
        }

        val totalHosts = IpUtils.calculateHostCount(start, end)
        if (totalHosts > 2048) {
            _uiState.value = state.copy(errorMessage = "Range exceeds safe mobile limit (maximum 2,048 hosts at once)")
            return
        }

        _uiState.value = state.copy(errorMessage = null)
        scanStartTime = System.currentTimeMillis()

        val parsedPorts = PortUtils.parsePortString(state.portsInput)

        val target = ScanTarget(
            startIp = start,
            endIp = end,
            timeoutMs = currentPreferences.pingTimeoutMs,
            maxConcurrency = currentPreferences.concurrency,
            scanPorts = FetcherId.PORTS in state.enabledFetchers,
            portsToScan = if (parsedPorts.isEmpty()) listOf(80, 443, 22) else parsedPorts,
            enabledFetchers = state.enabledFetchers
        )

        viewModelScope.launch {
            scannerEngine.startScan(target).collect {
                // Hosts update reactive flows
            }
        }
    }

    fun setHostComment(ip: String, comment: String) {
        val updatedMap = _uiState.value.hostComments.toMutableMap()
        if (comment.isBlank()) {
            updatedMap.remove(ip)
        } else {
            updatedMap[ip] = comment
        }
        _uiState.value = _uiState.value.copy(hostComments = updatedMap)
    }

    fun reprobeHost(ip: String, onComplete: ((DiscoveredHost) -> Unit)? = null) {
        val state = _uiState.value
        val parsedPorts = PortUtils.parsePortString(state.portsInput)
        val target = ScanTarget(
            startIp = ip,
            endIp = ip,
            timeoutMs = currentPreferences.pingTimeoutMs,
            scanPorts = FetcherId.PORTS in state.enabledFetchers,
            portsToScan = if (parsedPorts.isEmpty()) listOf(80, 443, 22) else parsedPorts,
            enabledFetchers = state.enabledFetchers
        )

        viewModelScope.launch {
            val updated = scannerEngine.probeSingleHost(ip, target)
            val comment = state.hostComments[ip]
            val finalHost = if (comment != null) updated.copy(comment = comment) else updated
            onComplete?.invoke(finalHost)
        }
    }

    fun stopScan() {
        scannerEngine.stopScan()
    }

    fun clearResults() {
        stopScan()
        _uiState.value = _uiState.value.copy(searchQuery = "", errorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        scannerEngine.stopScan()
    }
}
