package logging

import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

actual object FileLogger {
    private val logFile: File by lazy {
        var currentDir: File? = File(System.getProperty("user.dir"))
        while (currentDir != null && !File(currentDir, "settings.gradle.kts").exists()) {
            currentDir = currentDir.parentFile
        }
        val projectRoot = currentDir ?: File(System.getProperty("user.dir"))
        File(projectRoot, "wordle-logs.txt").also {
            if (!it.exists()) it.createNewFile()
        }
    }
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS")
    private val lock = ReentrantLock()
    private const val MAX_LOGS = 1000
    private const val LOGS_TO_KEEP = 500

    actual fun info(source: String, message: String) {
        write("INFO", source, message)
    }

    actual fun error(source: String, message: String) {
        write("ERROR", source, message)
    }

    actual fun warning(source: String, message: String) {
        write("WARN", source, message)
    }

    actual fun debug(source: String, message: String) {
        write("DEBUG", source, message)
    }

    actual fun clear() {
        lock.withLock {
            logFile.writeText("")
        }
    }

    private fun write(level: String, source: String, msg: String) {
        lock.withLock {
            val timestamp = dateFormat.format(Date())
            val logLine = "[$timestamp] [$level] [$source] $msg\n"

            // Leer todas las líneas actuales
            val lines = if (logFile.exists() && logFile.length() > 0) {
                logFile.readLines().toMutableList()
            } else {
                mutableListOf()
            }

            // Añadir nueva línea
            lines.add(logLine.trimEnd())

            // Si superamos el límite, mantener solo las últimas LOGS_TO_KEEP líneas
            if (lines.size > MAX_LOGS) {
                val linesToKeep = lines.takeLast(LOGS_TO_KEEP)
                logFile.writeText(linesToKeep.joinToString("\n") + "\n")
            } else {
                // Simplemente añadir la nueva línea
                logFile.appendText(logLine)
            }
        }
    }
}
