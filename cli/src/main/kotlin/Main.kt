package org.shiyi

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.*
import kotlinx.coroutines.*
import java.io.File
import java.util.*

class DrComCli : CliktCommand(name = "drcom", help = "郑州轻工业大学 DrCOM 校园网认证工具") {
    override fun run() = Unit
}

class LoginCommand : CliktCommand(name = "login", help = "登录校园网") {
    private val username by option("-u", "--username", help = "用户名").prompt("请输入用户名")
    private val password by option("-p", "--password", help = "密码").prompt("请输入密码", hideInput = true)
    private val server by option("-s", "--server", help = "DrCOM 服务器地址").default("10.30.1.19")
    private val saveConfig by option("--save", help = "保存配置到文件").flag()

    override fun run() {
        echo("=== DrCOM 校园网认证工具 ===")

        // 使用自动检测创建配置
        val config = DrComConfig.create(
            username = username,
            password = password,
            server = server
        )

        if (saveConfig) {
            saveConfiguration(config)
        }

        val client = DrComClient(config)

        runBlocking {
            try {
                echo("正在连接到 DrCOM 服务器...")
                if (client.login()) {
                    echo("✓ 登录成功！")
                    echo("启动保持在线服务...")
                    echo("按 Ctrl+C 退出")

                    // 添加关闭钩子
                    Runtime.getRuntime().addShutdownHook(Thread {
                        client.disconnect()
                        echo("已断开连接")
                    })

                    client.startKeepAlive()
                } else {
                    echo("✗ 登录失败", err = true)
                }
            } catch (e: Exception) {
                echo("错误: ${e.message}", err = true)
            }
        }
    }

    private fun saveConfiguration(config: DrComConfig) {
        val configFile = File(System.getProperty("user.home"), ".drcom.properties")
        val properties = Properties()

        properties.setProperty("server", config.server)
        properties.setProperty("username", config.username)
        properties.setProperty("hostIp", config.hostIp)
        properties.setProperty("hostName", config.hostName)
        properties.setProperty("mac", config.mac.toString())
        properties.setProperty("hostOs", config.hostOs)

        configFile.outputStream().use { output ->
            properties.store(output, "DrCOM Configuration - Auto-detected")
        }

        echo("配置已保存到 ${configFile.absolutePath}")
    }
}

class QuickLoginCommand : CliktCommand(name = "quick", help = "快速登录（只需输入用户名密码）") {
    override fun run() {
        echo("=== DrCOM 快速登录 ===")

        val username = prompt("请输入用户名")!!
        val password = prompt("请输入密码", hideInput = true)!!

        // 使用默认服务器和自动检测配置
        val config = DrComConfig.create(
            username = username,
            password = password
        )

        val client = DrComClient(config)

        runBlocking {
            try {
                echo("正在连接到 DrCOM 服务器...")
                if (client.login()) {
                    echo("✓ 登录成功！")
                    echo("保持在线中，按 Ctrl+C 退出")

                    Runtime.getRuntime().addShutdownHook(Thread {
                        client.disconnect()
                        echo("已断开连接")
                    })

                    client.startKeepAlive()
                } else {
                    echo("✗ 登录失败", err = true)
                }
            } catch (e: Exception) {
                echo("错误: ${e.message}", err = true)
            }
        }
    }
}

class StatusCommand : CliktCommand(name = "status", help = "检查网络连接状态") {
    override fun run() {
        echo("检查网络连接状态...")

        try {
            val process = ProcessBuilder("ping", "-c", "1", "www.baidu.com")
                .redirectErrorStream(true)
                .start()

            val result = process.waitFor()

            if (result == 0) {
                echo("✓ 网络连接正常")
            } else {
                echo("✗ 网络连接异常，可能需要重新认证", err = true)
            }
        } catch (e: Exception) {
            echo("无法检查网络状态: ${e.message}", err = true)
        }
    }
}

class ConfigCommand : CliktCommand(name = "config", help = "配置管理") {
    private val show by option("--show", help = "显示当前配置").flag()
    private val clear by option("--clear", help = "清除保存的配置").flag()

    override fun run() {
        val configFile = File(System.getProperty("user.home"), ".drcom.properties")

        when {
            show -> showConfiguration(configFile)
            clear -> clearConfiguration(configFile)
            else -> echo("使用 --show 显示配置或 --clear 清除配置")
        }
    }

    private fun showConfiguration(configFile: File) {
        if (!configFile.exists()) {
            echo("未找到配置文件")
            return
        }

        val properties = Properties()
        configFile.inputStream().use { input ->
            properties.load(input)
        }

        echo("当前配置:")
        echo("服务器: ${properties.getProperty("server", "未设置")}")
        echo("用户名: ${properties.getProperty("username", "未设置")}")
        echo("本机IP: ${properties.getProperty("hostIp", "未设置")}")
        echo("主机名: ${properties.getProperty("hostName", "未设置")}")
        echo("MAC地址: ${properties.getProperty("mac", "未设置")}")
        echo("操作系统: ${properties.getProperty("hostOs", "未设置")}")
        echo("配置文件: ${configFile.absolutePath}")
    }

    private fun clearConfiguration(configFile: File) {
        if (configFile.exists()) {
            configFile.delete()
            echo("配置已清除")
        } else {
            echo("没有找到配置文件")
        }
    }
}

class AutoLoginCommand : CliktCommand(name = "auto", help = "使用保存的配置自动登录") {
    override fun run() {
        val configFile = File(System.getProperty("user.home"), ".drcom.properties")

        if (!configFile.exists()) {
            echo("未找到配置文件，请先使用 'drcom login --save' 保存配置", err = true)
            return
        }

        val properties = Properties()
        configFile.inputStream().use { input ->
            properties.load(input)
        }

        val username = properties.getProperty("username")
        val server = properties.getProperty("server", "10.30.1.19")
        val hostIp = properties.getProperty("hostIp", "192.168.1.100")
        val hostName = properties.getProperty("hostName", "DRCOM-CLIENT")
        val mac = properties.getProperty("mac", "0")?.toLongOrNull() ?: 0x20689df3d066L
        val hostOs = properties.getProperty("hostOs", "UNKNOWN")

        if (username.isNullOrEmpty()) {
            echo("配置文件中未找到用户名", err = true)
            return
        }

        val password = prompt("请输入密码", hideInput = true)!!

        val config = DrComConfig(
            server = server,
            username = username,
            password = password,
            hostIp = hostIp,
            hostName = hostName,
            mac = mac,
            hostOs = hostOs
        )

        val client = DrComClient(config)

        runBlocking {
            try {
                echo("正在连接到 DrCOM 服务器...")
                if (client.login()) {
                    echo("✓ 登录成功！")
                    echo("启动保持在线服务...")
                    echo("按 Ctrl+C 退出")

                    Runtime.getRuntime().addShutdownHook(Thread {
                        client.disconnect()
                    })

                    client.startKeepAlive()
                } else {
                    echo("✗ 登录失败", err = true)
                }
            } catch (e: Exception) {
                echo("错误: ${e.message}", err = true)
            }
        }
    }
}

fun main(args: Array<String>) {
    // 如果没有参数，直接运行快速登录
    if (args.isEmpty()) {
        QuickLoginCommand().main(emptyArray())
        return
    }

    DrComCli()
        .subcommands(
            QuickLoginCommand(),
            LoginCommand(),
            StatusCommand(),
            ConfigCommand(),
            AutoLoginCommand()
        )
        .main(args)
}