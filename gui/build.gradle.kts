plugins {
    java
    application
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.javamodularity.moduleplugin") version "1.8.15"
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("org.beryx.jlink") version "2.25.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.shiyi"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val junitVersion = "5.12.1"


tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainModule.set("org.shiyi.zzuli_drcom")
    mainClass.set("org.shiyi.zzuli_drcom.HelloApplication")
}
kotlin {
    jvmToolchain( 21 )
}

javafx {
    version = "21.0.6"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.web", "javafx.swing" )
}

dependencies {
    implementation("org.controlsfx:controlsfx:11.2.1")
    implementation("org.kordamp.ikonli:ikonli-javafx:12.3.1")
    implementation("org.kordamp.bootstrapfx:bootstrapfx-core:0.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.7.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jlink {
    imageZip.set(layout.buildDirectory.file("distributions/app-${javafx.platform.classifier}.zip"))
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    launcher {
        name = "app"
    }


    targetPlatform("linux-x64") {
        setJdkHome("/Volumes/shiyi512/jdk/21/zulu/zulu21.44.17-ca-fx-jdk21.0.8-linux_x64")
    }

    targetPlatform("windows-x64") {
        setJdkHome("/Volumes/shiyi512/jdk/21/zulu/zulu21.44.17-ca-fx-jdk21.0.8-win_x64")
    }

    targetPlatform("mac-x64") {
        setJdkHome("/Volumes/shiyi512/jdk/21/zulu/zulu21.44.17-ca-fx-jdk21.0.8-macosx_x64")
    }

    targetPlatform("mac-aarch64") {
        setJdkHome("/Volumes/shiyi512/jdk/21/zulu/zulu21.44.17-ca-fx-jdk21.0.8-macosx_aarch64")
    }


}
