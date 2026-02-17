package network

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import logging.FileLogger
import protocol.Message
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class NetworkClient {
    private var socket: Socket? = null
    private var input: BufferedReader? = null
    private var output: PrintWriter? = null
    private var receiveJob: Job? = null
    
    private val _messagesFlow = MutableSharedFlow<Message>(replay = 0)
    val messagesFlow: SharedFlow<Message> = _messagesFlow.asSharedFlow()
    
    private val _connectionState = MutableSharedFlow<Boolean>(replay = 1)
    val connectionState: SharedFlow<Boolean> = _connectionState.asSharedFlow()
    
    suspend fun connect(host: String, port: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            FileLogger.info("CLIENT", "🔌 Conectando al servidor $host:$port...")
            disconnect()

            socket = Socket(host, port)
            input = BufferedReader(InputStreamReader(socket!!.getInputStream()))
            output = PrintWriter(socket!!.getOutputStream(), true)

            _connectionState.emit(true)
            FileLogger.info("CLIENT", "✅ Conectado exitosamente a $host:$port")

            // Iniciar recepción de mensajes
            receiveJob = CoroutineScope(Dispatchers.IO).launch {
                receiveMessages()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            FileLogger.error("CLIENT", "❌ Error conectando al servidor: ${e.message}")
            _connectionState.emit(false)
            Result.failure(e)
        }
    }
    
    private suspend fun receiveMessages() {
        try {
            while (socket?.isConnected == true) {
                val line = input?.readLine() ?: break
                if (line.isBlank()) continue

                try {
                    val message = kotlinx.serialization.json.Json.decodeFromString<Message>(line)
                    FileLogger.debug("CLIENT", "📥 Recibido: ${message.type}")
                    _messagesFlow.emit(message)
                } catch (e: Exception) {
                    FileLogger.error("CLIENT", "❌ Error deserializando mensaje: '${line.take(100)}...' | Error: ${e.javaClass.simpleName}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            FileLogger.error("CLIENT", "❌ Error en bucle de recepción: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
        } finally {
            FileLogger.info("CLIENT", "🔌 Desconectado del servidor")
            _connectionState.emit(false)
        }
    }
    
    suspend fun send(type: String, payload: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (socket?.isConnected != true) {
                FileLogger.error("CLIENT", "❌ Intento de envío fallido: Socket no conectado (tipo=$type)")
                return@withContext Result.failure(Exception("No conectado al servidor"))
            }

            val message = Message(type, payload)
            val json = kotlinx.serialization.json.Json.encodeToString(Message.serializer(), message)
            FileLogger.debug("CLIENT", "📨 Enviando: $type | Payload: ${payload.take(50)}${if (payload.length > 50) "..." else ""}")

            output?.println(json)
            output?.flush()

            Result.success(Unit)
        } catch (e: Exception) {
            FileLogger.error("CLIENT", "❌ Error enviando mensaje tipo '$type': ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun disconnect() {
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
    
    fun isConnected(): Boolean {
        return socket?.isConnected == true
    }
}
