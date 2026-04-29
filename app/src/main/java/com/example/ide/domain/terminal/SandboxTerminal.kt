package com.example.ide.domain.terminal

import java.io.File

class SandboxTerminal(
    private val workspaceRoot: File
) {
    private var currentDir: File = workspaceRoot

    fun run(input: String): TerminalResult {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return TerminalResult(currentDir.path, "")

        val parts = trimmed.split(Regex("\\s+"))
        val command = parts.first()
        val args = parts.drop(1)

        return try {
            when (command) {
                "pwd" -> TerminalResult(currentDir.path, currentDir.path)
                "ls" -> TerminalResult(currentDir.path, list(args.firstOrNull()))
                "cd" -> changeDir(args.firstOrNull() ?: workspaceRoot.path)
                "cat" -> readFile(args.firstOrNull())
                "mkdir" -> mkdir(args.firstOrNull())
                "touch" -> touch(args.firstOrNull())
                "rm" -> remove(args.firstOrNull())
                "help" -> TerminalResult(currentDir.path, help())
                "clear" -> TerminalResult(currentDir.path, "__CLEAR__")
                else -> TerminalResult(currentDir.path, "Command not allowed on Android sandbox: $command\nType help for supported commands.")
            }
        } catch (e: Exception) {
            TerminalResult(currentDir.path, "Error: ${e.message}")
        }
    }

    private fun list(pathArg: String?): String {
        val dir = resolve(pathArg ?: ".")
        if (!dir.exists()) return "Not found: $pathArg"
        if (dir.isFile) return dir.name
        return dir.listFiles()?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
            ?.joinToString("\n") { if (it.isDirectory) "${it.name}/" else it.name }
            .orEmpty()
    }

    private fun changeDir(pathArg: String): TerminalResult {
        val target = resolve(pathArg)
        if (!target.exists() || !target.isDirectory) {
            return TerminalResult(currentDir.path, "Not a directory: $pathArg")
        }
        currentDir = target
        return TerminalResult(currentDir.path, currentDir.path)
    }

    private fun readFile(pathArg: String?): TerminalResult {
        if (pathArg == null) return TerminalResult(currentDir.path, "Usage: cat <file>")
        val file = resolve(pathArg)
        if (!file.exists() || !file.isFile) return TerminalResult(currentDir.path, "Not a file: $pathArg")
        if (file.length() > 300_000) return TerminalResult(currentDir.path, "File too large to print")
        return TerminalResult(currentDir.path, file.readText())
    }

    private fun mkdir(pathArg: String?): TerminalResult {
        if (pathArg == null) return TerminalResult(currentDir.path, "Usage: mkdir <dir>")
        val dir = resolve(pathArg)
        dir.mkdirs()
        return TerminalResult(currentDir.path, "created ${dir.name}")
    }

    private fun touch(pathArg: String?): TerminalResult {
        if (pathArg == null) return TerminalResult(currentDir.path, "Usage: touch <file>")
        val file = resolve(pathArg)
        file.parentFile?.mkdirs()
        if (!file.exists()) file.writeText("")
        return TerminalResult(currentDir.path, "touched ${file.name}")
    }

    private fun remove(pathArg: String?): TerminalResult {
        if (pathArg == null) return TerminalResult(currentDir.path, "Usage: rm <file>")
        val file = resolve(pathArg)
        if (!file.exists()) return TerminalResult(currentDir.path, "Not found: $pathArg")
        if (file.isDirectory) return TerminalResult(currentDir.path, "Refusing to remove directory. Use editor tools for folders.")
        file.delete()
        return TerminalResult(currentDir.path, "removed ${file.name}")
    }

    private fun resolve(path: String): File {
        val target = if (path.startsWith("/")) File(path) else File(currentDir, path)
        val canonicalRoot = workspaceRoot.canonicalFile
        val canonicalTarget = target.canonicalFile
        require(canonicalTarget.path.startsWith(canonicalRoot.path)) { "Path outside workspace is blocked" }
        return canonicalTarget
    }

    private fun help(): String = """
Supported Android sandbox commands:
  pwd
  ls [path]
  cd <dir>
  cat <file>
  mkdir <dir>
  touch <file>
  rm <file>
  clear

This terminal is intentionally sandboxed to the app workspace on Samsung/Android.
    """.trimIndent()
}

data class TerminalResult(
    val cwd: String,
    val output: String
)
