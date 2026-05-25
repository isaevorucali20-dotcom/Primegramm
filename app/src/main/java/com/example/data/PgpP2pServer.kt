package com.example.data

import android.util.Log
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import kotlinx.coroutines.*
import java.nio.ByteBuffer

class PgpP2pServer(
    private val port: Int = 55555,
    private val onMessageReceived: (String, String) -> Unit, // (senderAddress, text)
    private val onStatusChanged: (String) -> Unit
) {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        if (isRunning) return
        isRunning = true
        scope.launch {
            try {
                serverSocket = ServerSocket(port)
                onStatusChanged("Слушает порт $port")
                while (isRunning) {
                    val clientSocket = serverSocket?.accept() ?: break
                    launch {
                        handleClient(clientSocket)
                    }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    onStatusChanged("Ошибка: ${e.localizedMessage}")
                } else {
                    onStatusChanged("Остановлен")
                }
            } finally {
                isRunning = false
            }
        }
    }

    private suspend fun handleClient(socket: Socket) {
        withContext(Dispatchers.IO) {
            val remoteAddress = socket.inetAddress?.hostAddress ?: socket.remoteSocketAddress.toString()
            try {
                val input = socket.getInputStream()
                val header = ByteArray(5)
                while (isRunning && socket.isConnected && !socket.isClosed) {
                    // Read header: 1 byte type, 4 bytes length
                    var totalHeaderRead = 0
                    while (totalHeaderRead < 5) {
                        val read = input.read(header, totalHeaderRead, 5 - totalHeaderRead)
                        if (read == -1) return@withContext // client closed
                        totalHeaderRead += read
                    }
                    val buffer = ByteBuffer.wrap(header)
                    val type = buffer.get()
                    val length = buffer.int
                    
                    if (length < 0 || length > 10 * 1024 * 1024) { // 10MB safety limit
                        break
                    }

                    // Read payload
                    val payload = ByteArray(length)
                    var totalPayloadRead = 0
                    while (totalPayloadRead < length) {
                        val read = input.read(payload, totalPayloadRead, length - totalPayloadRead)
                        if (read == -1) return@withContext
                        totalPayloadRead += read
                    }

                    if (type == PgpPacket.TYPE_TEXT) {
                        val text = payload.toString(Charsets.UTF_8)
                        onMessageReceived(remoteAddress, text)
                    }
                }
            } catch (e: Exception) {
                Log.e("PgpP2pServer", "Client error: ${e.message}")
            } finally {
                try {
                    socket.close()
                } catch (_: Exception) {}
            }
        }
    }

    fun stop() {
        isRunning = false
        scope.launch(Dispatchers.IO) {
            try {
                serverSocket?.close()
            } catch (_: Exception) {}
            serverSocket = null
            onStatusChanged("Остановлен")
        }
    }
}
