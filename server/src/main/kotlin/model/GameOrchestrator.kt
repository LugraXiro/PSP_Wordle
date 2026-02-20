package model

import model.data.DictionaryManager
import model.data.RecordsManager
import model.data.StatsManager
import protocol.GameConfig
import java.util.*

/**
 * Coordina la creación y el ciclo de vida de las partidas activas (PVE y PVP).
 *
 * Es el punto de entrada para iniciar una partida: recibe los parámetros necesarios,
 * construye el objeto de partida correspondiente y lo registra internamente.
 *
 * @param dictionaryManager Fuente de palabras para las partidas.
 * @param recordsManager Gestor de récords donde se guardarán los resultados.
 * @param statsManager Gestor de estadísticas de jugadores.
 */
class GameOrchestrator(
    private val dictionaryManager: DictionaryManager,
    private val recordsManager: RecordsManager,
    val statsManager: StatsManager
) {
    private val activePVEGames = mutableMapOf<String, PVEGame>()
    private val activePVPGames = mutableMapOf<String, PVPGame>()

    fun createPVEGame(clientHandler: ClientHandler, config: GameConfig, playerName: String): PVEGame {
        val gameId = UUID.randomUUID().toString().substring(0, 8)
        val game = PVEGame(
            gameId = gameId,
            config = config,
            clientHandler = clientHandler,
            dictionaryManager = dictionaryManager,
            recordsManager = recordsManager,
            statsManager = statsManager,
            playerName = playerName
        )
        activePVEGames[gameId] = game
        return game
    }

    fun createPVPGame(room: Room): PVPGame {
        val gameId = UUID.randomUUID().toString().substring(0, 8)
        val config = GameConfig(
            wordLength = 5,
            maxAttempts = 6,
            rounds = 5,
            timeoutSeconds = 90
        )
        val game = PVPGame(
            gameId = gameId,
            config = config,
            players = room.players.toList(),
            playerNames = room.playerNames.toMap(),
            dictionaryManager = dictionaryManager,
            recordsManager = recordsManager
        )
        activePVPGames[gameId] = game
        return game
    }

    fun removeGame(gameId: String) {
        activePVEGames.remove(gameId)
        activePVPGames.remove(gameId)
    }
}
