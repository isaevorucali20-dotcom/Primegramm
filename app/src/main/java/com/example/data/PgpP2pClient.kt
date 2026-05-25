package com.example.data

import android.util.Log
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.*

class PgpP2pClient {
    private var socket: Socket? = null
    private val port = 55555

    suspend fun connectToPeer(peerIp: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                disconnect()
                val newSocket = Socket()
                newSocket.connect(InetSocketAddress(peerIp, port), 5000)
                socket = newSocket
                true
            } catch (e: Exception) {
                Log.e("PgpP2pClient", "Connection failed: ${e.message}")
                false
            }
        }
    }

    suspend fun sendMessage(text: String): Boolean {
        return withContext(Dispatchers.IO) {
            val currentSocket = socket
            if (currentSocket != null && currentSocket.isConnected && !currentSocket.isClosed) {
                try {
                    val packetBytes = PgpPacket.packText(text)
                    val output = currentSocket.getOutputStream()
                    output.write(packetBytes)
                    output.flush()
                    true
                } catch (e: Exception) {
                    Log.e("PgpP2pClient", "Send failed: ${e.message}")
                    false
                }
            } else {
                false
            }
        }
    }

    fun disconnect() {
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
    }

    fun isConnected(): Boolean {
        val currentSocket = socket
        return currentSocket != null && currentSocket.isConnected && !currentSocket.isClosed
    }
}
