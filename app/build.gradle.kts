import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // AGP 9 compiles Kotlin itself ("built-in Kotlin"); the Compose compiler
    // plugin version tracks the Kotlin version bundled with AGP.
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Every ABI we actually ship a libopenconnect.so for.
val bundledAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
    .filter { abi -> file("src/main/jniLibs/$abi/libopenconnect.so").exists() }

android {
    namespace = "com.deepconnect.vpn"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.deepconnect.vpn"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "2.0.0"

        if (bundledAbis.isNotEmpty()) {
            ndk {
                abiFilters += bundledAbis
            }
        }
    }

    signingConfigs {
        create("release") {
            // keystore.properties (git-ignored) points at the release key. Without it the
            // release variant stays unsigned and cannot be installed on a device.
            val props = Properties()
            val f = rootProject.file("keystore.properties")
            if (f.exists()) {
                f.inputStream().use { props.load(it) }
                storeFile = rootProject.file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            } else {
                logger.warn("keystore.properties missing: release APK will be UNSIGNED")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.10.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
