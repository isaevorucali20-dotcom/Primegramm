package com.example.data

import java.nio.ByteBuffer

object PgpPacket {
    const val TYPE_TEXT: Byte = 0x01

    fun packText(text: String): ByteArray {
        val payloadBytes = text.toByteArray(Charsets.UTF_8)
        val length = payloadBytes.size
        
        val buffer = ByteBuffer.allocate(1 + 4 + length)
        buffer.put(TYPE_TEXT)
        buffer.putInt(length)
        buffer.put(payloadBytes)
        
        return buffer.array()
    }

    class Packet(val type: Byte, val payload: ByteArray) {
        val text: String
            get() = payload.toString(Charsets.UTF_8)
    }

    fun unpack(rawBytes: ByteArray): Packet {
        if (rawBytes.size < 5) {
            throw IllegalArgumentException("Пакет слишком мал, поврежден")
        }
        val buffer = ByteBuffer.wrap(rawBytes)
        val type = buffer.get()
        val length = buffer.int
        if (rawBytes.size < 5 + length) {
            throw IllegalArgumentException("Пакет поврежден: размер payload не совпадает")
        }
        val payload = ByteArray(length)
        buffer.get(payload)
        return Packet(type, payload)
    }
}
