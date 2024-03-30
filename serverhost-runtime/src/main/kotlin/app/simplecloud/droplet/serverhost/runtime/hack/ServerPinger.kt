package app.simplecloud.droplet.serverhost.runtime.hack

import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException


object ServerPinger {
    fun ping(address: InetSocketAddress?): CompletableFuture<PingResult> {
        return CompletableFuture.supplyAsync {
            try {
                Socket().use { socket ->
                    socket.setSoTimeout(3000)
                    socket.connect(address, 3000)
                    DataOutputStream(socket.getOutputStream()).use { out ->
                        socket.getInputStream().use { `in` ->
                            InputStreamReader(`in`, StandardCharsets.UTF_16BE).use { reader ->
                                out.write(byteArrayOf(0xFE.toByte(), 0x01))
                                val packetId: Int = `in`.read()
                                val length: Int = reader.read()

                                if (packetId != 0xFF) {
                                    throw IOException("Invalid packet id: $packetId")
                                }

                                if (length <= 0) {
                                    throw IOException("Invalid length: $length")
                                }

                                val chars = CharArray(length)

                                if (reader.read(chars, 0, length) != length) {
                                    throw IOException("Premature end of stream")
                                }

                                val string = String(chars)

                                if (!string.startsWith("§")) {
                                    throw IOException("Unexpected response: $string")
                                }
                                val data = string.split("\u0000")
                                val players = data[4].toInt()
                                val maxPlayers = data[5].toInt()
                                return@supplyAsync PingResult(players, maxPlayers, data[3])
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                throw CompletionException(e)
            }
        }
    }

    class PingResult(val players: Int, val maxPlayers: Int, val motd: String) {
        override fun toString(): String {
            return "PingResult{players=" + this.players + ", maxPlayers=" + this.maxPlayers + '}'
        }
    }
}