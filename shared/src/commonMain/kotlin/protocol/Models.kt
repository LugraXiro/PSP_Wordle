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
    val timeoutSeconds: Int = 90
)

@Serializable
data class WordData(
    val palabra: String,
    val pista: String
)
