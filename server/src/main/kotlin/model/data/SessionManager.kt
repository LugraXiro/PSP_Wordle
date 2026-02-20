package model.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import logging.FileLogger
import protocol.GameConfig
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Datos de una partida PVE interrumpida por desconexión.
 *
 * Se persiste en disco para que el jugador pueda reanudarla al reconectar.
 * Las sesiones expiran automáticamente tras 2 horas.
 *
 * @param playerName Identificador del jugador (usado como clave).
 * @param config Configuración exacta de la partida interrumpida.
 * @param nextRound Ronda desde la que se debe reanudar.
 * @param totalScore Puntuación acumulada hasta la desconexión.
 * @param roundsWon Rondas ganadas hasta la desconexión.
 * @param savedAt Marca de tiempo de guardado (ms desde epoch).
 */
@Serializable
data class GameSession(
    val playerName: String,
    val config: GameConfig,
    val nextRound: Int,
    val totalScore: Int,
    val roundsWon: Int,
    val savedAt: Long = System.currentTimeMillis()
)

@Serializable
private data class SessionsStore(val sessions: List<GameSession> = emptyList())

/**
 * Persiste y gestiona sesiones de partidas PVE interrumpidas por desconexión.
 *
 * Las sesiones se guardan en un archivo JSON en disco. Al cargar, elimina
 * automáticamente las sesiones expiradas (más de 2 horas de antigüedad).
 *
 * @param sessionsFile Ruta al archivo JSON donde se guardan las sesiones.
 */
class SessionManager(sessionsFile: String = "sessions.json") {

    private val file: File = if (File("server").isDirectory) File("server", sessionsFile) else File(sessionsFile)
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val sessions = ConcurrentHashMap<String, GameSession>()
    private val expirationMs = 2 * 60 * 60 * 1000L // 2 horas

    init {
        loadSessions()
    }

    private fun loadSessions() {
        if (!file.exists()) return
        try {
            val store = json.decodeFromString<SessionsStore>(file.readText())

            val now = System.currentTimeMillis()
            var expired = 0
            store.sessions.forEach { session ->
                if (now - session.savedAt <= expirationMs) {
                    sessions[session.playerName] = session
                } else {
                    expired++
                }
            }
            val loaded = sessions.size
            FileLogger.info("SERVER", "📂 Sesiones cargadas: $loaded activas, $expired expiradas eliminadas")
        } catch (e: Exception) {
            FileLogger.warning("SERVER", "⚠️  Error cargando sesiones: ${e.message}. Empezando con lista vacía")
        }
    }

    private fun saveToDisk() {
        try {
            val store = SessionsStore(sessions = sessions.values.toList())
            file.writeText(json.encodeToString(SessionsStore.serializer(), store))
        } catch (e: Exception) {
            FileLogger.error("SERVER", "❌ Error guardando sesiones en disco: ${e.message}")
        }
    }

    fun saveSession(session: GameSession) {
        sessions[session.playerName] = session
        saveToDisk()
        FileLogger.info("SERVER", "💾 Sesión guardada para '${session.playerName}': ronda ${session.nextRound}/${session.config.rounds}, score=${session.totalScore}")
    }

    fun getSession(playerName: String): GameSession? {
        val session = sessions[playerName] ?: return null
        if (System.currentTimeMillis() - session.savedAt > expirationMs) {
            sessions.remove(playerName)
            saveToDisk()
            FileLogger.info("SERVER", "🗑️  Sesión expirada eliminada para '$playerName'")
            return null
        }
        return session
    }

    fun deleteSession(playerName: String) {
        if (sessions.remove(playerName) != null) {
            saveToDisk()
            FileLogger.info("SERVER", "🗑️  Sesión eliminada para '$playerName'")
        }
    }
}
