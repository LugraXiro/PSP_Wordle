package protocol

import kotlinx.serialization.Serializable

// ==================== MENSAJES CLIENTE → SERVIDOR ====================

@Serializable
data class StartGameRequest(
    val mode: GameMode,
    val rounds: Int = 5,
    val wordLength: Int = 5,
    val maxAttempts: Int = 6
)

@Serializable
data class GuessRequest(
    val word: String,
    val attemptNumber: Int
)

@Serializable
data class SetPlayerNameRequest(
    val playerName: String
)

@Serializable
class GetRecordsRequest

@Serializable
class AbandonGameRequest  // Cliente solicita abandonar la partida

// ==================== MENSAJES SERVIDOR → CLIENTE ====================

@Serializable
data class GameStarted(
    val gameId: String,
    val mode: GameMode,
    val rounds: Int,
    val currentRound: Int,
    val wordLength: Int,
    val maxAttempts: Int
)

@Serializable
data class LetterResult(
    val letter: Char,
    val status: LetterStatus
)

@Serializable
data class GuessResult(
    val word: String,
    val result: List<LetterResult>,
    val isCorrect: Boolean,
    val attemptsUsed: Int,
    val timeElapsed: Int  // segundos desde inicio de ronda
)

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

@Serializable
data class GameEnd(
    val mode: GameMode,
    val finalScore: Int,
    val roundsWon: Int,
    val totalRounds: Int,
    val isNewRecord: Boolean
)

@Serializable
data class GameAbandoned(
    val message: String = "Partida abandonada"
)

@Serializable
data class OpponentUpdate(  // Solo PVP
    val opponentAttempts: Int,
    val opponentFinished: Boolean,
    val opponentRoundScore: Int? = null
)

@Serializable
data class RecordEntry(
    val playerName: String,
    val score: Int,
    val wordLength: Int = 5  // Default para compatibilidad con records existentes
)

@Serializable
data class RecordsResponse(
    val pveRecords: Map<String, List<RecordEntry>> = emptyMap(),  // "wordLength" -> records
    val pvpRecords: Map<String, List<RecordEntry>> = emptyMap()   // "wordLength" -> records
)

@Serializable
data class ErrorMessage(
    val code: String,
    val message: String
)

// ==================== WRAPPER PARA DESERIALIZACIÓN ====================

@Serializable
data class Message(
    val type: String,
    val payload: String
)
