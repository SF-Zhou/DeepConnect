// Top-level build file.
// AGP 9 compiles Kotlin itself ("built-in Kotlin"), so the kotlin-android plugin is
// no longer applied. The Compose compiler still has to be selected explicitly; its
// version must match the Kotlin version bundled with AGP 9.5 (Kotlin 2.2.10), so bump
// it together with the AGP version above and never on its own.
plugins {
    id("com.android.application") version "9.5.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}
