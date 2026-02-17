import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import protocol.GameMode
import ui.screens.*
import viewmodel.GameViewModel

enum class Screen { MENU, SELECT_WORD_LENGTH, GAME, RECORDS, PVP_LOBBY, PVP_ROOM }

@Composable
fun App() {
    val viewModel = remember { GameViewModel() }
    var currentScreen by remember { mutableStateOf(Screen.MENU) }
    var selectedMode by remember { mutableStateOf(GameMode.PVE) }
    val coroutineScope = rememberCoroutineScope()

    MaterialTheme {
        when (currentScreen) {
            Screen.MENU -> MenuScreen(
                viewModel = viewModel,
                onStartPVE = {
                    selectedMode = GameMode.PVE
                    currentScreen = Screen.SELECT_WORD_LENGTH
                },
                onStartPVP = {
                    currentScreen = Screen.PVP_LOBBY
                },
                onViewRecords = { currentScreen = Screen.RECORDS }
            )

            Screen.SELECT_WORD_LENGTH -> WordLengthSelectionScreen(
                mode = selectedMode,
                onSelectWordLength = { wordLength ->
                    coroutineScope.launch {
                        viewModel.startGame(
                            mode = selectedMode,
                            rounds = 5,
                            wordLength = wordLength,
                            maxAttempts = 6
                        )
                        currentScreen = Screen.GAME
                    }
                },
                onBack = { currentScreen = Screen.MENU }
            )

            Screen.GAME -> GameScreen(
                viewModel = viewModel,
                onBackToMenu = { currentScreen = Screen.MENU }
            )

            Screen.RECORDS -> RecordsScreen(
                viewModel = viewModel,
                onBack = { currentScreen = Screen.MENU }
            )

            Screen.PVP_LOBBY -> PVPLobbyScreen(
                viewModel = viewModel,
                onRoomCreated = { currentScreen = Screen.PVP_ROOM },
                onRoomJoined = { currentScreen = Screen.PVP_ROOM },
                onBack = { currentScreen = Screen.MENU }
            )

            Screen.PVP_ROOM -> PVPRoomScreen(
                viewModel = viewModel,
                onGameStarted = { currentScreen = Screen.GAME },
                onBack = { currentScreen = Screen.PVP_LOBBY }
            )
        }
    }
}
