import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":app"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

// Release signing reads keystore.properties (git-ignored, see keystore.properties.example) or,
// on CI, the ANDROID_KEYSTORE_* environment variables. Without either, the release build is unsigned.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun signingValue(property: String, env: String): String? =
    keystoreProperties.getProperty(property) ?: System.getenv(env)

// Version from the release tag on CI: `-Pbt.versionName=1.2.3 -Pbt.versionCode=10203`
// (v1.2.3 → major·10000 + minor·100 + patch). Local builds without them are 1.0 / 1.
val releaseVersionName = findProperty("bt.versionName") as String?
val releaseVersionCode = (findProperty("bt.versionCode") as String?)?.toInt()

android {
    namespace = "com.majidbahmani.bluetoothterminal"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.majidbahmani.bluetoothterminal"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = releaseVersionCode ?: 1
        versionName = releaseVersionName ?: "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        val storeFile = signingValue("storeFile", "ANDROID_KEYSTORE_FILE")
        if (storeFile != null) {
            create("release") {
                this.storeFile = rootProject.file(storeFile)
                storePassword = signingValue("storePassword", "ANDROID_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "ANDROID_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "ANDROID_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}