// Top-level build file.
// AGP 9 compiles Kotlin itself ("built-in Kotlin"), so the kotlin-android plugin is
// no longer applied. The Compose compiler still has to be selected explicitly; its
// version tracks the Kotlin version bundled with AGP 9.5 (Kotlin 2.2.10).
plugins {
    id("com.android.application") version "9.5.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}
