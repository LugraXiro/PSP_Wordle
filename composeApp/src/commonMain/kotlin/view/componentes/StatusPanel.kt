package view.componentes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Barra de estado de la partida.
 *
 * Muestra en una fila cuatro indicadores: ronda actual, intentos usados,
 * tiempo restante (en rojo cuando quedan ≤10 s) y puntuación acumulada.
 *
 * @param currentRound Ronda en curso.
 * @param totalRounds Total de rondas de la partida.
 * @param attemptsUsed Intentos realizados en la ronda actual.
 * @param maxAttempts Máximo de intentos permitidos por ronda.
 * @param timeElapsed Segundos restantes en la ronda.
 * @param score Puntuación total acumulada.
 */
@Composable
fun StatusPanel(
    currentRound: Int,
    totalRounds: Int,
    attemptsUsed: Int,
    maxAttempts: Int,
    timeElapsed: Int,
    score: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ronda actual
            StatusItem(
                label = "Ronda",
                value = "$currentRound/$totalRounds"
            )

            // Intentos
            StatusItem(
                label = "Intentos",
                value = "$attemptsUsed/$maxAttempts"
            )

            // Tiempo restante
            StatusItem(
                label = "Tiempo",
                value = formatTime(timeElapsed),
                isWarning = timeElapsed <= 10
            )

            // Puntuación
            StatusItem(
                label = "Puntuación",
                value = "$score pts"
            )
        }
    }
}

@Composable
private fun StatusItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isWarning: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", mins, secs)
}
