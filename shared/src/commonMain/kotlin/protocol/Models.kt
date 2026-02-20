package protocol

import kotlinx.serialization.Serializable

@Serializable
enum class GameMode {
    PVE, PVP
}

@Serializable
enum class LetterStatus {
    CORRECT,   // Verde: letra en posición correcta
    PRESENT,   // Amarillo: letra existe pero en otra posición
    ABSENT     // Gris: letra no existe en la palabra
}

@Serializable
data class GameConfig(
    val wordLength: Int = 5,
    val maxAttempts: Int = 6,
    val rounds: Int = 5,
    val timeoutSeconds: Int = 90,
    val saveRecords: Boolean = true
)

@Serializable
data class WordData(
    val palabra: String,
    val pista: String
)

@Serializable
data class PlayerStats(
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val guessDistribution: Map<String, Int> = emptyMap(), // "1".."N" -> veces que se acertó en N intentos
    val totalWordsAttempted: Int = 0,
    val totalWordsGuessed: Int = 0,
    val totalTimeSeconds: Long = 0  // suma de tiempos de palabras acertadas (para promedio)
)
