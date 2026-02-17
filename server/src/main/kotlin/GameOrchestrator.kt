import protocol.GameConfig
import java.util.*

class GameOrchestrator(
    private val dictionaryManager: DictionaryManager,
    private val recordsManager: RecordsManager
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
