# DrCOM 校园网认证工具

郑州轻工业大学 DrCOM 校园网认证客户端，支持自动登录和保持在线。

## 功能特性

- 🚀 **快速登录** - 只需输入用户名和密码，自动检测网络配置
- 💾 **配置保存** - 支持保存配置信息，方便下次使用
- 🔄 **自动保活** - 登录后自动保持在线状态
- 🌐 **网络检测** - 检查当前网络连接状态
- ⚙️ **配置管理** - 查看和管理保存的配置

## 系统要求

- Java 21
- 网络连接

## 快速开始

### 1. 下载和运行

从 releases 页面下载最新的 `cli-1.0-SNAPSHOT-all.jar` 文件，或者使用以下命令构建：

```bash
./gradlew shadowJar
```

### 2. 运行程序

#### 方式一：快速登录（推荐）

直接运行程序，无需任何参数：

```bash
java -jar cli-1.0-SNAPSHOT-all.jar
```

程序会提示你输入用户名和密码，然后自动检测网络配置并登录。

#### 方式二：使用命令行参数

```bash
# 显示帮助信息
java -jar cli-1.0-SNAPSHOT-all.jar --help

# 使用子命令登录
java -jar cli-1.0-SNAPSHOT-all.jar quick
```

## 详细使用说明

### 可用命令

#### 1. `quick` - 快速登录（默认）

最简单的使用方式，只需要输入用户名和密码：

```bash
java -jar cli-1.0-SNAPSHOT-all.jar quick
```

**示例：**
```
=== DrCOM 快速登录 ===
请输入用户名: your_username
请输入密码: ********
正在连接到 DrCOM 服务器...
✓ 登录成功！
保持在线中，按 Ctrl+C 退出
```

#### 2. `login` - 完整登录

支持更多选项的登录方式：

```bash
java -jar cli-1.0-SNAPSHOT-all.jar login [选项]
```

**选项：**
- `-u, --username` : 用户名
- `-p, --password` : 密码  
- `-s, --server` : DrCOM 服务器地址（默认：10.30.1.19）
- `--save` : 保存配置到文件

**示例：**
```bash
# 使用命令行参数
java -jar cli-1.0-SNAPSHOT-all.jar login -u your_username -p your_password --save

# 交互式输入
java -jar cli-1.0-SNAPSHOT-all.jar login --save
```

#### 3. `auto` - 自动登录

使用之前保存的配置进行登录：

```bash
java -jar cli-1.0-SNAPSHOT-all.jar auto
```

**前提条件：** 需要先使用 `login --save` 保存过配置。

#### 4. `status` - 网络状态检查

检查当前网络连接状态：

```bash
java -jar cli-1.0-SNAPSHOT-all.jar status
```

#### 5. `config` - 配置管理

管理保存的配置信息：

```bash
# 显示当前配置
java -jar cli-1.0-SNAPSHOT-all.jar config --show

# 清除保存的配置
java -jar cli-1.0-SNAPSHOT-all.jar config --clear
```

## 自动检测功能

程序会自动检测以下信息，无需手动配置：

- **本机IP地址** - 自动获取活跃网络接口的IP地址
- **主机名** - 自动获取系统主机名
- **MAC地址** - 自动获取网络接口的MAC地址
- **操作系统** - 自动检测操作系统类型

这些信息对于 DrCOM 认证是必需的，程序会在后台自动处理。

## 配置文件

配置文件保存在用户主目录下的 `.drcom.properties` 文件中：

```
# 配置文件位置
# Windows: C:\Users\用户名\.drcom.properties
# macOS/Linux: /home/用户名/.drcom.properties
```

**配置文件内容示例：**
```properties
server=10.30.1.19
username=your_username
hostIp=192.168.1.100
hostName=YOUR-COMPUTER
mac=123456789012
hostOs=Windows 10
```

## 常见问题

### Q: 程序提示"登录失败"怎么办？

A: 请检查：
1. 用户名和密码是否正确
2. 网络连接是否正常
3. 是否在校园网环境中
4. DrCOM 服务器地址是否正确

### Q: 如何更换 DrCOM 服务器地址？

A: 使用 `login` 命令的 `-s` 选项：
```bash
java -jar cli-1.0-SNAPSHOT-all.jar login -s 新的服务器地址
```

### Q: 程序可以后台运行吗？

A: 可以，使用 nohup 或 screen 等工具：
```bash
# Linux/macOS
nohup java -jar cli-1.0-SNAPSHOT-all.jar quick > drcom.log 2>&1 &

# 或使用 screen
screen -S drcom java -jar cli-1.0-SNAPSHOT-all.jar quick
```

### Q: 如何停止程序？

A: 按 `Ctrl+C` 或发送中断信号，程序会自动断开连接并退出。

### Q: 配置文件在哪里？

A: 配置文件位于用户主目录下：
- Windows: `C:\Users\用户名\.drcom.properties`
- macOS: `/Users/用户名/.drcom.properties`  
- Linux: `/home/用户名/.drcom.properties`

## 开发和构建

### 构建项目

```bash
# 编译项目
./gradlew build

# 生成可执行 JAR
./gradlew shadowJar
```

生成的文件位于 `build/libs/cli-1.0-SNAPSHOT-all.jar`

### 运行开发版本

```bash
./gradlew run --args="quick"
```

## 技术支持

如遇到问题，请检查：

1. **Java 版本**：确保使用 Java 21
2. **网络环境**：确保在校园网环境中
3. **防火墙设置**：确保程序可以访问网络
4. **日志信息**：查看控制台输出的错误信息


## 更新日志

### v1.0-SNAPSHOT
- 实现基本的 DrCOM 认证功能
- 支持自动网络配置检测
- 提供多种登录方式
- 支持配置管理功能
