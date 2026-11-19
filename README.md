# BluetoothTerminal

A Bluetooth terminal for Android and iOS (Kotlin Multiplatform + Compose Multiplatform): scan for
devices, connect, and send and receive data. BLE first on both platforms; Bluetooth Classic (SPP)
is planned for Android only, because iOS has no public API for it.

## Screenshots

| Devices | Terminal | Settings |
|---|---|---|
| <img src="art/screenshots/devices.png" width="250" alt="Devices screen: nearby BLE devices with signal strength"> | <img src="art/screenshots/terminal.png" width="250" alt="Terminal screen: messages sent to and received from an ESP32 over Nordic UART"> | <img src="art/screenshots/settings.png" width="250" alt="Settings screen: services and characteristics, with the send target and listening choices"> |

Android, connected to an ESP32 running the Nordic UART Service. MAC addresses and other nearby devices are blurred.

## Features

| | Android | iOS |
|---|---|---|
| Bluetooth availability, permission request, "turn on Bluetooth" | ✅ | placeholder |
| Scan for BLE devices, stop / scan again | ✅ | placeholder |
| Connect, connection status (connecting, failed, lost), disconnect | ✅ | placeholder |
| Terminal: send text, see sent and received messages | ✅ | placeholder |
| Serial profile detection (Nordic UART, HM-10, FFF0, Microchip) | ✅ | placeholder |
| Settings: services and characteristics, choose where to send and what to listen to, read a value | ✅ | placeholder |

iOS builds and shows "Bluetooth not supported" until the CoreBluetooth implementation of the library lands.

## Architecture

![Modules and layers](art/module-architecture.svg)

### Modules

| Module | Role | Depends on |
|---|---|---|
| `androidApp` / `iosApp` | Platform shells: `MainActivity` + `Application` (starts Koin) / SwiftUI entry point | `:app` |
| `:app` | Composition root: `App()` (theme, `NavHost`), Koin setup (`initKoin`, app scope, `BluetoothSession`, platform `BluetoothClient`), iOS framework | everything |
| `:feature:terminal` | One feature for one user goal (connect and talk): Devices, Terminal and Settings screens | `:bluetooth`, `:systemdesign` |
| `:bluetooth` | Reusable KMP BLE library: no UI, no DI, no app types; only coroutines | — |
| `:systemdesign` | Shared theme (planned, empty for now) | — |

Modules are cut per **feature**, not per screen. Independent features (e.g. macros, history) will get
their own modules.

### Layers inside `:feature:terminal`

One package per screen, each with its own Clean Architecture layers, plus a shared `connection/`
package. Screen packages never import each other.

| Package | presentation | domain | data |
|---|---|---|---|
| `connection/` (shared) | — | `ConnectionStatus`, channel models, `ConnectionRepository`, `ChannelRepository`, `ObserveConnection`, `Disconnect` | `ConnectionRepositoryImpl`, `ChannelRepositoryImpl` |
| `devices/` | `DevicesScreen`, `DevicesViewModel`, `BluetoothActions` (expect/actual) | `Device`, `DeviceScan`, `DeviceRepository`, `ScanDevices`, `ConnectToDevice` | `DeviceRepositoryImpl` |
| `terminal/` | `TerminalScreen`, `TerminalViewModel` | `TerminalMessage`, `ObserveMessages`, `SendMessage` | — (uses `ChannelRepository`) |
| `settings/` | `SettingsScreen`, `SettingsViewModel` | `ServiceSettings`, `ObserveServiceSettings`, `SetSendTarget`, `SetListening`, `ReadCharacteristic` | — (uses `ChannelRepository`) |

- **presentation** depends on **domain** (use cases) only; **domain** is pure Kotlin with no `:bluetooth`
  types; only **data** uses the library.
- Each package has its own `internal` Koin module (`di/`). The feature exposes two public entry points:
  `terminalScreens()` (`navigation/`) and `terminalFeatureModule` (`di/`), which includes the package modules.

### Inside `:bluetooth`

| Source set | Contents |
|---|---|
| `commonMain` | `BluetoothClient` (adapter: state, scan, connect), `BleConnection` (one link: read, write, observe), `BluetoothSession` (holds the current link: Idle, Connecting, Connected, Failed, Lost), `model/`, `serial/` (`SerialProfiles`, `writeChunked`), `testing/` (fakes for other modules' tests) |
| `androidMain` | `AndroidBleClient`, `AndroidBleConnection` (`BluetoothGatt`), `GattQueue` (one GATT operation at a time) |
| `iosMain` | Placeholder client reporting `Unsupported`; CoreBluetooth implementation planned |

## Project structure

```
androidApp/          Android shell (MainActivity, Application)
iosApp/              iOS shell (Xcode project, SwiftUI)
app/                 composition root (App.kt, di/)
feature/terminal/    connection/, devices/, terminal/, settings/, navigation/, di/
bluetooth/           BLE library (commonMain, androidMain, iosMain)
systemdesign/        theme (planned)
art/                 README images and screenshots, app icon and store graphics
store/               Google Play listing texts and privacy policy
```

## Running the apps

- Android: `./gradlew :androidApp:installDebug` (a real phone: the emulator has no BLE devices nearby)
- iOS: open [/iosApp](./iosApp) in Xcode and run it

Minimum Android version: 12 (API 31), so only `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` are needed (no location).

## Running tests

```bash
./gradlew :bluetooth:testAndroidHostTest :feature:terminal:testAndroidHostTest :app:testAndroidHostTest
./gradlew :bluetooth:iosSimulatorArm64Test :feature:terminal:iosSimulatorArm64Test :app:iosSimulatorArm64Test
```

Tests use fakes, no hardware: `FakeBluetoothClient` / `FakeBleConnection` from `:bluetooth`, fake
repositories in the feature.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
### Releasing the Android app

Releases run on GitHub Actions: test on Google Play's internal track first, then promote that same
build to production.

1. Update `distribution/whatsnew/whatsnew-en-US` (Play release notes, ≤ 500 characters).
2. Tag and push: `git tag v1.2.0 && git push origin v1.2.0`. `release.yml` builds the signed
   APK and AAB (version 1.2.0, code 10200), creates the GitHub Release and uploads the AAB to
   **internal testing**.
3. Install it from the Play Store as an internal tester and test it.
4. **Actions → Promote to production → Run workflow**, version `1.2.0`. `promote.yml` moves that
   tested build to **production** (no rebuild); Google reviews it, then it goes live.

A fix after testing is a new tag (`v1.2.1`); Play never accepts the same version code twice.

Secrets (Settings → Secrets and variables → Actions): `ANDROID_KEYSTORE_BASE64`,
`ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_PASSWORD`, `PLAY_SERVICE_ACCOUNT_JSON`. Variables:
`PLAY_UPLOAD_ENABLED=true`, `PLAY_RELEASE_STATUS=completed`, optional `ANDROID_KEY_ALIAS`
(default `upload`).

Local release build: copy `keystore.properties.example` to `keystore.properties`, fill in the
upload key, then `./gradlew :androidApp:bundleRelease`.
