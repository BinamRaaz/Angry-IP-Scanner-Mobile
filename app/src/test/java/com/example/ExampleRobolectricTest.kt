package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.export.ExportConfig
import com.example.core.export.ExportFormat
import com.example.core.export.ScanExportManager
import com.example.core.models.DiscoveredHost
import com.example.core.models.HostStatus
import com.example.data.local.AppDatabase
import com.example.data.local.entities.ScanSessionEntity
import com.example.data.local.entities.ScannedHostEntity
import com.example.data.repository.DefaultScanHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var historyRepository: DefaultScanHistoryRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        historyRepository = DefaultScanHistoryRepository(database.scanHistoryDao())
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun testStringResourcesFromContext() {
        val appName = context.getString(R.string.app_name)
        val shortTitle = context.getString(R.string.app_short_title)
        val startScan = context.getString(R.string.btn_start_scan)
        val stopScan = context.getString(R.string.btn_stop_scan)

        assertEquals("Angry IP Scanner Mobile", appName)
        assertEquals("Angry IP", shortTitle)
        assertEquals("Start Scan", startScan)
        assertEquals("Stop Scan", stopScan)
    }

    @Test
    fun testRoomDatabaseSessionAndHostsPersistence() = runTest {
        val dao = database.scanHistoryDao()

        val session = ScanSessionEntity(
            id = 1L,
            timestamp = 1700000000000L,
            startIp = "192.168.1.1",
            endIp = "192.168.1.10",
            cidr = "192.168.1.0/28",
            totalScanned = 10,
            aliveCount = 2,
            durationMs = 1200L
        )
        dao.insertSession(session)

        val host1 = ScannedHostEntity(
            sessionId = 1L,
            ip = "192.168.1.1",
            hostname = "router.lan",
            status = "ALIVE",
            responseTimeMs = 2L,
            ttl = 64,
            macAddress = "AA:BB:CC:DD:EE:01",
            vendor = "RouterTech",
            openPorts = "80, 443",
            comment = "Main Gateway"
        )
        val host2 = ScannedHostEntity(
            sessionId = 1L,
            ip = "192.168.1.2",
            hostname = null,
            status = "DEAD",
            responseTimeMs = null,
            ttl = null,
            macAddress = null,
            vendor = null,
            openPorts = "",
            comment = null
        )
        dao.insertHosts(listOf(host1, host2))

        val sessions = dao.getAllSessions().first()
        assertEquals(1, sessions.size)
        assertEquals("192.168.1.1", sessions[0].startIp)
        assertEquals("192.168.1.0/28", sessions[0].cidr)

        val sessionWithHosts = dao.getSessionWithHosts(1L)
        assertNotNull(sessionWithHosts)
        assertEquals(2, sessionWithHosts!!.hosts.size)

        val retrievedHost1 = sessionWithHosts.hosts.first { it.ip == "192.168.1.1" }
        assertEquals("ALIVE", retrievedHost1.status)
        assertEquals("router.lan", retrievedHost1.hostname)
        assertEquals("80, 443", retrievedHost1.openPorts)
        assertEquals("Main Gateway", retrievedHost1.comment)
        assertEquals(64, retrievedHost1.ttl)
    }

    @Test
    fun testScanHistoryRepositorySaveAndCascadeDelete() = runTest {
        val hosts = listOf(
            DiscoveredHost(
                ip = "10.0.0.1",
                hostname = "gateway.local",
                responseTimeMs = 1L,
                ttl = 64,
                status = HostStatus.ALIVE,
                openPorts = listOf(80),
                comment = "Core Switch"
            ),
            DiscoveredHost(
                ip = "10.0.0.2",
                status = HostStatus.DEAD
            )
        )

        val sessionId = historyRepository.saveScan(
            startIp = "10.0.0.1",
            endIp = "10.0.0.5",
            cidr = "10.0.0.0/29",
            durationMs = 850L,
            hosts = hosts
        )
        assertTrue(sessionId > 0)

        val historyList = historyRepository.getAllSessions().first()
        assertEquals(1, historyList.size)
        assertEquals("10.0.0.0/29", historyList[0].cidr)

        val sessionWithHosts = historyRepository.getSessionWithHosts(sessionId)
        assertNotNull(sessionWithHosts)
        assertEquals(2, sessionWithHosts!!.hosts.size)

        // Delete session and verify deletion
        historyRepository.deleteSession(sessionId)
        val afterDelete = historyRepository.getAllSessions().first()
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun testScanExportManagerExecution() {
        val hosts = listOf(
            DiscoveredHost(
                ip = "192.168.1.1",
                hostname = "router.home",
                responseTimeMs = 4L,
                ttl = 64,
                status = HostStatus.ALIVE,
                macAddress = "00:11:22:33:44:55",
                vendor = "Cisco",
                openPorts = listOf(80, 443),
                comment = "Primary Router"
            )
        )

        val config = ExportConfig(
            format = ExportFormat.CSV,
            aliveOnly = false
        )

        val csvResult = ScanExportManager.generateContent(hosts, config)

        assertTrue(csvResult.contains("IP Address"))
        assertTrue(csvResult.contains("192.168.1.1"))
        assertTrue(csvResult.contains("router.home"))
        assertTrue(csvResult.contains("Cisco"))
        assertTrue(csvResult.contains("80(HTTP)"))
        assertTrue(csvResult.contains("Primary Router"))
    }

    @Test
    fun testHistoryViewModelFullLifecycleAndClearAll() = runTest {
        // Empty state initially
        val initialSessions = historyRepository.getAllSessions().first()
        assertTrue(initialSessions.isEmpty())

        // Save two scans
        val hosts1 = listOf(
            DiscoveredHost(ip = "192.168.1.1", hostname = "router", status = HostStatus.ALIVE),
            DiscoveredHost(ip = "192.168.1.2", status = HostStatus.DEAD)
        )
        val id1 = historyRepository.saveScan("192.168.1.1", "192.168.1.2", "192.168.1.0/30", 500L, hosts1)

        val hosts2 = listOf(
            DiscoveredHost(ip = "10.0.0.1", hostname = "server", status = HostStatus.ALIVE)
        )
        val id2 = historyRepository.saveScan("10.0.0.1", "10.0.0.1", null, 250L, hosts2)

        val sessions = historyRepository.getAllSessions().first()
        assertEquals(2, sessions.size)

        // Select session 1 and verify detail
        val session1Detail = historyRepository.getSessionWithHosts(id1)
        assertNotNull(session1Detail)
        assertEquals(id1, session1Detail?.session?.id)
        assertEquals(2, session1Detail?.hosts?.size)

        // Delete individual session
        historyRepository.deleteSession(id1)
        val afterDeleteOne = historyRepository.getAllSessions().first()
        assertEquals(1, afterDeleteOne.size)
        assertEquals(id2, afterDeleteOne[0].id)

        // Clear all history
        historyRepository.clearHistory()
        val afterClearAll = historyRepository.getAllSessions().first()
        assertTrue(afterClearAll.isEmpty())
    }

    @Test
    fun testHistoryPersistenceAcrossDatabaseReopen() = runTest {
        // Step 1: Insert into database
        val hosts = listOf(
            DiscoveredHost(ip = "172.16.0.1", hostname = "firewall", status = HostStatus.ALIVE)
        )
        val id = historyRepository.saveScan("172.16.0.1", "172.16.0.1", "172.16.0.0/24", 300L, hosts)

        // Step 2: Query from repository
        val sessionWithHosts = historyRepository.getSessionWithHosts(id)
        assertNotNull(sessionWithHosts)
        assertEquals("172.16.0.0/24", sessionWithHosts?.session?.cidr)
        assertEquals(1, sessionWithHosts?.hosts?.size)
        assertEquals("172.16.0.1", sessionWithHosts?.hosts?.get(0)?.ip)
    }
}
