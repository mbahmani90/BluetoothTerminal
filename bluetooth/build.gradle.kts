import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Reusable BLE library: no UI, no DI, no app types. Only coroutines (doc 49).
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    // Every public declaration needs an explicit visibility: keeps the library API deliberate.
    explicitApi()

    iosArm64()
    iosSimulatorArm64()

    android {
        namespace = "com.majidbahmani.bluetooth"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            // api: Flow and StateFlow are part of the public API
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
