# ANDROID PORTING PLAN: Angry IP Scanner Mobile

**Project:** Angry IP Scanner Mobile (Native Android Fork)  
**Upstream Project:** Angry IP Scanner (`ipscan`) by Anton Keks / Angryziber  
**Upstream Repository:** https://github.com/angryip/ipscan  
**Upstream License:** GNU General Public License v2.0 (GPL-2.0)  
**Target Platform:** Android 7.0+ (API Level 24 to 36), Native Kotlin + Jetpack Compose  
**Document Status:** Phase 0 Completed Audit

---

## 1. Upstream Project Architecture Audit

### 1.1 Project Structure & Build System
* **Upstream Build System:** Gradle with Java 8/11/17 plugins, packaging desktop bundles for Linux (deb/rpm), macOS (dmg/app), and Windows (exe/portable zip).
* **Package Namespace:** `net.azib.ipscan`
  * `net.azib.ipscan.core`: Core scanning engine, thread pool dispatchers, scanning subjects, state listeners.
  * `net.azib.ipscan.feeders`: Generator abstractions (`Feeder`) yielding target IP addresses (`RangeFeeder`, `RandomFeeder`, `FileFeeder`, `NetmaskFeeder`).
  * `net.azib.ipscan.fetchers`: Host attribute extractors (`Fetcher`) collecting per-host data (`IPFetcher`, `PingFetcher`, `HostnameFetcher`, `PortsFetcher`, `MacFetcher`, `TTLFetcher`, `NetbiosFetcher`, `HttpFetcher`).
  * `net.azib.ipscan.exporters`: Formatters for file output (`TxtExporter`, `CsvExporter`, `XmlExporter`).
  * `net.azib.ipscan.config`: XML/properties-based persistence for preferences and favorites.
  * `net.azib.ipscan.gui`: Standard Widget Toolkit (SWT) desktop interface (`MainWindow`, SWT tables, menus, preference dialogs).

### 1.2 Upstream Concurrency & Threading Model
* **Thread Pool:** Uses a standard Java `ExecutorService` (FixedThreadPool / ThreadPoolExecutor) with user-configurable thread counts (default ~100 threads).
* **Work Item:** Each target IP forms a `ScanningSubject`. A scanning thread runs sequential fetchers on that single subject:
  1. Ping probe (`PingFetcher`) to determine if host is alive.
  2. If alive (or configured to scan dead hosts), execute remaining enabled fetchers (Hostname, MAC, Ports, etc.).
  3. Emit progress callback to GUI thread via SWT `Display.asyncExec()`.

### 1.3 Upstream Ping Mechanisms
Upstream supports multiple selectable ping implementations:
1. **Java ICMP (`InetAddress.isReachable`):** Platform-dependent; on POSIX systems without root privileges, `InetAddress.isReachable` falls back to TCP port 7 (echo) which often fails on firewalled modern devices.
2. **Raw ICMP Socket / JNI:** Requires administrative / root privileges or custom setuid binaries (`pinger`).
3. **Command-Line Ping:** Invokes external OS binary `ping -c 1 -W <timeout> <ip>`.
4. **TCP Port Probing:** Attempts TCP connection on configurable echo or common ports (e.g. 80, 443, 7, 139).

### 1.4 Upstream Port Scanning
* Opens standard Java blocking `Socket` connections to each requested port with a configurable timeout.
* Iterates sequentially or with micro-thread pools across specified port ranges or comma-separated lists.

---

## 2. Component Reusability & Adaptation Matrix

| Upstream Component | Status | Adaptation / Replacement Strategy for Android |
| :--- | :--- | :--- |
| **IP Math & Range Feeder** | **Reused / Adapted** | IP integer arithmetic, bitmask calculations, subnet boundary traversal, and CIDR parsing logic can be adapted directly into pure Kotlin core domain models without desktop dependencies. |
| **Feeder Architecture** | **Adapted** | Replace Java Iterators with Kotlin `Sequence<InetAddress>` and Coroutine `Flow<InetAddress>` for non-blocking lazy generation. |
| **Fetcher Interface** | **Adapted** | Convert `Fetcher` interface to Kotlin coroutine-based contract (`suspend fun fetch(...)`). |
| **Hostname Fetcher** | **Adapted** | Use Android's `InetAddress.getCanonicalHostName()` and `Dns.resolve()` with timeouts to prevent thread starvation. |
| **Ping Fetcher** | **Adapted / Multi-strategy** | Implement a three-tiered Android probe: (1) TCP handshake on standard ports (80, 443, 8080, 53, 22), (2) `InetAddress.isReachable()`, (3) native Linux ICMP `/system/bin/ping -c 1 -W 1` process execution. |
| **Port Scanner** | **Adapted** | Non-blocking NIO / Coroutine-based TCP socket connect with strict per-host concurrency limits to avoid file descriptor exhaustion (`EMFILE`). |
| **MAC Address Fetcher** | **Rewritten (Android Restricted)** | Modern Android (API 24-36) blocks raw access to `/proc/net/arp` and restricts `NetworkInterface.getHardwareAddress()` for remote hosts due to MAC address randomization and privacy sandboxing. Fallback: Parse ARP cache where readable, identify local interface MAC, or mark as restricted by Android OS. |
| **NetBIOS / Windows Info** | **Optional / Modular** | Pure UDP datagram probes (port 137) can be supported on local Wi-Fi without native binaries. |
| **Export Engine** | **Rewritten** | Replace desktop file writes with Android Storage Access Framework (SAF) (`ActivityResultContracts.CreateDocument`) for CSV, JSON, and TXT. |
| **SWT GUI** | **Completely Rewritten** | 100% replaced by Jetpack Compose Material 3 UI with virtualized `LazyColumn`, animated state transitions, and responsive multi-window / landscape support. |
| **Preferences** | **Rewritten** | Replace desktop XML config with Android Jetpack `DataStore` (Preferences DataStore) or Room database. |

---

## 3. Android Platform Constraints & Limitations

### 3.1 Network Stack & Permissions
1. **Normal Permissions (Manifest Only):**
   * `android.permission.INTERNET`: Required for socket creation, HTTP probes, and DNS resolution.
   * `android.permission.ACCESS_NETWORK_STATE`: Required to query active `NetworkCapabilities` (Wi-Fi, Cellular, VPN).
   * `android.permission.ACCESS_WIFI_STATE`: Required to inspect Wi-Fi link details, SSID, and gateway IP.
2. **Runtime Location Permissions (`ACCESS_FINE_LOCATION`):**
   * On Android 8.1+ (API 27+), reading the active Wi-Fi SSID / BSSID requires location permissions. The scanner must work seamlessly without location permission by displaying generic local IP / subnet details if permission is not granted.
3. **No Root / Raw Sockets:**
   * Standard Android apps cannot open raw `AF_INET / SOCK_RAW` ICMP sockets without root privileges. Probing must rely on unprivileged ICMP process calls (`/system/bin/ping`) or high-speed TCP SYN/connect probes.
4. **MAC Address Privacy Protections:**
   * Starting with Android 10 (API 29) and Android 11 (API 30), reading `/proc/net/arp` is restricted by SELinux policies for non-system apps. The UI must clearly explain this OS security limitation when MAC addresses cannot be inspected on remote devices.
5. **Socket File Descriptor Limits:**
   * Desktop Angry IP Scanner spawns hundreds of threads opening sockets simultaneously. On Android, socket descriptors are strictly constrained per-process (usually 1024 max open FDs including UI/system resources). Coroutine concurrency must be clamped to safe limits (default 24-48 concurrent probes) to prevent `java.net.SocketException: Too many open files`.

### 3.2 Concurrency & UI Thread Safety
* **Zero Main-Thread Network Operations:** `android.os.NetworkOnMainThreadException` is strictly enforced.
* **Coroutines vs. Thread Pools:** Kotlin Coroutines (`Dispatchers.IO`) combined with `Semaphore` permit-based throttling will replace heavy Java OS threads, reducing memory footprint by >85%.
* **Recomposition Throttling:** Rapid IP progress updates (e.g. scanning a `/24` subnet in 2 seconds) will flood the Compose recomposition queue if emitted individually. State updates must be batched using a small buffer/time throttle (e.g. 50-100ms) or `StateFlow` conflation.

### 3.3 Background Execution Restrictions
* Android will aggressively throttle or kill background tasks once the app is minimized if using standard threads.
* Scans should run bound to the `ViewModel` scope (surviving configuration changes like device rotation).
* If the user switches apps during a scan, long-running scans can optionally notify the user or pause cleanly rather than draining battery or triggering Android ANRs.

---

## 4. Proposed Clean Architecture for Android Fork

```
app/
 ├── core/
 │   ├── ip/                      # Pure IP math & address parsing
 │   │   ├── Ipv4Address.kt
 │   │   ├── Ipv4Range.kt
 │   │   └── CidrBlock.kt
 │   ├── scanner/                 # Scanner engine & concurrency control
 │   │   ├── ScannerEngine.kt
 │   │   ├── ScanTarget.kt
 │   │   ├── ScanProgress.kt
 │   │   └── ScanConfiguration.kt
 │   ├── fetchers/                # Modular host attributes extractors
 │   │   ├── HostFetcher.kt
 │   │   ├── PingFetcher.kt
 │   │   ├── HostnameFetcher.kt
 │   │   ├── PortFetcher.kt
 │   │   └── MacFetcher.kt
 │   └── network/                 # Android network state & discovery
 │       ├── AndroidNetworkDetector.kt
 │       └── SubnetInfo.kt
 │
 ├── data/
 │   ├── models/                  # Scan results, Host details, History
 │   │   ├── DiscoveredHost.kt
 │   │   ├── HostStatus.kt
 │   │   └── ScanSession.kt
 │   ├── export/                  # CSV, JSON, TXT exporters (SAF)
 │   │   └── ScanExporter.kt
 │   └── repository/              # Scan repository & settings repository
 │       └── ScannerRepository.kt
 │
 └── ui/
     ├── theme/                   # Material 3 Dynamic Theme
     ├── scanner/                 # Main scanning dashboard
     │   ├── ScannerViewModel.kt
     │   ├── ScannerScreen.kt
     │   └── components/
     ├── details/                 # Host detail view & quick actions
     ├── history/                 # Saved past scans
     └── settings/                # Scanner preferences & timeouts
```

---

## 5. Phase-by-Phase Roadmap

* **Phase 0:** Project audit, licensing verification, and porting plan (Current).
* **Phase 1:** Android project foundation, clean architecture scaffolding, theme setup, base navigation.
* **Phase 2:** Basic IP range scanner engine (IPv4 range parsing, CIDR validation, asynchronous probes).
* **Phase 3:** Modern scanner UI (Compose M3, responsive list, real-time counters, cancel/start controls).
* **Phase 4:** Host information fetchers (hostname resolution, ping RTT measurement, modular toggle).
* **Phase 5:** TCP port scanning (configurable common ports, custom port lists, latency measurement).
* **Phase 6:** Local Wi-Fi & subnet discovery (automatic subnet detection, gateway detection).
* **Phase 7:** Result interaction (host inspection sheet, copy IP, ping/open port actions, share).
* **Phase 8:** Export system (CSV, JSON, TXT generation via Storage Access Framework).
* **Phase 9:** Scan history & persistence.
* **Phase 10:** Comprehensive settings (concurrency, timeouts, ping methods, dark/light theme).
* **Phase 11:** Performance optimization (batching, memory efficiency, high-volume /24 and /16 testing).
* **Phase 12:** Android background lifecycle & rotation management.
* **Phase 13:** Security, privacy documentation, and zero-telemetry auditing.
* **Phase 14:** Visual polish, adaptive tablet/landscape layouts, Material 3 iconography.
* **Phase 15:** Unit, Robolectric, and integration testing.
* **Phase 16:** Production release readiness (ProGuard/R8, documentation, GPL compliance).
