import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import logging.FileLogger
import protocol.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.util.*

class ClientHandler(
    private val socket: Socket,
    private val gameOrchestrator: GameOrchestrator,
    private val recordsManager: RecordsManager,
    private val roomManager: RoomManager
) {
    private val clientId = UUID.randomUUID().toString().substring(0, 8)
    private val input = BufferedReader(InputStreamReader(socket.getInputStream()))
    private val output = PrintWriter(socket.getOutputStream(), true)
    private val json = Json { ignoreUnknownKeys = true }
    private var currentGame: PVEGame? = null
    private var currentPVPGame: PVPGame? = null
    private var currentRoomId: String? = null
    private var playerName: String = "Jugador"

    suspend fun handle() = coroutineScope {
        FileLogger.info("SERVER", "👤 Cliente conectado: $clientId desde ${socket.inetAddress.hostAddress}")

        try {
            while (isActive && !socket.isClosed) {
                val line = withContext(Dispatchers.IO) {
                    input.readLine()
                } ?: break

                if (line.isBlank()) continue

                FileLogger.debug("SERVER", "📨 [$clientId] Recibido: $line")
                processMessage(line)
            }
        } catch (e: Exception) {
            FileLogger.error("SERVER", "❌ [$clientId] Error: ${e.message}")
        } finally {
            cleanup()
        }
    }

    private suspend fun processMessage(message: String) {
        try {
            val msg = json.decodeFromString<Message>(message)

            when (msg.type) {
                "SET_PLAYER_NAME" -> {
                    val request = json.decodeFromString<SetPlayerNameRequest>(msg.payload)
                    playerName = request.playerName
                    FileLogger.info("SERVER", "👤 [$clientId] Nombre del jugador establecido: $playerName")
                }
                "START_GAME" -> {
                    val request = json.decodeFromString<StartGameRequest>(msg.payload)
                    handleStartGame(request)
                }
                "GUESS" -> {
                    val request = json.decodeFromString<GuessRequest>(msg.payload)
                    if (currentPVPGame != null) {
                        currentPVPGame?.handleGuess(this, request)
                    } else if (currentGame != null) {
                        currentGame?.handleGuess(request)
                    } else {
                        FileLogger.warning("SERVER", "⚠️  [$clientId] GUESS recibido pero no hay juego activo")
                    }
                }
                "GET_RECORDS" -> {
                    handleGetRecords()
                }
                "ABANDON_GAME" -> {
                    handleAbandonGame()
                }
                "CREATE_ROOM" -> {
                    handleCreateRoom()
                }
                "JOIN_ROOM" -> {
                    val request = json.decodeFromString<JoinRoomRequest>(msg.payload)
                    handleJoinRoom(request)
                }
                "LIST_ROOMS" -> {
                    handleListRooms()
                }
                "LEAVE_ROOM" -> {
                    handleLeaveRoom()
                }
                "START_PVP_GAME" -> {
                    handleStartPVPGame()
                }
                "READY_NEXT_ROUND" -> {
                    currentPVPGame?.playerReady(this)
                }
                "REQUEST_HINT" -> {
                    if (currentPVPGame != null) {
                        currentPVPGame?.handleRequestHint(this)
                    } else {
                        currentGame?.handleRequestHint()
                    }
                }
                else -> {
                    FileLogger.warning("SERVER", "⚠️  [$clientId] Tipo de mensaje desconocido: ${msg.type}, payload: ${msg.payload}")
                }
            }
        } catch (e: Exception) {
            FileLogger.error("SERVER", "❌ [$clientId] Error procesando mensaje: '${message.take(100)}...' | Error: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun handleStartGame(request: StartGameRequest) {
        FileLogger.info("SERVER", "🎮 [$clientId] Iniciando partida ${request.mode} para jugador: $playerName")

        when (request.mode) {
            GameMode.PVE -> {
                val config = GameConfig(
                    wordLength = request.wordLength,
                    maxAttempts = request.maxAttempts,
                    rounds = request.rounds,
                    timeoutSeconds = 90,
                    saveRecords = request.saveRecords
                )
                currentGame = gameOrchestrator.createPVEGame(this, config, playerName)
                CoroutineScope(Dispatchers.IO).launch {
                    currentGame?.start()
                }
            }
            GameMode.PVP -> {
                val error = ErrorMessage(
                    code = "USE_ROOMS",
                    message = "Para PVP usa el sistema de salas"
                )
                send("ERROR", json.encodeToString(ErrorMessage.serializer(), error))
            }
        }
    }

    private fun handleCreateRoom() {
        if (currentRoomId != null) {
            send("ERROR", json.encodeToString(ErrorMessage.serializer(), ErrorMessage("ALREADY_IN_ROOM", "Ya estás en una sala")))
            return
        }

        val room = roomManager.createRoom(this, playerName)
        currentRoomId = room.roomId

        val response = RoomCreated(roomId = room.roomId)
        send("ROOM_CREATED", json.encodeToString(RoomCreated.serializer(), response))

        // Enviar actualización de sala
        val update = RoomUpdate(
            roomId = room.roomId,
            players = room.getPlayerNamesList(),
            hostName = room.hostName
        )
        send("ROOM_UPDATE", json.encodeToString(RoomUpdate.serializer(), update))
    }

    private fun handleJoinRoom(request: JoinRoomRequest) {
        if (currentRoomId != null) {
            send("ERROR", json.encodeToString(ErrorMessage.serializer(), ErrorMessage("ALREADY_IN_ROOM", "Ya estás en una sala")))
            return
        }

        val room = roomManager.joinRoom(request.roomId, this, playerName)
        if (room == null) {
            send("ERROR", json.encodeToString(ErrorMessage.serializer(), ErrorMessage("ROOM_NOT_FOUND", "Sala no encontrada o llena")))
            return
        }

        currentRoomId = room.roomId

        val response = RoomJoined(roomId = room.roomId, players = room.getPlayerNamesList())
        send("ROOM_JOINED", json.encodeToString(RoomJoined.serializer(), response))

        // Notificar a todos los jugadores de la sala
        val update = RoomUpdate(
            roomId = room.roomId,
            players = room.getPlayerNamesList(),
            hostName = room.hostName
        )
        room.players.forEach { player ->
            player.send("ROOM_UPDATE", json.encodeToString(RoomUpdate.serializer(), update))
        }
    }

    private fun handleListRooms() {
        val rooms = roomManager.listAvailableRooms()
        val response = RoomListResponse(rooms = rooms)
        send("ROOM_LIST", json.encodeToString(RoomListResponse.serializer(), response))
    }

    private fun handleLeaveRoom() {
        val roomId = currentRoomId
        if (roomId == null) {
            send("ERROR", json.encodeToString(ErrorMessage.serializer(), ErrorMessage("NOT_IN_ROOM", "No estás en ninguna sala")))
            return
        }

        val (isEmpty, room) = roomManager.leaveRoom(roomId, this)
        currentRoomId = null

        if (!isEmpty && room != null) {
            // Notificar al resto
            val update = RoomUpdate(
                roomId = room.roomId,
                players = room.getPlayerNamesList(),
                hostName = room.hostName
            )
            room.players.forEach { player ->
                player.send("ROOM_UPDATE", json.encodeToString(RoomUpdate.serializer(), update))
            }

            val playerLeft = PlayerLeft(playerName = playerName)
            room.players.forEach { player ->
                player.send("PLAYER_LEFT", json.encodeToString(PlayerLeft.serializer(), playerLeft))
            }
        }
    }

    private fun handleStartPVPGame() {
        val roomId = currentRoomId
        if (roomId == null) {
            send("ERROR", json.encodeToString(ErrorMessage.serializer(), ErrorMessage("NOT_IN_ROOM", "No estás en ninguna sala")))
            return
        }

        val room = roomManager.getRoom(roomId)
        if (room == null) {
            send("ERROR", json.encodeToString(ErrorMessage.serializer(), ErrorMessage("ROOM_NOT_FOUND", "Sala no encontrada")))
            return
        }

        if (room.host != this) {
            send("ERROR", json.encodeToString(ErrorMessage.serializer(), ErrorMessage("NOT_HOST", "Solo el creador puede iniciar la partida")))
            return
        }

        if (room.players.size < 2) {
            send("ERROR", json.encodeToString(ErrorMessage.serializer(), ErrorMessage("NOT_ENOUGH_PLAYERS", "Se necesitan al menos 2 jugadores")))
            return
        }

        room.status = RoomStatus.IN_GAME
        val pvpGame = gameOrchestrator.createPVPGame(room)

        // Asignar el juego PVP a todos los jugadores
        room.players.forEach { player ->
            player.currentPVPGame = pvpGame
        }

        FileLogger.info("SERVER", "🎮 Partida PVP iniciada desde sala $roomId con ${room.players.size} jugadores")

        CoroutineScope(Dispatchers.IO).launch {
            pvpGame.start()
            // Limpiar cuando termine la partida
            room.players.forEach { player ->
                player.currentPVPGame = null
                player.currentRoomId = null
            }
            roomManager.removeRoom(roomId)
        }
    }

    private suspend fun handleGetRecords() {
        val records = recordsManager.getRecords()
        val response = RecordsResponse(
            pveRecords = records.pveRecordsByLength.map { (k, v) -> k.toString() to v.toList() }.toMap(),
            pvpRecords = records.pvpRecordsByLength.map { (k, v) -> k.toString() to v.toList() }.toMap()
        )
        send("RECORDS", json.encodeToString(RecordsResponse.serializer(), response))
    }

    private suspend fun handleAbandonGame() {
        if (currentPVPGame != null) {
            currentPVPGame?.removePlayer(this)
            currentPVPGame = null
            currentRoomId = null
            return
        }

        if (currentGame == null) {
            FileLogger.warning("SERVER", "⚠️  [$clientId] ABANDON_GAME recibido pero no hay juego activo")
            val error = ErrorMessage("NO_ACTIVE_GAME", "No hay partida activa para abandonar")
            send("ERROR", json.encodeToString(ErrorMessage.serializer(), error))
            return
        }

        currentGame?.abandon()
        currentGame = null
    }

    fun send(type: String, payload: String) {
        val message = Message(type, payload)
        val json = Json.encodeToString(Message.serializer(), message)
        output.println(json)
        output.flush()
        FileLogger.debug("SERVER", "📤 [$clientId] Enviado: $type")
    }

    private fun cleanup() {
        FileLogger.info("SERVER", "👋 Cliente desconectado: $clientId")

        // Si estaba en una sala, salir
        currentRoomId?.let { roomId ->
            val (isEmpty, room) = roomManager.leaveRoom(roomId, this)
            if (!isEmpty && room != null) {
                val update = RoomUpdate(
                    roomId = room.roomId,
                    players = room.getPlayerNamesList(),
                    hostName = room.hostName
                )
                room.players.forEach { player ->
                    player.send("ROOM_UPDATE", json.encodeToString(RoomUpdate.serializer(), update))
                }
            }
        }

        // Si estaba en partida PVP, notificar
        currentPVPGame?.let { game ->
            CoroutineScope(Dispatchers.IO).launch {
                game.removePlayer(this@ClientHandler)
            }
        }

        try {
            socket.close()
        } catch (e: Exception) {
            // Ignorar errores al cerrar
        }
    }
}
