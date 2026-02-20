package view.pantallas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import viewmodel.GameViewModel
import kotlin.system.exitProcess

@Composable
fun MenuScreen(viewModel: GameViewModel, onStartPVE: () -> Unit, onStartPVP: () -> Unit, onViewRecords: () -> Unit, onResumeGame: () -> Unit, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()
    var host by remember { mutableStateOf("localhost") }
    var port by remember { mutableStateOf("5678") }
    var playerName by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    val isReconnecting = uiState.isReconnecting

    val canConnect = !isConnecting && !isReconnecting && playerName.isNotBlank()
    val doConnect: () -> Unit = {
        if (canConnect) {
            isConnecting = true
            kotlinx.coroutines.GlobalScope.launch {
                viewModel.setPlayerName(playerName.ifBlank { "Jugador" })
                viewModel.connect(host, port.toIntOrNull() ?: 5678)
                isConnecting = false
            }
        }
    }
    val onEnterKey: (KeyEvent) -> Boolean = { event ->
        if (event.key == Key.Enter && event.type == KeyEventType.KeyUp && canConnect) {
            doConnect()
            true
        } else false
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.width(450.dp).padding(24.dp), elevation = CardDefaults.cardElevation(8.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = "🎮 WORDLE", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(text = "Multiplatform", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isReconnecting) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Reconectando al servidor...", color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }

                if (!uiState.isConnected) {
                    OutlinedTextField(
                        value = playerName,
                        onValueChange = { if (it.length <= 20) playerName = it },
                        label = { Text("Tu Nombre") },
                        modifier = Modifier.fillMaxWidth().onKeyEvent(onEnterKey),
                        singleLine = true,
                        enabled = !isConnecting,
                        placeholder = { Text("Introduce tu nombre") }
                    )
                    OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Host") }, modifier = Modifier.fillMaxWidth().onKeyEvent(onEnterKey), singleLine = true, enabled = !isConnecting)
                    OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("Puerto") }, modifier = Modifier.fillMaxWidth().onKeyEvent(onEnterKey), singleLine = true, enabled = !isConnecting)
                    Button(
                        onClick = doConnect,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canConnect
                    ) {
                        if (isConnecting || isReconnecting) { CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary); Spacer(modifier = Modifier.width(8.dp)) }
                        Text(when {
                            isReconnecting -> "Reconectando..."
                            isConnecting   -> "Conectando..."
                            else           -> "Conectar al Servidor"
                        })
                    }
                } else {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("✅", fontSize = 20.sp); Spacer(modifier = Modifier.width(8.dp)); Text("Conectado a $host:$port", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onStartPVE, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(8.dp)) { Text("🎯 Jugar PVE", fontSize = 18.sp) }
                    Button(onClick = onStartPVP, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(8.dp)) { Text("👥 Jugar PVP", fontSize = 18.sp) }
                    OutlinedButton(onClick = onViewRecords, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(8.dp)) { Text("🏆 Ver Records", fontSize = 18.sp) }
                }
                
                uiState.error?.let { error -> Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Text(text = error, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer) } }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showExitDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Salir de la aplicación")
                }
            }
        }
    }

    uiState.savedSession?.let { session ->
        AlertDialog(
            onDismissRequest = { viewModel.discardSession() },
            title = { Text("Partida guardada", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Tienes una partida PVE guardada:\n" +
                    "• Ronda ${session.currentRound} de ${session.totalRounds}\n" +
                    "• ${session.wordLength} letras  •  ${session.totalScore} puntos\n\n" +
                    "¿Quieres continuar donde lo dejaste?"
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.resumeSession()
                    onResumeGame()
                }) { Text("Continuar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.discardSession() }) { Text("Descartar") }
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    text = "¿Salir de la aplicación?",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que quieres cerrar la aplicación?",
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { exitProcess(0) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Salir")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExitDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
