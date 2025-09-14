package org.shiyi.zzuli_drcom

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

class HelloApplication : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(HelloApplication::class.java.getResource("drcom-login.fxml"))
        val scene = Scene(fxmlLoader.load(), 400.0, 600.0)

        // 添加CSS样式
        scene.stylesheets.add(HelloApplication::class.java.getResource("ios-style.css")?.toExternalForm())

        stage.title = "DrCOM 校园网登录"
        stage.scene = scene
        stage.isResizable = false
        stage.show()
    }
}

fun main() {
    Application.launch(HelloApplication::class.java)
}
