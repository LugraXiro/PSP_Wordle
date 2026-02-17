package ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import protocol.RecordEntry
import viewmodel.GameViewModel

@Composable
fun RecordsScreen(viewModel: GameViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val wordLengths = listOf(4, 5, 6, 7)
    var selectedLength by remember { mutableStateOf(5) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.width(700.dp).padding(24.dp).heightIn(max = 850.dp),
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

                // Pestañas para seleccionar longitud de palabra
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

                Divider()

                // Contenido scrollable con los records
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val pveRecords = uiState.pveRecordsByLength[selectedLength] ?: emptyList()
                    val pvpRecords = uiState.pvpRecordsByLength[selectedLength] ?: emptyList()

                    RecordTable(title = "Top 10 PVE", records = pveRecords)
                    Spacer(modifier = Modifier.height(8.dp))
                    RecordTable(title = "Top 10 PVP", records = pvpRecords)
                }

                // Botón volver
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
            Divider(modifier = Modifier.padding(vertical = 8.dp))

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
    val medal = when (position) {
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
                    text = medal,
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
