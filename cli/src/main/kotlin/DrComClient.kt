package org.shiyi

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
                println("[自动获取IP] 获取本机IP失败: ${e.message}, 使用默认值")
                "192.168.1.100"
            }
        }

        private fun getHostName(): String {
            return try {
                val hostname = InetAddress.getLocalHost().hostName
                println("[自动获取主机名] 检测到主机名: $hostname")
                hostname
            } catch (e: Exception) {
                println("[自动获取主机名] 获取主机名失败: ${e.message}, 使用默认值")
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

                    val macStr = macBytes.joinToString(":") { "%02x".format(it) }
                    println("[自动获取MAC] 检测到MAC地址: $macStr")
                    return mac
                }

                println("[自动获取MAC] 未找到可用网络接口，使用默认MAC地址")
                0x20689df3d066L
            } catch (e: Exception) {
                println("[自动获取MAC] 获取MAC地址失败: ${e.message}, 使用默认值")
                0x20689df3d066L
            }
        }

        private fun getOsInfo(): String {
            val osName = System.getProperty("os.name", "Unknown")
            val osVersion = System.getProperty("os.version", "")

            val detectedOs = when {
                osName.contains("Windows", ignoreCase = true) -> "WINDOWS"
                osName.contains("Mac", ignoreCase = true) -> "MACOS"
                osName.contains("Linux", ignoreCase = true) -> "LINUX"
                osName.contains("Unix", ignoreCase = true) -> "UNIX"
                else -> "UNKNOWN"
            }

            println("[自动获取系统] 检测到操作系统: $osName $osVersion -> $detectedOs")
            return detectedOs
        }

        fun create(username: String, password: String, server: String = "10.30.1.19"): DrComConfig {
            println("=== 自动检测网络配置 ===")

            val hostIp = getLocalIpAddress()
            val hostName = getHostName()
            val mac = getMacAddress()
            val hostOs = getOsInfo()

            val config = DrComConfig(
                server = server,
                username = username,
                password = password,
                hostIp = hostIp,
                hostName = hostName,
                mac = mac,
                hostOs = hostOs
            )

            println("检测到的配置信息:")
            println("- 服务器地址: ${config.server}")
            println("- 本机IP: ${config.hostIp}")
            println("- 主机名: ${config.hostName}")
            println("- 操作系统: ${config.hostOs}")
            println("- MAC地址: ${String.format("%012x", config.mac)}")
            println("========================")

            return config
        }
    }
}

class DrComClient(private val config: DrComConfig) {
    private lateinit var socket: DatagramSocket
    private var salt: ByteArray = byteArrayOf()
    private var packageTail: ByteArray = byteArrayOf()
    private var isAuthenticated = false

    companion object {
        const val PORT = 61440
        const val CONTROL_CHECK_STATUS = 0x20.toByte()
        const val ADAPTER_NUM = 0x03.toByte()
        const val IPDOG = 0x01.toByte()
        val AUTH_VERSION = byteArrayOf(0x22, 0x00)
    }

    init {
        initializeSocket()
    }

    private fun initializeSocket() {
        try {
            socket = DatagramSocket(PORT)
            socket.soTimeout = 3000
            println("Socket initialized successfully on port $PORT")
        } catch (e: Exception) {
            println("Error initializing socket: ${e.message}")
            println("Cannot get the right port, this program may not authenticate correctly")
            Thread.sleep(5000)
            throw e
        }
    }

    fun printVersion() {
        println("=====================================================================")
        println("DrCOM Auth Client for Kotlin")
        println("Version 1.0.0")
        println("Based on DrCOM d version with keep-alive support")
        println("=====================================================================")
    }

    private fun md5(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("MD5").digest(data)
    }

    private fun challenge(server: String, random: Int): ByteArray {
        val serverAddr = InetAddress.getByName(server)

        while (true) {
            val randomBytes = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
                .putShort((random % 0xFFFF).toShort()).array()

            val packet = byteArrayOf(0x01, 0x02) + randomBytes + byteArrayOf(0x09) + ByteArray(15)

            socket.send(DatagramPacket(packet, packet.size, serverAddr, PORT))

            try {
                val buffer = ByteArray(1024)
                val receivePacket = DatagramPacket(buffer, buffer.size)
                socket.receive(receivePacket)

                if (receivePacket.address == serverAddr && receivePacket.port == PORT) {
                    val data = receivePacket.data.sliceArray(0..receivePacket.length - 1)
                    if (data[0] == 0x02.toByte()) {
                        return data.sliceArray(4..7)
                    }
                }
            } catch (_: SocketTimeoutException) {
                println("[challenge] timeout, retrying...")
                continue
            }
        }
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

    private fun makeLoginPacket(salt: ByteArray, username: String, password: String, mac: Long): ByteArray {
        val hostIpBytes = config.hostIp.split('.').map { it.toInt().toByte() }.toByteArray()

        var data = byteArrayOf(0x03, 0x01, 0x00, (username.length + 20).toByte())
        data += md5(byteArrayOf(0x03, 0x01) + salt + password.toByteArray())
        data += username.toByteArray().padEnd(36)
        data += CONTROL_CHECK_STATUS
        data += ADAPTER_NUM

        // MAC XOR MD5
        val macBytes = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(mac).array().sliceArray(2..7)
        val md5Hex = data.sliceArray(4..9)
        val xorResult = ByteArray(6)
        for (i in 0..5) {
            xorResult[i] = (md5Hex[i].toInt() xor macBytes[i].toInt()).toByte()
        }
        data += xorResult

        // MD5-2
        data += md5(byteArrayOf(0x01) + password.toByteArray() + salt + ByteArray(4))

        data += byteArrayOf(0x01) // NIC count
        data += hostIpBytes // IP address
        data += ByteArray(12) // IP addresses 2,3,4

        // MD5-3
        data += md5(data + byteArrayOf(0x14, 0x00, 0x07, 0x0b)).sliceArray(0..7)

        data += IPDOG
        data += ByteArray(4) // delimiter
        data += config.hostName.toByteArray().padEnd(32)
        data += byteArrayOf(0x72, 0x72, 0x72, 0x72) // Primary DNS
        data += byteArrayOf(0x0a, 0xff.toByte(), 0x00, 0xc5.toByte()) // DHCP server
        data += byteArrayOf(0x08, 0x08, 0x08, 0x08) // Secondary DNS
        data += ByteArray(8) // delimiter
        data += byteArrayOf(0x94.toByte(), 0x00, 0x00, 0x00) // unknown
        data += byteArrayOf(0x05, 0x00, 0x00, 0x00) // OS major
        data += byteArrayOf(0x01, 0x00, 0x00, 0x00) // OS minor
        data += byteArrayOf(0x28, 0x0a, 0x00, 0x00) // OS build
        data += byteArrayOf(0x02, 0x00, 0x00, 0x00) // OS unknown
        data += config.hostOs.toByteArray().padEnd(32)
        data += ByteArray(96)
        data += AUTH_VERSION
        data += byteArrayOf(0x02, 0x0c)

        val macBytes8 = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(mac).array()
        data += checksum(data + byteArrayOf(0x01, 0x26, 0x07, 0x11, 0x00, 0x00) + macBytes8.sliceArray(2..7))
        data += byteArrayOf(0x00, 0x00) // delimiter
        data += macBytes8.sliceArray(2..7) // MAC
        data += byteArrayOf(0x00) // auto logout
        data += byteArrayOf(0x00) // broadcast mode
        data += byteArrayOf(0xc2.toByte(), 0x66)

        return data
    }

    suspend fun login(): Boolean {
        val serverAddr = InetAddress.getByName(config.server)
        var attempts = 0

        while (true) {
            try {
                salt = challenge(config.server, (Instant.now().epochSecond + Random.nextInt(0xF, 0xFF)).toInt())
                val packet = makeLoginPacket(salt, config.username, config.password, config.mac)

                socket.send(DatagramPacket(packet, packet.size, serverAddr, PORT))
                println("[login] packet sent.")

                val buffer = ByteArray(1024)
                val receivePacket = DatagramPacket(buffer, buffer.size)
                socket.receive(receivePacket)

                if (receivePacket.address == serverAddr && receivePacket.port == PORT) {
                    val data = receivePacket.data.sliceArray(0..receivePacket.length - 1)
                    if (data[0] == 0x04.toByte()) {
                        println("[login] login successful")
                        packageTail = data.sliceArray(23..38)
                        isAuthenticated = true
                        return true
                    }
                }
            } catch (e: Exception) {
                attempts++
                if (attempts >= 5) {
                    println("[login] login failed after 5 attempts")
                    return false
                }
                println("[login] attempt $attempts failed, retrying...")
                delay(1000)
            }
        }
    }

    private fun keepAlive1() {
        try {
            val time = (Instant.now().epochSecond % 0xFFFF).toShort()
            val timeBytes = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort(time).array()

            var data = byteArrayOf(0xff.toByte())
            data += md5(byteArrayOf(0x03, 0x01) + salt + config.password.toByteArray())
            data += ByteArray(3)
            data += packageTail
            data += timeBytes
            data += ByteArray(4)

            val serverAddr = InetAddress.getByName(config.server)
            socket.send(DatagramPacket(data, data.size, serverAddr, PORT))

            val buffer = ByteArray(1024)
            val receivePacket = DatagramPacket(buffer, buffer.size)
            socket.receive(receivePacket)

            println("[keep-alive1] heartbeat sent")
        } catch (e: Exception) {
            println("[keep-alive1] error: ${e.message}")
        }
    }

    suspend fun startKeepAlive() = withContext(Dispatchers.IO) {
        println("[keep-alive] starting keep-alive daemon...")

        while (isAuthenticated) {
            try {
                keepAlive1()
                delay(20000) // 20 seconds interval
            } catch (e: Exception) {
                println("[keep-alive] error: ${e.message}")
                delay(5000)
            }
        }
    }

    fun disconnect() {
        isAuthenticated = false
        if (::socket.isInitialized) {
            socket.close()
        }
        println("Disconnected from DrCOM server")
    }

    private fun ByteArray.padEnd(length: Int): ByteArray {
        return if (this.size >= length) {
            this.sliceArray(0 until length)
        } else {
            this + ByteArray(length - this.size)
        }
    }
}
