package org.shiyi.zzuli_drcom

import javafx.application.Platform
import javafx.concurrent.Task
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.*
import javafx.scene.layout.VBox
import kotlinx.coroutines.*
import java.net.URL
import java.util.*
import java.util.prefs.Preferences

class LoginController : Initializable {

    @FXML private lateinit var rootContainer: VBox
    @FXML private lateinit var titleLabel: Label
    @FXML private lateinit var usernameField: TextField
    @FXML private lateinit var passwordField: PasswordField
    @FXML private lateinit var serverField: TextField
    @FXML private lateinit var loginButton: Button
    @FXML private lateinit var statusLabel: Label
    @FXML private lateinit var progressIndicator: ProgressIndicator
    @FXML private lateinit var rememberCheckBox: CheckBox
    @FXML private lateinit var autoLoginCheckBox: CheckBox
    @FXML private lateinit var disconnectButton: Button
    @FXML private lateinit var networkInfoLabel: Label

    private var drcomClient: DrComClient? = null
    private val prefs = Preferences.userNodeForPackage(LoginController::class.java)
    private var loginJob: Job? = null

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        setupUI()
        loadSavedCredentials()
        updateNetworkInfo()
    }

    private fun setupUI() {
        // 初始状态
        progressIndicator.isVisible = false
        disconnectButton.isVisible = false
        statusLabel.text = "请输入用户名和密码"

        // 设置默认服务器
        serverField.text = "10.30.1.19"

        // 绑定事件
        loginButton.setOnAction { handleLogin() }
        disconnectButton.setOnAction { handleDisconnect() }
        rememberCheckBox.setOnAction { handleRememberCredentials() }

        // 回车键登录
        usernameField.setOnAction { passwordField.requestFocus() }
        passwordField.setOnAction { if (loginButton.isVisible) handleLogin() }
    }

    private fun loadSavedCredentials() {
        if (prefs.getBoolean("remember", false)) {
            usernameField.text = prefs.get("username", "")
            passwordField.text = prefs.get("password", "")
            serverField.text = prefs.get("server", "10.30.1.19")
            rememberCheckBox.isSelected = true
            autoLoginCheckBox.isSelected = prefs.getBoolean("autoLogin", false)

            // 如果开启了自动登录且有保存的凭据
            if (autoLoginCheckBox.isSelected && usernameField.text.isNotEmpty() && passwordField.text.isNotEmpty()) {
                Platform.runLater {
                    handleLogin()
                }
            }
        }
    }

    private fun saveCredentials() {
        if (rememberCheckBox.isSelected) {
            prefs.put("username", usernameField.text)
            prefs.put("password", passwordField.text)
            prefs.put("server", serverField.text)
            prefs.putBoolean("remember", true)
            prefs.putBoolean("autoLogin", autoLoginCheckBox.isSelected)
        } else {
            prefs.clear()
        }
    }

    private fun handleRememberCredentials() {
        if (!rememberCheckBox.isSelected) {
            autoLoginCheckBox.isSelected = false
            prefs.clear()
        }
    }

    private fun updateNetworkInfo() {
        try {
            val config = DrComConfig.create("", "")
            networkInfoLabel.text = "本机IP: ${config.hostIp}  主机名: ${config.hostName}"
        } catch (e: Exception) {
            networkInfoLabel.text = "网络信息获取失败"
        }
    }

    @FXML
    private fun handleLogin() {
        println("登录开始...")

        // 防止重复点击 - 如果正在登录中，直接返回
        if (loginJob?.isActive == true) {
            println("登录已在进行中，忽略重复点击")
            return
        }

        // 先清理之前的连接
        drcomClient?.disconnect()
        drcomClient = null

        val username = usernameField.text.trim()
        val password = passwordField.text
        val server = serverField.text.trim()

        // 输入验证
        if (username.isEmpty()) {
            println("用户名为空")
            showError("请输入用户名")
            usernameField.requestFocus()
            return
        }

        if (password.isEmpty()) {
            println("密码为空")
            showError("请输入密码")
            passwordField.requestFocus()
            return
        }

        if (server.isEmpty()) {
            println("服务器地址为空")
            showError("请输入服务器地址")
            serverField.requestFocus()
            return
        }

        println("开始设置登录状态...")
        // 开始登录 - 直接设置 UI 状态
        loginButton.isVisible = false
        disconnectButton.isVisible = false
        progressIndicator.isVisible = true
        usernameField.isDisable = true
        passwordField.isDisable = true
        serverField.isDisable = true
        rememberCheckBox.isDisable = true
        autoLoginCheckBox.isDisable = true
        statusLabel.text = "正在连接到服务器..."

        println("启动协程...")
        loginJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                println("创建配置...")
                val config = DrComConfig.create(username, password, server)
                println("创建客户端...")
                drcomClient = DrComClient(config)

                println("开始登录...")
                val success = drcomClient?.login() ?: false
                println("登录结果: $success")

                Platform.runLater {
                    println("更新UI...")
                    if (success) {
                        println("登录成功，更新UI...")
                        statusLabel.text = "登录成功！"
                        statusLabel.styleClass.removeAll("error-text", "info-text")
                        statusLabel.styleClass.add("success-text")
                        saveCredentials()

                        // 设置连接状态
                        loginButton.isVisible = false
                        disconnectButton.isVisible = true
                        progressIndicator.isVisible = false
                        // 保持输入框禁用状态
                    } else {
                        println("登录失败，清理连接并更新UI...")
                        // 登录失败时清理连接
                        drcomClient?.disconnect()
                        drcomClient = null

                        statusLabel.text = "登录失败，请检查用户名和密码"
                        statusLabel.styleClass.removeAll("success-text", "info-text")
                        statusLabel.styleClass.add("error-text")

                        // 恢复登录状态
                        loginButton.isVisible = true
                        disconnectButton.isVisible = false
                        progressIndicator.isVisible = false
                        usernameField.isDisable = false
                        passwordField.isDisable = false
                        serverField.isDisable = false
                        rememberCheckBox.isDisable = false
                        autoLoginCheckBox.isDisable = false
                    }
                }

            } catch (e: Exception) {
                println("登录异常: ${e.message}")
                e.printStackTrace()

                // 异常时清理连接
                drcomClient?.disconnect()
                drcomClient = null

                Platform.runLater {
                    println("异常，更新UI...")
                    statusLabel.text = "连接失败: ${e.message}"
                    statusLabel.styleClass.removeAll("success-text", "info-text")
                    statusLabel.styleClass.add("error-text")

                    // 恢复登录状态
                    loginButton.isVisible = true
                    disconnectButton.isVisible = false
                    progressIndicator.isVisible = false
                    usernameField.isDisable = false
                    passwordField.isDisable = false
                    serverField.isDisable = false
                    rememberCheckBox.isDisable = false
                    autoLoginCheckBox.isDisable = false
                }
            }
        }
        println("协程已启动")
    }

    @FXML
    private fun handleDisconnect() {
        println("开始断开连接...")

        // 取消登录任务
        loginJob?.cancel()
        loginJob = null

        // 清理客户端连接
        try {
            drcomClient?.disconnect()
            println("客户端连接已清理")
        } catch (e: Exception) {
            println("清理连接时出错: ${e.message}")
        } finally {
            drcomClient = null
        }

        // 恢复UI状态
        loginButton.isVisible = true
        disconnectButton.isVisible = false
        progressIndicator.isVisible = false
        usernameField.isDisable = false
        passwordField.isDisable = false
        serverField.isDisable = false
        rememberCheckBox.isDisable = false
        autoLoginCheckBox.isDisable = false

        statusLabel.text = "已断开连接"
        statusLabel.styleClass.removeAll("success-text", "error-text")
        statusLabel.styleClass.add("info-text")

        println("断开连接完成")
    }

    private fun setLoginState(isLogging: Boolean) {
        Platform.runLater {
            if (isLogging) {
                loginButton.isVisible = false
                disconnectButton.isVisible = false
                progressIndicator.isVisible = true

                usernameField.isDisable = true
                passwordField.isDisable = true
                serverField.isDisable = true
                rememberCheckBox.isDisable = true
                autoLoginCheckBox.isDisable = true
            } else {
                loginButton.isVisible = true
                disconnectButton.isVisible = false
                progressIndicator.isVisible = false

                usernameField.isDisable = false
                passwordField.isDisable = false
                serverField.isDisable = false
                rememberCheckBox.isDisable = false
                autoLoginCheckBox.isDisable = false
            }
        }
    }

    private fun setConnectedState() {
        Platform.runLater {
            loginButton.isVisible = false
            disconnectButton.isVisible = true
            progressIndicator.isVisible = false

            usernameField.isDisable = true
            passwordField.isDisable = true
            serverField.isDisable = true
            rememberCheckBox.isDisable = true
            autoLoginCheckBox.isDisable = true
        }
    }

    private fun showSuccess(message: String) {
        Platform.runLater {
            statusLabel.text = message
            statusLabel.styleClass.removeAll("error-text", "info-text")
            statusLabel.styleClass.add("success-text")
        }
    }

    private fun showError(message: String) {
        Platform.runLater {
            statusLabel.text = message
            statusLabel.styleClass.removeAll("success-text", "info-text")
            statusLabel.styleClass.add("error-text")
        }
    }

    private fun showInfo(message: String) {
        Platform.runLater {
            statusLabel.text = message
            statusLabel.styleClass.removeAll("success-text", "error-text")
            statusLabel.styleClass.add("info-text")
        }
    }
}
