package model

import kotlinx.coroutines.flow.SharedFlow
import protocol.Message

/**
 * Contrato de comunicación de red, independiente de plataforma.
 * La implementación concreta reside en cada source set (desktopMain, etc.).
 */
expect class NetworkClient() {
    val clientId: String
    val clientTag: String
    val messagesFlow: SharedFlow<Message>
    val connectionState: SharedFlow<Boolean>

    suspend fun connect(host: String, port: Int): Result<Unit>
    suspend fun send(type: String, payload: String): Result<Unit>
    suspend fun disconnect()
    fun isConnected(): Boolean
}
