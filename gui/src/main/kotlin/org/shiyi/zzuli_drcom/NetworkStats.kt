package org.shiyi.zzuli_drcom

import java.time.LocalDateTime

/**
 * 网络统计数据类
 */
data class NetworkStats(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val uploadSpeed: Double = 0.0,      // KB/s
    val downloadSpeed: Double = 0.0,    // KB/s
    val totalUpload: Long = 0L,         // 总上传量 (字节)
    val totalDownload: Long = 0L,       // 总下载量 (字节)
    val activeConnections: Int = 0,     // 活跃连接数
    val memoryUsage: Double = 0.0       // 内存使用量 (MB)
)

/**
 * 网络监控管理器
 */
class NetworkMonitor {
    private val statsHistory = mutableListOf<NetworkStats>()
    private var lastUploadBytes = 0L
    private var lastDownloadBytes = 0L
    private var lastUpdateTime = System.currentTimeMillis()

    /**
     * 更新网络统计
     */
    fun updateStats(currentUploadBytes: Long, currentDownloadBytes: Long, activeConnections: Int): NetworkStats {
        val currentTime = System.currentTimeMillis()
        val timeDiff = (currentTime - lastUpdateTime) / 1000.0 // 秒

        val uploadSpeed = if (timeDiff > 0 && lastUploadBytes > 0) {
            ((currentUploadBytes - lastUploadBytes) / timeDiff) / 1024.0 // KB/s
        } else 0.0

        val downloadSpeed = if (timeDiff > 0 && lastDownloadBytes > 0) {
            ((currentDownloadBytes - lastDownloadBytes) / timeDiff) / 1024.0 // KB/s
        } else 0.0

        val memoryUsage = getMemoryUsage()

        val stats = NetworkStats(
            uploadSpeed = maxOf(uploadSpeed, 0.0),
            downloadSpeed = maxOf(downloadSpeed, 0.0),
            totalUpload = currentUploadBytes,
            totalDownload = currentDownloadBytes,
            activeConnections = activeConnections,
            memoryUsage = memoryUsage
        )

        // 保存历史数据，只保留最近1小时
        statsHistory.add(stats)
        val oneHourAgo = LocalDateTime.now().minusHours(1)
        statsHistory.removeAll { it.timestamp.isBefore(oneHourAgo) }

        lastUploadBytes = currentUploadBytes
        lastDownloadBytes = currentDownloadBytes
        lastUpdateTime = currentTime

        return stats
    }

    /**
     * 获取历史统计数据
     */
    fun getStatsHistory(): List<NetworkStats> = statsHistory.toList()

    /**
     * 获取当前内存使用量
     */
    private fun getMemoryUsage(): Double {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        return usedMemory / (1024.0 * 1024.0) // MB
    }

    /**
     * 格式化数据大小
     */
    fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return String.format("%.2f %s", size, units[unitIndex])
    }

    /**
     * 格式化速度
     */
    fun formatSpeed(speedKbps: Double): String {
        return when {
            speedKbps < 1024 -> String.format("%.2f KB/s", speedKbps)
            else -> String.format("%.2f MB/s", speedKbps / 1024)
        }
    }
}
