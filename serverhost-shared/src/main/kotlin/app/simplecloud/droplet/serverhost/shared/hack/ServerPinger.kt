package app.simplecloud.droplet.serverhost.shared.hack

import com.google.gson.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import java.io.*
import java.lang.reflect.Type
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

object ServerPinger {

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Description::class.java, Description.Deserializer())
        .create()

    // Constants for Bedrock edition
    private const val MAGIC = "00ffff00fefefefefdfdfdfd12345678"
    private const val UNCONNECTED_PING: Byte = 0x01
    private const val UNCONNECTED_PONG: Byte = 0x1C

    private const val CONNECT_TIMEOUT = 5000
    private const val READ_TIMEOUT = 5000

    private const val CACHE_REVALIDATION_TIME = 10 * 60 * 1000L // 10 minutes

    private val serverMethodCache = ConcurrentHashMap<String, CachedMethod>()

    enum class ServerType {
        JAVA, BEDROCK
    }

    data class StatusResponse(
        val description: Description,
        val players: Players,
        val version: Version?,
        val favicon: String?,
        val time: Int?,
        val serverType: ServerType
    )

    data class Players(
        val max: Int,
        val online: Int,
        val sample: List<Player> = emptyList()
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

    private data class CachedMethod(
        val methodIndex: Int,
        val timestamp: Long
    )

    private val pingMethods = listOf(
        Method("Standard Java", { address -> pingJava(address) }, ServerType.JAVA),
        Method("Java with PROXY Protocol", { address -> pingJavaWithProxyProtocol(address) }, ServerType.JAVA),
        Method("Standard Bedrock", { address -> pingBedrock(address) }, ServerType.BEDROCK),
        Method("Bedrock with PROXY Protocol", { address -> pingBedrockWithProxyProtocol(address) }, ServerType.BEDROCK)
    )

    private data class Method(
        val name: String,
        val pingFunction: suspend (InetSocketAddress) -> StatusResponse?,
        val serverType: ServerType
    )

    suspend fun ping(address: InetSocketAddress): StatusResponse {
        return coroutineScope {
            val serverKey = "${address.hostString}:${address.port}"
            val cachedMethod = serverMethodCache[serverKey]
            val currentTime = System.currentTimeMillis()

            // Check if we have a cached method that's still valid
            if (cachedMethod != null && currentTime - cachedMethod.timestamp < CACHE_REVALIDATION_TIME) {
                // Try the cached method first
                try {
                    val method = pingMethods[cachedMethod.methodIndex]
                    val response = method.pingFunction(address)
                    if (response != null) {
                        // Update the timestamp
                        serverMethodCache[serverKey] = CachedMethod(cachedMethod.methodIndex, currentTime)
                        return@coroutineScope response.copy(serverType = method.serverType)
                    }
                } catch (e: Exception) {
                    // Cached method failed, continue to try all methods
                }
            }

            // If cached method failed or needs revalidation, try all methods
            var lastException: Exception? = null

            // Try each method in sequence
            pingMethods.forEachIndexed { index, method ->
                // Skip if this was the cached method we already tried
                if (cachedMethod != null && index == cachedMethod.methodIndex &&
                    currentTime - cachedMethod.timestamp < CACHE_REVALIDATION_TIME) {
                    return@forEachIndexed
                }

                try {
                    val response = method.pingFunction(address)
                    if (response != null) {
                        // Cache the successful method
                        serverMethodCache[serverKey] = CachedMethod(index, currentTime)
                        return@coroutineScope response.copy(serverType = method.serverType)
                    }
                } catch (e: Exception) {
                    lastException = e
                    // Continue to the next method
                }
            }

            throw IOException("Failed to ping server $serverKey (all methods failed)", lastException)
        }
    }

    private suspend fun pingJava(address: InetSocketAddress): StatusResponse? {
        return withTimeoutOrNull(CONNECT_TIMEOUT.toLong()) {
            Socket().use { socket ->
                socket.setSoTimeout(READ_TIMEOUT)
                socket.connect(address, CONNECT_TIMEOUT)

                socket.getOutputStream().use { outputStream ->
                    DataOutputStream(outputStream).use { dataOutputStream ->
                        socket.getInputStream().use { inputStream ->
                            DataInputStream(inputStream).use { dataInputStream ->
                                val handshake = ByteArrayOutputStream()
                                DataOutputStream(handshake).use { hs ->
                                    hs.writeByte(0x00)
                                    writeVarInt(hs, 4)
                                    writeVarInt(hs, address.hostString.length)
                                    hs.writeBytes(address.hostString)
                                    hs.writeShort(address.port)
                                    writeVarInt(hs, 1)

                                    writeVarInt(dataOutputStream, handshake.size())
                                    dataOutputStream.write(handshake.toByteArray())
                                }

                                dataOutputStream.writeByte(0x01)
                                dataOutputStream.writeByte(0x00)

                                val json = fetchJson(dataInputStream)
                                return@withTimeoutOrNull parseJsonResponse(json)
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun pingJavaWithProxyProtocol(address: InetSocketAddress): StatusResponse? {
        return withTimeoutOrNull(CONNECT_TIMEOUT.toLong()) {
            Socket().use { socket ->
                socket.setSoTimeout(READ_TIMEOUT)
                socket.connect(address, CONNECT_TIMEOUT)

                socket.getOutputStream().use { outputStream ->
                    DataOutputStream(outputStream).use { dataOutputStream ->
                        // Send PROXY protocol header first
                        val proxyHeader = "PROXY TCP4 127.0.0.1 ${address.hostString} 12345 ${address.port}\r\n"
                        dataOutputStream.write(proxyHeader.toByteArray())

                        socket.getInputStream().use { inputStream ->
                            DataInputStream(inputStream).use { dataInputStream ->
                                val handshake = ByteArrayOutputStream()
                                DataOutputStream(handshake).use { hs ->
                                    hs.writeByte(0x00)
                                    writeVarInt(hs, 4)
                                    writeVarInt(hs, address.hostString.length)
                                    hs.writeBytes(address.hostString)
                                    hs.writeShort(address.port)
                                    writeVarInt(hs, 1)

                                    writeVarInt(dataOutputStream, handshake.size())
                                    dataOutputStream.write(handshake.toByteArray())
                                }

                                dataOutputStream.writeByte(0x01)
                                dataOutputStream.writeByte(0x00)

                                val json = fetchJson(dataInputStream)
                                return@withTimeoutOrNull parseJsonResponse(json)
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun pingBedrock(address: InetSocketAddress): StatusResponse? {
        return withTimeoutOrNull(CONNECT_TIMEOUT.toLong()) {
            DatagramSocket().use { socket ->
                socket.soTimeout = READ_TIMEOUT

                val pingPacket = createBedrockPingPacket()
                socket.send(DatagramPacket(pingPacket, pingPacket.size, address))

                val buffer = ByteArray(1024)
                val response = DatagramPacket(buffer, buffer.size)
                socket.receive(response)

                return@withTimeoutOrNull parseBedrockResponse(response.data.copyOfRange(0, response.length))
            }
        }
    }

    private suspend fun pingBedrockWithProxyProtocol(address: InetSocketAddress): StatusResponse? {
        return withTimeoutOrNull(CONNECT_TIMEOUT.toLong()) {
            Socket().use { socket ->
                socket.setSoTimeout(READ_TIMEOUT)
                socket.connect(address, CONNECT_TIMEOUT)

                socket.getOutputStream().use { outputStream ->
                    DataOutputStream(outputStream).use { dataOutputStream ->
                        // Send PROXY protocol header first
                        val proxyHeader = "PROXY UDP4 127.0.0.1 ${address.hostString} 12345 ${address.port}\r\n"
                        dataOutputStream.write(proxyHeader.toByteArray())

                        // Send Bedrock ping packet
                        val pingPacket = createBedrockPingPacket()
                        dataOutputStream.write(pingPacket)
                        dataOutputStream.flush()

                        // Read response
                        socket.getInputStream().use { inputStream ->
                            DataInputStream(inputStream).use { dataInputStream ->
                                // Read response data
                                val responseSize = Math.min(1024, dataInputStream.available())
                                if (responseSize <= 0) {
                                    throw IOException("No data received from Bedrock server")
                                }

                                val responseData = ByteArray(responseSize)
                                dataInputStream.readFully(responseData)

                                return@withTimeoutOrNull parseBedrockResponse(responseData)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createBedrockPingPacket(): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { dos ->
            dos.writeByte(UNCONNECTED_PING.toInt())
            dos.writeLong(System.currentTimeMillis())
            dos.write(MAGIC.chunked(2).map { it.toInt(16).toByte() }.toByteArray())
            dos.writeLong(2)
        }
        return out.toByteArray()
    }

    private fun parseBedrockResponse(data: ByteArray): StatusResponse {
        val input = DataInputStream(ByteArrayInputStream(data))

        val packetId = input.read()
        if (packetId != UNCONNECTED_PONG.toInt()) {
            throw IOException("Invalid Bedrock response packet ID")
        }

        val timestamp = input.readLong()
        val serverGuid = input.readLong()
        input.skip(16)

        val serverInfo = ByteArray(input.available()).also { input.read(it) }.toString(Charsets.UTF_8)
        val parts = serverInfo.split(";")

        if (parts.size < 6) {
            throw IOException("Invalid Bedrock server info format")
        }

        return StatusResponse(
            description = Description("${parts[1]} [Bedrock]"), // MOTD
            players = Players(
                max = parts[5].toIntOrNull() ?: 0,
                online = parts[4].toIntOrNull() ?: 0
            ),
            version = Version(
                name = "${parts[0]} ${parts[3]}", // Edition + Version
                protocol = parts[2].toIntOrNull()
            ),
            favicon = null,
            time = null,
            serverType = ServerType.BEDROCK
        )
    }

    private fun fetchJson(dataInputStream: DataInputStream): String {
        val size = readVarInt(dataInputStream)
        val id = readVarInt(dataInputStream)
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
}
