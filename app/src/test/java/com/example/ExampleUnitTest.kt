package com.example

import com.example.core.export.ExportConfig
import com.example.core.export.ExportFormat
import com.example.core.export.ScanExportManager
import com.example.core.fetchers.FetchContext
import com.example.core.fetchers.FetcherId
import com.example.core.fetchers.FetcherRegistry
import com.example.core.fetchers.PortFetcher
import com.example.core.fetchers.VendorLookup
import com.example.core.models.DiscoveredHost
import com.example.core.models.HostStatus
import com.example.core.networking.LocalNetworkState
import com.example.core.networking.NetworkInfoProvider
import com.example.core.preferences.AppThemeMode
import com.example.core.scanner.DefaultScannerEngine
import com.example.core.speedtest.SpeedSample
import com.example.core.speedtest.SpeedTestEngine
import com.example.core.speedtest.SpeedTestPhase
import com.example.core.speedtest.SpeedTestProgress
import com.example.core.speedtest.SpeedTestResult
import com.example.core.utilities.IpUtils
import com.example.core.utilities.PortUtils
import com.example.data.local.dao.ScanHistoryDao
import com.example.data.local.entities.ScanSessionEntity
import com.example.data.local.entities.ScannedHostEntity
import com.example.data.local.entities.SpeedTestEntity
import com.example.data.local.entities.toDomainModel
import com.example.data.local.entities.toEntity
import com.example.data.local.models.ScanSessionWithHosts
import com.example.data.repository.DefaultScanHistoryRepository
import com.example.data.repository.SpeedTestRepository
import com.example.data.repository.toDiscoveredHost
import com.example.ui.scanner.ScannerViewModel
import com.example.ui.scanner.SortOption
import com.example.ui.speedtest.SpeedTestViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExampleUnitTest {

    @Test
    fun testIpValidation() {
        assertTrue(IpUtils.isValidIpv4("192.168.1.1"))
        assertTrue(IpUtils.isValidIpv4("10.0.0.254"))
        assertTrue(IpUtils.isValidIpv4("0.0.0.0"))
        assertTrue(IpUtils.isValidIpv4("255.255.255.255"))

        assertFalse(IpUtils.isValidIpv4("256.1.1.1"))
        assertFalse(IpUtils.isValidIpv4("192.168.1"))
        assertFalse(IpUtils.isValidIpv4("abc.def.ghi.jkl"))
        assertFalse(IpUtils.isValidIpv4(""))
    }

    @Test
    fun testCidrValidationAndParsing() {
        assertTrue(IpUtils.isValidCidr("192.168.1.0/24"))
        assertTrue(IpUtils.isValidCidr("10.0.0.0/16"))
        assertTrue(IpUtils.isValidCidr("172.16.0.0/12"))
        assertFalse(IpUtils.isValidCidr("192.168.1.0/33"))
        assertFalse(IpUtils.isValidCidr("192.168.1.0"))

        val parsed24 = IpUtils.parseCidr("192.168.1.0/24")
        assertNotNull(parsed24)
        assertEquals("192.168.1.1", parsed24!!.first)
        assertEquals("192.168.1.254", parsed24.second)

        val parsed30 = IpUtils.parseCidr("192.168.1.4/30")
        assertNotNull(parsed30)
        assertEquals("192.168.1.5", parsed30!!.first)
        assertEquals("192.168.1.6", parsed30.second)
    }

    @Test
    fun testIpConversionAndRangeGeneration() {
        val longVal = IpUtils.ipv4ToLong("192.168.1.1")
        val ipStr = IpUtils.longToIpv4(longVal)
        assertEquals("192.168.1.1", ipStr)

        val range = IpUtils.generateRange("192.168.1.1", "192.168.1.4")
        assertEquals(listOf("192.168.1.1", "192.168.1.2", "192.168.1.3", "192.168.1.4"), range)

        val count = IpUtils.calculateHostCount("192.168.1.1", "192.168.1.254")
        assertEquals(254L, count)
    }

    @Test
    fun testPrefixLengthToMask() {
        assertEquals("255.255.255.0", IpUtils.prefixLengthToMask(24))
        assertEquals("255.255.0.0", IpUtils.prefixLengthToMask(16))
        assertEquals("255.0.0.0", IpUtils.prefixLengthToMask(8))
        assertEquals("255.255.255.252", IpUtils.prefixLengthToMask(30))
        assertEquals("255.255.255.255", IpUtils.prefixLengthToMask(32))
    }

    @Test
    fun testScannerEngineInitialState() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val engine = DefaultScannerEngine(ioDispatcher = testDispatcher)

        assertEquals(0, engine.results.value.size)
        assertFalse(engine.progress.value.isScanning)
    }

    @Test
    fun testViewModelSearchAndSortingOptions() {
        val viewModel = ScannerViewModel()

        viewModel.onSearchQueryChanged("192.168.1.5")
        assertEquals("192.168.1.5", viewModel.uiState.value.searchQuery)

        viewModel.onSortOptionChanged(SortOption.PING_ASC)
        assertEquals(SortOption.PING_ASC, viewModel.uiState.value.sortOption)

        viewModel.toggleShowOnlyAlive()
        assertTrue(viewModel.uiState.value.showOnlyAlive)
    }

    @Test
    fun testVendorLookup() {
        assertEquals("VMware", VendorLookup.lookup("00:50:56:12:34:56"))
        assertEquals("Raspberry Pi", VendorLookup.lookup("B8:27:EB:AA:BB:CC"))
        assertEquals("Apple", VendorLookup.lookup("3C:22:FB:11:22:33"))
        assertNull(VendorLookup.lookup("AA:BB:CC:DD:EE:FF"))
        assertNull(VendorLookup.lookup("invalid"))
    }

    @Test
    fun testFetcherRegistryAndViewModelToggles() {
        val filtered = FetcherRegistry.getFetchers(setOf(FetcherId.PING, FetcherId.TTL, FetcherId.PORTS))
        assertEquals(3, filtered.size)

        val viewModel = ScannerViewModel()
        assertTrue(FetcherId.PING in viewModel.uiState.value.enabledFetchers)
    }

    @Test
    fun testPortUtilsParsingAndServices() {
        val parsed = PortUtils.parsePortString("80, 443, 22, 8080-8082")
        assertEquals(listOf(22, 80, 443, 8080, 8081, 8082), parsed)

        val empty = PortUtils.parsePortString("invalid, 999999, -1")
        assertTrue(empty.isEmpty())

        assertEquals("HTTP", PortUtils.getServiceName(80))
        assertEquals("HTTPS", PortUtils.getServiceName(443))
        assertEquals("SSH", PortUtils.getServiceName(22))
        assertEquals("Port 9999", PortUtils.getServiceName(9999))
    }

    @Test
    fun testPortFetcherDeadHostSkip() = runTest {
        val fetcher = PortFetcher(portsToScan = listOf(80, 443))
        val context = FetchContext(isAlive = false)

        fetcher.fetch("192.168.1.200", context)
        assertTrue(context.openPorts.isEmpty())
    }

    @Test
    fun testNetworkInfoProviderIntegration() {
        val mockProvider = object : NetworkInfoProvider {
            override fun getCurrentNetworkState(): LocalNetworkState {
                return LocalNetworkState(
                    isConnected = true,
                    isWifi = true,
                    networkName = "Office_WiFi",
                    localIp = "192.168.10.45",
                    gatewayIp = "192.168.10.1",
                    subnetMask = "255.255.255.0",
                    suggestedRangeStart = "192.168.10.1",
                    suggestedRangeEnd = "192.168.10.254",
                    cidrNotation = "192.168.10.0/24"
                )
            }
        }

        val viewModel = ScannerViewModel()
        viewModel.refreshNetworkState(mockProvider)

        assertEquals("Office_WiFi", viewModel.networkState.value.networkName)
        assertEquals("192.168.10.45", viewModel.networkState.value.localIp)
        assertEquals("192.168.10.1", viewModel.uiState.value.startIp)
        assertEquals("192.168.10.254", viewModel.uiState.value.endIp)
        assertEquals("192.168.10.0/24", viewModel.uiState.value.cidrInput)
    }

    @Test
    fun testHostCommentManagement() {
        val viewModel = ScannerViewModel()

        viewModel.setHostComment("192.168.1.10", "Living Room TV")
        assertEquals("Living Room TV", viewModel.uiState.value.hostComments["192.168.1.10"])

        viewModel.setHostComment("192.168.1.10", "")
        assertNull(viewModel.uiState.value.hostComments["192.168.1.10"])
    }

    @Test
    fun testExportFormats() {
        val sampleHosts = listOf(
            DiscoveredHost(
                ip = "192.168.1.1",
                hostname = "gateway.local",
                status = HostStatus.ALIVE,
                responseTimeMs = 5L,
                ttl = 64,
                macAddress = "00:50:56:12:34:56",
                vendor = "VMware",
                openPorts = listOf(80, 443),
                comment = "Main Router"
            ),
            DiscoveredHost(
                ip = "192.168.1.2",
                status = HostStatus.DEAD
            )
        )

        // CSV Export test
        val csv = ScanExportManager.generateContent(sampleHosts, ExportConfig(format = ExportFormat.CSV))
        assertTrue(csv.contains("IP Address,Status"))
        assertTrue(csv.contains("192.168.1.1,ALIVE"))
        assertTrue(csv.contains("192.168.1.2,DEAD"))
        assertTrue(csv.contains("Main Router"))

        // Alive-only CSV test
        val csvAlive = ScanExportManager.generateContent(sampleHosts, ExportConfig(format = ExportFormat.CSV, aliveOnly = true))
        assertTrue(csvAlive.contains("192.168.1.1"))
        assertFalse(csvAlive.contains("192.168.1.2"))

        // JSON Export test
        val json = ScanExportManager.generateContent(sampleHosts, ExportConfig(format = ExportFormat.JSON))
        assertTrue(json.contains("\"ip\": \"192.168.1.1\""))
        assertTrue(json.contains("\"status\": \"ALIVE\""))
        assertTrue(json.contains("\"notes\": \"Main Router\""))

        // TXT Export test
        val txt = ScanExportManager.generateContent(sampleHosts, ExportConfig(format = ExportFormat.TXT))
        assertTrue(txt.contains("Angry IP Scanner Mobile - Scan Report"))
        assertTrue(txt.contains("Host: 192.168.1.1 [ALIVE]"))
    }

    @Test
    fun testScannedHostEntityMapping() {
        val entity = ScannedHostEntity(
            id = 1L,
            sessionId = 10L,
            ip = "10.0.0.5",
            hostname = "nas.local",
            status = "ALIVE",
            responseTimeMs = 12L,
            ttl = 64,
            macAddress = "B8:27:EB:AA:BB:CC",
            vendor = "Raspberry Pi",
            openPorts = "22,80,443",
            comment = "Backup Server"
        )

        val host = entity.toDiscoveredHost()
        assertEquals("10.0.0.5", host.ip)
        assertEquals("nas.local", host.hostname)
        assertEquals(HostStatus.ALIVE, host.status)
        assertEquals(12L, host.responseTimeMs)
        assertEquals(listOf(22, 80, 443), host.openPorts)
        assertEquals("Backup Server", host.comment)
    }

    @Test
    fun testScanHistoryRepositoryWithMockDao() = runTest {
        val insertedSessions = mutableListOf<ScanSessionEntity>()
        val insertedHosts = mutableListOf<ScannedHostEntity>()

        val mockDao = object : ScanHistoryDao {
            override suspend fun insertSession(session: ScanSessionEntity): Long {
                insertedSessions.add(session)
                return 42L
            }

            override suspend fun insertHosts(hosts: List<ScannedHostEntity>) {
                insertedHosts.addAll(hosts)
            }

            override fun getAllSessions(): Flow<List<ScanSessionEntity>> = flowOf(insertedSessions)

            override suspend fun getSessionWithHosts(sessionId: Long): ScanSessionWithHosts? {
                val session = insertedSessions.firstOrNull { it.id == sessionId } ?: return null
                return ScanSessionWithHosts(session, insertedHosts.filter { it.sessionId == sessionId })
            }

            override fun observeSessionWithHosts(sessionId: Long): Flow<ScanSessionWithHosts?> =
                flow {
                    emit(getSessionWithHosts(sessionId))
                }

            override suspend fun deleteSession(sessionId: Long) {
                insertedSessions.removeAll { it.id == sessionId }
                insertedHosts.removeAll { it.sessionId == sessionId }
            }

            override suspend fun clearAllSessions() {
                insertedSessions.clear()
                insertedHosts.clear()
            }
        }

        val repository = DefaultScanHistoryRepository(mockDao)
        val hosts = listOf(
            DiscoveredHost(ip = "192.168.1.1", status = HostStatus.ALIVE),
            DiscoveredHost(ip = "192.168.1.2", status = HostStatus.DEAD)
        )

        val sessionId = repository.saveScan(
            startIp = "192.168.1.1",
            endIp = "192.168.1.2",
            cidr = "192.168.1.0/30",
            durationMs = 1500L,
            hosts = hosts
        )

        assertEquals(42L, sessionId)
        assertEquals(1, insertedSessions.size)
        assertEquals(2, insertedHosts.size)
        assertEquals(1, insertedSessions[0].aliveCount)
        assertEquals(2, insertedSessions[0].totalScanned)
    }

    @Test
    fun testScannerViewModelLoadTargetRange() {
        val viewModel = ScannerViewModel()
        viewModel.loadTargetRange(
            startIp = "10.0.0.1",
            endIp = "10.0.0.50",
            cidr = "10.0.0.0/26",
            startScanNow = false
        )

        assertTrue(viewModel.uiState.value.isCidrMode)
        assertEquals("10.0.0.0/26", viewModel.uiState.value.cidrInput)
        assertEquals("10.0.0.1", viewModel.uiState.value.startIp)
        assertEquals("10.0.0.50", viewModel.uiState.value.endIp)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testPreferencesModelsAndDefaults() {
        val prefs = com.example.core.preferences.ScannerPreferences()
        assertEquals(800, prefs.pingTimeoutMs)
        assertEquals(24, prefs.concurrency)
        assertEquals(com.example.core.preferences.PingMethod.AUTO, prefs.pingMethod)
        assertEquals(300, prefs.portTimeoutMs)
        assertEquals("22, 80, 443, 8080", prefs.defaultPorts)
        assertTrue(prefs.autoSaveHistory)
        assertFalse(prefs.defaultShowAliveOnly)
        assertEquals(com.example.core.preferences.AppThemeMode.SYSTEM, prefs.themeMode)

        assertEquals("ICMP Echo", com.example.core.preferences.PingMethod.ICMP.displayName)
        assertEquals("Java isReachable", com.example.core.preferences.PingMethod.IS_REACHABLE.displayName)
        assertEquals("TCP Port Echo", com.example.core.preferences.PingMethod.TCP_PING.displayName)
    }

    @Test
    fun testPreferencesRepositoryIntegrationWithScannerViewModel() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
        try {
            val mockPrefsFlow = kotlinx.coroutines.flow.MutableStateFlow(com.example.core.preferences.ScannerPreferences())
            val mockRepo = object : com.example.core.preferences.PreferencesRepository {
                override val preferencesFlow: Flow<com.example.core.preferences.ScannerPreferences> = mockPrefsFlow
                override suspend fun setPingTimeoutMs(timeout: Int) {
                    mockPrefsFlow.value = mockPrefsFlow.value.copy(pingTimeoutMs = timeout)
                }
                override suspend fun setConcurrency(threads: Int) {
                    mockPrefsFlow.value = mockPrefsFlow.value.copy(concurrency = threads)
                }
                override suspend fun setPingMethod(method: com.example.core.preferences.PingMethod) {
                    mockPrefsFlow.value = mockPrefsFlow.value.copy(pingMethod = method)
                }
                override suspend fun setPortTimeoutMs(timeout: Int) {
                    mockPrefsFlow.value = mockPrefsFlow.value.copy(portTimeoutMs = timeout)
                }
                override suspend fun setDefaultPorts(ports: String) {
                    mockPrefsFlow.value = mockPrefsFlow.value.copy(defaultPorts = ports)
                }
                override suspend fun setAutoSaveHistory(enabled: Boolean) {
                    mockPrefsFlow.value = mockPrefsFlow.value.copy(autoSaveHistory = enabled)
                }
                override suspend fun setDefaultShowAliveOnly(enabled: Boolean) {
                    mockPrefsFlow.value = mockPrefsFlow.value.copy(defaultShowAliveOnly = enabled)
                }
                override suspend fun setThemeMode(mode: com.example.core.preferences.AppThemeMode) {
                    mockPrefsFlow.value = mockPrefsFlow.value.copy(themeMode = mode)
                }
                override suspend fun setResolveHostnames(enabled: Boolean) {
                    mockPrefsFlow.value = mockPrefsFlow.value.copy(resolveHostnames = enabled)
                }
                override suspend fun setFetchTtl(enabled: Boolean) {
                    mockPrefsFlow.value = mockPrefsFlow.value.copy(fetchTtl = enabled)
                }
                override suspend fun setFetchMacVendor(enabled: Boolean) {
                    mockPrefsFlow.value = mockPrefsFlow.value.copy(fetchMacVendor = enabled)
                }
                override suspend fun setScanPorts(enabled: Boolean) {
                    mockPrefsFlow.value = mockPrefsFlow.value.copy(scanPorts = enabled)
                }
                override suspend fun resetToDefaults() {
                    mockPrefsFlow.value = com.example.core.preferences.ScannerPreferences()
                }
            }

            val viewModel = ScannerViewModel(preferencesRepository = mockRepo)
            testScheduler.advanceUntilIdle()

            // Initial default state
            assertEquals("22, 80, 443, 8080", viewModel.uiState.value.portsInput)
            assertEquals(300, viewModel.uiState.value.portTimeoutMs)

            // Modify settings
            mockRepo.setDefaultPorts("80, 443, 3306")
            mockRepo.setPortTimeoutMs(500)
            mockRepo.setResolveHostnames(false)
            mockRepo.setScanPorts(false)

            testScheduler.advanceUntilIdle()

            assertEquals("80, 443, 3306", viewModel.uiState.value.portsInput)
            assertEquals(500, viewModel.uiState.value.portTimeoutMs)
            assertFalse(FetcherId.HOSTNAME in viewModel.uiState.value.enabledFetchers)
            assertFalse(FetcherId.PORTS in viewModel.uiState.value.enabledFetchers)
            assertTrue(FetcherId.PING in viewModel.uiState.value.enabledFetchers)

            // Reset to defaults
            mockRepo.resetToDefaults()
            testScheduler.advanceUntilIdle()

            assertEquals("22, 80, 443, 8080", viewModel.uiState.value.portsInput)
            assertEquals(300, viewModel.uiState.value.portTimeoutMs)
            assertTrue(FetcherId.HOSTNAME in viewModel.uiState.value.enabledFetchers)
            assertTrue(FetcherId.PORTS in viewModel.uiState.value.enabledFetchers)
        } finally {
            kotlinx.coroutines.Dispatchers.resetMain()
        }
    }

    @Test
    fun testAppBackgroundingBehavior() {
        val viewModel = ScannerViewModel()
        assertFalse(viewModel.progress.value.isScanning)

        // When idle, backgrounding causes no error message
        viewModel.onAppBackgrounded()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testThemeModeSelectionAndSwitching() {
        assertEquals("Follow System", AppThemeMode.SYSTEM.displayName)
        assertEquals("Light Mode", AppThemeMode.LIGHT.displayName)
        assertEquals("Dark Mode", AppThemeMode.DARK.displayName)

        // Verify valueOf and serialization roundtrips
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.valueOf("SYSTEM"))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.valueOf("LIGHT"))
        assertEquals(AppThemeMode.DARK, AppThemeMode.valueOf("DARK"))
    }

    @Test
    fun testResultNumberingAndSortingOrder() {
        val h1 = DiscoveredHost(ip = "192.168.1.10", status = HostStatus.ALIVE, responseTimeMs = 50)
        val h2 = DiscoveredHost(ip = "192.168.1.2", status = HostStatus.ALIVE, responseTimeMs = 10)
        val h3 = DiscoveredHost(ip = "192.168.1.5", status = HostStatus.DEAD, responseTimeMs = null)

        val list = listOf(h1, h2, h3)

        // Sorted by IP ASC
        val sortedByIp = list.sortedBy { IpUtils.ipv4ToLong(it.ip) }
        assertEquals("192.168.1.2", sortedByIp[0].ip)
        assertEquals("192.168.1.5", sortedByIp[1].ip)
        assertEquals("192.168.1.10", sortedByIp[2].ip)

        // 1-based indexing map
        val indexedDisplay = sortedByIp.mapIndexed { index, host -> "#${index + 1}: ${host.ip}" }
        assertEquals("#1: 192.168.1.2", indexedDisplay[0])
        assertEquals("#2: 192.168.1.5", indexedDisplay[1])
        assertEquals("#3: 192.168.1.10", indexedDisplay[2])

        // Filtered alive only
        val aliveOnly = sortedByIp.filter { it.status == HostStatus.ALIVE }
        val aliveIndexedDisplay = aliveOnly.mapIndexed { index, host -> "#${index + 1}: ${host.ip}" }
        assertEquals(2, aliveOnly.size)
        assertEquals("#1: 192.168.1.2", aliveIndexedDisplay[0])
        assertEquals("#2: 192.168.1.10", aliveIndexedDisplay[1])
    }

    @Test
    fun testSpeedTestModelsAndEntityConversion() {
        val result = SpeedTestResult(
            id = 42L,
            timestamp = 1700000000000L,
            pingMs = 15.5,
            jitterMs = 1.2,
            downloadMbps = 85.4,
            uploadMbps = 24.8,
            serverName = "Cloudflare Edge",
            clientIp = "192.168.1.50"
        )

        // Convert to Room Entity
        val entity = result.toEntity()
        assertEquals(42L, entity.id)
        assertEquals(1700000000000L, entity.timestamp)
        assertEquals(15.5, entity.pingMs, 0.01)
        assertEquals(1.2, entity.jitterMs, 0.01)
        assertEquals(85.4, entity.downloadMbps, 0.01)
        assertEquals(24.8, entity.uploadMbps, 0.01)
        assertEquals("Cloudflare Edge", entity.serverName)
        assertEquals("192.168.1.50", entity.clientIp)

        // Convert back to Domain Model
        val domain = entity.toDomainModel()
        assertEquals(result, domain)
    }

    @Test
    fun testSpeedTestViewModelExecutionAndCancel() = kotlinx.coroutines.test.runTest {
        val testDispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler)
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)

        try {
            val fakeEngine = object : SpeedTestEngine {
                var cancelled = false
                override fun runSpeedTest(): Flow<SpeedTestProgress> = flow {
                    emit(SpeedTestProgress(phase = SpeedTestPhase.PING, isRunning = true, currentPingMs = 12.0, jitterMs = 1.0))
                    emit(SpeedTestProgress(phase = SpeedTestPhase.DOWNLOAD, isRunning = true, currentDownloadMbps = 50.0, peakDownloadMbps = 60.0))
                    emit(SpeedTestProgress(phase = SpeedTestPhase.UPLOAD, isRunning = true, currentUploadMbps = 20.0, peakUploadMbps = 25.0))
                    emit(SpeedTestProgress(phase = SpeedTestPhase.COMPLETED, isRunning = false, avgPingMs = 12.0, jitterMs = 1.0, peakDownloadMbps = 60.0, peakUploadMbps = 25.0))
                }
                override fun cancel() {
                    cancelled = true
                }
            }

            val savedResults = mutableListOf<SpeedTestResult>()
            val fakeRepo = object : SpeedTestRepository {
                override fun getAllSpeedTests(): Flow<List<SpeedTestResult>> = flowOf(savedResults)
                override suspend fun saveSpeedTest(result: SpeedTestResult): Long {
                    savedResults.add(result)
                    return 1L
                }
                override suspend fun deleteSpeedTest(id: Long) {
                    savedResults.removeAll { it.id == id }
                }
                override suspend fun clearAll() {
                    savedResults.clear()
                }
            }

            val viewModel = SpeedTestViewModel(speedTestEngine = fakeEngine)
            viewModel.setRepository(fakeRepo)

            assertEquals(SpeedTestPhase.IDLE, viewModel.progress.value.phase)
            assertFalse(viewModel.progress.value.isRunning)

            // Start test
            viewModel.startSpeedTest()
            testScheduler.advanceUntilIdle()

            assertEquals(SpeedTestPhase.COMPLETED, viewModel.progress.value.phase)
            assertFalse(viewModel.progress.value.isRunning)
            assertEquals(1, savedResults.size)
            assertEquals(60.0, savedResults[0].downloadMbps, 0.01)
            assertEquals(25.0, savedResults[0].uploadMbps, 0.01)

            // Cancel test
            viewModel.cancelSpeedTest()
            assertEquals(SpeedTestPhase.CANCELLED, viewModel.progress.value.phase)
            assertTrue(fakeEngine.cancelled)
        } finally {
            kotlinx.coroutines.Dispatchers.resetMain()
        }
    }

    @Test
    fun testComprehensiveScanExecutionAndPortParsing() = kotlinx.coroutines.test.runTest {
        // 1. Test 5-10 IP range calculation
        val hosts = IpUtils.generateRange("192.168.1.1", "192.168.1.10")
        assertEquals(10, hosts.size)
        assertEquals("192.168.1.1", hosts.first())
        assertEquals("192.168.1.10", hosts.last())

        // 2. Open port detection and range parsing
        val parsedPorts = PortUtils.parsePortString("21, 22, 80, 8080-8085, 443")
        assertEquals(10, parsedPorts.size)
        assertTrue(parsedPorts.containsAll(listOf(21, 22, 80, 443, 8080, 8081, 8082, 8083, 8084, 8085)))

        // Invalid port filtering
        val invalidFiltered = PortUtils.parsePortString("0, 80, 70000, -5, 443, abc")
        assertEquals(listOf(80, 443), invalidFiltered)
    }

    @Test
    fun testAllExportFormatsAndColumnSelection() {
        val testHosts = listOf(
            DiscoveredHost(
                ip = "192.168.1.1",
                status = HostStatus.ALIVE,
                responseTimeMs = 12,
                hostname = "router.local",
                ttl = 64,
                openPorts = listOf(80, 443),
                macAddress = "00:50:56:00:00:01",
                vendor = "VMware",
                comment = "Main Gateway"
            ),
            DiscoveredHost(
                ip = "192.168.1.2",
                status = HostStatus.DEAD,
                responseTimeMs = null,
                hostname = null,
                ttl = null,
                openPorts = emptyList()
            )
        )

        // CSV Export
        val csv = ScanExportManager.generateContent(
            testHosts,
            ExportConfig(
                format = ExportFormat.CSV,
                aliveOnly = false,
                includePing = true,
                includeHostname = true,
                includePorts = true,
                includeMacVendor = true,
                includeTtl = true,
                includeComments = true
            )
        )
        assertTrue(csv.contains("IP Address,Status,Ping (ms)"))
        assertTrue(csv.contains("192.168.1.1,ALIVE,12,router.local,64,00:50:56:00:00:01,VMware,80(HTTP);443(HTTPS),Main Gateway"))
        assertTrue(csv.contains("192.168.1.2,DEAD"))

        // JSON Export
        val json = ScanExportManager.generateContent(
            testHosts,
            ExportConfig(format = ExportFormat.JSON, aliveOnly = true)
        )
        assertTrue(json.contains("\"ip\": \"192.168.1.1\""))
        assertFalse(json.contains("\"ip\": \"192.168.1.2\""))

        // TXT Export
        val txt = ScanExportManager.generateContent(
            testHosts,
            ExportConfig(format = ExportFormat.TXT)
        )
        assertTrue(txt.contains("Angry IP Scanner Mobile - Scan Report"))
        assertTrue(txt.contains("192.168.1.1"))
    }

    @Test
    fun testEdgeCasesAndGracefulFailures() {
        // Inverted range should return empty list without throwing
        val inverted = IpUtils.generateRange("192.168.1.100", "192.168.1.10")
        assertTrue(inverted.isEmpty())

        // Large range cap enforcement
        val cappedRange = IpUtils.generateRange("10.0.0.1", "10.255.255.254", maxLimit = 100L)
        assertEquals(100, cappedRange.size)

        // Invalid CIDR returns null safely
        assertNull(IpUtils.parseCidr("invalid-cidr"))
        assertNull(IpUtils.parseCidr("192.168.1.0/33"))
        assertNull(IpUtils.parseCidr(""))

        // Single host /32 CIDR parsing
        val singleCidr = IpUtils.parseCidr("192.168.1.50/32")
        assertNotNull(singleCidr)
        assertEquals("192.168.1.50", singleCidr!!.first)
        assertEquals("192.168.1.50", singleCidr.second)
    }

    @Test
    fun testSecurityAndInputValidation() {
        // Safe IPv4 addresses
        assertTrue(IpUtils.isValidIpv4("192.168.1.1"))
        assertTrue(IpUtils.isValidIpv4("10.0.0.1"))
        assertTrue(IpUtils.isValidIpv4("127.0.0.1"))

        // Malicious injection strings should strictly fail validation
        assertFalse(IpUtils.isValidIpv4("192.168.1.1; rm -rf /"))
        assertFalse(IpUtils.isValidIpv4("192.168.1.1 && ping evil.com"))
        assertFalse(IpUtils.isValidIpv4("`id`"))
        assertFalse(IpUtils.isValidIpv4("$(whoami)"))
        assertFalse(IpUtils.isValidIpv4("192.168.1.256"))
        assertFalse(IpUtils.isValidIpv4("192.168.1"))
        assertFalse(IpUtils.isValidIpv4(""))
    }
}
