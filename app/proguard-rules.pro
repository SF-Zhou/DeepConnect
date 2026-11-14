# Keep the JNI-bound wrapper classes exactly as the native library expects them.
-keep class org.infradead.libopenconnect.** { *; }
-keepclassmembers class * extends org.infradead.libopenconnect.LibOpenConnect {
    public *;
    protected *;
}
-keepclasseswithmembernames class * {
    native <methods>;
}
