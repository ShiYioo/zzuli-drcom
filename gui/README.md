# ZZULI DrCOM GUI客户端

一个基于JavaFX和Kotlin开发的现代化校园网认证客户端，专为郑州轻工业大学DrCOM网络认证系统设计。

## ✨ 特性

- 🎨 **iOS风格界面** - 采用现代化UI设计，提供流畅的动画效果
- 🔐 **安全认证** - 支持DrCOM协议的校园网认证
- 💾 **记住密码** - 支持保存登录凭据，提供自动登录功能
- 📊 **网络监控** - 实时显示网络状态和连接信息
- ⚙️ **高级设置** - 支持自定义服务器地址等高级配置

## 🛠️ 技术栈

- **编程语言**: Kotlin
- **UI框架**: JavaFX 21
- **构建工具**: Gradle
- **Java版本**: JDK 21
- **协程库**: Kotlinx Coroutines
- **UI组件**: ControlsFX, BootstrapFX

## 📦 系统要求

- Java 21 或更高版本
- Windows 10+, macOS 10.14+, 或 Linux (Ubuntu 18.04+)
- 网络连接

## 🚀 快速开始

### 使用预编译版本

1. 从 [Releases](../../releases) 页面下载适合你操作系统的安装包
2. 解压并运行应用程序
3. 输入你的学号和密码
4. 点击"登录"按钮连接到校园网

#### INFO
- 目前只适用于zzuli办公区的校园网连接（宿舍区还有待测试）
### 从源码构建

```bash
# 克隆项目
git clone https://github.com/your-username/zzuli-drcom.git
cd zzuli-drcom/gui

# 构建项目
./gradlew build

# 运行应用
./gradlew run

# 生成发布包
./gradlew jpackage
```

## 📖 使用说明

### 基本登录
1. 启动应用程序
2. 输入学号和密码
3. 选择是否记住密码和自动登录
4. 点击"登录"按钮

### 高级设置
- 点击"高级设置"展开更多配置选项
- 可以自定义DrCOM服务器地址
- 支持手动配置网络参数

### 网络监控
- 应用会实时显示当前网络状态
- 显示连接时长和网络使用统计
- 支持手动断开和重连

## 🎨 界面预览

应用采用iOS风格设计，包含以下界面：

- **登录界面**: 简洁的用户认证界面
- **网络监控**: 实时网络状态显示
- **设置界面**: 高级配置选项

## 📁 项目结构

```
gui/
├── src/main/kotlin/org/shiyi/zzuli_drcom/
│   ├── HelloApplication.kt      # 主应用程序类
│   ├── LoginController.kt       # 登录界面控制器
│   ├── MonitorController.kt     # 监控界面控制器
│   ├── DrComClient.kt          # DrCOM协议客户端
│   ├── NetworkStats.kt         # 网络统计
│   └── IOSAnimations.kt        # iOS风格动画（暂时没有应用）
├── src/main/resources/org/shiyi/zzuli_drcom/
│   ├── drcom-login.fxml        # 登录界面布局
│   ├── monitor.fxml            # 监控界面布局
│   └── ios-style.css           # iOS风格样式
├── build.gradle.kts            # 构建配置
└── README.md                   # 项目文档
```

## 🔧 配置文件

应用会在用户目录下创建配置文件来保存设置：

- **Windows**: `%USERPROFILE%\.zzuli-drcom\`
- **macOS**: `~/Library/Preferences/org.shiyi.zzuli_drcom/`
- **Linux**: `~/.config/zzuli-drcom/`

## 🐛 故障排除

### 常见问题

**Q: 无法连接到服务器**
- 检查网络连接是否正常
- 确认DrCOM服务器地址是否正确
- 尝试重启应用程序

**Q: 登录后没有网络**
- 检查用户名和密码是否正确
- 确认账户是否有欠费或被禁用
- 联系网络管理员

**Q: 应用启动失败**
- 确认Java版本是否为21或更高
- 检查系统是否支持JavaFX
- 查看错误日志获取详细信息

## 🤝 贡献

欢迎贡献代码！请遵循以下步骤：

1. Fork 这个项目
2. 创建你的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的改动 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建一个Pull Request

**注意**: 本项目仅供学习和研究使用，请遵守学校网络使用规定。本项目未支持校园网防检测功能，将来也不会去支持，本项目符合zzuli校规和国家法律法规。

**由于长时间未找到宿舍区有线校园网的使用者进行配合测试，暂时不发布包含宿舍区的版本，如果你愿意参加我的测试，请及时联系我，谢谢**
