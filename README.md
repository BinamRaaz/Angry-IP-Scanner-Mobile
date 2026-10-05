# Angry IP Scanner Mobile

A modern, fast, native Android network and IP range scanner derived from the popular open-source [Angry IP Scanner](https://angryip.org/) desktop project.

## Overview

**Angry IP Scanner Mobile** brings the rapid host discovery, modular fetcher philosophy, and simplicity of Angry IP Scanner to Android devices, reimagined with a native Kotlin and Jetpack Compose Material 3 interface.

> **Notice:** This project is an independent native Android fork of the upstream [Angry IP Scanner (`ipscan`)](https://github.com/angryip/ipscan) by Anton Keks and contributors. It is licensed under the **GNU General Public License v2.0 (GPL-2.0)**. See [NOTICE](NOTICE) and [LICENSE](LICENSE) for details.

---

## Features

- **Blazing Fast Scanning:**
  - High-performance asynchronous ping and port discovery powered by Kotlin Coroutines with throttled thread pool dispatching (`limitedParallelism`).
  - Scans full `/24` subnets (254 hosts) across multiple TCP ports in just a few seconds.
- **Multiple Ping & Discovery Methods:**
  - **ICMP Echo (`/system/bin/ping`):** Subprocess-based ICMP ping for direct echo responses and TTL retrieval.
  - **TCP Port Echo:** Multi-socket probes across common network ports (e.g. 80, 443, 22, 8080) for firewalled subnets.
  - **Java isReachable:** Native standard socket fallback.
- **Modular Host Fetchers:**
  - **Ping Time & Status:** Alive/dead classification with millisecond latency.
  - **Hostname Resolver:** Reverse DNS lookup (`InetAddress.getCanonicalHostName`).
  - **TTL & OS Guessing:** Time To Live packet analysis.
  - **MAC Address & Vendor Identification:** Local ARP table resolution (`/proc/net/arp`) with embedded IEEE OUI vendor database.
  - **Multi-Port Scanner:** Port banner detection and well-known service mapping (HTTP, HTTPS, SSH, FTP, Telnet, SMB, RDP, DNS, RTSP, MySQL, Postgres, etc.).
- **Interactive Host Actions:**
  - Comprehensive host details sheet with copy-to-clipboard for IP, MAC, hostname, and open ports.
  - Quick launch shortcuts for Web Browser (`http://` and `https://`), Telnet, and SSH client apps.
  - Custom notes/comments saved per host with real-time editing.
- **History & Offline Persistence:**
  - Automatically records scan sessions in an on-device Jetpack Room SQLite database.
  - One-tap rescan of prior targets or CIDR blocks.
  - Search, filter, and delete session history.
- **Zero-Permission File Export:**
  - Export discovered hosts and telemetry to **CSV**, **JSON**, or **TXT** formats.
  - Compliant with Android Scoped Storage using the Storage Access Framework (SAF) `CreateDocument` contract.
- **Adaptive Layouts & Window Size Classes:**
  - Responsive Material 3 design supporting phones, foldables, and tablets.
  - Automatically transitions between Bottom `NavigationBar` on phones and side `NavigationRail` with centered max-width layouts on tablets and landscape screens.
  - Tactile Haptic Feedback on button taps, pull-to-refresh, host selections, and scan completion.
- **Privacy & Security First:**
  - 100% on-device operation with zero third-party telemetry, ads, or tracking SDKs.
  - Zero broad storage permissions (`READ/WRITE_EXTERNAL_STORAGE` omitted).
  - Shell command injection defense with strict IPv4 validation.
  - RFC 1918 local cleartext network security configuration.

---

## Architecture Overview

```
com.example
├── core/
│   ├── export/         # Export formats (CSV, JSON, TXT) and SAF engine
│   ├── fetchers/       # Modular host fetcher pipeline (Ping, Hostname, TTL, MAC, Ports)
│   ├── models/         # Domain models (DiscoveredHost, HostStatus, ScanProgress)
│   ├── networking/     # AndroidNetworkInfoProvider & NetworkConnectivityMonitor
│   ├── preferences/    # Jetpack DataStore preferences (SettingsRepository)
│   ├── scanner/        # DefaultScannerEngine coroutine concurrency dispatcher
│   └── utilities/      # IP math, CIDR calculator, and port service mapping
├── data/
│   ├── local/          # Jetpack Room database, entities, and DAOs
│   └── repository/     # ScanHistoryRepository implementation
└── ui/
    ├── about/          # About screen, upstream attribution, and GPL-2.0 license
    ├── components/     # HostDetailSheet, HostStatusBadge, and custom widgets
    ├── export/         # SAF Export dialog with format selection
    ├── history/        # Scan history viewer and rescan launcher
    ├── home/           # Adaptive HomeScreen with NavigationRail / NavigationBar
    ├── navigation/     # Navigation destinations and routes
    ├── scanner/        # Core ScannerScreen, controls, search, and ScannerViewModel
    ├── settings/       # Preferences screen for threads, timeouts, and ports
    └── theme/          # Material 3 ColorScheme, Typography, and Theme
```

---

## Key Differences from Desktop Upstream

| Feature | Desktop Upstream (`ipscan`) | Mobile Fork (`Angry IP Scanner Mobile`) |
| :--- | :--- | :--- |
| **Language & Runtime** | Java 8/11/17 on desktop JVM | Kotlin on Android Runtime (ART) |
| **User Interface** | Eclipse SWT (Desktop widgets) | Modern Jetpack Compose (Material Design 3) |
| **Concurrency** | Java `ThreadPoolExecutor` | Kotlin Coroutines & `Flow` with permit throttling |
| **Discovery** | Java ICMP / setuid pinger / raw sockets | High-speed multi-port TCP connect + ICMP `/system/bin/ping` |
| **Network Context** | Static desktop adapter | Real-time Android `ConnectivityManager` & Wi-Fi subnet detection |
| **File Export** | Java desktop File I/O | Android Storage Access Framework (SAF) |
| **Data Storage** | Desktop XML / properties file | Jetpack DataStore & local Room persistence |

---

## Development Roadmap & Completion

- [x] **Phase 0:** Project Audit & Porting Plan (`ANDROID_PORTING_PLAN.md`, `LICENSE`, `NOTICE`)
- [x] **Phase 1:** Android Project Foundation & Clean Architecture
- [x] **Phase 2:** Basic IP Range Scanner Engine
- [x] **Phase 3:** Modern Scanner UI (Material 3)
- [x] **Phase 4:** Host Information Fetchers
- [x] **Phase 5:** TCP Port Scanning
- [x] **Phase 6:** Local Wi-Fi & Subnet Discovery
- [x] **Phase 7:** Host Interaction & Details
- [x] **Phase 8:** CSV / JSON / TXT Export (SAF)
- [x] **Phase 9:** Scan History & Persistence
- [x] **Phase 10:** Configurable Settings & Preferences
- [x] **Phase 11:** Performance Optimization
- [x] **Phase 12:** Android Lifecycle & Background Behavior
- [x] **Phase 13:** Privacy & Security Audit
- [x] **Phase 14:** Polish & Adaptive Layouts
- [x] **Phase 15:** Unit & Robolectric Testing
- [x] **Phase 16:** Release Packaging & Final Documentation

---

## Building & Testing

The project is built with Gradle and targets Android SDK 36 (`minSdk 24`, `targetSdk 36`).

```bash
# Build the Android debug APK
gradle :app:assembleDebug

# Run unit and Robolectric tests
gradle :app:testDebugUnitTest
```

---

## License

Copyright (C) 2000-2023 Anton Keks and contributors.  
Copyright (C) 2026 Angry IP Scanner Mobile contributors.

Licensed under the **GNU General Public License v2.0**. See the [LICENSE](LICENSE) file for complete terms.
