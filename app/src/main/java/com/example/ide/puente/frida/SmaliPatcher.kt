package com.example.ide.puente.frida

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File

/**
 * Minimal smali-patching utility: finds the launcher Activity from the decoded
 * AndroidManifest.xml and injects a static System.loadLibrary("...") call at
 * the top of its onCreate() method.
 *
 * This is intentionally narrow-scope — it covers the single case needed for
 * the Frida gadget injection pipeline. For more intricate patches the UI will
 * let the user edit smali directly in the Editor screen.
 */
object SmaliPatcher {

    data class Result(val success: Boolean, val detail: String)

    fun findLauncherActivityClass(decodedRoot: File): String? {
        val manifest = File(decodedRoot, "AndroidManifest.xml")
        if (!manifest.exists()) return null

        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(manifest.inputStream(), "UTF-8")

        var packageName = ""
        var currentActivity: String? = null
        var inIntentFilter = false
        var sawMain = false
        var sawLauncher = false
        var launcher: String? = null

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "manifest" -> {
                        packageName = parser.getAttributeValue(null, "package") ?: ""
                    }
                    "activity", "activity-alias" -> {
                        currentActivity = parser.getAttributeValue(
                            "http://schemas.android.com/apk/res/android",
                            "name"
                        )
                        inIntentFilter = false
                        sawMain = false
                        sawLauncher = false
                    }
                    "intent-filter" -> inIntentFilter = true
                    "action" -> if (inIntentFilter) {
                        val n = parser.getAttributeValue(
                            "http://schemas.android.com/apk/res/android",
                            "name"
                        )
                        if (n == "android.intent.action.MAIN") sawMain = true
                    }
                    "category" -> if (inIntentFilter) {
                        val n = parser.getAttributeValue(
                            "http://schemas.android.com/apk/res/android",
                            "name"
                        )
                        if (n == "android.intent.category.LAUNCHER") sawLauncher = true
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                when (parser.name) {
                    "intent-filter" -> {
                        if (sawMain && sawLauncher && currentActivity != null) {
                            launcher = normalizeClass(packageName, currentActivity!!)
                        }
                        inIntentFilter = false
                    }
                    "activity", "activity-alias" -> {
                        currentActivity = null
                    }
                }
            }
            if (launcher != null) break
            eventType = parser.next()
        }
        return launcher
    }

    private fun normalizeClass(pkg: String, name: String): String {
        return when {
            name.startsWith(".") -> "$pkg$name"
            !name.contains(".") -> "$pkg.$name"
            else -> name
        }
    }

    fun injectLoadLibrary(decodedRoot: File, fqcn: String, libName: String): Result {
        val smaliPath = "smali/${fqcn.replace('.', '/')}.smali"
        val primary = File(decodedRoot, smaliPath)
        val candidates = mutableListOf(primary)
        // apktool may split into smali_classes2/, smali_classes3/ etc.
        for (i in 2..8) {
            candidates.add(File(decodedRoot, "smali_classes$i/${fqcn.replace('.', '/')}.smali"))
        }
        val smali = candidates.firstOrNull { it.exists() }
            ?: return Result(false, "smali not found for $fqcn (looked in ${candidates.size} roots)")

        val original = smali.readText()
        if (original.contains("\"$libName\"") && original.contains("loadLibrary")) {
            return Result(true, "${smali.name} already loads $libName — skipping injection")
        }

        val injection = """
            |    const-string v0, "$libName"
            |    invoke-static {v0}, Ljava/lang/System;->loadLibrary(Ljava/lang/String;)V
            |
        """.trimMargin()

        val onCreateRegex = Regex("""(\.method[^\n]+onCreate\(Landroid/os/Bundle;\)V\n(?:\s*\.locals \d+\n)?)""")
        val match = onCreateRegex.find(original)
            ?: return Result(false, "onCreate not found in ${smali.name}")

        val patched = original.substring(0, match.range.last + 1) +
                injection +
                original.substring(match.range.last + 1)

        smali.writeText(patched)
        return Result(true, "patched ${smali.name} (inserted ${injection.length} B)")
    }
}
