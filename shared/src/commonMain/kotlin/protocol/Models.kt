package protocol

import kotlinx.serialization.Serializable

/** Modo de juego: jugador contra entorno o jugador contra jugador. */
@Serializable
enum class GameMode {
    PVE, PVP
}

/**
 * Estado de una letra tras evaluar un intento.
 *
 * - [CORRECT]: la letra está en la posición correcta (verde).
 * - [PRESENT]: la letra existe en la palabra pero en otra posición (amarillo).
 * - [ABSENT]: la letra no aparece en la palabra (gris).
 */
@Serializable
enum class LetterStatus {
    CORRECT,
    PRESENT,
    ABSENT
}

/**
 * Parámetros de configuración de una partida.
 *
 * @property wordLength Número de letras de la palabra a adivinar.
 * @property maxAttempts Intentos máximos por ronda.
 * @property rounds Número de rondas que componen la partida.
 * @property timeoutSeconds Segundos de tiempo máximo por ronda antes de que expire automáticamente.
 * @property saveRecords Si la puntuación final debe guardarse en el ranking de récords.
 */
@Serializable
data class GameConfig(
    val wordLength: Int = 5,
    val maxAttempts: Int = 6,
    val rounds: Int = 5,
    val timeoutSeconds: Int = 90,
    val saveRecords: Boolean = true
)

/**
 * Entrada del diccionario que asocia una palabra con su pista.
 *
 * @property palabra La palabra a adivinar.
 * @property pista Texto de ayuda que el servidor puede revelar con penalización de puntuación.
 */
@Serializable
data class WordData(
    val palabra: String,
    val pista: String
)

/**
 * Estadísticas acumuladas de un jugador a lo largo de todas sus partidas PVE.
 *
 * @property gamesPlayed Total de partidas jugadas.
 * @property gamesWon Total de partidas ganadas.
 * @property currentStreak Racha actual de partidas ganadas consecutivamente.
 * @property maxStreak Racha máxima histórica de victorias consecutivas.
 * @property guessDistribution Mapa de distribución de intentos: clave = número de intentos (como string),
 *   valor = cuántas veces se acertó en ese número de intentos.
 * @property totalWordsAttempted Total de palabras a las que se ha intentado responder.
 * @property totalWordsGuessed Total de palabras acertadas.
 * @property totalTimeSeconds Suma de segundos empleados en palabras acertadas, usado para calcular el promedio.
 */
@Serializable
data class PlayerStats(
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val guessDistribution: Map<String, Int> = emptyMap(),
    val totalWordsAttempted: Int = 0,
    val totalWordsGuessed: Int = 0,
    val totalTimeSeconds: Long = 0
)
