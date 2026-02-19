package ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CustomGameConfig(
    val wordLength: Int,
    val rounds: Int,
    val maxAttempts: Int
)

private fun difficultyLabel(wordLength: Int): String = when (wordLength) {
    4 -> "Fácil"
    7 -> "Difícil"
    else -> "Medio"
}

private fun difficultyEmoji(wordLength: Int): String = when (wordLength) {
    4 -> "🟢"
    7 -> "🔴"
    else -> "🟡"
}

@Composable
fun PVECustomConfigScreen(
    onStart: (CustomGameConfig) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var wordLength by remember { mutableStateOf(5) }
    var rounds by remember { mutableStateOf(5) }
    var maxAttempts by remember { mutableStateOf(6) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.width(480.dp).padding(24.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "⚙️ Partida Personalizada",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Indicador de dificultad
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (wordLength) {
                            4 -> MaterialTheme.colorScheme.tertiaryContainer
                            7 -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${difficultyEmoji(wordLength)} Dificultad: ${difficultyLabel(wordLength)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                HorizontalDivider()

                // Longitud de palabra
                ConfigSection(title = "Letras por palabra") {
                    OptionRow(
                        options = listOf(4, 5, 6, 7),
                        selected = wordLength,
                        onSelect = { wordLength = it },
                        label = { "$it letras" }
                    )
                }

                // Número de rondas
                ConfigSection(title = "Número de rondas") {
                    OptionRow(
                        options = listOf(3, 5, 7),
                        selected = rounds,
                        onSelect = { rounds = it },
                        label = { "Mejor de $it" }
                    )
                }

                // Intentos máximos
                ConfigSection(title = "Intentos por ronda") {
                    OptionRow(
                        options = listOf(4, 6, 8, 10),
                        selected = maxAttempts,
                        onSelect = { maxAttempts = it },
                        label = { "$it intentos" }
                    )
                }

                HorizontalDivider()

                // Aviso de récords
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ℹ️ Las partidas personalizadas no cuentan para los récords.",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Botones
                Button(
                    onClick = { onStart(CustomGameConfig(wordLength, rounds, maxAttempts)) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("▶ Iniciar Partida", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("← Volver", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun ConfigSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}

@Composable
private fun <T> OptionRow(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            if (isSelected) {
                Button(
                    onClick = { onSelect(option) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(label(option), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = { onSelect(option) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(label(option), fontSize = 13.sp)
                }
            }
        }
    }
}
