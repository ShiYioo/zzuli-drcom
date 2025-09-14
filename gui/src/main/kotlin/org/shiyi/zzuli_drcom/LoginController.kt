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
        val username = usernameField.text.trim()
        val password = passwordField.text
        val server = serverField.text.trim()

        // 输入验证
        if (username.isEmpty()) {
            showError("请输入用户名")
            usernameField.requestFocus()
            return
        }

        if (password.isEmpty()) {
            showError("请输入密码")
            passwordField.requestFocus()
            return
        }

        if (server.isEmpty()) {
            showError("请输入服务器地址")
            serverField.requestFocus()
            return
        }

        // 开始登录
        setLoginState(true)

        loginJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val config = DrComConfig.create(username, password, server)
                drcomClient = DrComClient(config)

                statusLabel.text = "正在连接到服务器..."

                val success = withContext(Dispatchers.IO) {
                    drcomClient?.login() ?: false
                }

                if (success) {
                    showSuccess("登录成功！")
                    saveCredentials()
                    setConnectedState()
                } else {
                    showError("登录失败，请检查用户名和密码")
                    setLoginState(false)
                }

            } catch (e: Exception) {
                showError("连接失败: ${e.message}")
                setLoginState(false)
            }
        }
    }

    @FXML
    private fun handleDisconnect() {
        loginJob?.cancel()
        drcomClient?.disconnect()
        drcomClient = null

        setLoginState(false)
        statusLabel.text = "已断开连接"
        statusLabel.styleClass.removeAll("success-text", "error-text")
        statusLabel.styleClass.add("info-text")
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
