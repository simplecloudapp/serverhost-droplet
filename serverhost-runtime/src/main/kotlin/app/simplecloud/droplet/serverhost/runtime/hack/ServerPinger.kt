package app.simplecloud.droplet.serverhost.runtime.hack

import com.google.gson.*
import java.io.*
import java.lang.reflect.Type
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.CompletableFuture

object ServerPinger {
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Description::class.java, Description.Deserializer())
        .create()

    fun ping(address: InetSocketAddress): CompletableFuture<StatusResponse> {
        return CompletableFuture.supplyAsync {
            Socket().use { socket ->
                socket.setSoTimeout(3000)
                socket.connect(address, 3000)

                socket.getOutputStream().use { outputStream ->
                    DataOutputStream(outputStream).use { dataOutputStream ->
                        socket.getInputStream().use { inputStream ->
                            DataInputStream(inputStream).use { dataInputStream ->

                                val handshake = ByteArrayOutputStream()
                                DataOutputStream(handshake).use { hs ->
                                    hs.writeByte(0x00) // packet id for handshake
                                    writeVarInt(hs, 4) // protocol version
                                    writeVarInt(hs, address.hostString.length) // host length
                                    hs.writeBytes(address.hostString) // host string
                                    hs.writeShort(address.port) // port
                                    writeVarInt(hs, 1) // state (1 for handshake)

                                    writeVarInt(dataOutputStream, handshake.size()) // prepend size
                                    dataOutputStream.write(handshake.toByteArray()) // write handshake packet
                                }

                                // Ping packet
                                dataOutputStream.writeByte(0x01) // size is only 1
                                dataOutputStream.writeByte(0x00) // packet id for ping

                                val json = fetchJson(dataInputStream)
                                return@supplyAsync parseJsonResponse(json)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun fetchJson(dataInputStream: DataInputStream): String {
        val size = readVarInt(dataInputStream) // Size of packet
        val id = readVarInt(dataInputStream) // Packet ID
        if (id != 0x00) throw IOException("Invalid packetID")

        val length = readVarInt(dataInputStream)
        if (length == 0) throw IOException("Invalid string length.")

        val jsonBytes = ByteArray(length)
        dataInputStream.readFully(jsonBytes)
        return String(jsonBytes)
    }

    private fun parseJsonResponse(json: String): StatusResponse {
        return try {
            gson.fromJson(json, StatusResponse::class.java)
        } catch (ex: JsonSyntaxException) {
            throw IOException("Error parsing JSON", ex)
        } ?: throw IOException("Unsupported JSON format")
    }

    private fun readVarInt(dataInput: DataInput): Int {
        var numRead = 0
        var result = 0
        var read: Int
        do {
            read = dataInput.readByte().toInt()
            val value = read and 0x7f
            result = result or (value shl (7 * numRead))
            numRead++
            if (numRead > 5) throw RuntimeException("VarInt too big")
        } while (read and 0x80 == 0x80)
        return result
    }

    private fun writeVarInt(out: DataOutput, value: Int) {
        var value = value
        do {
            var temp = value and 0x7F
            value = value ushr 7
            if (value != 0) {
                temp = temp or 0x80
            }
            out.writeByte(temp)
        } while (value != 0)
    }

    data class StatusResponse(
        val description: Description,
        val players: Players,
        val version: Version?,
        val favicon: String?,
        val time: Int?
    )

    data class Players(
        val max: Int,
        val online: Int,
        val sample: List<Player>
    )

    data class Player(
        val name: String,
        val id: String
    )

    data class Version(
        val name: String?,
        val protocol: Int?
    )

    data class Description(
        var text: String
    ) {
        class Deserializer : JsonDeserializer<Description> {
            override fun deserialize(
                json: JsonElement,
                typeOfT: Type,
                context: JsonDeserializationContext
            ): Description {
                return if (json.isJsonObject) {
                    Description(json.asJsonObject["text"].asString)
                } else {
                    Description(json.asString)
                }
            }
        }
    }
}