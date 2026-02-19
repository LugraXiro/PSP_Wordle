import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import logging.FileLogger
import protocol.*
import java.util.*

class PVEGame(
    private val gameId: String,
    private val config: GameConfig,
    private val clientHandler: ClientHandler,
    private val dictionaryManager: DictionaryManager,
    private val recordsManager: RecordsManager,
    private val playerName: String
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var currentRound = 1
    private var totalScore = 0
    private var roundsWon = 0
    private var currentWord: WordData? = null
    private var roundStartTime = 0L
    private var currentAttempts = 0
    private var roundActive = false
    private var gameAbandoned = false
    private var roundHintUsed = false

    suspend fun start() {
        FileLogger.info("SERVER", "🎮 Iniciando partida PVE: $gameId para jugador: $playerName")

        repeat(config.rounds) { roundIndex ->
            if (gameAbandoned) {
                FileLogger.info("SERVER", "🚪 Partida abandonada, terminando sin guardar puntuación")
                return
            }
            currentRound = roundIndex + 1
            playRound()
        }

        if (!gameAbandoned) {
            endGame()
        }
    }

    /**
     * Abandona la partida actual. No se guarda puntuación.
     */
    suspend fun abandon() {
        FileLogger.info("SERVER", "🚪 Jugador $playerName abandona partida $gameId en ronda $currentRound/${config.rounds}")
        gameAbandoned = true
        roundActive = false

        val response = GameAbandoned("Partida abandonada. Puntuación no guardada.")
        clientHandler.send("GAME_ABANDONED", json.encodeToString(response))
    }

    fun isAbandoned(): Boolean = gameAbandoned
    
    private suspend fun playRound() {
        // Seleccionar palabra
        currentWord = dictionaryManager.getRandomWord(config.wordLength)
        if (currentWord == null) {
            FileLogger.error("SERVER", "❌ Error crítico: No hay palabras disponibles de ${config.wordLength} letras en el diccionario")
            sendError("NO_WORDS", "No hay palabras disponibles de ${config.wordLength} letras")
            return
        }

        FileLogger.info("SERVER", "📝 Ronda $currentRound/${config.rounds} iniciada: Palabra objetivo = '${currentWord!!.palabra}' (${config.wordLength} letras, max ${config.maxAttempts} intentos)")

        // Notificar inicio de ronda
        val gameStarted = GameStarted(
            gameId = gameId,
            mode = GameMode.PVE,
            rounds = config.rounds,
            currentRound = currentRound,
            wordLength = config.wordLength,
            maxAttempts = config.maxAttempts
        )
        clientHandler.send("GAME_STARTED", json.encodeToString(gameStarted))

        // Iniciar ronda
        roundStartTime = System.currentTimeMillis()
        currentAttempts = 0
        roundActive = true
        roundHintUsed = false
        FileLogger.debug("SERVER", "🔵 Ronda $currentRound marcada como activa, esperando jugadas del jugador...")
        
        // Timeout de 90 segundos
        val timeoutJob = CoroutineScope(Dispatchers.IO).launch {
            delay(config.timeoutSeconds * 1000L)
            if (roundActive) {
                handleTimeout()
            }
        }

        // Esperar intentos del jugador (se manejan en handleGuess)
        // La ronda termina cuando:
        // 1. El jugador acierta
        // 2. Se agotan los intentos
        // 3. Timeout

        // Esperar hasta que la ronda termine
        FileLogger.debug("SERVER", "🔄 Esperando a que termine la ronda $currentRound...")
        while (roundActive) {
            delay(100)
        }
        FileLogger.debug("SERVER", "✔️ Ronda $currentRound terminada, cancelando timeout")

        timeoutJob.cancel()
    }
    
    suspend fun handleGuess(guess: GuessRequest) {
        // Normalizar palabra: mayúsculas y sin tildes (pero preservando Ñ)
        val word = DictionaryManager.normalizeWord(guess.word)
        FileLogger.debug("SERVER", "🎯 Procesando intento #${guess.attemptNumber}: '$word' (ronda $currentRound)")

        if (!roundActive) {
            FileLogger.warning("SERVER", "⚠️  Intento rechazado: No hay ronda activa (palabra='$word')")
            sendError("ROUND_NOT_ACTIVE", "No hay ronda activa")
            return
        }

        // Validaciones
        if (word.length != config.wordLength) {
            FileLogger.warning("SERVER", "⚠️  Intento rechazado: Longitud inválida '$word' (${word.length} != ${config.wordLength})")
            sendError("INVALID_LENGTH", "La palabra debe tener ${config.wordLength} letras")
            return
        }

        if (!dictionaryManager.isValidWord(word, config.wordLength)) {
            FileLogger.warning("SERVER", "⚠️  Intento rechazado: '$word' no está en el diccionario")
            sendError("INVALID_WORD", "\"$word\" no es una palabra válida")
            return
        }

        currentAttempts++
        val timeElapsed = ((System.currentTimeMillis() - roundStartTime) / 1000).toInt()

        // Comparar con palabra objetivo
        val result = compareWords(word, currentWord!!.palabra)
        val isCorrect = word == currentWord!!.palabra

        FileLogger.info("SERVER", "✓ Intento procesado: '$word' vs '${currentWord!!.palabra}' → ${if (isCorrect) "CORRECTO ✅" else "INCORRECTO ❌"} (intento $currentAttempts/${config.maxAttempts}, tiempo=${timeElapsed}s)")

        // Enviar resultado del intento
        val guessResult = GuessResult(
            word = word,
            result = result,
            isCorrect = isCorrect,
            attemptsUsed = currentAttempts,
            timeElapsed = timeElapsed
        )
        clientHandler.send("GUESS_RESULT", json.encodeToString(guessResult))

        // Verificar fin de ronda
        if (isCorrect) {
            FileLogger.info("SERVER", "🎉 ¡Palabra acertada en intento $currentAttempts!")
            endRound(true, timeElapsed)
        } else if (currentAttempts >= config.maxAttempts) {
            FileLogger.info("SERVER", "💔 Se agotaron los intentos. Palabra correcta era: ${currentWord!!.palabra}")
            endRound(false, timeElapsed)
        }
    }
    
    private fun compareWords(guess: String, target: String): List<LetterResult> {
        val result = mutableListOf<LetterResult>()
        val targetChars = target.toMutableList()
        val guessChars = guess.toList()
        
        // Primera pasada: marcar CORRECT
        for (i in guessChars.indices) {
            if (guessChars[i] == targetChars[i]) {
                result.add(LetterResult(guessChars[i], LetterStatus.CORRECT))
                targetChars[i] = '_' // Marcar como usada
            } else {
                result.add(LetterResult(guessChars[i], LetterStatus.ABSENT)) // Temporal
            }
        }
        
        // Segunda pasada: marcar PRESENT
        for (i in guessChars.indices) {
            if (result[i].status == LetterStatus.ABSENT) {
                val index = targetChars.indexOf(guessChars[i])
                if (index != -1) {
                    result[i] = LetterResult(guessChars[i], LetterStatus.PRESENT)
                    targetChars[index] = '_' // Marcar como usada
                }
            }
        }
        
        return result
    }
    
    private suspend fun endRound(won: Boolean, timeSeconds: Int) {
        roundActive = false

        val baseScore = if (won) {
            ((7 - currentAttempts) * 1000 - timeSeconds).coerceAtLeast(0)
        } else {
            0
        }
        val roundScore = if (roundHintUsed) (baseScore - 1000).coerceAtLeast(0) else baseScore

        totalScore += roundScore
        if (won) roundsWon++

        FileLogger.info("SERVER", "🏁 Ronda $currentRound/${config.rounds} finalizada: ${if (won) "GANADA 🎉" else "PERDIDA 💔"} | Palabra='${currentWord!!.palabra}' | Intentos=$currentAttempts/${config.maxAttempts} | Tiempo=${timeSeconds}s | Score=$roundScore | Total=$totalScore")

        val roundEnd = RoundEnd(
            roundNumber = currentRound,
            won = won,
            correctWord = currentWord!!.palabra,
            attemptsUsed = currentAttempts,
            timeSeconds = timeSeconds,
            roundScore = roundScore,
            totalScore = totalScore
        )
        clientHandler.send("ROUND_END", json.encodeToString(roundEnd))

        // Pausa antes de siguiente ronda (5s si se acertó, 2s si se perdió)
        val delayTime = if (won) 5000L else 2000L
        delay(delayTime)
    }
    
    private suspend fun handleTimeout() {
        if (!roundActive) return

        FileLogger.warning("SERVER", "⏰ Timeout en ronda $currentRound después de ${config.timeoutSeconds}s sin respuesta. Palabra correcta era: '${currentWord!!.palabra}'")
        val timeElapsed = config.timeoutSeconds
        endRound(won = false, timeSeconds = timeElapsed)
    }

    private suspend fun endGame() {
        val isNewRecord = if (config.saveRecords) {
            recordsManager.updateScore(GameMode.PVE, playerName, totalScore, config.wordLength)
        } else {
            FileLogger.info("SERVER", "📊 Partida personalizada: puntuación no guardada en récords")
            false
        }

        FileLogger.info("SERVER", "🏆 Partida PVE finalizada: ID=$gameId | Jugador=$playerName | Score final=$totalScore | Rondas ganadas=$roundsWon/${config.rounds} | ${config.wordLength} letras | ${if (isNewRecord) "¡NUEVO RÉCORD! 🎉" else "Sin récord"}")

        val gameEnd = GameEnd(
            mode = GameMode.PVE,
            finalScore = totalScore,
            roundsWon = roundsWon,
            totalRounds = config.rounds,
            isNewRecord = isNewRecord
        )
        clientHandler.send("GAME_END", json.encodeToString(gameEnd))
    }
    
    suspend fun handleRequestHint() {
        if (!roundActive) {
            sendError("ROUND_NOT_ACTIVE", "No hay ronda activa")
            return
        }
        if (roundHintUsed) {
            sendError("HINT_ALREADY_USED", "Ya usaste la pista en esta ronda")
            return
        }
        val hint = currentWord?.pista ?: "Sin pista disponible"
        roundHintUsed = true
        FileLogger.info("SERVER", "💡 Pista solicitada en ronda $currentRound: \"$hint\" (penalización -1000 pts)")
        val response = HintResponse(hint = hint)
        clientHandler.send("HINT_RESPONSE", json.encodeToString(response))
    }

    private suspend fun sendError(code: String, message: String) {
        FileLogger.error("SERVER", "❌ Enviando error al cliente: [$code] $message")
        val error = ErrorMessage(code, message)
        clientHandler.send("ERROR", json.encodeToString(error))
    }
}
