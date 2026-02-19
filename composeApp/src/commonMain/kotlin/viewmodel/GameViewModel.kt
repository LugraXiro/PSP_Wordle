package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import logging.FileLogger
import network.NetworkClient
import protocol.*

data class GameUiState(
    val isConnected: Boolean = false,
    val isInGame: Boolean = false,
    val playerName: String = "Jugador",
    val mode: GameMode? = null,
    val currentRound: Int = 0,
    val totalRounds: Int = 0,
    val wordLength: Int = 5,
    val maxAttempts: Int = 6,
    val attempts: List<List<LetterResult>> = emptyList(),
    val currentInput: String = "",
    val usedLetters: Map<Char, LetterStatus> = emptyMap(),
    val timeRemaining: Int = 90,
    val score: Int = 0,
    val roundScore: Int = 0,
    val roundsWon: Int = 0,
    val correctWord: String? = null,
    val showingRoundResult: Boolean = false,
    val gameEnded: Boolean = false,
    val gameAbandoned: Boolean = false,
    val finalScore: Int = 0,
    val isNewRecord: Boolean = false,
    val error: String? = null,
    // Records por longitud de palabra: wordLength -> lista de records
    val pveRecordsByLength: Map<Int, List<RecordEntry>> = emptyMap(),
    val pvpRecordsByLength: Map<Int, List<RecordEntry>> = emptyMap(),
    // PVP Rooms
    val rooms: List<RoomInfo> = emptyList(),
    val currentRoomId: String? = null,
    val roomPlayers: List<String> = emptyList(),
    val roomHostName: String? = null,
    val isHost: Boolean = false,
    // PVP Rankings
    val pvpRankings: List<PlayerScore> = emptyList(),
    // PVP Players status (en tiempo real durante la ronda)
    val pvpPlayersStatus: List<PlayerRoundStatus> = emptyList(),
    // PVP: esperando a otros jugadores entre rondas
    val waitingForPlayers: Boolean = false,
    val readyCount: Int = 0,
    val totalPlayerCount: Int = 0,
    val readyForNextRound: Boolean = false,
    val showInvalidWordDialog: Boolean = false,
    val hintText: String? = null,
    val hintUsed: Boolean = false
)

class GameViewModel : ViewModel() {
    private val networkClient = NetworkClient()
    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var roundStartTime = 0L
    private var isSubmitting = false
    private var pendingGameStarted: GameStarted? = null
    private var playerName: String = "Jugador"

    init {
        // Observar mensajes del servidor
        viewModelScope.launch {
            networkClient.messagesFlow.collect { message ->
                handleServerMessage(message)
            }
        }
        
        // Observar estado de conexión
        viewModelScope.launch {
            networkClient.connectionState.collect { connected ->
                _uiState.update { it.copy(isConnected = connected) }
                if (!connected && _uiState.value.isInGame) {
                    FileLogger.error(networkClient.clientTag, "❌ Conexión perdida durante la partida (ronda ${_uiState.value.currentRound}/${_uiState.value.totalRounds})")
                    _uiState.update { it.copy(
                        error = "Conexión perdida con el servidor",
                        isInGame = false
                    )}
                    stopTimer()
                } else if (!connected) {
                    FileLogger.warning(networkClient.clientTag, "⚠️  Conexión con servidor perdida")
                }
            }
        }
    }
    
    fun setPlayerName(name: String) {
        playerName = name
        _uiState.update { it.copy(playerName = playerName) }
        FileLogger.info(networkClient.clientTag, "👤 Nombre del jugador establecido: $playerName")
    }

    suspend fun connect(host: String, port: Int): Boolean {
        FileLogger.info(networkClient.clientTag, "🔌 Intentando conectar a $host:$port...")
        val result = networkClient.connect(host, port)
        if (result.isSuccess) {
            FileLogger.info(networkClient.clientTag, "✅ Conexión establecida exitosamente")
            // Enviar nombre del jugador al servidor
            sendPlayerName()
            // Solicitar records al conectar
            requestRecords()
            return true
        } else {
            val errorMsg = "Error al conectar a $host:$port: ${result.exceptionOrNull()?.javaClass?.simpleName}: ${result.exceptionOrNull()?.message}"
            FileLogger.error(networkClient.clientTag, "❌ $errorMsg")
            _uiState.update { it.copy(error = errorMsg) }
            return false
        }
    }

    private suspend fun sendPlayerName() {
        val payload = """{"playerName":"$playerName"}"""
        networkClient.send("SET_PLAYER_NAME", payload)
        FileLogger.info(networkClient.clientTag, "📤 Nombre de jugador enviado: $playerName")
    }
    
    private suspend fun requestRecords() {
        networkClient.send("GET_RECORDS", "{}")
    }
    
    suspend fun startGame(mode: GameMode, rounds: Int = 5, wordLength: Int = 5, maxAttempts: Int = 6, saveRecords: Boolean = true) {
        FileLogger.info(networkClient.clientTag, "🎮 Iniciando partida $mode (rounds=$rounds, wordLength=$wordLength, saveRecords=$saveRecords)")
        val request = StartGameRequest(
            mode = mode,
            rounds = rounds,
            wordLength = wordLength,
            maxAttempts = maxAttempts,
            saveRecords = saveRecords
        )

        val payload = json.encodeToString(StartGameRequest.serializer(), request)
        networkClient.send("START_GAME", payload)
    }
    
    private fun handleServerMessage(message: Message) {
        try {
            when (message.type) {
                "GAME_STARTED" -> {
                    val gameStarted = json.decodeFromString<GameStarted>(message.payload)
                    handleGameStarted(gameStarted)
                }
                "GUESS_RESULT" -> {
                    val result = json.decodeFromString<GuessResult>(message.payload)
                    handleGuessResult(result)
                }
                "ROUND_END" -> {
                    val roundEnd = json.decodeFromString<RoundEnd>(message.payload)
                    handleRoundEnd(roundEnd)
                }
                "GAME_END" -> {
                    val gameEnd = json.decodeFromString<GameEnd>(message.payload)
                    handleGameEnd(gameEnd)
                }
                "RECORDS" -> {
                    val records = json.decodeFromString<RecordsResponse>(message.payload)
                    handleRecords(records)
                }
                "ERROR" -> {
                    val error = json.decodeFromString<ErrorMessage>(message.payload)
                    handleError(error)
                }
                "GAME_ABANDONED" -> {
                    val abandoned = json.decodeFromString<GameAbandoned>(message.payload)
                    handleGameAbandoned(abandoned)
                }
                "ROOM_CREATED" -> {
                    val roomCreated = json.decodeFromString<RoomCreated>(message.payload)
                    handleRoomCreated(roomCreated)
                }
                "ROOM_JOINED" -> {
                    val roomJoined = json.decodeFromString<RoomJoined>(message.payload)
                    handleRoomJoined(roomJoined)
                }
                "ROOM_UPDATE" -> {
                    val roomUpdate = json.decodeFromString<RoomUpdate>(message.payload)
                    handleRoomUpdate(roomUpdate)
                }
                "ROOM_LIST" -> {
                    val roomList = json.decodeFromString<RoomListResponse>(message.payload)
                    handleRoomList(roomList)
                }
                "PLAYER_LEFT" -> {
                    val playerLeft = json.decodeFromString<PlayerLeft>(message.payload)
                    FileLogger.info(networkClient.clientTag, "🚪 Jugador ${playerLeft.playerName} salió de la sala")
                }
                "PVP_ROUND_END" -> {
                    val pvpRoundEnd = json.decodeFromString<PVPRoundEnd>(message.payload)
                    handlePVPRoundEnd(pvpRoundEnd)
                }
                "PVP_GAME_END" -> {
                    val pvpGameEnd = json.decodeFromString<PVPGameEnd>(message.payload)
                    handlePVPGameEnd(pvpGameEnd)
                }
                "OPPONENT_UPDATE" -> {
                    val update = json.decodeFromString<OpponentUpdate>(message.payload)
                    FileLogger.debug(networkClient.clientTag, "👤 Oponente: intentos=${update.opponentAttempts}, terminó=${update.opponentFinished}")
                }
                "PLAYERS_STATUS" -> {
                    val update = json.decodeFromString<PlayersStatusUpdate>(message.payload)
                    _uiState.update { it.copy(pvpPlayersStatus = update.players) }
                }
                "WAITING_FOR_PLAYERS" -> {
                    val waiting = json.decodeFromString<WaitingForPlayers>(message.payload)
                    _uiState.update { it.copy(
                        readyCount = waiting.readyCount,
                        totalPlayerCount = waiting.totalCount
                    )}
                }
                "HINT_RESPONSE" -> {
                    val hint = json.decodeFromString<HintResponse>(message.payload)
                    _uiState.update { it.copy(hintText = hint.hint, hintUsed = true) }
                }
            }
        } catch (e: Exception) {
            FileLogger.error(networkClient.clientTag, "❌ Error procesando mensaje ${message.type}: ${e.javaClass.simpleName}: ${e.message} | Payload: ${message.payload.take(100)}")
            e.printStackTrace()
        }
    }
    
    private fun handleGameStarted(gameStarted: GameStarted) {
        // En PVE, si se está mostrando el resultado de la ronda anterior, guardar para procesar después
        // En PVP, siempre procesar inmediatamente (el servidor controla el timing)
        if (_uiState.value.showingRoundResult && gameStarted.mode != GameMode.PVP) {
            FileLogger.info(networkClient.clientTag, "📦 Guardando GAME_STARTED pendiente (ronda ${gameStarted.currentRound}/${gameStarted.rounds}) mientras se muestra resultado")
            pendingGameStarted = gameStarted
            return
        }

        FileLogger.info(networkClient.clientTag, "🎮 Ronda ${gameStarted.currentRound}/${gameStarted.rounds} iniciada")
        isSubmitting = false  // Resetear en nueva ronda
        roundStartTime = System.currentTimeMillis()
        startTimer()

        _uiState.update {
            it.copy(
                isInGame = true,
                mode = gameStarted.mode,
                currentRound = gameStarted.currentRound,
                totalRounds = gameStarted.rounds,
                wordLength = gameStarted.wordLength,
                maxAttempts = gameStarted.maxAttempts,
                attempts = emptyList(),
                currentInput = "",
                usedLetters = emptyMap(),
                timeRemaining = 90,
                roundScore = 0,
                correctWord = null,
                showingRoundResult = false,
                waitingForPlayers = false,
                readyForNextRound = false,
                gameEnded = false,
                error = null,
                hintText = null,
                hintUsed = false
            )
        }
    }
    
    private fun handleGuessResult(result: GuessResult) {
        isSubmitting = false  // Permitir nuevo envío

        val newAttempts = _uiState.value.attempts + listOf(result.result)
        val newUsedLetters = _uiState.value.usedLetters.toMutableMap()

        // Actualizar estado de letras usadas
        result.result.forEach { letterResult ->
            val current = newUsedLetters[letterResult.letter]
            // Prioridad: CORRECT > PRESENT > ABSENT
            if (current == null ||
                (letterResult.status == LetterStatus.CORRECT) ||
                (letterResult.status == LetterStatus.PRESENT && current == LetterStatus.ABSENT)) {
                newUsedLetters[letterResult.letter] = letterResult.status
            }
        }

        _uiState.update {
            it.copy(
                attempts = newAttempts,
                usedLetters = newUsedLetters,
                currentInput = "",
                timeRemaining = (90 - result.timeElapsed).coerceAtLeast(0)
            )
        }
    }
    
    private fun handleRoundEnd(roundEnd: RoundEnd) {
        FileLogger.info(networkClient.clientTag, "🏁 Ronda finalizada: ${if (roundEnd.won) "GANADA" else "PERDIDA"}, score=${roundEnd.roundScore}, palabra=${roundEnd.correctWord}")
        stopTimer()

        _uiState.update {
            it.copy(
                correctWord = roundEnd.correctWord,
                roundScore = roundEnd.roundScore,
                score = roundEnd.totalScore,
                roundsWon = if (roundEnd.won) it.roundsWon + 1 else it.roundsWon,
                showingRoundResult = true,
                timeRemaining = (90 - roundEnd.timeSeconds).coerceAtLeast(0)
            )
        }
    }

    private fun handleGameEnd(gameEnd: GameEnd) {
        FileLogger.info(networkClient.clientTag, "🏆 Partida finalizada: score=${gameEnd.finalScore}, rondas ganadas=${gameEnd.roundsWon}/${gameEnd.totalRounds}, ${if (gameEnd.isNewRecord) "¡NUEVO RÉCORD!" else "sin récord"}")
        _uiState.update {
            it.copy(
                gameEnded = true,
                finalScore = gameEnd.finalScore,
                isNewRecord = gameEnd.isNewRecord,
                isInGame = false
            )
        }
    }
    
    private fun handleRecords(records: RecordsResponse) {
        // Convertir claves String a Int
        val pveByLength = records.pveRecords.mapKeys { (k, _) -> k.toIntOrNull() ?: 5 }
        val pvpByLength = records.pvpRecords.mapKeys { (k, _) -> k.toIntOrNull() ?: 5 }
        _uiState.update {
            it.copy(
                pveRecordsByLength = pveByLength,
                pvpRecordsByLength = pvpByLength
            )
        }
    }
    
    private fun handleError(error: ErrorMessage) {
        isSubmitting = false  // Permitir reintento en caso de error
        FileLogger.error(networkClient.clientTag, "❌ Error recibido del servidor: [${error.code}] ${error.message}")
        if (error.code == "INVALID_WORD") {
            _uiState.update { it.copy(showInvalidWordDialog = true) }
        } else {
            _uiState.update { it.copy(error = "${error.code}: ${error.message}") }
        }
    }

    fun dismissInvalidWordDialog() {
        _uiState.update { it.copy(showInvalidWordDialog = false) }
    }

    private fun handleGameAbandoned(abandoned: GameAbandoned) {
        if (!_uiState.value.isInGame) return
        FileLogger.info(networkClient.clientTag, "🚪 Partida abandonada: ${abandoned.message}")
        stopTimer()
        _uiState.update {
            it.copy(
                gameAbandoned = true,
                isInGame = false
            )
        }
    }

    suspend fun abandonGame() {
        if (!_uiState.value.isInGame) {
            FileLogger.warning(networkClient.clientTag, "⚠️  No hay partida activa para abandonar")
            return
        }

        FileLogger.info(networkClient.clientTag, "🚪 Solicitando abandonar partida...")
        networkClient.send("ABANDON_GAME", "{}")
    }

    fun updateInput(letter: Char) {
        val current = _uiState.value.currentInput
        if (current.length < _uiState.value.wordLength) {
            _uiState.update { it.copy(currentInput = current + letter.uppercase()) }
        }
    }
    
    fun deleteLetter() {
        val current = _uiState.value.currentInput
        if (current.isNotEmpty()) {
            _uiState.update { it.copy(currentInput = current.dropLast(1)) }
        }
    }
    
    suspend fun submitGuess() {
        // Prevenir envíos múltiples
        if (isSubmitting) {
            FileLogger.debug(networkClient.clientTag, "⚠️  Ya hay un envío en progreso, ignorando")
            return
        }

        val input = _uiState.value.currentInput
        val wordLength = _uiState.value.wordLength
        val attemptNumber = _uiState.value.attempts.size + 1

        FileLogger.debug(networkClient.clientTag, "🎯 Intentando enviar palabra: '$input' (longitud=$wordLength)")

        if (input.length != wordLength) {
            FileLogger.debug(networkClient.clientTag, "⚠️  Longitud incorrecta: ${input.length} != $wordLength")
            _uiState.update { it.copy(error = "La palabra debe tener $wordLength letras") }
            return
        }

        if (!_uiState.value.isInGame) {
            FileLogger.debug(networkClient.clientTag, "⚠️  No hay partida activa")
            _uiState.update { it.copy(error = "No hay partida activa") }
            return
        }

        isSubmitting = true
        try {
            FileLogger.info(networkClient.clientTag, "📨 Enviando intento: $input")
            val guess = GuessRequest(
                word = input,
                attemptNumber = attemptNumber
            )

            val payload = json.encodeToString(GuessRequest.serializer(), guess)
            val result = networkClient.send("GUESS", payload)

            if (result.isFailure) {
                FileLogger.error(networkClient.clientTag, "❌ Error al enviar GUESS: ${result.exceptionOrNull()?.message}")
                _uiState.update { it.copy(error = "Error al enviar: ${result.exceptionOrNull()?.message}") }
                isSubmitting = false
            } else {
                FileLogger.debug(networkClient.clientTag, "✅ GUESS enviado exitosamente")
                // isSubmitting se pondrá en false cuando recibamos GUESS_RESULT
            }
        } catch (e: Exception) {
            FileLogger.error(networkClient.clientTag, "❌ Excepción en submitGuess: ${e.message}")
            isSubmitting = false
        }
    }
    
    fun sendReadyForNextRound() {
        if (_uiState.value.readyForNextRound) return
        _uiState.update { it.copy(readyForNextRound = true) }
        viewModelScope.launch {
            networkClient.send("READY_NEXT_ROUND", "{}")
            FileLogger.info(networkClient.clientTag, "📤 READY_NEXT_ROUND enviado")
        }
    }

    fun requestHint() {
        if (_uiState.value.hintUsed || !_uiState.value.isInGame) return
        viewModelScope.launch {
            networkClient.send("REQUEST_HINT", "{}")
            FileLogger.info(networkClient.clientTag, "📤 REQUEST_HINT enviado")
        }
    }

    fun continueToNextRound() {
        _uiState.update { it.copy(showingRoundResult = false) }

        // Si hay un GAME_STARTED pendiente, procesarlo ahora (PVE)
        pendingGameStarted?.let { gameStarted ->
            FileLogger.info(networkClient.clientTag, "📦 Procesando GAME_STARTED pendiente")
            pendingGameStarted = null
            handleGameStarted(gameStarted)
        }
    }
    
    // ==================== PVP ROOM HANDLERS ====================

    private fun handleRoomCreated(roomCreated: RoomCreated) {
        FileLogger.info(networkClient.clientTag, "🏠 Sala creada: ${roomCreated.roomId}")
        _uiState.update { it.copy(
            currentRoomId = roomCreated.roomId,
            isHost = true
        )}
    }

    private fun handleRoomJoined(roomJoined: RoomJoined) {
        FileLogger.info(networkClient.clientTag, "🏠 Unido a sala: ${roomJoined.roomId}")
        _uiState.update { it.copy(
            currentRoomId = roomJoined.roomId,
            roomPlayers = roomJoined.players,
            isHost = false
        )}
    }

    private fun handleRoomUpdate(roomUpdate: RoomUpdate) {
        FileLogger.info(networkClient.clientTag, "🏠 Sala ${roomUpdate.roomId} actualizada: ${roomUpdate.players}")
        _uiState.update { it.copy(
            roomPlayers = roomUpdate.players,
            roomHostName = roomUpdate.hostName
        )}
    }

    private fun handleRoomList(roomList: RoomListResponse) {
        FileLogger.debug(networkClient.clientTag, "🏠 Salas disponibles: ${roomList.rooms.size}")
        _uiState.update { it.copy(rooms = roomList.rooms) }
    }

    private fun handlePVPRoundEnd(pvpRoundEnd: PVPRoundEnd) {
        FileLogger.info(networkClient.clientTag, "🏁 PVP Ronda finalizada: ${if (pvpRoundEnd.won) "GANADA" else "PERDIDA"}, score=${pvpRoundEnd.roundScore}")
        stopTimer()
        _uiState.update { it.copy(
            correctWord = pvpRoundEnd.correctWord,
            roundScore = pvpRoundEnd.roundScore,
            score = pvpRoundEnd.totalScore,
            roundsWon = if (pvpRoundEnd.won) it.roundsWon + 1 else it.roundsWon,
            showingRoundResult = true,
            pvpRankings = pvpRoundEnd.rankings,
            timeRemaining = (90 - pvpRoundEnd.timeSeconds).coerceAtLeast(0),
            readyForNextRound = false,
            readyCount = 0,
            totalPlayerCount = 0
        )}
    }

    private fun handlePVPGameEnd(pvpGameEnd: PVPGameEnd) {
        FileLogger.info(networkClient.clientTag, "🏆 PVP Partida finalizada: score=${pvpGameEnd.finalScore}, ${if (pvpGameEnd.isNewRecord) "¡NUEVO RÉCORD!" else "sin récord"}")
        _uiState.update { it.copy(
            gameEnded = true,
            finalScore = pvpGameEnd.finalScore,
            isNewRecord = pvpGameEnd.isNewRecord,
            pvpRankings = pvpGameEnd.rankings,
            isInGame = false,
            showingRoundResult = false
        )}
    }

    // ==================== PVP ROOM ACTIONS ====================

    suspend fun requestRoomList() {
        networkClient.send("LIST_ROOMS", "{}")
    }

    suspend fun createRoom() {
        networkClient.send("CREATE_ROOM", "{}")
    }

    suspend fun joinRoom(roomId: String) {
        val payload = json.encodeToString(JoinRoomRequest.serializer(), JoinRoomRequest(roomId))
        networkClient.send("JOIN_ROOM", payload)
    }

    suspend fun leaveRoom() {
        networkClient.send("LEAVE_ROOM", "{}")
        _uiState.update { it.copy(
            currentRoomId = null,
            roomPlayers = emptyList(),
            roomHostName = null,
            isHost = false
        )}
    }

    suspend fun startPVPGame() {
        networkClient.send("START_PVP_GAME", "{}")
    }

    fun backToMenu() {
        stopTimer()
        // Capturar estado ANTES de resetearlo para notificar al servidor correctamente
        val wasInGame = _uiState.value.isInGame
        val wasInRoom = _uiState.value.currentRoomId != null

        _uiState.update { GameUiState(
            isConnected = _uiState.value.isConnected,
            playerName = _uiState.value.playerName,
            pveRecordsByLength = _uiState.value.pveRecordsByLength,
            pvpRecordsByLength = _uiState.value.pvpRecordsByLength,
            currentRoomId = null,
            roomPlayers = emptyList(),
            roomHostName = null,
            isHost = false,
            pvpRankings = emptyList(),
            pvpPlayersStatus = emptyList()
        )}
        viewModelScope.launch {
            // Notificar al servidor según el estado previo
            when {
                wasInGame -> networkClient.send("ABANDON_GAME", "{}")
                wasInRoom -> networkClient.send("LEAVE_ROOM", "{}")
            }
            requestRecords()
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    private fun startTimer() {
        stopTimer()
        roundStartTime = System.currentTimeMillis()

        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val elapsed = ((System.currentTimeMillis() - roundStartTime) / 1000).toInt()
                val remaining = (90 - elapsed).coerceAtLeast(0)
                _uiState.update { it.copy(timeRemaining = remaining) }

                // Si se acaba el tiempo, detener el timer (el servidor manejará el timeout)
                if (remaining <= 0) {
                    break
                }
            }
        }
    }
    
    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }
    
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            networkClient.disconnect()
        }
    }
}
