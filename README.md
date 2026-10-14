# BluetoothTerminal

A Bluetooth terminal for Android and iOS (Kotlin Multiplatform + Compose Multiplatform): scan for
devices, connect, and send and receive data. BLE first on both platforms; Bluetooth Classic (SPP)
is planned for Android only, because iOS has no public API for it.

## Architecture

![Multi-module architecture](art/module-architecture.svg)

- **`androidApp` / `iosApp`**: platform shells; they only start the app.
- **`:app`**: composition root with navigation, Koin setup and the iOS framework.
- **`:feature:terminal`**: one feature module for one user goal (connect and talk) with two screens,
  Devices (scan, connect) and Terminal (send, receive), and its own data / domain / presentation
  layers. Modules are cut per feature, not per screen; independent features (e.g. macros, history)
  will get their own modules.
- **`:bluetooth`**: reusable KMP BLE library (Android `BluetoothGatt`, iOS CoreBluetooth) with no UI,
  no DI and no app types, so it can be used in other projects.
- **`:systemdesign`**: light and dark theme.

---

This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/app](./app/src) is the composition root shared by the Android and iOS apps.
  It contains several subfolders:
  - [commonMain](./app/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./app/src/iosMain/kotlin) folder would be the right place for such calls.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :app:testAndroidHostTest`
- iOS tests: `./gradlew :app:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
### Releasing the Android app

1. Create an upload key once (keep the file and passwords safe, outside git; an absolute
   `storeFile` path lets the keystore live outside the repo):
   `keytool -genkeypair -v -keystore keystore/upload-keystore.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000`
2. Copy `keystore.properties.example` to `keystore.properties` and fill in the passwords.
   On CI, set `ANDROID_KEYSTORE_FILE`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD` instead.
3. Build the bundle for Google Play: `./gradlew :androidApp:bundleRelease`
   (output: `androidApp/build/outputs/bundle/release/androidApp-release.aab`).
