import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition

/**
 * Punto de entrada de la aplicación desktop (JVM).
 * Crea la ventana principal de 800×900 px y lanza el composable raíz [App].
 */
fun main() = application {
    val mainWindowState = rememberWindowState(
        width = 800.dp,
        height = 900.dp,
        position = WindowPosition(x = 50.dp, y = 50.dp)
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "Wordle Multiplatform",
        state = mainWindowState
    ) {
        App()
    }
}
