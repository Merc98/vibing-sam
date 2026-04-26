package com.example.ide.puente.binary

/**
 * Every binary / payload that Puente bootstraps into filesDir/bin on first launch.
 * Source files live under app/src/main/assets/bin/ (populated by Gradle download tasks).
 */
enum class BinaryTool(
    val assetName: String,
    val extractedName: String,
    val isExecutable: Boolean,
    val isXzCompressed: Boolean = false
) {
    /** apktool_<ver>.jar — loaded in-process via DexClassLoader, not exec'd. */
    APKTOOL_JAR("apktool.jar", "apktool.jar", isExecutable = false),

    /**
     * Frida gadget shared object. Ships XZ-compressed to stay under asset size
     * limits. BinaryManager decompresses to filesDir/bin/libfrida-gadget.so on
     * first use.
     */
    FRIDA_GADGET("frida-gadget.so.xz", "libfrida-gadget.so", isExecutable = false, isXzCompressed = true),

    /** SHA-256 manifest written by Gradle at build time. */
    MANIFEST("manifest.properties", "manifest.properties", isExecutable = false);
}
