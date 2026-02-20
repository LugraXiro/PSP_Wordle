package model.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import logging.FileLogger
import protocol.PlayerStats
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@Serializable
private data class AllStats(
    val players: MutableMap<String, PlayerStats> = mutableMapOf()
)

/**
 * Persiste y gestiona las estadísticas individuales de cada jugador en disco.
 *
 * Registra partidas jugadas, ganadas, racha actual, racha máxima, distribución
 * de intentos y tiempo total. Las estadísticas solo se actualizan en partidas
 * con `saveRecords = true`.
 *
 * @param statsFile Archivo JSON donde se persistirán las estadísticas.
 */
class StatsManager(
    private val statsFile: File = if (File("server").isDirectory) File("server", "stats.json") else File("stats.json")
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val lock = ReentrantReadWriteLock()
    private var allStats = AllStats()

    init {
        load()
    }

    private fun load() {
        if (!statsFile.exists()) return
        try {
            allStats = json.decodeFromString(statsFile.readText())
            FileLogger.info("SERVER", "📊 Estadísticas cargadas para ${allStats.players.size} jugadores")
        } catch (e: Exception) {
            FileLogger.error("SERVER", "❌ Error cargando stats.json: ${e.message}")
            allStats = AllStats()
        }
    }

    private fun save() {
        try {
            statsFile.writeText(json.encodeToString(AllStats.serializer(), allStats))
        } catch (e: Exception) {
            FileLogger.error("SERVER", "❌ Error guardando stats.json: ${e.message}")
        }
    }

    fun getStats(playerName: String): PlayerStats = lock.read {
        allStats.players[playerName] ?: PlayerStats()
    }

    /** Llamar al final de cada ronda clásica (saveRecords=true) */
    fun updateRound(playerName: String, won: Boolean, attemptsUsed: Int, timeSeconds: Int) {
        lock.write {
            val current = allStats.players[playerName] ?: PlayerStats()
            val newDist = current.guessDistribution.toMutableMap()
            val newTotalWords = current.totalWordsAttempted + 1
            val newWordsGuessed: Int
            val newTotalTime: Long

            if (won) {
                val key = attemptsUsed.toString()
                newDist[key] = (newDist[key] ?: 0) + 1
                newWordsGuessed = current.totalWordsGuessed + 1
                newTotalTime = current.totalTimeSeconds + timeSeconds
            } else {
                newWordsGuessed = current.totalWordsGuessed
                newTotalTime = current.totalTimeSeconds
            }

            allStats.players[playerName] = current.copy(
                totalWordsAttempted = newTotalWords,
                totalWordsGuessed = newWordsGuessed,
                totalTimeSeconds = newTotalTime,
                guessDistribution = newDist
            )
            save()
        }
    }

    /** Llamar al final del juego completo (saveRecords=true). Una partida se gana si roundsWon > totalRounds/2 */
    fun updateGame(playerName: String, roundsWon: Int, totalRounds: Int) {
        lock.write {
            val current = allStats.players[playerName] ?: PlayerStats()
            val gameWon = roundsWon > totalRounds / 2
            val newStreak = if (gameWon) current.currentStreak + 1 else 0
            val newMaxStreak = maxOf(current.maxStreak, newStreak)

            allStats.players[playerName] = current.copy(
                gamesPlayed = current.gamesPlayed + 1,
                gamesWon = if (gameWon) current.gamesWon + 1 else current.gamesWon,
                currentStreak = newStreak,
                maxStreak = newMaxStreak
            )
            FileLogger.info("SERVER", "📊 Stats de $playerName actualizadas: juego ${if (gameWon) "ganado" else "perdido"}, racha=$newStreak")
            save()
        }
    }
}
