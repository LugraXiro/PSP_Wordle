package ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import protocol.PlayerStats
import protocol.RecordEntry
import viewmodel.GameViewModel

@Composable
fun RecordsScreen(viewModel: GameViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val wordLengths = listOf(4, 5, 6, 7)
    var selectedLength by remember { mutableStateOf(5) }

    LaunchedEffect(Unit) {
        viewModel.requestStats()
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.width(700.dp).padding(24.dp).heightIn(max = 900.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "TABLA DE RECORDS",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                TabRow(
                    selectedTabIndex = wordLengths.indexOf(selectedLength),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    wordLengths.forEach { length ->
                        Tab(
                            selected = selectedLength == length,
                            onClick = { selectedLength = length },
                            text = {
                                Text(
                                    text = "$length Letras",
                                    fontWeight = if (selectedLength == length) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val pveRecords = uiState.pveRecordsByLength[selectedLength] ?: emptyList()
                    val pvpRecords = uiState.pvpRecordsByLength[selectedLength] ?: emptyList()

                    if (selectedLength == 5) {
                        uiState.playerStats?.let { stats ->
                            StatsPanel(stats = stats)
                        }
                    }

                    RecordTable(title = "Top 10 PVE", records = pveRecords)
                    Spacer(modifier = Modifier.height(8.dp))
                    RecordTable(title = "Top 10 PVP", records = pvpRecords)
                }

                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("← Volver al Menu")
                }
            }
        }
    }
}

@Composable
private fun StatsPanel(stats: PlayerStats) {
    val winRate = if (stats.gamesPlayed > 0) stats.gamesWon * 100 / stats.gamesPlayed else 0
    val wordPct = if (stats.totalWordsAttempted > 0) stats.totalWordsGuessed * 100 / stats.totalWordsAttempted else 0
    val avgTime = if (stats.totalWordsGuessed > 0) stats.totalTimeSeconds / stats.totalWordsGuessed else 0L

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Mis Estadísticas · Clásico 5 letras",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBubble(value = "${stats.gamesPlayed}", label = "Partidas\njugadas")
                StatBubble(value = "$winRate%", label = "% Partidas\nganadas")
                StatBubble(value = "${stats.currentStreak}", label = "Racha\nactual")
                StatBubble(value = "${stats.maxStreak}", label = "Racha\nmáxima")
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBubble(value = "$wordPct%", label = "Palabras\nacertadas")
                StatBubble(value = "${avgTime}s", label = "Tiempo\npromedio")
                StatBubble(value = "${stats.totalWordsGuessed}/${stats.totalWordsAttempted}", label = "Palabras\nresueltas")
            }

            if (stats.guessDistribution.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                Text(
                    text = "Distribución de intentos",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                val maxCount = stats.guessDistribution.values.maxOrNull() ?: 1
                (1..6).forEach { attempt ->
                    val count = stats.guessDistribution[attempt.toString()] ?: 0
                    GuessDistributionRow(attempt = attempt, count = count, maxCount = maxCount)
                }
            }
        }
    }
}

@Composable
private fun StatBubble(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GuessDistributionRow(attempt: Int, count: Int, maxCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().height(22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$attempt",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(16.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        val fraction = if (maxCount > 0) count.toFloat() / maxCount else 0f
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceAtLeast(if (count > 0) 0.04f else 0f))
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(4.dp)
            ) {}
        }
        Text(
            text = "$count",
            fontSize = 13.sp,
            modifier = Modifier.width(28.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun RecordTable(title: String, records: List<RecordEntry>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (records.isEmpty()) {
                Text(
                    text = "No hay records aun",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp)
                )
            } else {
                records.forEachIndexed { index, record ->
                    RecordRow(position = index + 1, record = record)
                }
            }
        }
    }
}

@Composable
private fun RecordRow(position: Int, record: RecordEntry) {
    val label = when (position) {
        1 -> "1"
        2 -> "2"
        3 -> "3"
        else -> "#$position"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (position) {
                1 -> MaterialTheme.colorScheme.primaryContainer
                2 -> MaterialTheme.colorScheme.secondaryContainer
                3 -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    fontSize = if (position <= 3) 24.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(45.dp)
                )
                Text(
                    text = record.playerName,
                    fontSize = 16.sp,
                    fontWeight = if (position <= 3) FontWeight.Bold else FontWeight.Medium
                )
            }
            Text(
                text = "${record.score} pts",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
