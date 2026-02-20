import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import protocol.GameMode
import view.pantallas.*
import viewmodel.GameViewModel

/**
 * Enum de pantallas de la aplicación.
 * Cada valor representa una pantalla navegable desde [App].
 */
enum class Screen { MENU, PVE_MODE_SELECT, PVE_CUSTOM_CONFIG, SELECT_WORD_LENGTH, GAME, RECORDS, PVP_LOBBY, PVP_ROOM }

/**
 * Composable raíz de la aplicación.
 * Gestiona la navegación entre pantallas con un estado [Screen] simple
 * y un único [viewmodel.GameViewModel] compartido por todas ellas.
 */
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
                onStartPVE = { currentScreen = Screen.PVE_MODE_SELECT },
                onStartPVP = { currentScreen = Screen.PVP_LOBBY },
                onViewRecords = { currentScreen = Screen.RECORDS },
                onResumeGame = { currentScreen = Screen.GAME }
            )

            Screen.PVE_MODE_SELECT -> PVEModeSelectionScreen(
                onClassic = {
                    coroutineScope.launch {
                        viewModel.startGame(
                            mode = GameMode.PVE,
                            rounds = 5,
                            wordLength = 5,
                            maxAttempts = 6,
                            saveRecords = true
                        )
                        currentScreen = Screen.GAME
                    }
                },
                onCustom = { currentScreen = Screen.PVE_CUSTOM_CONFIG },
                onBack = { currentScreen = Screen.MENU }
            )

            Screen.PVE_CUSTOM_CONFIG -> PVECustomConfigScreen(
                onStart = { config ->
                    coroutineScope.launch {
                        viewModel.startGame(
                            mode = GameMode.PVE,
                            rounds = config.rounds,
                            wordLength = config.wordLength,
                            maxAttempts = config.maxAttempts,
                            saveRecords = false
                        )
                        currentScreen = Screen.GAME
                    }
                },
                onBack = { currentScreen = Screen.PVE_MODE_SELECT }
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
