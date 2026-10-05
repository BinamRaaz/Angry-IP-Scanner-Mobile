package com.example.core.scanner

import com.example.core.fetchers.FetchContext
import com.example.core.fetchers.FetcherId
import com.example.core.fetchers.FetcherRegistry
import com.example.core.fetchers.PingFetcher
import com.example.core.models.DiscoveredHost
import com.example.core.models.HostStatus
import com.example.core.models.ScanProgress
import com.example.core.models.ScanTarget
import com.example.core.utilities.IpUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * High-performance, battery-conscious implementation of [ScannerEngine].
 *
 * Performance features:
 * - Limited Coroutine Dispatcher Parallelism: confines network threads to the configured
 *   concurrency bound, preventing thread exhaustion.
 * - Flow Emission Batching: flushes results and progress every 100ms instead of on every host
 *   arrival, preventing Jetpack Compose recomposition storms and maintaining 60/120 FPS.
 * - Socket Resource Reclamation: uses immediate soLinger reset to avoid OS file descriptor leaks.
 */
class DefaultScannerEngine(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ScannerEngine {

    companion object {
        private const val BATCH_EMIT_INTERVAL_MS = 100L
    }

    private val _progress = MutableStateFlow(ScanProgress())
    override val progress: StateFlow<ScanProgress> = _progress.asStateFlow()

    private val _results = MutableStateFlow<List<DiscoveredHost>>(emptyList())
    override val results: StateFlow<List<DiscoveredHost>> = _results.asStateFlow()

    private var activeScanJob: Job? = null

    override fun startScan(target: ScanTarget): Flow<DiscoveredHost> = callbackFlow {
        stopScan()

        val ipList = IpUtils.generateRange(target.startIp, target.endIp)
        val total = ipList.size

        if (total == 0) {
            _progress.value = ScanProgress(isScanning = false)
            close()
            return@callbackFlow
        }

        _results.value = emptyList()
        _progress.value = ScanProgress(
            scannedCount = 0,
            totalCount = total,
            aliveCount = 0,
            deadCount = 0,
            isScanning = true
        )

        val concurrency = target.maxConcurrency.coerceIn(4, 64)
        val scanDispatcher = ioDispatcher.limitedParallelism(concurrency)
        val semaphore = Semaphore(concurrency)

        val currentResults = ArrayList<DiscoveredHost>(total)
        var scannedCount = 0
        var aliveCount = 0
        var deadCount = 0
        var hasNewData = false
        var lastScannedIp: String? = null

        activeScanJob = CoroutineScope(scanDispatcher).launch {
            // UI Batch Flusher: batches UI state updates to prevent Compose recomposition storms
            val batchFlusherJob = launch {
                while (isActive) {
                    delay(BATCH_EMIT_INTERVAL_MS)
                    if (hasNewData) {
                        val snapshot: List<DiscoveredHost>
                        val scanProg: ScanProgress
                        synchronized(currentResults) {
                            hasNewData = false
                            snapshot = ArrayList(currentResults)
                            scanProg = ScanProgress(
                                scannedCount = scannedCount,
                                totalCount = total,
                                aliveCount = aliveCount,
                                deadCount = deadCount,
                                isScanning = scannedCount < total && isActive,
                                currentIp = lastScannedIp
                            )
                        }
                        _results.value = snapshot
                        _progress.value = scanProg
                    }
                }
            }

            val jobs = ipList.map { ip ->
                launch {
                    if (!isActive) return@launch

                    val host = semaphore.withPermit {
                        if (!isActive) return@withPermit null
                        lastScannedIp = ip
                        probeHost(ip, target)
                    } ?: return@launch

                    synchronized(currentResults) {
                        currentResults.add(host)
                        scannedCount++
                        if (host.status == HostStatus.ALIVE) {
                            aliveCount++
                        } else {
                            deadCount++
                        }
                        hasNewData = true
                    }

                    trySend(host)
                }
            }

            jobs.forEach { it.join() }
            batchFlusherJob.cancel()

            // Final atomic flush
            synchronized(currentResults) {
                _results.value = ArrayList(currentResults)
                _progress.value = ScanProgress(
                    scannedCount = scannedCount,
                    totalCount = total,
                    aliveCount = aliveCount,
                    deadCount = deadCount,
                    isScanning = false,
                    currentIp = null
                )
            }
            close()
        }

        awaitClose {
            stopScan()
        }
    }

    override fun stopScan() {
        activeScanJob?.cancel()
        activeScanJob = null
        _progress.value = _progress.value.copy(
            isScanning = false,
            currentIp = null
        )
    }

    override suspend fun probeSingleHost(ip: String, target: ScanTarget): DiscoveredHost {
        val updated = probeHost(ip, target)
        synchronized(_results) {
            val current = _results.value.toMutableList()
            val index = current.indexOfFirst { it.ip == ip }
            if (index >= 0) {
                current[index] = updated
                _results.value = current
            }
        }
        return updated
    }

    /**
     * Executes modular fetchers on [ip] using [target.enabledFetchers].
     */
    suspend fun probeHost(ip: String, target: ScanTarget): DiscoveredHost = withContext(ioDispatcher) {
        val context = FetchContext(timeoutMs = target.timeoutMs)
        val fetchers = FetcherRegistry.getFetchers(
            enabledIds = target.enabledFetchers,
            portsToScan = target.portsToScan,
            portTimeoutMs = (target.timeoutMs / 2).coerceIn(150, 1000)
        )

        // 1. Run PingFetcher if enabled, or default alive check
        val pingFetcher = fetchers.find { it.id == FetcherId.PING } ?: PingFetcher()
        pingFetcher.fetch(ip, context)

        // 2. If alive, execute secondary fetchers
        if (context.isAlive) {
            for (fetcher in fetchers) {
                if (fetcher.id != FetcherId.PING) {
                    fetcher.fetch(ip, context)
                }
            }
        }

        DiscoveredHost(
            ip = ip,
            hostname = context.hostname,
            status = if (context.isAlive) HostStatus.ALIVE else HostStatus.DEAD,
            responseTimeMs = context.responseTimeMs,
            ttl = context.ttl,
            macAddress = context.macAddress,
            vendor = context.vendor,
            openPorts = context.openPorts,
            scannedAt = System.currentTimeMillis()
        )
    }
}
