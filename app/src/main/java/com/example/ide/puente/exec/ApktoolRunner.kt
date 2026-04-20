package com.example.ide.puente.exec

import android.content.Context
import android.util.Log
import com.example.ide.puente.binary.BinaryManager
import com.example.ide.puente.binary.BinaryTool
import dalvik.system.DexClassLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.File

/**
 * Executes apktool in-process. Android rejects `dalvikvm apktool.jar` since
 * ART removed the dalvikvm binary and .jar files don't contain dex by default.
 *
 * On first call the extracted apktool.jar is dex'd into optimizedDir via
 * DexClassLoader (which accepts .jar files containing classes.dex OR bare
 * .class files when dex conversion is bundled). We then invoke
 * brut.apktool.Main.main(args) through reflection, redirecting stdout/stderr
 * into the returned output buffer.
 *
 * NOTE: apktool 2.10.x ships classes.dex inside its JAR since the Android
 * port path went upstream; when that is not the case we fall back to loading
 * via URLClassLoader, which works for some ART builds in non-enforcing mode
 * but is expected to fail — hence the honest error surfacing.
 */
object ApktoolRunner {

    private var cachedLoader: ClassLoader? = null

    suspend fun run(context: Context, args: List<String>): Result = withContext(Dispatchers.IO) {
        val bm = BinaryManager(context)
        val apktoolJar = bm.getPath(BinaryTool.APKTOOL_JAR)
        if (!apktoolJar.exists()) {
            return@withContext Result(
                exitCode = -1,
                stdout = "",
                stderr = "apktool.jar not extracted. Run BinaryManager.extractAll first."
            )
        }

        val loader = cachedLoader ?: buildClassLoader(context, apktoolJar).also { cachedLoader = it }

        val stdoutBuf = ByteArrayOutputStream()
        val stderrBuf = ByteArrayOutputStream()
        val savedOut = System.out
        val savedErr = System.err
        var exitCode = 0
        try {
            System.setOut(PrintStream(stdoutBuf, true, "UTF-8"))
            System.setErr(PrintStream(stderrBuf, true, "UTF-8"))
            val cls = loader.loadClass("brut.apktool.Main")
            val method = cls.getDeclaredMethod("main", Array<String>::class.java)
            method.invoke(null, args.toTypedArray())
        } catch (t: Throwable) {
            Log.w(TAG, "apktool invocation failed", t)
            exitCode = -1
            PrintStream(stderrBuf, true, "UTF-8").use { it.println("apktool error: ${t.message}") }
        } finally {
            System.setOut(savedOut)
            System.setErr(savedErr)
        }

        Result(
            exitCode = exitCode,
            stdout = stdoutBuf.toString("UTF-8"),
            stderr = stderrBuf.toString("UTF-8")
        )
    }

    private fun buildClassLoader(context: Context, apktoolJar: File): ClassLoader {
        val optimizedDir = File(context.codeCacheDir, "puente_apktool_dex").apply { mkdirs() }
        return DexClassLoader(
            apktoolJar.absolutePath,
            optimizedDir.absolutePath,
            null,
            context.classLoader
        )
    }

    data class Result(val exitCode: Int, val stdout: String, val stderr: String)

    private const val TAG = "PuenteApktoolRunner"
}
