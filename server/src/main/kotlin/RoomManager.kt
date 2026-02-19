import logging.FileLogger
import protocol.RoomInfo
import java.util.*
import java.util.concurrent.ConcurrentHashMap

enum class RoomStatus {
    WAITING, IN_GAME
}

class Room(
    val roomId: String,
    val host: ClientHandler,
    val hostName: String,
    val maxPlayers: Int = 4
) {
    val players: MutableList<ClientHandler> = mutableListOf(host)
    val playerNames: MutableMap<ClientHandler, String> = mutableMapOf(host to hostName)
    var status: RoomStatus = RoomStatus.WAITING

    fun addPlayer(client: ClientHandler, name: String): Boolean {
        if (players.size >= maxPlayers || status != RoomStatus.WAITING) return false
        players.add(client)
        playerNames[client] = name
        return true
    }

    fun removePlayer(client: ClientHandler): Boolean {
        players.remove(client)
        playerNames.remove(client)
        return players.isEmpty()
    }

    fun getPlayerNamesList(): List<String> = players.mapNotNull { playerNames[it] }

    fun toRoomInfo(): RoomInfo = RoomInfo(
        roomId = roomId,
        hostName = hostName,
        playerCount = players.size,
        maxPlayers = maxPlayers
    )
}

class RoomManager {
    private val rooms = ConcurrentHashMap<String, Room>()

    fun createRoom(host: ClientHandler, playerName: String): Room {
        val roomId = UUID.randomUUID().toString().substring(0, 6).uppercase()
        val room = Room(roomId = roomId, host = host, hostName = playerName)
        rooms[roomId] = room
        FileLogger.info("SERVER", "🏠 Sala creada: $roomId por $playerName")
        return room
    }

    fun joinRoom(roomId: String, client: ClientHandler, playerName: String): Room? {
        val room = rooms[roomId] ?: return null
        synchronized(room) {
            if (!room.addPlayer(client, playerName)) return null
        }
        FileLogger.info("SERVER", "🏠 $playerName se unió a sala $roomId (${room.players.size}/${room.maxPlayers})")
        return room
    }

    fun leaveRoom(roomId: String, client: ClientHandler): Pair<Boolean, Room?> {
        val room = rooms[roomId] ?: return Pair(false, null)
        val isEmpty: Boolean
        synchronized(room) {
            isEmpty = room.removePlayer(client)
        }
        if (isEmpty) {
            rooms.remove(roomId)
            FileLogger.info("SERVER", "🏠 Sala $roomId eliminada (vacía)")
            return Pair(true, null)
        }
        FileLogger.info("SERVER", "🏠 Jugador salió de sala $roomId (${room.players.size}/${room.maxPlayers})")
        return Pair(false, room)
    }

    fun listAvailableRooms(): List<RoomInfo> {
        return rooms.values
            .filter { it.status == RoomStatus.WAITING }
            .map { it.toRoomInfo() }
    }

    fun getRoom(roomId: String): Room? = rooms[roomId]

    fun removeRoom(roomId: String) {
        rooms.remove(roomId)
        FileLogger.info("SERVER", "🏠 Sala $roomId eliminada")
    }

    fun findRoomByClient(client: ClientHandler): Room? {
        return rooms.values.find { client in it.players }
    }
}
