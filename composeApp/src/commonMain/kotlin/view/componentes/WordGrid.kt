package view.componentes

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import protocol.LetterResult

/**
 * Cuadrícula de intentos del juego Wordle.
 *
 * Renderiza [maxAttempts] filas de [wordLength] celdas [LetterCell]:
 * - Filas ya enviadas: muestran letra y resultado con animación de flip escalonada.
 * - Fila activa: muestra el texto que el jugador está escribiendo ([currentInput]).
 * - Filas vacías: celdas en blanco.
 *
 * @param attempts Lista de resultados de intentos ya resueltos por el servidor.
 * @param currentInput Texto introducido por el jugador en la fila activa.
 * @param maxAttempts Número máximo de intentos (determina el número de filas).
 * @param wordLength Longitud de la palabra objetivo (determina el número de columnas).
 */
@Composable
fun WordGrid(
    attempts: List<List<LetterResult>>,
    currentInput: String,
    maxAttempts: Int,
    wordLength: Int,
    modifier: Modifier = Modifier
) {
    var lastAttemptCount by remember { mutableStateOf(0) }
    val shouldAnimateRow = remember { mutableStateOf(-1) }

    // Detectar cuando se agrega un nuevo intento
    LaunchedEffect(attempts.size) {
        if (attempts.size > lastAttemptCount) {
            shouldAnimateRow.value = attempts.size - 1
            lastAttemptCount = attempts.size
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        repeat(maxAttempts) { attemptIndex ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Mostrar intentos previos
                if (attemptIndex < attempts.size) {
                    val attempt = attempts[attemptIndex]
                    val shouldAnimate = shouldAnimateRow.value == attemptIndex
                    repeat(wordLength) { letterIndex ->
                        val letterResult = attempt.getOrNull(letterIndex)
                        LetterCell(
                            letter = letterResult?.letter,
                            status = letterResult?.status,
                            shouldAnimate = shouldAnimate,
                            animationDelay = letterIndex * 100
                        )
                    }
                }
                // Mostrar intento actual
                else if (attemptIndex == attempts.size) {
                    repeat(wordLength) { letterIndex ->
                        val letter = currentInput.getOrNull(letterIndex)
                        LetterCell(
                            letter = letter,
                            status = null
                        )
                    }
                }
                // Mostrar filas vacías
                else {
                    repeat(wordLength) {
                        LetterCell(
                            letter = null,
                            status = null
                        )
                    }
                }
            }
        }
    }
}
