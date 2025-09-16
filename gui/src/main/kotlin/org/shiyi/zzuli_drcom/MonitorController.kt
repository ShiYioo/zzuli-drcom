package org.shiyi.zzuli_drcom

import javafx.animation.Timeline
import javafx.animation.KeyFrame
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Scene
import javafx.scene.chart.LineChart
import javafx.scene.chart.XYChart
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.NumberAxis
import javafx.scene.control.*
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.util.Duration
import kotlinx.coroutines.*
import java.net.URL
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.atomic.AtomicLong

class MonitorController : Initializable {

    @FXML private lateinit var monitorContainer: VBox
    @FXML private lateinit var backButton: Button
    @FXML private lateinit var refreshButton: Button
    @FXML private lateinit var uploadSpeedLabel: Label
    @FXML private lateinit var downloadSpeedLabel: Label
    @FXML private lateinit var totalUploadLabel: Label
    @FXML private lateinit var totalDownloadLabel: Label
    @FXML private lateinit var connectionsLabel: Label
    @FXML private lateinit var memoryLabel: Label
    @FXML private lateinit var speedChart: LineChart<String, Number>
    @FXML private lateinit var timeAxis: CategoryAxis
    @FXML private lateinit var speedAxis: NumberAxis
    @FXML private lateinit var statusLabel: Label

    private val networkMonitor = NetworkMonitor()
    private var updateTimeline: Timeline? = null
    private var monitoringJob: Job? = null

    // 模拟网络数据计数器
    private val uploadBytes = AtomicLong(0)
    private val downloadBytes = AtomicLong(0)
    private var activeConnections = 1

    // 图表数据系列
    private lateinit var uploadSeries: XYChart.Series<String, Number>
    private lateinit var downloadSeries: XYChart.Series<String, Number>

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        setupUI()
        setupChart()
        startMonitoring()
    }

    private fun setupUI() {
        // 设置按钮事件
        backButton.setOnAction { goBackToLogin() }
        refreshButton.setOnAction { refreshData() }

        // 初始状态
        statusLabel.text = "正在初始化监控..."
    }

    private fun setupChart() {
        // 创建数据系列
        uploadSeries = XYChart.Series<String, Number>()
        uploadSeries.name = "上传速度"

        downloadSeries = XYChart.Series<String, Number>()
        downloadSeries.name = "下载速度"

        // 添加到图表
        speedChart.data.addAll(uploadSeries, downloadSeries)

        // 设置图表样式
        speedChart.createSymbols = false
        speedChart.isLegendVisible = true
        speedChart.animated = false

        // 设置坐标轴
        timeAxis.label = "时间"
        speedAxis.label = "速度 (KB/s)"
        speedAxis.isAutoRanging = true
    }

    private fun startMonitoring() {
        statusLabel.text = "正在监控网络状态..."

        // 启动定时更新任务
        updateTimeline = Timeline(KeyFrame(Duration.seconds(2.0), { updateNetworkStats() }))
        updateTimeline?.cycleCount = Timeline.INDEFINITE
        updateTimeline?.play()

        // 启动后台数据模拟任务
        monitoringJob = CoroutineScope(Dispatchers.IO).launch {
            simulateNetworkTraffic()
        }
    }

    private fun updateNetworkStats() {
        try {
            // 获取当前网络统计
            val stats = networkMonitor.updateStats(
                uploadBytes.get(),
                downloadBytes.get(),
                activeConnections
            )

            // 更新UI标签
            Platform.runLater {
                uploadSpeedLabel.text = networkMonitor.formatSpeed(stats.uploadSpeed)
                downloadSpeedLabel.text = networkMonitor.formatSpeed(stats.downloadSpeed)
                totalUploadLabel.text = networkMonitor.formatBytes(stats.totalUpload)
                totalDownloadLabel.text = networkMonitor.formatBytes(stats.totalDownload)
                connectionsLabel.text = stats.activeConnections.toString()
                memoryLabel.text = String.format("%.2f MB", stats.memoryUsage)

                // 更新图表
                updateChart(stats)

                statusLabel.text = "监控正常 - 最后更新: ${stats.timestamp.format(timeFormatter)}"
            }
        } catch (e: Exception) {
            Platform.runLater {
                statusLabel.text = "监控错误: ${e.message}"
            }
        }
    }

    private fun updateChart(stats: NetworkStats) {
        val timeLabel = stats.timestamp.format(timeFormatter)

        // 添加新数据点
        uploadSeries.data.add(XYChart.Data(timeLabel, stats.uploadSpeed))
        downloadSeries.data.add(XYChart.Data(timeLabel, stats.downloadSpeed))

        // 限制显示的数据点数量（最多30个点，即1分钟的数据）
        val maxDataPoints = 30
        if (uploadSeries.data.size > maxDataPoints) {
            uploadSeries.data.removeAt(0)
        }
        if (downloadSeries.data.size > maxDataPoints) {
            downloadSeries.data.removeAt(0)
        }
    }

    private suspend fun simulateNetworkTraffic() {
        // 模拟网络流量数据
        val random = Random()

        while (monitoringJob?.isActive == true) {
            try {
                // 模拟上传和下载字节数增长
                val uploadIncrement = random.nextInt(1024, 8192) // 1-8KB 每次
                val downloadIncrement = random.nextInt(2048, 16384) // 2-16KB 每次

                uploadBytes.addAndGet(uploadIncrement.toLong())
                downloadBytes.addAndGet(downloadIncrement.toLong())

                // 模拟连接数变化
                if (random.nextDouble() < 0.1) { // 10% 概率改变连接数
                    activeConnections = random.nextInt(1, 6)
                }

                delay(1000) // 每秒更新一次模拟数据
            } catch (e: Exception) {
                break
            }
        }
    }

    private fun refreshData() {
        statusLabel.text = "刷新数据中..."

        // 清空图表数据
        Platform.runLater {
            uploadSeries.data.clear()
            downloadSeries.data.clear()
        }

        // 重置计数器
        uploadBytes.set(0)
        downloadBytes.set(0)

        statusLabel.text = "数据已刷新"
    }

    private fun goBackToLogin() {
        try {
            // 停止监控
            stopMonitoring()

            // 加载登录界面
            val fxmlLoader = FXMLLoader(javaClass.getResource("drcom-login.fxml"))
            val scene = Scene(fxmlLoader.load(), 400.0, 600.0)

            // 获取LoginController并恢复登录状态
            val loginController = fxmlLoader.getController<LoginController>()
            loginController.restoreLoginState() // 恢复登录状态

            // 添加CSS样式
            scene.stylesheets.add(javaClass.getResource("ios-style.css")?.toExternalForm())

            // 获取当前窗口并切换场景
            val stage = backButton.scene.window as Stage
            stage.scene = scene
            stage.title = "DrCOM 校园网登录"
            stage.centerOnScreen()

        } catch (e: Exception) {
            statusLabel.text = "返回登录界面失败: ${e.message}"
        }
    }

    private fun stopMonitoring() {
        updateTimeline?.stop()
        updateTimeline = null

        monitoringJob?.cancel()
        monitoringJob = null
    }

    // 当窗口关闭时调用
    fun onClose() {
        stopMonitoring()
    }
}
