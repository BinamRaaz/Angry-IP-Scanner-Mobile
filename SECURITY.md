# Security & Privacy Policy

## 1. Google Play Store Compliance

- **App Title & Metadata:**
  - App Title: `Angry IP Scanner Mobile` (23 characters, <= 30 character Google Play limit).
  - No emojis, ALL CAPS, or promotional buzzwords ("Free", "#1", "Best").
- **Scoped Storage & Permissions:**
  - Zero broad storage permissions (`READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `MANAGE_EXTERNAL_STORAGE` are **NOT** requested).
  - Result export strictly uses Android's **Storage Access Framework (SAF)** via `ActivityResultContracts.CreateDocument()`.
  - Zero camera, microphone, SMS, contacts, or background location permissions requested.
- **Least-Privilege Network Permissions:**
  - `android.permission.INTERNET`: Required for socket ping and TCP port probes.
  - `android.permission.ACCESS_NETWORK_STATE`: Required for detecting network connection changes.
  - `android.permission.ACCESS_WIFI_STATE`: Required for discovering local Wi-Fi SSID and gateway details.

## 2. Privacy & Data Confidentiality

- **100% On-Device Operation:**
  - All scan results, history sessions, comments, and network statistics are stored locally on the user's device in an encrypted/sandboxed SQLite database via Android Jetpack Room.
  - No user data, network topology, IP addresses, or device identifiers are ever transmitted to any third-party server, cloud backend, or telemetry service.
- **Zero Third-Party Trackers:**
  - No ad SDKs, analytics beacons, or behavioral tracking libraries are bundled in the application.

## 3. Defense-in-Depth & Network Safety

- **Command Injection Prevention:**
  - Low-level system ping subprocess invocations (`TtlFetcher`) strictly validate inputs using `IpUtils.isValidIpv4()`.
  - Arguments are passed through `ProcessBuilder` string arrays rather than shell string execution.
- **Socket Resource Reclamation:**
  - All TCP port probe sockets use `setSoLinger(true, 0)` for immediate abortive connection close, preventing Linux kernel file descriptor exhaustion and `TIME_WAIT` socket state accumulation.
- **Network Security Configuration:**
  - `network_security_config.xml` is configured to permit local network cleartext traffic (RFC 1918) for probing local router web interfaces without compromising TLS standards for public web traffic.
