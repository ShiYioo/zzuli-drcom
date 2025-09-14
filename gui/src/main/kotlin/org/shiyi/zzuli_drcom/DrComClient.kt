package org.shiyi.zzuli_drcom

import kotlinx.coroutines.*
import java.net.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.time.Instant
import kotlin.random.Random

data class DrComConfig(
    val server: String,
    val username: String,
    val password: String,
    val hostIp: String,
    val hostName: String,
    val primaryDns: String = "59.70.159.10",
    val dhcpServer: String = "0.0.0.0",
    val mac: Long,
    val hostOs: String
) {
    companion object {
        private fun getLocalIpAddress(): String {
            return try {
                val interfaces = NetworkInterface.getNetworkInterfaces().toList()
                val activeInterface = interfaces
                    .filter { !it.isLoopback && it.isUp && it.hardwareAddress != null }
                    .flatMap { it.inetAddresses.toList() }
                    .firstOrNull { !it.isLoopbackAddress && it.address.size == 4 }

                activeInterface?.hostAddress ?: "192.168.1.100"
            } catch (e: Exception) {
                "192.168.1.100"
            }
        }

        private fun getHostName(): String {
            return try {
                InetAddress.getLocalHost().hostName
            } catch (e: Exception) {
                "DRCOM-CLIENT"
            }
        }

        private fun getMacAddress(): Long {
            return try {
                val networkInterface = NetworkInterface.getNetworkInterfaces().toList()
                    .firstOrNull { !it.isLoopback && it.isUp && it.hardwareAddress != null }

                if (networkInterface != null) {
                    val macBytes = networkInterface.hardwareAddress
                    var mac = 0L
                    for (i in macBytes.indices) {
                        mac = (mac shl 8) + (macBytes[i].toInt() and 0xFF)
                    }
                    return mac
                }
                0x20689df3d066L
            } catch (e: Exception) {
                0x20689df3d066L
            }
        }

        private fun getOsInfo(): String {
            val osName = System.getProperty("os.name", "Unknown")
            return when {
                osName.contains("Windows", ignoreCase = true) -> "WINDOWS"
                osName.contains("Mac", ignoreCase = true) -> "MACOS"
                osName.contains("Linux", ignoreCase = true) -> "LINUX"
                else -> "UNKNOWN"
            }
        }

        fun create(username: String, password: String, server: String = "10.30.1.19"): DrComConfig {
            return DrComConfig(
                server = server,
                username = username,
                password = password,
                hostIp = getLocalIpAddress(),
                hostName = getHostName(),
                mac = getMacAddress(),
                hostOs = getOsInfo()
            )
        }
    }
}

class DrComClient(private val config: DrComConfig) {
    private var socket: DatagramSocket? = null
    private var salt: ByteArray = byteArrayOf()
    private var packageTail: ByteArray = byteArrayOf()
    private var isAuthenticated = false
    private var keepAliveJob: Job? = null

    companion object {
        const val PORT = 61440
        const val CONTROL_CHECK_STATUS = 0x20.toByte()
        const val ADAPTER_NUM = 0x03.toByte()
        const val IPDOG = 0x01.toByte()
        val AUTH_VERSION = byteArrayOf(0x22, 0x00)
    }

    private fun initializeSocket() {
        try {
            socket = DatagramSocket(PORT)
            socket?.soTimeout = 3000
        } catch (e: Exception) {
            throw RuntimeException("无法初始化网络连接: ${e.message}")
        }
    }

    private fun md5(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("MD5").digest(data)
    }

    private fun challenge(server: String, random: Int): ByteArray {
        val serverAddr = InetAddress.getByName(server)

        repeat(5) { // 最多重试5次
            try {
                val randomBytes = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
                    .putShort((random % 0xFFFF).toShort()).array()

                val packet = byteArrayOf(0x01, 0x02) + randomBytes + byteArrayOf(0x09) + ByteArray(15)
                socket?.send(DatagramPacket(packet, packet.size, serverAddr, PORT))

                val buffer = ByteArray(1024)
                val receivePacket = DatagramPacket(buffer, buffer.size)
                socket?.receive(receivePacket)

                if (receivePacket.address == serverAddr && receivePacket.port == PORT) {
                    val data = receivePacket.data.sliceArray(0 until receivePacket.length)
                    if (data[0] == 0x02.toByte()) {
                        return data.sliceArray(4..7)
                    }
                }
            } catch (_: SocketTimeoutException) {
                return@repeat
            }
        }
        throw RuntimeException("Challenge 失败，无法连接到服务器")
    }

    private fun checksum(data: ByteArray): ByteArray {
        var ret = 1234L
        var i = 0
        while (i < data.size - 3) {
            val chunk = data.sliceArray(i..i + 3)
            val value = ByteBuffer.wrap(chunk.reversedArray()).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
            ret = ret xor value
            i += 4
        }

        ret = (1968 * ret) and 0xFFFFFFFFL
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ret.toInt()).array()
    }

    suspend fun login(): Boolean = withContext(Dispatchers.IO) {
        try {
            initializeSocket()

            // Challenge 阶段
            val random = Random.nextInt(0, 0xFFFF)
            salt = challenge(config.server, random)

            // Login 阶段
            val md5a = md5(byteArrayOf(0x03, 0x01) + salt + config.password.toByteArray())

            // 构建登录包
            val data = buildLoginPacket(md5a)

            val serverAddr = InetAddress.getByName(config.server)
            socket?.send(DatagramPacket(data, data.size, serverAddr, PORT))

            // 接收响应
            val buffer = ByteArray(1024)
            val receivePacket = DatagramPacket(buffer, buffer.size)
            socket?.receive(receivePacket)

            val response = receivePacket.data.sliceArray(0 until receivePacket.length)

            if (response[0] == 0x04.toByte()) {
                packageTail = response.sliceArray(23..38)
                isAuthenticated = true
                startKeepAlive()
                return@withContext true
            } else {
                return@withContext false
            }
        } catch (e: Exception) {
            throw RuntimeException("登录失败: ${e.message}")
        }
    }

    private fun buildLoginPacket(md5a: ByteArray): ByteArray {
        val packet = mutableListOf<Byte>()

        // 基本信息
        packet.add(0x03.toByte())
        packet.add(0x01.toByte())
        packet.add(0x00.toByte())
        packet.add((config.username.length + 20).toByte())

        // MD5A
        packet.addAll(md5a.toList())

        // 用户名
        packet.addAll(config.username.toByteArray().toList())
        repeat(36 - config.username.length) { packet.add(0x00) }

        // 其他必要字段
        packet.add(CONTROL_CHECK_STATUS)
        packet.add(ADAPTER_NUM)

        // IP 地址
        val ipBytes = config.hostIp.split(".").map { it.toInt().toByte() }
        packet.addAll(ipBytes)

        // MAC 地址
        val macBytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(config.mac).array()
        packet.addAll(macBytes.sliceArray(0..5).toList())

        // MD5B
        val md5b = md5(byteArrayOf(0x01) + config.password.toByteArray() + salt + ByteArray(4))
        packet.addAll(md5b.toList())

        // 主机信息
        packet.addAll(config.hostName.toByteArray().take(32).toList())
        repeat(32 - minOf(config.hostName.length, 32)) { packet.add(0x00) }

        // DNS
        val dnsBytes = config.primaryDns.split(".").map { it.toInt().toByte() }
        packet.addAll(dnsBytes)

        // DHCP
        val dhcpBytes = config.dhcpServer.split(".").map { it.toInt().toByte() }
        packet.addAll(dhcpBytes)

        // 其他字段
        packet.addAll(ByteArray(8).toList()) // OSVersionInfoSize
        packet.addAll(config.hostOs.toByteArray().take(32).toList())
        repeat(32 - minOf(config.hostOs.length, 32)) { packet.add(0x00) }

        // 添加其他必要的固定字段
        packet.addAll(ByteArray(96).toList())

        return packet.toByteArray()
    }

    private fun startKeepAlive() {
        keepAliveJob = CoroutineScope(Dispatchers.IO).launch {
            var tail = 0
            while (isAuthenticated) {
                try {
                    sendKeepAlive(tail)
                    tail = (tail + 1) % 256
                    delay(20000) // 20秒发送一次保活包
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    private fun sendKeepAlive(tail: Int) {
        val packet = byteArrayOf(0xff.toByte()) + md5("".toByteArray()).sliceArray(0..2) + byteArrayOf(tail.toByte()) + packageTail
        val serverAddr = InetAddress.getByName(config.server)
        socket?.send(DatagramPacket(packet, packet.size, serverAddr, PORT))
    }

    fun disconnect() {
        isAuthenticated = false
        keepAliveJob?.cancel()
        socket?.close()
    }

    fun isConnected(): Boolean = isAuthenticated
}
