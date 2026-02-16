import kotlinx.coroutines.runBlocking
import logging.FileLogger

fun main() = runBlocking {
    FileLogger.info("SERVER", "🚀 Iniciando servidor Wordle Multiplatform...")

    try {
        // Cargar configuración
        FileLogger.debug("SERVER", "📋 Cargando configuración...")
        val configManager = ConfigManager("server.properties")
        val config = configManager.loadConfig()

        // Cargar diccionario
        FileLogger.debug("SERVER", "📚 Cargando diccionario...")
        val dictionaryManager = DictionaryManager("palabras.json")

        // Cargar/crear records
        FileLogger.debug("SERVER", "🏆 Cargando records...")
        val recordsManager = RecordsManager("records.json")

        // Iniciar servidor
        FileLogger.debug("SERVER", "🌐 Iniciando servidor de red...")
        val server = GameServer(config, dictionaryManager, recordsManager)
        server.start()

    } catch (e: Exception) {
        FileLogger.error("SERVER", "❌ Error fatal al iniciar servidor: ${e.javaClass.simpleName}: ${e.message}")
        FileLogger.error("SERVER", "Stack trace completo:")
        e.printStackTrace()
    } finally {
        FileLogger.info("SERVER", "👋 Cerrando servidor Wordle Multiplatform...")
    }
}
