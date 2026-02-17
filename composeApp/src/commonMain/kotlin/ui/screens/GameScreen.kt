package ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import kotlinx.coroutines.launch
import protocol.PlayerRoundStatus
import protocol.PlayerScore
import ui.components.*
import viewmodel.GameViewModel

@Composable
fun GameScreen(viewModel: GameViewModel, onBackToMenu: () -> Unit, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var showAbandonDialog by remember { mutableStateOf(false) }

    // Solicitar foco cuando se monta el composable
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    if (uiState.showingRoundResult) {
        if (uiState.mode == protocol.GameMode.PVP && uiState.pvpRankings.isNotEmpty()) {
            PVPRoundResultDialog(
                won = uiState.correctWord?.let { uiState.attempts.lastOrNull()?.all { it.status == protocol.LetterStatus.CORRECT } } ?: false,
                correctWord = uiState.correctWord ?: "",
                roundScore = uiState.roundScore,
                totalScore = uiState.score,
                currentRound = uiState.currentRound,
                totalRounds = uiState.totalRounds,
                rankings = uiState.pvpRankings,
                readyForNextRound = uiState.readyForNextRound,
                readyCount = uiState.readyCount,
                totalCount = uiState.totalPlayerCount,
                onReady = { viewModel.sendReadyForNextRound() }
            )
        } else {
            RoundResultDialog(won = uiState.correctWord?.let { uiState.attempts.lastOrNull()?.all { it.status == protocol.LetterStatus.CORRECT } } ?: false, correctWord = uiState.correctWord ?: "", roundScore = uiState.roundScore, totalScore = uiState.score, currentRound = uiState.currentRound, totalRounds = uiState.totalRounds, onContinue = { viewModel.continueToNextRound() })
        }
    }

    if (uiState.gameEnded) {
        if (uiState.mode == protocol.GameMode.PVP && uiState.pvpRankings.isNotEmpty()) {
            PVPGameEndDialog(
                finalScore = uiState.finalScore,
                rankings = uiState.pvpRankings,
                isNewRecord = uiState.isNewRecord,
                playerName = uiState.playerName,
                onBackToMenu = { viewModel.backToMenu(); onBackToMenu() }
            )
        } else {
            GameEndDialog(finalScore = uiState.finalScore, roundsWon = uiState.roundsWon, totalRounds = uiState.totalRounds, isNewRecord = uiState.isNewRecord, onBackToMenu = { viewModel.backToMenu(); onBackToMenu() })
        }
    }

    if (uiState.waitingForPlayers && !uiState.showingRoundResult) {
        WaitingForPlayersDialog(
            readyCount = uiState.readyCount,
            totalCount = uiState.totalPlayerCount
        )
    }

    if (uiState.gameAbandoned) {
        GameAbandonedDialog(onBackToMenu = { viewModel.backToMenu(); onBackToMenu() })
    }

    if (showAbandonDialog) {
        AbandonConfirmDialog(
            onConfirm = {
                showAbandonDialog = false
                scope.launch {
                    viewModel.abandonGame()
                }
            },
            onDismiss = { showAbandonDialog = false }
        )
    }

    val isPVP = uiState.mode == protocol.GameMode.PVP

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .focusRequester(focusRequester)
            .focusTarget()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && !uiState.showingRoundResult && !uiState.gameEnded) {
                    when {
                        // Letras A-Z
                        keyEvent.key.keyCode >= Key.A.keyCode && keyEvent.key.keyCode <= Key.Z.keyCode -> {
                            val char = ('A'.code + (keyEvent.key.keyCode - Key.A.keyCode)).toInt().toChar()
                            viewModel.updateInput(char)
                            true
                        }
                        // Tecla Ñ (puede variar según el teclado)
                        keyEvent.utf16CodePoint == 'Ñ'.code || keyEvent.utf16CodePoint == 'ñ'.code -> {
                            viewModel.updateInput('Ñ')
                            true
                        }
                        // Vocales con tilde - normalizar a sin tilde
                        keyEvent.utf16CodePoint in listOf('Á'.code, 'á'.code) -> {
                            viewModel.updateInput('A')
                            true
                        }
                        keyEvent.utf16CodePoint in listOf('É'.code, 'é'.code) -> {
                            viewModel.updateInput('E')
                            true
                        }
                        keyEvent.utf16CodePoint in listOf('Í'.code, 'í'.code) -> {
                            viewModel.updateInput('I')
                            true
                        }
                        keyEvent.utf16CodePoint in listOf('Ó'.code, 'ó'.code) -> {
                            viewModel.updateInput('O')
                            true
                        }
                        keyEvent.utf16CodePoint in listOf('Ú'.code, 'ú'.code, 'Ü'.code, 'ü'.code) -> {
                            viewModel.updateInput('U')
                            true
                        }
                        // Backspace
                        keyEvent.key == Key.Backspace || keyEvent.key == Key.Delete -> {
                            viewModel.deleteLetter()
                            true
                        }
                        // Enter
                        keyEvent.key == Key.Enter -> {
                            scope.launch {
                                viewModel.submitGuess()
                            }
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Columna principal del juego
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Jugador: ${uiState.playerName}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                StatusPanel(currentRound = uiState.currentRound, totalRounds = uiState.totalRounds, attemptsUsed = uiState.attempts.size, maxAttempts = uiState.maxAttempts, timeElapsed = uiState.timeRemaining, score = uiState.score)
                Spacer(modifier = Modifier.height(8.dp))
                WordGrid(attempts = uiState.attempts, currentInput = uiState.currentInput, maxAttempts = uiState.maxAttempts, wordLength = uiState.wordLength)
                Spacer(modifier = Modifier.height(16.dp))
                VirtualKeyboard(
                    usedLetters = uiState.usedLetters,
                    onLetterClick = { viewModel.updateInput(it) },
                    onDeleteClick = { viewModel.deleteLetter() },
                    onEnterClick = {
                        scope.launch {
                            viewModel.submitGuess()
                        }
                    },
                    enabled = !uiState.showingRoundResult && !uiState.gameEnded
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showAbandonDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Abandonar Partida")
                }

                uiState.error?.let { error -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Text(text = error, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer) }; LaunchedEffect(error) { kotlinx.coroutines.delay(3000); viewModel.clearError() } }
            }
        }

        // Panel lateral de jugadores (solo PVP)
        if (isPVP && uiState.pvpPlayersStatus.isNotEmpty()) {
            Spacer(modifier = Modifier.width(16.dp))
            PVPPlayersPanel(
                playersStatus = uiState.pvpPlayersStatus,
                currentPlayerName = uiState.playerName,
                modifier = Modifier.width(220.dp)
            )
        }
    }
}

@Composable
private fun PVPPlayersPanel(
    playersStatus: List<PlayerRoundStatus>,
    currentPlayerName: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxHeight(0.6f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Jugadores",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()
            playersStatus.forEach { player ->
                val isCurrentPlayer = player.playerName == currentPlayerName
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentPlayer)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (player.finished) "\u2705" else "\u23F3",
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = player.playerName,
                                    fontWeight = if (isCurrentPlayer) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = if (player.finished) "Terminado" else "Intento ${player.attempts}/6",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${player.totalScore}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundResultDialog(won: Boolean, correctWord: String, roundScore: Int, totalScore: Int, currentRound: Int, totalRounds: Int, onContinue: () -> Unit) {
    var countdown by remember { mutableStateOf(10) }
    val focusRequester = remember { FocusRequester() }

    // Contador regresivo
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        while (countdown > 0) {
            kotlinx.coroutines.delay(1000)
            countdown--
        }
        onContinue()
    }

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text = if (won) "¡Correcto! 🎉" else "¡Oh no! 😔",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusTarget()
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Enter) {
                            onContinue()
                            true
                        } else {
                            false
                        }
                    }
            ) {
                Text(
                    text = "La palabra era: $correctWord",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Puntos ronda: $roundScore")
                Text("Puntuación total: $totalScore")
                if (currentRound < totalRounds) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ronda $currentRound de $totalRounds completada",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "Continuando en $countdown segundos...",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (currentRound < totalRounds) "Siguiente Ronda → (o presiona Enter)" else "Ver Resultados (o presiona Enter)")
            }
        }
    )
}

@Composable
private fun GameEndDialog(finalScore: Int, roundsWon: Int, totalRounds: Int, isNewRecord: Boolean, onBackToMenu: () -> Unit) {
    AlertDialog(onDismissRequest = {}, title = { Text(text = if (isNewRecord) "¡Nuevo Récord! 🏆" else "Partida Terminada", fontSize = 24.sp, fontWeight = FontWeight.Bold) }, text = { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { if (isNewRecord) { Text(text = "🎊 ¡Has superado tu récord! 🎊", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }; Text(text = "Puntuación Final", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(text = "$finalScore puntos", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Divider(); Text("Rondas ganadas: $roundsWon de $totalRounds"); Text("Porcentaje: ${(roundsWon * 100 / totalRounds)}%") } }, confirmButton = { Button(onClick = onBackToMenu, modifier = Modifier.fillMaxWidth()) { Text("Volver al Menú") } })
}

@Composable
private fun AbandonConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "¿Abandonar partida?",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Si abandonas la partida, perderás toda la puntuación acumulada.",
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Esta acción no se puede deshacer.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Abandonar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Continuar jugando")
            }
        }
    )
}

@Composable
private fun GameAbandonedDialog(onBackToMenu: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text = "Partida Abandonada",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Has abandonado la partida.",
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "La puntuación no ha sido guardada.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onBackToMenu,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver al Menú")
            }
        }
    )
}

@Composable
private fun RankingsList(rankings: List<PlayerScore>, highlightName: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rankings.forEachIndexed { index, playerScore ->
            val medal = when (index) {
                0 -> "🥇"
                1 -> "🥈"
                2 -> "🥉"
                else -> "${index + 1}."
            }
            val isHighlighted = playerScore.playerName == highlightName
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isHighlighted)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = medal,
                            fontSize = 16.sp,
                            modifier = Modifier.width(32.dp)
                        )
                        Text(
                            text = playerScore.playerName,
                            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Text(
                        text = "${playerScore.score} pts",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun PVPRoundResultDialog(
    won: Boolean,
    correctWord: String,
    roundScore: Int,
    totalScore: Int,
    currentRound: Int,
    totalRounds: Int,
    rankings: List<PlayerScore>,
    readyForNextRound: Boolean,
    readyCount: Int,
    totalCount: Int,
    onReady: () -> Unit
) {
    val isLastRound = currentRound >= totalRounds
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text = if (won) "¡Correcto! 🎉" else "¡Oh no! 😔",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusTarget()
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Enter) {
                            if (!isLastRound && !readyForNextRound) onReady()
                            true
                        } else {
                            false
                        }
                    }
            ) {
                Text(
                    text = "La palabra era: $correctWord",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("Puntos ronda: $roundScore")
                Text("Puntuación total: $totalScore")
                Text(
                    text = "Ronda $currentRound de $totalRounds completada",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "Clasificación",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                RankingsList(rankings)
                if (!isLastRound && readyForNextRound) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = if (totalCount > 0) "$readyCount de $totalCount jugadores listos"
                                       else "Esperando jugadores...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isLastRound && !readyForNextRound) {
                Button(
                    onClick = onReady,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Siguiente Ronda →")
                }
            }
        }
    )
}

@Composable
private fun PVPGameEndDialog(
    finalScore: Int,
    rankings: List<PlayerScore>,
    isNewRecord: Boolean,
    playerName: String,
    onBackToMenu: () -> Unit
) {
    val isWinner = rankings.firstOrNull()?.playerName == playerName

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text = when {
                    isWinner -> "¡Has Ganado! 🏆"
                    isNewRecord -> "¡Nuevo Récord! 🎉"
                    else -> "Partida Terminada"
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Tu puntuación",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$finalScore puntos",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (isNewRecord) {
                    Text(
                        text = "🎊 ¡Nuevo récord personal! 🎊",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "Clasificación Final",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                RankingsList(rankings, highlightName = playerName)
            }
        },
        confirmButton = {
            Button(
                onClick = onBackToMenu,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver al Menú")
            }
        }
    )
}

@Composable
private fun WaitingForPlayersDialog(readyCount: Int, totalCount: Int) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text = "Esperando jugadores...",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator()
                if (totalCount > 0) {
                    Text(
                        text = "$readyCount de $totalCount jugadores listos",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Preparando siguiente ronda...",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {}
    )
}
