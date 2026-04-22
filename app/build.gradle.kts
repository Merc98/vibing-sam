import org.gradle.api.DefaultTask
import java.net.URL
import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.ide"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.ide"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1-puente"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    splits {
        abi {
            isEnable = false
        }
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/DEPENDENCIES"
            )
        }
        // Keep .so.xz payload uncompressed so extraction is fast
        jniLibs {
            useLegacyPackaging = false
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.3")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Architecture
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.navigation)
    implementation(libs.androidx.datastore)
    implementation(libs.kotlinx.coroutines.android)

    // Puente: reverse engineering platform
    implementation(libs.apksig)
    implementation(libs.commons.compress)
    implementation(libs.xz)
    implementation(libs.jadx.core)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// ─────────────────────────────────────────────────────────────────────────────
// Puente binary bootstrap: download apktool + frida-gadget into assets/bin/
// Runs before mergeAssets so they ship inside the APK.
// Files persisted in the repo? No — downloaded per build, cached on disk.
// ─────────────────────────────────────────────────────────────────────────────

val puenteBinDir = layout.projectDirectory.dir("src/main/assets/bin")

val apktoolVersion = "2.10.0"
val fridaVersion = "16.5.7"

abstract class DownloadAndCacheTask : DefaultTask() {
    @get:org.gradle.api.tasks.Input
    abstract val url: org.gradle.api.provider.Property<String>

    @get:org.gradle.api.tasks.OutputFile
    abstract val target: org.gradle.api.file.RegularFileProperty

    @org.gradle.api.tasks.TaskAction
    fun run() {
        val outFile = target.get().asFile
        if (outFile.exists() && outFile.length() > 0) {
            logger.lifecycle("Puente: cached ${outFile.name} (${outFile.length()} bytes)")
            return
        }
        outFile.parentFile.mkdirs()
        logger.lifecycle("Puente: downloading ${url.get()}")
        URL(url.get()).openStream().use { input ->
            outFile.outputStream().use { out -> input.copyTo(out) }
        }
        logger.lifecycle("Puente: saved ${outFile.name} (${outFile.length()} bytes)")
    }
}

val downloadApktool by tasks.registering(DownloadAndCacheTask::class) {
    url.set("https://github.com/iBotPeaches/Apktool/releases/download/v$apktoolVersion/apktool_$apktoolVersion.jar")
    target.set(puenteBinDir.file("apktool.jar"))
}

val downloadFridaGadget by tasks.registering(DownloadAndCacheTask::class) {
    url.set("https://github.com/frida/frida/releases/download/$fridaVersion/frida-gadget-$fridaVersion-android-arm64.so.xz")
    target.set(puenteBinDir.file("frida-gadget.so.xz"))
}

val writePuenteManifest by tasks.registering {
    dependsOn(downloadApktool, downloadFridaGadget)
    val manifestFile = puenteBinDir.file("manifest.properties").asFile
    outputs.file(manifestFile)
    doLast {
        fun sha256(f: java.io.File): String {
            val md = MessageDigest.getInstance("SHA-256")
            f.inputStream().use { input ->
                val buf = ByteArray(8192)
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    md.update(buf, 0, n)
                }
            }
            return md.digest().joinToString("") { "%02x".format(it) }
        }
        val apktool = puenteBinDir.file("apktool.jar").asFile
        val gadget = puenteBinDir.file("frida-gadget.so.xz").asFile
        manifestFile.writeText(
            """
            apktool.version=$apktoolVersion
            apktool.sha256=${sha256(apktool)}
            frida.version=$fridaVersion
            frida.gadget.sha256=${sha256(gadget)}
            """.trimIndent()
        )
    }
}

tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(writePuenteManifest)
}
