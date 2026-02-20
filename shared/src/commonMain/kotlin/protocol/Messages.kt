package protocol

import kotlinx.serialization.Serializable

// ==================== MENSAJES CLIENTE → SERVIDOR ====================

/**
 * Solicita al servidor iniciar una nueva partida.
 *
 * @property mode Modo de juego ([GameMode.PVE] o [GameMode.PVP]).
 * @property rounds Número de rondas de la partida.
 * @property wordLength Longitud de la palabra a adivinar en cada ronda.
 * @property maxAttempts Número máximo de intentos por ronda.
 * @property saveRecords Si es `true`, la puntuación final se registra en el ranking.
 */
@Serializable
data class StartGameRequest(
    val mode: GameMode,
    val rounds: Int = 5,
    val wordLength: Int = 5,
    val maxAttempts: Int = 6,
    val saveRecords: Boolean = true
)

/**
 * Envía un intento de palabra al servidor.
 *
 * @property word La palabra introducida por el jugador.
 * @property attemptNumber Número de intento dentro de la ronda actual (empieza en 1).
 */
@Serializable
data class GuessRequest(
    val word: String,
    val attemptNumber: Int
)

/**
 * Establece el nombre visible del jugador en el servidor.
 *
 * El servidor valida que no esté vacío y que no supere los 20 caracteres.
 *
 * @property playerName Nombre elegido por el jugador.
 */
@Serializable
data class SetPlayerNameRequest(
    val playerName: String
)

/** Solicita al servidor la lista de récords actuales (PVE y PVP). */
@Serializable
class GetRecordsRequest

/** Solicita al servidor abandonar la partida activa. */
@Serializable
class AbandonGameRequest  // Cliente solicita abandonar la partida

/** Solicita al servidor crear una nueva sala PVP. */
@Serializable
class CreateRoomRequest  // Crear sala PVP

/**
 * Solicita unirse a una sala PVP existente.
 *
 * @property roomId Identificador de la sala a la que se quiere acceder.
 */
@Serializable
data class JoinRoomRequest(
    val roomId: String
)

/** Solicita al servidor la lista de salas PVP disponibles (no iniciadas y con hueco). */
@Serializable
class ListRoomsRequest  // Pedir lista de salas disponibles

/** Solicita salir de la sala PVP en la que se encuentra el jugador. */
@Serializable
class LeaveRoomRequest  // Salir de la sala actual

/** El host de una sala solicita iniciar la partida PVP. */
@Serializable
class StartPVPGameRequest  // El host inicia la partida PVP

/** El jugador indica al servidor que está listo para comenzar la siguiente ronda PVP. */
@Serializable
class ReadyNextRoundRequest  // Jugador listo para siguiente ronda PVP

// ==================== MENSAJES SERVIDOR → CLIENTE ====================

/**
 * Confirma el inicio de una partida y envía su configuración al cliente.
 *
 * @property gameId Identificador único de la partida.
 * @property mode Modo de juego activo.
 * @property rounds Total de rondas de la partida.
 * @property currentRound Ronda en la que comienza (1 en partidas nuevas, >1 si se reanuda).
 * @property wordLength Longitud de la palabra a adivinar.
 * @property maxAttempts Intentos máximos por ronda.
 */
@Serializable
data class GameStarted(
    val gameId: String,
    val mode: GameMode,
    val rounds: Int,
    val currentRound: Int,
    val wordLength: Int,
    val maxAttempts: Int
)

/**
 * Estado de una letra individual dentro de un intento evaluado.
 *
 * @property letter La letra evaluada.
 * @property status Resultado de la evaluación ([LetterStatus]).
 */
@Serializable
data class LetterResult(
    val letter: Char,
    val status: LetterStatus
)

/**
 * Resultado completo de un intento enviado por el jugador.
 *
 * @property word La palabra que se evaluó.
 * @property result Lista con el estado de cada letra.
 * @property isCorrect `true` si la palabra coincide exactamente con la solución.
 * @property attemptsUsed Número de intentos consumidos hasta este punto en la ronda.
 * @property timeElapsed Segundos transcurridos desde el inicio de la ronda.
 */
@Serializable
data class GuessResult(
    val word: String,
    val result: List<LetterResult>,
    val isCorrect: Boolean,
    val attemptsUsed: Int,
    val timeElapsed: Int  // segundos desde inicio de ronda
)

/**
 * Señala el fin de una ronda PVE con su puntuación.
 *
 * @property roundNumber Número de ronda que acaba de terminar.
 * @property won `true` si el jugador acertó la palabra.
 * @property correctWord La palabra correcta de esta ronda.
 * @property attemptsUsed Intentos empleados.
 * @property timeSeconds Tiempo total de la ronda en segundos.
 * @property roundScore Puntuación obtenida en esta ronda.
 * @property totalScore Puntuación acumulada hasta esta ronda.
 */
@Serializable
data class RoundEnd(
    val roundNumber: Int,
    val won: Boolean,
    val correctWord: String,
    val attemptsUsed: Int,
    val timeSeconds: Int,
    val roundScore: Int,
    val totalScore: Int
)

/**
 * Señala el fin de la partida completa con el resultado global.
 *
 * @property mode Modo de la partida finalizada.
 * @property finalScore Puntuación total final.
 * @property roundsWon Rondas ganadas por el jugador.
 * @property totalRounds Total de rondas jugadas.
 * @property isNewRecord `true` si la puntuación supera el récord previo del jugador.
 */
@Serializable
data class GameEnd(
    val mode: GameMode,
    val finalScore: Int,
    val roundsWon: Int,
    val totalRounds: Int,
    val isNewRecord: Boolean
)

/**
 * Notifica que la partida fue abandonada.
 *
 * @property message Mensaje descriptivo del abandono.
 */
@Serializable
data class GameAbandoned(
    val message: String = "Partida abandonada"
)

/**
 * Actualización del progreso del rival en una partida PVP.
 *
 * @property opponentAttempts Número de intentos que ha realizado el rival en la ronda actual.
 * @property opponentFinished `true` si el rival ya terminó su ronda (acertó o agotó intentos).
 * @property opponentRoundScore Puntuación del rival en la ronda, disponible cuando [opponentFinished] es `true`.
 */
@Serializable
data class OpponentUpdate(  // Solo PVP
    val opponentAttempts: Int,
    val opponentFinished: Boolean,
    val opponentRoundScore: Int? = null
)

// ==================== MENSAJES PVP: SALAS ====================

/**
 * Resumen de una sala PVP visible en el listado de salas disponibles.
 *
 * @property roomId Identificador único de la sala.
 * @property hostName Nombre del jugador que creó la sala.
 * @property playerCount Número de jugadores actualmente en la sala.
 * @property maxPlayers Capacidad máxima de la sala.
 */
@Serializable
data class RoomInfo(
    val roomId: String,
    val hostName: String,
    val playerCount: Int,
    val maxPlayers: Int
)

/**
 * Confirmación de que la sala fue creada correctamente.
 *
 * @property roomId Identificador asignado a la nueva sala.
 */
@Serializable
data class RoomCreated(
    val roomId: String
)

/**
 * Confirmación de que el jugador se unió a una sala.
 *
 * @property roomId Identificador de la sala.
 * @property players Lista de nombres de todos los jugadores en la sala en ese momento.
 */
@Serializable
data class RoomJoined(
    val roomId: String,
    val players: List<String>
)

/**
 * Actualización del estado de una sala enviada a todos sus miembros cuando cambia algo
 * (entra o sale un jugador, cambia el host).
 *
 * @property roomId Identificador de la sala.
 * @property players Lista actualizada de nombres de jugadores.
 * @property hostName Nombre del jugador que actúa como host.
 */
@Serializable
data class RoomUpdate(
    val roomId: String,
    val players: List<String>,
    val hostName: String
)

/**
 * Respuesta a [ListRoomsRequest] con las salas disponibles para unirse.
 *
 * @property rooms Lista de salas accesibles en este momento.
 */
@Serializable
data class RoomListResponse(
    val rooms: List<RoomInfo>
)

/**
 * Notifica a los miembros de una sala que un jugador la ha abandonado.
 *
 * @property playerName Nombre del jugador que salió.
 */
@Serializable
data class PlayerLeft(
    val playerName: String
)

/**
 * Puntuación de un jugador al final de una partida PVP.
 *
 * @property playerName Nombre del jugador.
 * @property score Puntuación total obtenida.
 * @property roundsWon Número de rondas ganadas.
 */
@Serializable
data class PlayerScore(
    val playerName: String,
    val score: Int,
    val roundsWon: Int
)

/**
 * Estado de un jugador dentro de la ronda PVP en curso.
 *
 * @property playerName Nombre del jugador.
 * @property attempts Intentos realizados en la ronda actual.
 * @property finished `true` si ya terminó la ronda (acertó o agotó intentos).
 * @property totalScore Puntuación acumulada hasta la ronda actual.
 */
@Serializable
data class PlayerRoundStatus(
    val playerName: String,
    val attempts: Int,
    val finished: Boolean,
    val totalScore: Int
)

/**
 * Actualización periódica del estado de todos los jugadores en la ronda PVP actual.
 *
 * @property players Lista con el estado de cada jugador.
 */
@Serializable
data class PlayersStatusUpdate(
    val players: List<PlayerRoundStatus>
)

/**
 * Señala el fin de una ronda PVP con el ranking actualizado.
 *
 * @property roundNumber Número de ronda que acaba de terminar.
 * @property won `true` si el jugador local acertó la palabra.
 * @property correctWord La palabra correcta de esta ronda.
 * @property attemptsUsed Intentos empleados por el jugador local.
 * @property timeSeconds Duración de la ronda en segundos.
 * @property roundScore Puntuación del jugador local en esta ronda.
 * @property totalScore Puntuación acumulada del jugador local.
 * @property rankings Clasificación de todos los jugadores tras esta ronda.
 */
@Serializable
data class PVPRoundEnd(
    val roundNumber: Int,
    val won: Boolean,
    val correctWord: String,
    val attemptsUsed: Int,
    val timeSeconds: Int,
    val roundScore: Int,
    val totalScore: Int,
    val rankings: List<PlayerScore>
)

/**
 * Señala el fin de la partida PVP con el ranking final.
 *
 * @property finalScore Puntuación total del jugador local.
 * @property rankings Clasificación final de todos los participantes.
 * @property isNewRecord `true` si el jugador local superó su récord personal.
 */
@Serializable
data class PVPGameEnd(
    val finalScore: Int,
    val rankings: List<PlayerScore>,
    val isNewRecord: Boolean
)

/**
 * Informa al jugador de que la sala está esperando a que el resto esté listo para la siguiente ronda.
 *
 * @property readyCount Jugadores que ya confirmaron estar listos.
 * @property totalCount Total de jugadores en la sala.
 */
@Serializable
data class WaitingForPlayers(
    val readyCount: Int,
    val totalCount: Int
)

/**
 * Entrada individual de un ranking de récords.
 *
 * @property playerName Nombre del jugador.
 * @property score Puntuación registrada.
 * @property wordLength Longitud de palabra con la que se consiguió la puntuación.
 */
@Serializable
data class RecordEntry(
    val playerName: String,
    val score: Int,
    val wordLength: Int = 5  // Default para compatibilidad con records existentes
)

/**
 * Respuesta completa a [GetRecordsRequest] con los rankings PVE y PVP.
 *
 * Las claves de los mapas son la longitud de palabra como string (p.ej. `"5"`, `"6"`).
 *
 * @property pveRecords Rankings del modo PVE agrupados por longitud de palabra.
 * @property pvpRecords Rankings del modo PVP agrupados por longitud de palabra.
 */
@Serializable
data class RecordsResponse(
    val pveRecords: Map<String, List<RecordEntry>> = emptyMap(),  // "wordLength" -> records
    val pvpRecords: Map<String, List<RecordEntry>> = emptyMap()   // "wordLength" -> records
)

/**
 * Mensaje de error enviado por el servidor ante una operación inválida.
 *
 * @property code Código de error en mayúsculas (p.ej. `"INVALID_WORD"`, `"NOT_IN_ROOM"`).
 * @property message Descripción legible del error.
 */
@Serializable
data class ErrorMessage(
    val code: String,
    val message: String
)

/**
 * Pista textual enviada por el servidor al cliente cuando la solicita.
 *
 * @property hint Texto de la pista asociada a la palabra de la ronda actual.
 */
@Serializable
data class HintResponse(
    val hint: String
)

/**
 * Notifica al cliente que existe una sesión PVE guardada que puede reanudar.
 *
 * @property currentRound Ronda desde la que se reanudaría la partida.
 * @property totalRounds Total de rondas de la partida original.
 * @property totalScore Puntuación acumulada hasta el momento de la interrupción.
 * @property wordLength Longitud de palabra de la sesión guardada.
 */
@Serializable
data class SessionAvailable(
    val currentRound: Int,
    val totalRounds: Int,
    val totalScore: Int,
    val wordLength: Int
)

/**
 * Respuesta con las estadísticas personales del jugador.
 *
 * @property stats Objeto [PlayerStats] con todos los datos acumulados del jugador.
 */
@Serializable
data class StatsResponse(
    val stats: PlayerStats
)

// ==================== WRAPPER PARA DESERIALIZACIÓN ====================

/**
 * Envoltorio genérico usado para todos los mensajes del protocolo de red.
 *
 * Cada mensaje que viaja por el socket TCP tiene esta estructura:
 * ```json
 * { "type": "GUESS", "payload": "{\"word\":\"perro\",\"attemptNumber\":1}" }
 * ```
 * El receptor primero deserializa el [Message] para conocer el [type], y luego
 * deserializa el [payload] en la data class concreta correspondiente.
 *
 * @property type Identificador del tipo de mensaje (p.ej. `"GUESS"`, `"GUESS_RESULT"`).
 * @property payload Contenido del mensaje serializado como JSON.
 */
@Serializable
data class Message(
    val type: String,
    val payload: String
)
