# Changelog

All notable changes to **Angry IP Scanner Mobile** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-10-05

### Added - Phase 16: Release Packaging & Final Documentation
- Successfully assembled and validated the debug release APK via `gradle :app:assembleDebug`.
- Finalized project documentation in `README.md` with full architectural layer map, feature breakdown, build/test commands, and upstream GPL-2.0 attribution.
- Synchronized `metadata.json` platform metadata with Android resource files (`app_name` = "Angry IP Scanner Mobile").
- Completed the entire 16-phase Android porting roadmap for Angry IP Scanner Mobile.

### Added - Phase 15: Unit & Robolectric Testing
- Built comprehensive local JVM test suite using **Robolectric** (`@Config(sdk = [36])`):
  - In-memory Room SQLite database creation, relational entity persistence with `@Transaction`, multiple-table joins, and cascade deletes (`AppDatabase` + `ScanHistoryDao`).
  - Repository Flow observation and deletion lifecycle (`DefaultScanHistoryRepository`).
  - End-to-end `ScanExportManager` byte stream verification with real Android Context.
  - Verified Android String and navigation resources directly from application context.
- Unit testing coverage across all core networking and domain engines (`ExampleUnitTest.kt`):
  - IP utilities: IPv4 validation, CIDR block expansion, IP to long conversions, subnet mask calculations, and malicious injection string rejection.
  - Port utilities: Parsing, default port sets, range expansions, and edge-case port bounding.
  - MAC/Vendor lookup: OUI table matching and hyphen/colon delimiter normalization.
  - Fetcher registry: Dynamic fetcher ordering, toggling, and dependency resolution.
  - Scanner engine state flow: Target progression, alive/dead accounting, and thread pool isolation.
  - Scanner ViewModel: Sort comparator orderings, alive-only filtering, live search queries, and lifecycle backgrounding.

### Added - Phase 14: Polish & Adaptive Layouts
- Implemented adaptive layouts for Window Size Classes using `BoxWithConstraints`:
  - **Expanded / Medium (Tablets, Foldables unfolded, Landscape)**: Material 3 `NavigationRail` side menu with centered `widthIn(max = 900.dp)` content container, preventing awkward stretching on large displays.
  - **Compact (Portrait Phones)**: Standard Material 3 `NavigationBar` bottom bar and top app bar.
- Added tactile Haptic Feedback across user interactions (`LocalHapticFeedback`):
  - Long press vibration feedback on Scan Start and Scan Stop actions.
  - Completion notification haptic pulse upon finishing a subnet sweep.
  - Subtle text-handle haptic feedback on pull-to-refresh and host card inspection taps.
- Accessibility & UI polish:
  - Ensured all interactive touch targets meet or exceed Material Design's 48x48dp standard.
  - Verified non-null TalkBack `contentDescription` on all navigation items, dialog buttons, and icons.

### Added - Phase 13: Privacy & Security Audit
- Audited app permissions in `AndroidManifest.xml` against Google Play Developer Program policies:
  - Verified zero broad storage permissions (`READ_EXTERNAL_STORAGE` and `WRITE_EXTERNAL_STORAGE` absent); all file export operations adhere to Scoped Storage using Android Storage Access Framework (SAF).
  - Confirmed least-privilege network permissions: `INTERNET`, `ACCESS_NETWORK_STATE`, and `ACCESS_WIFI_STATE`.
  - Zero sensitive or dangerous hardware permissions requested.
- Implemented `network_security_config.xml` to permit local RFC 1918 cleartext traffic for local router web interface inspection without compromising TLS for external domains.
- Hardened input validation: Added strict `IpUtils.isValidIpv4()` checks before `TtlFetcher` ping subprocess invocations, neutralizing command injection vulnerabilities.
- Verified zero hardcoded credentials, API keys, or user-tracking telemetry SDKs.
- Created `SECURITY.md` detailing security architecture, privacy principles, and Google Play compliance.
- Added security and input validation unit tests in `ExampleUnitTest.kt`.

### Added - Phase 12: Android Lifecycle & Background Behavior
- Built `NetworkConnectivityMonitor` utilizing `ConnectivityManager.NetworkCallback`:
  - Listens for live Wi-Fi/Ethernet disconnect and capability changes.
  - Automatically pauses/aborts active scans when network connection drops and alerts user.
- Integrated Android Lifecycle observation in `HomeScreen`:
  - Utilizes `DisposableEffect(lifecycleOwner)` with `LifecycleEventObserver` listening for `Lifecycle.Event.ON_STOP`.
  - Automatically pauses ongoing scans upon app backgrounding to conserve device battery and avoid OS process termination.
- Enhanced `BackHandler` on `ScannerScreen`:
  - Closes host inspection bottom sheets on back press.
  - Shows "Stop Active Scan?" confirmation dialog when back press occurs during an active scan, preventing accidental cancellation.
- Verified all flows use `collectAsStateWithLifecycle()` to cease collection while composables are in background.

### Added - Phase 11: Performance Optimization
- Implemented Coroutine Dispatcher tuning with `ioDispatcher.limitedParallelism(concurrency)`:
  - Constrains IO thread pool usage to user-configured limits (`4 - 64`), preventing thread starvation on Android devices.
- Implemented UI State Emission Batching (`BATCH_EMIT_INTERVAL_MS = 100L`):
  - Decoupled high-velocity probe results from Jetpack Compose UI emissions.
  - Eliminated UI recomposition storms by flushing snapshots at steady 100ms intervals instead of on every host probe, maintaining 60/120 FPS scrolling with zero dropped frames.
- Optimized Socket Resource Reclamation:
  - Configured `setSoLinger(true, 0)` in `PortFetcher` and `PingFetcher` for immediate abortive socket closure.
  - Prevents accumulation of socket file descriptors in `TIME_WAIT` state in the Linux kernel during intense scans.
- Added pre-sized `ArrayList` allocation based on total target range to eliminate dynamic resizing overhead.

### Added - Phase 10: Configurable Settings & Preferences (DataStore)
- Implemented persistent preferences using modern Jetpack `DataStore Preferences`:
  - `ScannerPreferences`: Data model covering ping timeout, concurrency thread count, ping method, port timeout, default ports list, auto-save toggle, default alive filter, and app theme.
  - `DefaultPreferencesRepository`: Reactive data store repository exposing `preferencesFlow` and atomic suspend setters.
  - `PingMethod`: Auto (Smart Ping), ICMP Echo, Java isReachable, and TCP Port Echo methods.
  - `AppThemeMode`: System Default, Light Mode, and Dark Mode with dynamic reactive theme updates in `MainActivity`.
- Built rich `SettingsScreen`:
  - Engine & Concurrency section with live sliders and step formatting.
  - Ping method selector chips with explanatory descriptions.
  - Fetchers configuration toggles (Hostname, TTL, MAC/Vendor).
  - Port scanner configuration with preset chips (Common, Extended) and port timeout slider.
  - History & storage auto-save toggle and default filter preferences.
  - Appearance theme chips with immediate theme switching.
  - Factory reset action with safety confirmation dialog.
- Connected `PreferencesRepository` to `ScannerViewModel` to apply custom concurrency, timeouts, and auto-save preferences during scans.
- Added unit tests for preference data models, enums, and default configurations.

### Added - Phase 9: Scan History & Persistence (Room Database)
- Designed and built local persistence layer using Android Room with KSP:
  - `ScanSessionEntity`: id, timestamp, startIp, endIp, cidr, totalScanned, aliveCount, durationMs.
  - `ScannedHostEntity`: foreign-key linked to `ScanSessionEntity` with cascading delete, indices, and host metadata.
  - `ScanSessionWithHosts`: Room 1-to-many relation model.
  - `ScanHistoryDao`: Reactive queries with Kotlin `Flow`, suspend transactions, and session deletion.
  - `AppDatabase`: Singleton database with schema versioning.
  - `DefaultScanHistoryRepository`: Repository layer abstracting DAO from ViewModels with domain model conversions.
- Auto-saving completed scans: `ScannerViewModel` automatically persists scan sessions and discovered hosts to the database upon completion.
- Interactive `HistoryScreen`:
  - List past scan sessions with timestamp, target subnet/range, alive/total badges, and scan duration.
  - Tapping a session opens `HistorySessionDetailDialog` with full recorded hosts list.
  - One-tap "Re-scan" action loading previous target subnet directly into the Scanner and launching probe.
  - "Clear History" action with confirmation dialog.
  - Delete individual session action.
  - Export historical sessions directly to CSV, JSON, or TXT.
- Added comprehensive unit tests for `ScannedHostEntity` conversion, `ScanHistoryRepository` with mock DAO, and `ScannerViewModel.loadTargetRange`.

### Added - Phase 8: Data Export (SAF: CSV / JSON / TXT)
- Implemented `ScanExportManager`:
  - Standard spreadsheet-ready CSV export with Angry IP Scanner compatible headers and proper quote escaping.
  - Formatted JSON export with optional attributes and timestamp metadata.
  - Plain-text formatted report (TXT) with scan overview and per-host summaries.
  - Flexible `ExportConfig` controlling "alive hosts only" filtering and column toggles.
- Built `ExportDialog` using modern Android Storage Access Framework (`ActivityResultContracts.CreateDocument`):
  - Zero broad storage permissions (`READ/WRITE_EXTERNAL_STORAGE` strictly avoided).
  - Format selection chips (CSV, JSON, TXT).
  - Toggles for individual export attributes (Hostname, Ping, TTL, MAC, Vendor, Ports, Notes).
  - Asynchronous background stream writing with progress and success feedback.
- Integrated "Export" action button in `ScannerScreen` above results list when discovered hosts are present.
- Added comprehensive unit tests for CSV, JSON, TXT generation and alive-only filtering.

### Added - Phase 7: Host Actions & Details
- Implemented `HostDetailSheet` using Material 3 `ModalBottomSheet`:
  - Header with monospace IP, hostname, status badge, and close button.
  - Quick actions bar: Copy IP to clipboard, single-host Re-probe, and Share host details via Android `Intent.ACTION_SEND`.
  - Service launchers dynamically adapting to open ports:
    - HTTP (port 80) -> Browser Intent (`http://<ip>`)
    - HTTPS (port 443) -> Browser Intent (`https://<ip>`)
    - SSH (port 22) -> SSH Client Intent (`ssh://<ip>:22`)
  - Interactive property table with one-tap copy actions for IP, Hostname, and MAC address.
  - Custom note/comment input with immediate save and persistence in `ScannerViewModel`.
- Added `probeSingleHost` in `ScannerEngine` and `DefaultScannerEngine` to support real-time per-host re-probing.
- Enhanced search filter in `ScannerViewModel` to search across IP addresses, hostnames, and custom host notes.
- Added unit tests for host comment management.

### Added - Phase 6: Local Wi-Fi & Subnet Discovery
- Implemented `AndroidNetworkInfoProvider` querying Android `ConnectivityManager`, `LinkProperties`, and `WifiManager` for active network status.
- Added automatic detection of:
  - Local device IPv4 address
  - Network prefix length and subnet mask (via `IpUtils.prefixLengthToMask`)
  - Default network gateway IP
  - Active Wi-Fi SSID with graceful fallback for ungranted location permissions
  - Computed base CIDR notation and subnet host range boundaries
- Added interactive Local Network card on the scanner dashboard displaying SSID/network name, device IP, gateway, and mask.
- Added one-tap "Scan Subnet" button (`btn_scan_current_subnet`) automatically populating subnet boundaries and starting a scan immediately.
- Added unit tests for CIDR prefix to subnet mask conversion and `NetworkInfoProvider` integration with ViewModel.

### Added - Phase 5: TCP Port Scanning
- Implemented `PortUtils` with port string parsing (e.g. `22, 80, 443, 8080-8085`), port range expansion, and well-known service mapping (HTTP, HTTPS, SSH, FTP, DNS, SMB, MySQL, Postgres, Redis, RDP, MongoDB, etc.).
- Implemented `PortFetcher` performing safe non-hanging TCP socket connection probes on alive hosts.
- Connected `PortFetcher` to `FetcherRegistry` and `DefaultScannerEngine` with clean socket closure and configurable port timeout.
- Added Port Scanner configuration card in `SettingsScreen` with presets (Web & SSH, Extended Server & DB), custom port list entry, and timeout slider.
- Added target port inputs in the Columns customization dialog on the scanner dashboard.
- Added dynamic "Ports" column in the scanner results table showing discovered open ports per host.
- Added interactive Host Details dialog displaying complete host information, ping, hostname, TTL, MAC/vendor, and styled open ports service badges.
- Added unit tests for port string parsing, service name lookup, and `PortFetcher`.

### Added - Phase 4: Host Information & Modular Fetchers
- Implemented modular fetcher architecture adapted from Angry IP Scanner:
  - `HostFetcher` interface and `FetchContext` preventing redundant network queries across fetchers.
  - `FetcherRegistry` managing registered fetchers and dynamic selection.
  - `PingFetcher` measuring roundtrip response latency.
  - `HostnameFetcher` resolving reverse DNS via `InetAddress.canonicalHostName`.
  - `TtlFetcher` extracting packet Time-to-Live from ICMP probes.
  - `MacFetcher` parsing ARP tables where accessible, designed to respect Android 10+ privacy protections.
  - `VendorLookup` containing an embedded IEEE OUI manufacturer database (VMware, VirtualBox, Raspberry Pi, Apple, Cisco, Intel, Google, etc.).
- Added user-customizable columns dialog (`btn_customize_columns`) on the scanner screen.
- Dynamically rendered table headers and host row cells according to active fetchers.
- Updated `SettingsScreen` with dedicated "Modular Fetchers" configuration card.
- Updated `DiscoveredHost` model to support `ttl`, `macAddress`, and `vendor`.
- Added unit tests for `VendorLookup`, `FetcherRegistry`, and ViewModel fetcher toggling.

### Added - Phase 3: Modern Scanner UI
- Integrated Material 3 `PullToRefreshBox` enabling pull-to-refresh to initiate and re-run scans.
- Added live search/filter bar to filter discovered hosts dynamically by IP address or hostname.
- Added multi-column sorting chips:
  - Numerical IP sort (Ascending & Descending via 32-bit integer arithmetic)
  - Ping latency sort (Fastest responding hosts first)
  - Status sort (Alive hosts prioritized)
  - Hostname sort (Alphabetical)
- Built modern live progress card displaying:
  - Real-time animated status dot indicator
  - Calculated percentage string (e.g. `72%`) and progress fraction bar
  - Active target IP label
  - Alive and Dead host count indicators
- Designed empty states for initial launch ("Scanner Ready") and filtered search queries ("No hosts match").
- Enhanced `ModernHostResultRow` component:
  - Monospace high-contrast IP typography
  - Accessible `HostStatusBadge` with colored status dots
  - Color-coded latency indicators (<30ms primary, <100ms warning, slow/dead muted)
  - Hostname display with DNS iconography
- Added ViewModel unit tests for search filtering and sort configuration.

### Added - Phase 2: Basic IP Range Scanner
- Enhanced `IpUtils` with CIDR validation (`isValidCidr`), subnet calculation, and host boundary extraction (`parseCidr`).
- Implemented `DefaultScannerEngine` using Kotlin Coroutines and semaphore-based concurrency control (`maxConcurrency = 24`).
- Implemented multi-strategy probe combining high-speed TCP handshake on standard ports with `InetAddress.isReachable` and canonical hostname resolution.
- Added live scanning metrics in `ScanProgress` (`scannedCount`, `totalCount`, `aliveCount`, `deadCount`, `currentIp`, `progressFraction`).
- Added robust cancellation via `stopScan()` terminating active socket connections cleanly.
- Implemented `ScannerViewModel` with range/CIDR mode switching, input validation, safe host range ceiling (max 2,048 hosts), and alive-only filter.
- Updated `ScannerScreen` with start/end IP fields, CIDR block input, live progress indicator, alive/dead counters, and virtualized results table displaying IP, Status, Ping latency, and Hostname.
- Added unit tests covering CIDR parsing, network boundary calculations, and scanner engine initial state.

### Added - Phase 1: Android Project Foundation
- Established clean architecture packages separating `core`, `data`, and `ui` layers.
- Created core domain models: `HostStatus`, `DiscoveredHost`, `ScanTarget`, and `ScanProgress`.
- Defined non-UI decoupled contracts: `ScannerEngine` and `NetworkInfoProvider`.
- Implemented `IpUtils` for IPv4 address validation, 32-bit conversion, and safe IP range generation.
- Created `ScanSession` data model and `ScanRepository` with in-memory implementation.
- Built Jetpack Compose Material 3 UI architecture:
  - Custom brand color palette supporting Light and Dark themes with dynamic color integration.
  - `HomeScreen` featuring `TopAppBar` and M3 `NavigationBar`.
  - `ScannerScreen` with target IP input fields, network status header, and status badges.
  - `HistoryScreen` with empty state placeholder and `BackHandler`.
  - `SettingsScreen` with scanner toggles and `BackHandler`.
  - `AboutScreen` detailing upstream Angry IP Scanner attribution and GPL-2.0 notice with `BackHandler`.
- Added unit tests for `IpUtils` and updated Robolectric test for app branding.
- Added install-time network permissions (`INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`).

### Added - Phase 0: Project Audit & Android Porting Plan
- Completed audit of upstream Angry IP Scanner (`ipscan`) architecture, feeders, fetchers, and SWT GUI.
- Created `ANDROID_PORTING_PLAN.md` documenting reusable components, platform-specific adaptations, Android network constraints, and 16-phase implementation roadmap.
- Added upstream `LICENSE` (GNU General Public License v2.0).
- Added `NOTICE` detailing upstream copyright attribution (Anton Keks & contributors) and fork disclosure.
- Configured project identity and initial metadata for Angry IP Scanner Mobile.
