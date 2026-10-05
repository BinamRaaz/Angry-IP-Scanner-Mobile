# Android Update Plan - Angry IP Scanner Mobile

## 1. Existing Architecture Overview
- **UI Framework:** Jetpack Compose with Material Design 3 (M3) components, adaptive layout with `NavigationRail` for wide screens / landscape and `NavigationBar` + `TopAppBar` for compact mobile screens.
- **State Management & MVVM:**
  - `ScannerViewModel`: Manages scanner engine lifecycle, input validation, host filtering, sorting, and host comments.
  - `HistoryViewModel`: Manages historical scan sessions using Room DAO.
  - `SettingsViewModel`: Manages scanner engine configurations and user preferences backed by Jetpack DataStore (`PreferencesDataStore`).
- **Core Engine & Fetchers:**
  - `ScannerEngine` / `DefaultScannerEngine`: Kotlin Coroutines flow-based subnet scanning with configurable concurrency (`CoroutineScope` with `Semaphore`), ping timeouts, and cancellation.
  - Modular fetchers (`HostFetcher` interface): `PingFetcher` (ICMP/isReachable/Socket Echo), `HostnameFetcher` (DNS reverse lookup), `TtlFetcher` (TTL packet header inspection), `VendorLookup` (OUI database prefix lookup), `PortFetcher` (TCP socket probe).
- **Data Persistence:**
  - **Room Database (`AppDatabase`):** `scan_sessions` and `scanned_hosts` tables with 1-to-many relationship (`ScanSessionWithHosts`), cascade deletion, and indexing on `sessionId`.
  - **DataStore (`scannerDataStore`):** Stores ping timeout, concurrency, default ports, ping method, auto-save toggle, alive-only toggle, and theme mode.
- **Exporting:** `ScanExportManager` supporting CSV, JSON, and TXT report generation with system share sheet integration (`ExportDialog`).

---

## 2. Existing Working Features
- Subnet IP Range scanning (e.g., `192.168.1.1` - `192.168.1.254`) and CIDR notation parsing (e.g., `192.168.1.0/24`).
- Local network detection via `AndroidNetworkInfoProvider` (SSID, Gateway, Subnet mask, IP).
- Fetchers: Ping, Hostname resolution, TTL extraction, MAC/Vendor lookup, Port probing.
- Live progress indicator with real-time host discovery updates.
- Host details sheet with port probing, MAC vendor info, and custom user notes/comments.
- Exporting scan results to CSV, JSON, and plain text.
- Adaptive navigation (Navigation Rail on tablets/foldables, Navigation Bar on mobile).
- Room database schema and DAO queries for history sessions and hosts.
- Unit tests for IP utilities, CIDR parsing, port parsing, vendor lookup, and export generation.

---

## 3. Existing Broken / Incomplete Areas Identified
1. **History:**
   - History auto-save in `ScannerViewModel` depends on `setScanHistoryRepository` being called from UI `LaunchedEffect` rather than reliable dependency injection or constructor initialization.
   - Opening historical scan results from `HistoryScreen` opens a dialog sheet instead of allowing full inspection or reloading the historical dataset into the main results view with numbering.
   - Deletion of individual history items requires smooth UI state refresh without race conditions.
2. **Settings:**
   - Setting changes need robust persistence across app restarts and immediate dynamic propagation into `ScannerEngine` instances without requiring app reload.
3. **Application Icon:**
   - Current icon uses generic Android vector placeholder (`ic_launcher_foreground.xml` / `ic_launcher_background.xml`) without a distinctive custom network scanner brand.
4. **Theme Selection:**
   - Theme switching currently checks DataStore preferences in `MainActivity`, but the theme switcher UI in `SettingsScreen` and appearance options need comprehensive verification across light/dark contrast and system theme dynamics.
5. **Scan Result Numbering:**
   - Results list and export tables do not currently display a continuous 1-based sequential `#` index column.
6. **Speed Test:**
   - Separate speed test feature is not yet present in the navigation destinations.

---

## 4. Files That Will Need Modification Across Phases
- **Phase 1 (History):**
  - `app/src/main/java/com/example/ui/history/HistoryScreen.kt`
  - `app/src/main/java/com/example/ui/history/HistoryViewModel.kt`
  - `app/src/main/java/com/example/ui/history/HistorySessionDetailDialog.kt`
  - `app/src/main/java/com/example/ui/scanner/ScannerViewModel.kt`
  - `app/src/main/java/com/example/data/repository/ScanHistoryRepository.kt`
- **Phase 2 (Settings):**
  - `app/src/main/java/com/example/ui/settings/SettingsScreen.kt`
  - `app/src/main/java/com/example/ui/settings/SettingsViewModel.kt`
  - `app/src/main/java/com/example/core/preferences/AppPreferences.kt`
  - `app/src/main/java/com/example/core/preferences/PreferencesRepository.kt`
- **Phase 3 (Logo & Icons):**
  - `app/src/main/res/drawable/ic_launcher_foreground.xml`
  - `app/src/main/res/drawable/ic_launcher_background.xml`
  - `app/src/main/res/mipmap-*/ic_launcher.xml`
  - `app/src/main/AndroidManifest.xml`
- **Phase 4 (Theme):**
  - `app/src/main/java/com/example/ui/theme/Theme.kt`
  - `app/src/main/java/com/example/ui/theme/Color.kt`
  - `app/src/main/java/com/example/ui/settings/SettingsScreen.kt`
  - `app/src/main/java/com/example/MainActivity.kt`
- **Phase 5 (Numbering):**
  - `app/src/main/java/com/example/ui/scanner/ScannerScreen.kt`
  - `app/src/main/java/com/example/ui/history/HistorySessionDetailDialog.kt`
  - `app/src/main/java/com/example/core/export/ScanExportManager.kt`
- **Phase 6 (Speed Test):**
  - `app/src/main/java/com/example/ui/speedtest/SpeedTestScreen.kt`
  - `app/src/main/java/com/example/ui/speedtest/SpeedTestViewModel.kt`
  - `app/src/main/java/com/example/core/speedtest/SpeedTestEngine.kt`
  - `app/src/main/java/com/example/ui/navigation/NavigationDestination.kt`
  - `app/src/main/java/com/example/ui/home/HomeScreen.kt`

---

## 5. Recommended Implementation Approach & Risks
- **Approach:** Strictly adhere to the phase-by-phase plan. Modify only files relevant to the active phase. Keep the core coroutine scanning engine isolated to prevent regressions.
- **Risks & Mitigations:**
  - *Risk:* DataStore / Room initialization timing during ViewModel creation.
    *Mitigation:* Use singleton `AppDatabase` and proper dependency propagation in ViewModel factory / constructor.
  - *Risk:* Fast.com scraping policy violation.
    *Mitigation:* Use an open, legitimate speed test backend (e.g. Cloudflare / Measurement Lab / HTTP range probes) with clear branding and disclosures.
