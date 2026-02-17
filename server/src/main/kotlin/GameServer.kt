import kotlinx.coroutines.*
import logging.FileLogger
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger

class GameServer(
    private val config: ServerConfig,
    private val dictionaryManager: DictionaryManager,
    private val recordsManager: RecordsManager
) {
    private val gameOrchestrator = GameOrchestrator(dictionaryManager, recordsManager)
    private val roomManager = RoomManager()
    private val activeClients = AtomicInteger(0)
    
    suspend fun start() = coroutineScope {
        val serverSocket: ServerSocket

        try {
            serverSocket = ServerSocket(
                config.port,
                50,
                InetAddress.getByName(config.host)
            )
        } catch (e: java.net.BindException) {
            FileLogger.error("SERVER", "╔═════════════════════════════════════════════════════════╗")
            FileLogger.error("SERVER", "║  ❌ ERROR: PUERTO ${config.port} YA ESTÁ EN USO ❌      ║")
            FileLogger.error("SERVER", "╚═════════════════════════════════════════════════════════╝")
            FileLogger.error("SERVER", "")
            FileLogger.error("SERVER", "El puerto ${config.port} ya está siendo usado por otro proceso.")
            FileLogger.error("SERVER", "Esto suele ocurrir cuando:")
            FileLogger.error("SERVER", "  1. Hay otra instancia del servidor ejecutándose")
            FileLogger.error("SERVER", "  2. El servidor anterior no se cerró correctamente")
            FileLogger.error("SERVER", "")
            FileLogger.error("SERVER", "SOLUCIONES:")
            FileLogger.error("SERVER", "  • Windows: Ejecuta 'netstat -ano | findstr :${config.port}' para ver qué proceso lo usa")
            FileLogger.error("SERVER", "            Luego usa 'taskkill /PID <numero> /F' para cerrarlo")
            FileLogger.error("SERVER", "  • Linux/Mac: Ejecuta 'lsof -i :${config.port}' o 'netstat -tulpn | grep ${config.port}'")
            FileLogger.error("SERVER", "               Luego usa 'kill -9 <PID>' para cerrarlo")
            FileLogger.error("SERVER", "  • O cambia el puerto en 'server.properties'")
            FileLogger.error("SERVER", "")
            throw e
        } catch (e: Exception) {
            FileLogger.error("SERVER", "❌ Error al crear ServerSocket: ${e.javaClass.simpleName}: ${e.message}")
            throw e
        }

        FileLogger.info("SERVER", "╔════════════════════════════════════════════╗")
        FileLogger.info("SERVER", "║   🎮 SERVIDOR WORDLE MULTIPLATFORM 🎮     ║")
        FileLogger.info("SERVER", "╚════════════════════════════════════════════╝")
        FileLogger.info("SERVER", "📍 Escuchando en: ${config.host}:${config.port}")
        FileLogger.info("SERVER", "👥 Máximo de clientes: ${config.maxClients}")
        val wordCounts = DictionaryManager.SUPPORTED_LENGTHS
            .map { "$it letras: ${dictionaryManager.getWordCount(it)}" }
            .joinToString(", ")
        FileLogger.info("SERVER", "📚 Palabras cargadas: $wordCounts")
        FileLogger.info("SERVER", "╔═══════════════════════════════════════════════════╗")
        FileLogger.info("SERVER", "║  ✅ ✅ ✅  SERVIDOR OPERATIVO  ✅ ✅ ✅        ║")
        FileLogger.info("SERVER", "║     El servidor está listo para recibir clientes  ║")
        FileLogger.info("SERVER", "╚═══════════════════════════════════════════════════╝")
        FileLogger.info("SERVER", "⏳ Esperando conexiones...")
        
        try {
            while (isActive) {
                val clientSocket = withContext(Dispatchers.IO) {
                    serverSocket.accept()
                }
                
                if (activeClients.get() >= config.maxClients) {
                    FileLogger.warning("SERVER", "⚠️  Servidor lleno (${activeClients.get()}/${config.maxClients}), rechazando conexión desde ${clientSocket.inetAddress.hostAddress}")
                    clientSocket.close()
                    continue
                }
                
                activeClients.incrementAndGet()
                
                launch {
                    try {
                        val handler = ClientHandler(
                            socket = clientSocket,
                            gameOrchestrator = gameOrchestrator,
                            recordsManager = recordsManager,
                            roomManager = roomManager
                        )
                        handler.handle()
                    } finally {
                        activeClients.decrementAndGet()
                        FileLogger.info("SERVER", "👥 Clientes activos: ${activeClients.get()}")
                    }
                }
            }
        } finally {
            serverSocket.close()
            FileLogger.info("SERVER", "╔═══════════════════════════════════════════════════╗")
            FileLogger.info("SERVER", "║  🛑 🛑 🛑  SERVIDOR DETENIDO  🛑 🛑 🛑        ║")
            FileLogger.info("SERVER", "║     El servidor se ha cerrado correctamente       ║")
            FileLogger.info("SERVER", "╚═══════════════════════════════════════════════════╝")
        }
    }
}
