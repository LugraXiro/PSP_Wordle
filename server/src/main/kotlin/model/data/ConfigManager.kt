package model.data

import logging.FileLogger
import java.io.File
import java.util.Properties

/**
 * Configuración de red del servidor, cargada desde `server.properties`.
 *
 * @property host Dirección IP o hostname en la que escucha el servidor.
 * @property port Puerto TCP en el que escucha el servidor.
 * @property maxClients Número máximo de clientes conectados simultáneamente.
 */
data class ServerConfig(
    val host: String,
    val port: Int,
    val maxClients: Int
)

/**
 * Carga la configuración del servidor desde un archivo `.properties`.
 *
 * Si el archivo no existe, lo crea con valores por defecto
 * (host: localhost, puerto: 5678, máximo de clientes: 10).
 *
 * @param configFile Ruta al archivo de configuración.
 */
class ConfigManager(private val configFile: String = "server.properties") {

    fun loadConfig(): ServerConfig {
        val props = Properties()
        val file = File(configFile)

        return if (file.exists()) {
            try {
                file.inputStream().use { props.load(it) }
                val config = ServerConfig(
                    host = props.getProperty("server.host", "localhost"),
                    port = props.getProperty("server.port", "5678").toInt(),
                    maxClients = props.getProperty("max.clients", "10").toInt()
                )
                FileLogger.info("SERVER", "✅ Configuración cargada desde '$configFile': host=${config.host}, port=${config.port}, maxClients=${config.maxClients}")
                config
            } catch (e: Exception) {
                FileLogger.error("SERVER", "❌ Error parseando '$configFile': ${e.javaClass.simpleName}: ${e.message}. Usando configuración por defecto")
                createDefaultConfig(file)
                ServerConfig(host = "localhost", port = 5678, maxClients = 10)
            }
        } else {
            FileLogger.warning("SERVER", "⚠️  Archivo de configuración '$configFile' no encontrado en ${file.absolutePath}. Creando con valores por defecto")
            createDefaultConfig(file)
            ServerConfig(
                host = "localhost",
                port = 5678,
                maxClients = 10
            )
        }
    }

    private fun createDefaultConfig(file: File) {
        try {
            file.writeText("""
                # Configuración del Servidor Wordle
                server.host=localhost
                server.port=5678
                max.clients=10
            """.trimIndent())
            FileLogger.info("SERVER", "✅ Archivo de configuración '$configFile' creado con valores por defecto en: ${file.absolutePath}")
        } catch (e: Exception) {
            FileLogger.error("SERVER", "❌ Error crítico al crear archivo de configuración '$configFile': ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
        }
    }
}
