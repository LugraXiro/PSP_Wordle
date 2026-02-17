import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File

@Composable
fun LogsWindow(onClose: () -> Unit) {
    var logs by remember { mutableStateOf<List<String>>(emptyList()) }
    val listState = rememberLazyListState()

    // Buscar el archivo de logs en el directorio raíz del proyecto
    val logFile = remember {
        var currentDir = File(System.getProperty("user.dir"))
        while (currentDir != null && !File(currentDir, "settings.gradle.kts").exists()) {
            currentDir = currentDir.parentFile
        }
        val projectRoot = currentDir ?: File(System.getProperty("user.dir"))
        File(projectRoot, "wordle-logs.txt")
    }

    // Leer logs periódicamente
    LaunchedEffect(Unit) {
        while (isActive) {
            try {
                if (logFile.exists()) {
                    val newLogs = logFile.readLines()
                    if (newLogs != logs) {
                        logs = newLogs
                        // Auto-scroll al final
                        if (logs.isNotEmpty()) {
                            listState.animateScrollToItem(logs.size - 1)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignorar errores de lectura
            }
            delay(500) // Actualizar cada 500ms
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF1E1E1E)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF2D2D2D),
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "📋 Logs del Sistema",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Button(
                        onClick = {
                            logFile.writeText("")
                            logs = emptyList()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF444444)
                        )
                    ) {
                        Text("🗑️ Limpiar")
                    }
                }
            }

            // Logs list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                state = listState
            ) {
                items(logs) { log ->
                    LogEntry(log)
                }
            }
        }
    }
}

@Composable
fun LogEntry(log: String) {
    // Colores para diferentes clientes (basado en hash del ID)
    val clientColors = listOf(
        Color(0xFF00D9FF), // Cian
        Color(0xFFFF6B9D), // Rosa
        Color(0xFFFFC145), // Naranja
        Color(0xFFB565FF), // Púrpura
        Color(0xFF00FFA3), // Verde agua
        Color(0xFFFF6B6B), // Rojo claro
        Color(0xFF4ECDC4), // Turquesa
        Color(0xFFF7B731), // Amarillo
        Color(0xFF57C7FF), // Azul cielo
        Color(0xFFFF85A2), // Rosa salmón
        Color(0xFF5FD068), // Verde lima
        Color(0xFFFFD93D)  // Amarillo oro
    )

    // Extraer ID de cliente del log si existe [CLIENT-xxxx]
    val clientIdRegex = """\[CLIENT-([a-f0-9]{4})\]""".toRegex()
    val clientIdMatch = clientIdRegex.find(log)
    val clientId = clientIdMatch?.groupValues?.get(1)

    // Determinar color del texto
    val textColor = when {
        log.contains("[SERVER]") -> Color(0xFF00FF41) // Verde Matrix
        clientId != null -> {
            // Asignar color basado en hash del ID del cliente
            val colorIndex = clientId.hashCode().mod(clientColors.size)
            clientColors[colorIndex]
        }
        log.contains("[CLIENT]") -> Color(0xFF4ECDC4) // Turquesa por defecto
        log.contains("[ERROR]") -> Color(0xFFFF6B6B)
        log.contains("[WARN]") -> Color(0xFFFFA500)
        log.contains("[INFO]") -> Color(0xFF95E1D3)
        log.contains("[DEBUG]") -> Color(0xFF888888)
        else -> Color(0xFFCCCCCC)
    }

    val backgroundColor = when {
        log.contains("[SERVER]") -> Color(0xFF0A1F0A) // Fondo oscuro verdoso para servidor
        clientId != null -> Color(0xFF1A1A2E) // Fondo oscuro para clientes
        log.contains("[CLIENT]") -> Color(0xFF1A1A2E)
        else -> Color(0xFF1E1E1E)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        color = backgroundColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = log,
            modifier = Modifier.padding(4.dp),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = textColor,
            lineHeight = 16.sp
        )
    }
}
