package viewmodel

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Nivel semántico de una línea de log, determinado por su contenido textual.
 * Permite que la vista asigne colores sin depender del formato concreto del log.
 */
enum class LogLevel { SERVER, CLIENT, ERROR, WARN, INFO, DEBUG, DEFAULT }

/**
 * Estado del servidor expuesto a la vista.
 */
enum class ServerStatus { STARTING, RUNNING, STOPPED }

/**
 * Representa una línea de log ya parseada, lista para ser renderizada por la vista.
 *
 * @property text Contenido textual original de la línea.
 * @property level Nivel semántico determinado por el contenido.
 * @property clientId Identificador corto del cliente si la línea le pertenece, o `null`.
 */
data class LogLine(
    val text: String,
    val level: LogLevel,
    val clientId: String? = null
)

/**
 * ViewModel del servidor. Actúa como puente entre el modelo y la vista [view.LogsWindow].
 *
 * Gestiona el polling del archivo de logs cada 500 ms, parsea cada línea para
 * determinar su nivel semántico y expone el estado como [StateFlow]s observables.
 * También encapsula los comandos de apagado y limpieza de logs, desacoplando
 * completamente la vista de cualquier lógica de datos.
 */
class ServerViewModel {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null

    private val _logs = MutableStateFlow<List<LogLine>>(emptyList())
    val logs: StateFlow<List<LogLine>> = _logs

    private val _status = MutableStateFlow(ServerStatus.STARTING)
    val status: StateFlow<ServerStatus> = _status

    private var logFile: File? = null
    private var lastLines: List<String> = emptyList()
    private var shutdownCallback: (() -> Unit)? = null

    private val clientIdRegex = """\[CLIENT-([a-f0-9]{4})\]""".toRegex()

    init {
        logFile = findLogFile()
    }

    private fun findLogFile(): File {
        var currentDir: File? = File(System.getProperty("user.dir"))
        while (currentDir != null && !File(currentDir, "settings.gradle.kts").exists()) {
            currentDir = currentDir.parentFile
        }
        val projectRoot = currentDir ?: File(System.getProperty("user.dir"))
        return File(projectRoot, "wordle-logs.txt").also {
            if (!it.exists()) it.createNewFile()
        }
    }

    private fun parseLine(line: String): LogLine {
        val clientMatch = clientIdRegex.find(line)
        val clientId = clientMatch?.groupValues?.get(1)
        val level = when {
            line.contains("[SERVER]") -> LogLevel.SERVER
            clientId != null          -> LogLevel.CLIENT
            line.contains("[CLIENT]") -> LogLevel.CLIENT
            line.contains("[ERROR]")  -> LogLevel.ERROR
            line.contains("[WARN]")   -> LogLevel.WARN
            line.contains("[INFO]")   -> LogLevel.INFO
            line.contains("[DEBUG]")  -> LogLevel.DEBUG
            else                      -> LogLevel.DEFAULT
        }
        return LogLine(text = line, level = level, clientId = clientId)
    }

    /** Inicia el polling del archivo de logs y marca el estado del servidor como [ServerStatus.RUNNING]. */
    fun startPolling() {
        _status.value = ServerStatus.RUNNING
        pollingJob = scope.launch {
            while (isActive) {
                refreshLogs()
                delay(500)
            }
        }
    }

    /** Detiene el polling y marca el servidor como [ServerStatus.STOPPED]. */
    fun stopPolling() {
        pollingJob?.cancel()
        _status.value = ServerStatus.STOPPED
    }

    private fun refreshLogs() {
        val file = logFile ?: return
        try {
            if (!file.exists()) return
            val newLines = file.readLines()
            if (newLines == lastLines) return
            lastLines = newLines
            _logs.value = newLines.map { parseLine(it) }
        } catch (_: Exception) {}
    }

    /** Borra el contenido del fichero de logs y limpia el estado en memoria. */
    fun clearLogs() {
        logFile?.writeText("")
        lastLines = emptyList()
        _logs.value = emptyList()
    }

    /** Registra el callback que se invocará al pulsar "Apagar servidor" en la UI. */
    fun setShutdownCallback(callback: () -> Unit) {
        shutdownCallback = callback
    }

    /** Invoca el callback de apagado registrado con [setShutdownCallback]. */
    fun shutdown() {
        shutdownCallback?.invoke()
    }
}
