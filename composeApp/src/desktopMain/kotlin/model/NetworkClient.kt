package model

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import logging.FileLogger
import protocol.Message
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.UUID

/**
 * Implementación JVM de [NetworkClient] para Compose Desktop.
 *
 * Abre un socket TCP contra el servidor (timeout de conexión 10 s), envía
 * mensajes JSON serializados con [send] y escucha respuestas en un hilo de IO
 * dedicado, emitiéndolas a [messagesFlow]. Emite el estado de la conexión
 * en [connectionState] (`true` = conectado, `false` = desconectado).
 */
actual class NetworkClient actual constructor() {
    actual val clientId: String = UUID.randomUUID().toString().substring(0, 4)
    actual val clientTag: String = "CLIENT-$clientId"

    private var socket: Socket? = null
    private var input: BufferedReader? = null
    private var output: PrintWriter? = null
    private var receiveJob: Job? = null

    private val _messagesFlow = MutableSharedFlow<Message>(replay = 0)
    actual val messagesFlow: SharedFlow<Message> = _messagesFlow.asSharedFlow()

    private val _connectionState = MutableSharedFlow<Boolean>(replay = 1)
    actual val connectionState: SharedFlow<Boolean> = _connectionState.asSharedFlow()

    actual suspend fun connect(host: String, port: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            FileLogger.info(clientTag, "🔌 Conectando al servidor $host:$port...")
            disconnect()

            socket = Socket().also {
                it.connect(InetSocketAddress(host, port), 10_000)
                it.soTimeout = 30_000
            }
            input = BufferedReader(InputStreamReader(socket!!.getInputStream()))
            output = PrintWriter(socket!!.getOutputStream(), true)

            _connectionState.emit(true)
            FileLogger.info(clientTag, "✅ Conectado exitosamente a $host:$port")

            receiveJob = CoroutineScope(Dispatchers.IO).launch {
                receiveMessages()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            FileLogger.error(clientTag, "❌ Error conectando al servidor: ${e.message}")
            _connectionState.emit(false)
            Result.failure(e)
        }
    }

    private suspend fun receiveMessages() {
        try {
            while (socket?.isConnected == true) {
                val line = try {
                    input?.readLine()
                } catch (e: SocketTimeoutException) {
                    continue  // Sin datos en 30 s, seguir esperando (el socket sigue vivo)
                } ?: break
                if (line.isBlank()) continue

                try {
                    val message = kotlinx.serialization.json.Json.decodeFromString<Message>(line)
                    FileLogger.debug(clientTag, "📥 Recibido: ${message.type}")
                    _messagesFlow.emit(message)
                } catch (e: Exception) {
                    FileLogger.error(clientTag, "❌ Error deserializando mensaje: '${line.take(100)}...' | Error: ${e.javaClass.simpleName}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            FileLogger.error(clientTag, "❌ Error en bucle de recepción: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
        } finally {
            FileLogger.info(clientTag, "🔌 Desconectado del servidor")
            _connectionState.emit(false)
        }
    }

    actual suspend fun send(type: String, payload: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (socket?.isConnected != true) {
                FileLogger.error(clientTag, "❌ Intento de envío fallido: Socket no conectado (tipo=$type)")
                return@withContext Result.failure(Exception("No conectado al servidor"))
            }

            val message = Message(type, payload)
            val json = kotlinx.serialization.json.Json.encodeToString(Message.serializer(), message)
            FileLogger.debug(clientTag, "📨 Enviando: $type | Payload: ${payload.take(50)}${if (payload.length > 50) "..." else ""}")

            output?.println(json)
            output?.flush()

            Result.success(Unit)
        } catch (e: Exception) {
            FileLogger.error(clientTag, "❌ Error enviando mensaje tipo '$type': ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    actual suspend fun disconnect() {
        receiveJob?.cancel()
        receiveJob = null

        try {
            socket?.close()
        } catch (e: Exception) {
            // Ignorar errores al cerrar
        }

        socket = null
        input = null
        output = null

        _connectionState.emit(false)
    }

    actual fun isConnected(): Boolean {
        return socket?.isConnected == true
    }
}
