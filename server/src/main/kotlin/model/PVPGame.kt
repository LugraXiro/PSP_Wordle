package model

import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import logging.FileLogger
import model.data.DictionaryManager
import model.data.RecordsManager
import protocol.*

/**
 * Lógica de una partida multijugador Player vs Player.
 *
 * Gestiona múltiples rondas sincronizadas entre varios jugadores: todos reciben
 * la misma palabra y compiten en tiempo real. Entre rondas se coordina mediante
 * un mecanismo de confirmación ("listo") con timeout de 15 segundos. Aplica
 * timeout global por ronda y notifica a todos los jugadores el estado del resto.
 *
 * @param gameId Identificador único de la partida.
 * @param config Configuración de la partida (longitud de palabra, intentos, rondas, timeout).
 * @param players Lista de handlers de los jugadores participantes.
 * @param playerNames Mapa de handler a nombre de jugador.
 * @param dictionaryManager Fuente de palabras objetivo.
 * @param recordsManager Donde guardar los récords finales de cada jugador.
 */
class PVPGame(
    val gameId: String,
    private val config: GameConfig,
    private val players: List<ClientHandler>,
    private val playerNames: Map<ClientHandler, String>,
    private val dictionaryManager: DictionaryManager,
    private val recordsManager: RecordsManager
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var currentRound = 1
    private val totalScores = mutableMapOf<ClientHandler, Int>()
    private val roundsWon = mutableMapOf<ClientHandler, Int>()
    private var currentWord: WordData? = null
    private var roundStartTime = 0L
    private var roundActive = false
    private var roundComplete = false
    private var gameAbandoned = false

    // Estado por jugador en la ronda actual
    private val playerAttempts = mutableMapOf<ClientHandler, Int>()
    private val playerFinished = mutableMapOf<ClientHandler, Boolean>()
    private val playerRoundScores = mutableMapOf<ClientHandler, Int>()
    private val playerHintUsed = mutableMapOf<ClientHandler, Boolean>()
    private val activePlayers = mutableListOf<ClientHandler>()

    // Sincronización entre rondas
    private val playersReady = mutableSetOf<ClientHandler>()
    private var waitingForReady = false

    init {
        players.forEach { player ->
            totalScores[player] = 0
            roundsWon[player] = 0
        }
        activePlayers.addAll(players)
    }

    /** Inicia la partida ejecutando todas las rondas secuencialmente. */
    suspend fun start() {
        FileLogger.info("SERVER", "🎮 Iniciando partida PVP: $gameId con ${activePlayers.size} jugadores")

        repeat(config.rounds) { roundIndex ->
            if (gameAbandoned || activePlayers.isEmpty()) {
                FileLogger.info("SERVER", "🚪 Partida PVP terminada prematuramente")
                return
            }
            currentRound = roundIndex + 1
            playRound()
        }

        if (!gameAbandoned && activePlayers.isNotEmpty()) {
            endGame()
        }
    }

    private suspend fun playRound() {
        currentWord = dictionaryManager.getRandomWord(config.wordLength)
        if (currentWord == null) {
            FileLogger.error("SERVER", "❌ Error crítico: No hay palabras disponibles de ${config.wordLength} letras")
            broadcastError("NO_WORDS", "No hay palabras disponibles de ${config.wordLength} letras")
            return
        }

        FileLogger.info("SERVER", "📝 PVP Ronda $currentRound/${config.rounds}: Palabra = '${currentWord!!.palabra}' (${activePlayers.size} jugadores)")

        // Resetear estado de ronda
        playerAttempts.clear()
        playerFinished.clear()
        playerRoundScores.clear()
        playerHintUsed.clear()
        activePlayers.forEach { player ->
            playerAttempts[player] = 0
            playerFinished[player] = false
            playerRoundScores[player] = 0
            playerHintUsed[player] = false
        }

        // Notificar inicio de ronda a todos
        val gameStarted = GameStarted(
            gameId = gameId,
            mode = GameMode.PVP,
            rounds = config.rounds,
            currentRound = currentRound,
            wordLength = config.wordLength,
            maxAttempts = config.maxAttempts
        )
        broadcast("GAME_STARTED", json.encodeToString(gameStarted))
        broadcastPlayersStatus()

        roundStartTime = System.currentTimeMillis()
        roundActive = true
        roundComplete = false

        // Timeout
        val timeoutJob = CoroutineScope(Dispatchers.IO).launch {
            delay(config.timeoutSeconds * 1000L)
            if (roundActive) {
                handleTimeout()
            }
        }

        // Esperar hasta que la ronda esté completamente finalizada (incluyendo espera entre rondas)
        while (!roundComplete) {
            delay(100)
        }

        timeoutJob.cancel()
    }

    /**
     * Procesa el intento de [client] durante la ronda activa.
     * Valida longitud y pertenencia al diccionario, actualiza puntuaciones
     * y comprueba si todos los jugadores han terminado la ronda.
     */
    suspend fun handleGuess(client: ClientHandler, guess: GuessRequest) {
        val word = DictionaryManager.normalizeWord(guess.word)
        val name = playerNames[client] ?: "Desconocido"

        if (!roundActive || playerFinished[client] == true) {
            sendToPlayer(client, "ERROR", json.encodeToString(ErrorMessage("ROUND_NOT_ACTIVE", "No puedes enviar más intentos")))
            return
        }

        if (word.length != config.wordLength) {
            sendToPlayer(client, "ERROR", json.encodeToString(ErrorMessage("INVALID_LENGTH", "La palabra debe tener ${config.wordLength} letras")))
            return
        }

        if (!dictionaryManager.isValidWord(word, config.wordLength)) {
            FileLogger.warning("SERVER", "⚠️  PVP [$name] intento rechazado: '$word' no está en el diccionario")
            sendToPlayer(client, "ERROR", json.encodeToString(ErrorMessage("INVALID_WORD", "\"$word\" no es una palabra válida")))
            return
        }

        val attempts = (playerAttempts[client] ?: 0) + 1
        playerAttempts[client] = attempts
        val timeElapsed = ((System.currentTimeMillis() - roundStartTime) / 1000).toInt()

        val result = compareWords(word, currentWord!!.palabra)
        val isCorrect = word == currentWord!!.palabra

        FileLogger.info("SERVER", "🎯 PVP [$name] intento #$attempts: '$word' → ${if (isCorrect) "CORRECTO" else "INCORRECTO"}")

        val guessResult = GuessResult(
            word = word,
            result = result,
            isCorrect = isCorrect,
            attemptsUsed = attempts,
            timeElapsed = timeElapsed
        )
        sendToPlayer(client, "GUESS_RESULT", json.encodeToString(guessResult))
        broadcastPlayersStatus()

        // Notificar al resto sobre el progreso del oponente
        val opponentUpdate = OpponentUpdate(
            opponentAttempts = attempts,
            opponentFinished = isCorrect || attempts >= config.maxAttempts,
            opponentRoundScore = null
        )
        broadcastExcept(client, "OPPONENT_UPDATE", json.encodeToString(opponentUpdate))

        if (isCorrect) {
            val baseScore = ((7 - attempts) * 1000 - timeElapsed).coerceAtLeast(0)
            val roundScore = if (playerHintUsed[client] == true) (baseScore - 1000).coerceAtLeast(0) else baseScore
            playerRoundScores[client] = roundScore
            playerFinished[client] = true
            totalScores[client] = (totalScores[client] ?: 0) + roundScore
            roundsWon[client] = (roundsWon[client] ?: 0) + 1
            checkRoundEnd(timeElapsed)
        } else if (attempts >= config.maxAttempts) {
            playerRoundScores[client] = 0
            playerFinished[client] = true
            checkRoundEnd(timeElapsed)
        }
    }

    private suspend fun checkRoundEnd(timeElapsed: Int) {
        val allFinished = activePlayers.all { playerFinished[it] == true }
        if (allFinished) {
            endRound(timeElapsed)
        }
    }

    private suspend fun endRound(timeSeconds: Int) {
        roundActive = false

        val rankings = buildRankings()

        // Enviar resultado personalizado a cada jugador
        activePlayers.forEach { player ->
            val name = playerNames[player] ?: "Desconocido"
            val won = (playerRoundScores[player] ?: 0) > 0
            val pvpRoundEnd = PVPRoundEnd(
                roundNumber = currentRound,
                won = won,
                correctWord = currentWord!!.palabra,
                attemptsUsed = playerAttempts[player] ?: config.maxAttempts,
                timeSeconds = timeSeconds,
                roundScore = playerRoundScores[player] ?: 0,
                totalScore = totalScores[player] ?: 0,
                rankings = rankings
            )
            sendToPlayer(player, "PVP_ROUND_END", json.encodeToString(pvpRoundEnd))
        }

        FileLogger.info("SERVER", "🏁 PVP Ronda $currentRound finalizada. Rankings: ${rankings.joinToString { "${it.playerName}=${it.score}" }}")

        // Esperar a que todos los jugadores confirmen que están listos (o timeout de 15s)
        if (currentRound < config.rounds && activePlayers.size > 1) {
            playersReady.clear()
            waitingForReady = true

            // Notificar estado inicial de espera (0/total)
            val initialWaiting = WaitingForPlayers(
                readyCount = 0,
                totalCount = activePlayers.size
            )
            broadcast("WAITING_FOR_PLAYERS", json.encodeToString(initialWaiting))

            val readyTimeout = CoroutineScope(Dispatchers.IO).launch {
                delay(15_000L)
                if (waitingForReady) {
                    FileLogger.warning("SERVER", "⏰ Timeout esperando jugadores listos, continuando...")
                    waitingForReady = false
                }
            }

            while (waitingForReady) {
                delay(100)
            }

            readyTimeout.cancel()
        }

        roundComplete = true
    }

    private suspend fun handleTimeout() {
        if (!roundActive) return

        FileLogger.warning("SERVER", "⏰ PVP Timeout en ronda $currentRound")

        // Marcar como terminados a los que no han acabado
        activePlayers.forEach { player ->
            if (playerFinished[player] != true) {
                playerRoundScores[player] = 0
                playerFinished[player] = true
            }
        }

        endRound(config.timeoutSeconds)
    }

    private suspend fun endGame() {
        val rankings = buildRankings()

        activePlayers.forEach { player ->
            val score = totalScores[player] ?: 0
            val isNewRecord = recordsManager.updateScore(GameMode.PVP, playerNames[player] ?: "Desconocido", score, config.wordLength)

            val pvpGameEnd = PVPGameEnd(
                finalScore = score,
                rankings = rankings,
                isNewRecord = isNewRecord
            )
            sendToPlayer(player, "PVP_GAME_END", json.encodeToString(pvpGameEnd))
        }

        FileLogger.info("SERVER", "🏆 Partida PVP finalizada: $gameId | Rankings: ${rankings.joinToString { "${it.playerName}=${it.score}" }}")
    }

    /**
     * Envía la pista de la ronda actual a [client] con penalización de -1000 puntos.
     * Solo se puede usar una vez por jugador por ronda.
     */
    suspend fun handleRequestHint(client: ClientHandler) {
        if (!roundActive || playerFinished[client] == true) {
            sendToPlayer(client, "ERROR", json.encodeToString(ErrorMessage("ROUND_NOT_ACTIVE", "No hay ronda activa")))
            return
        }
        if (playerHintUsed[client] == true) {
            sendToPlayer(client, "ERROR", json.encodeToString(ErrorMessage("HINT_ALREADY_USED", "Ya usaste la pista en esta ronda")))
            return
        }
        val hint = currentWord?.pista ?: "Sin pista disponible"
        playerHintUsed[client] = true
        val name = playerNames[client] ?: "Desconocido"
        FileLogger.info("SERVER", "💡 PVP [$name] solicitó pista en ronda $currentRound: \"$hint\"")
        val response = HintResponse(hint = hint)
        sendToPlayer(client, "HINT_RESPONSE", json.encodeToString(response))
    }

    /**
     * Marca a [client] como listo para la siguiente ronda.
     * Cuando todos los jugadores activos están listos, la espera entre rondas termina.
     */
    fun playerReady(client: ClientHandler) {
        if (!waitingForReady) return
        playersReady.add(client)
        val name = playerNames[client] ?: "Desconocido"
        FileLogger.info("SERVER", "✅ PVP [$name] listo (${playersReady.size}/${activePlayers.size})")

        // Notificar progreso a todos
        val waiting = WaitingForPlayers(
            readyCount = playersReady.size,
            totalCount = activePlayers.size
        )
        broadcast("WAITING_FOR_PLAYERS", json.encodeToString(waiting))

        if (playersReady.containsAll(activePlayers)) {
            waitingForReady = false
        }
    }

    /**
     * Retira a [client] de la partida (por desconexión o abandono).
     * Si solo queda un jugador, finaliza la partida y le declara ganador.
     */
    suspend fun removePlayer(client: ClientHandler) {
        val name = playerNames[client] ?: "Desconocido"
        activePlayers.remove(client)
        playersReady.remove(client)
        playerFinished[client] = true

        // Si estábamos esperando y ahora todos los activos están listos, continuar
        if (waitingForReady && activePlayers.isNotEmpty() && playersReady.containsAll(activePlayers)) {
            waitingForReady = false
        }

        FileLogger.info("SERVER", "🚪 PVP Jugador $name abandonó la partida $gameId")

        // Notificar al resto
        val playerLeft = PlayerLeft(playerName = name)
        broadcast("PLAYER_LEFT", json.encodeToString(playerLeft))

        // Si solo queda 1 jugador, terminar la partida
        if (activePlayers.size <= 1) {
            roundActive = false
            roundComplete = true
            if (activePlayers.size == 1) {
                endGame()
            }
        } else {
            // Verificar si la ronda puede terminar
            if (roundActive) {
                val timeElapsed = ((System.currentTimeMillis() - roundStartTime) / 1000).toInt()
                checkRoundEnd(timeElapsed)
            }
        }
    }

    /** Fuerza el fin inmediato de la partida sin guardar resultados. */
    fun abandon() {
        gameAbandoned = true
        roundActive = false
        roundComplete = true
    }

    private fun buildRankings(): List<PlayerScore> {
        return activePlayers
            .map { player ->
                PlayerScore(
                    playerName = playerNames[player] ?: "Desconocido",
                    score = totalScores[player] ?: 0,
                    roundsWon = roundsWon[player] ?: 0
                )
            }
            .sortedByDescending { it.score }
    }

    private fun broadcastPlayersStatus() {
        val statuses = activePlayers.map { player ->
            PlayerRoundStatus(
                playerName = playerNames[player] ?: "Desconocido",
                attempts = playerAttempts[player] ?: 0,
                finished = playerFinished[player] ?: false,
                totalScore = totalScores[player] ?: 0
            )
        }
        val update = PlayersStatusUpdate(players = statuses)
        broadcast("PLAYERS_STATUS", json.encodeToString(update))
    }

    private fun compareWords(guess: String, target: String): List<LetterResult> {
        val result = mutableListOf<LetterResult>()
        val targetChars = target.toMutableList()
        val guessChars = guess.toList()

        for (i in guessChars.indices) {
            if (guessChars[i] == targetChars[i]) {
                result.add(LetterResult(guessChars[i], LetterStatus.CORRECT))
                targetChars[i] = '_'
            } else {
                result.add(LetterResult(guessChars[i], LetterStatus.ABSENT))
            }
        }

        for (i in guessChars.indices) {
            if (result[i].status == LetterStatus.ABSENT) {
                val index = targetChars.indexOf(guessChars[i])
                if (index != -1) {
                    result[i] = LetterResult(guessChars[i], LetterStatus.PRESENT)
                    targetChars[index] = '_'
                }
            }
        }

        return result
    }

    private fun sendToPlayer(client: ClientHandler, type: String, payload: String) {
        try {
            client.send(type, payload)
        } catch (e: Exception) {
            FileLogger.error("SERVER", "❌ Error enviando a jugador: ${e.message}")
        }
    }

    private fun broadcast(type: String, payload: String) {
        activePlayers.forEach { sendToPlayer(it, type, payload) }
    }

    private fun broadcastExcept(exclude: ClientHandler, type: String, payload: String) {
        activePlayers.filter { it != exclude }.forEach { sendToPlayer(it, type, payload) }
    }

    private fun broadcastError(code: String, message: String) {
        val error = ErrorMessage(code, message)
        broadcast("ERROR", json.encodeToString(error))
    }
}
