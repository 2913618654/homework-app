# 高校作业助手 (原生安卓端 App)

这是一个 100% 运行在安卓手机本地的原生 App 项目（Kotlin + Material 3 + OkHttp + Jsoup），**完全脱离电脑，手机安装后直接连接学习通和云班课**。

---

## 📱 核心特性

- 🚀 **100% 手机本地运行**：手机安装后，直接由手机网络向超星与云班课发送请求，不需要电脑开着任何服务。
- 🔗 **学习通 + 云班课聚合**：统一拉取两个平台的所有作业与活动。
- 🎯 **精准状态识别**：学生提交后等待老师打分的 **“待批阅”** 状态精准归入【已提交/已完成】，真正待交作业才会提醒。
- ⏳ **智能 DDL 预警**：24 小时内即将截止的待交任务标红高亮。
- 💾 **离线本地缓存**：内置 SharedPreferences 本地存储，无网络或网络卡顿也可以随时翻看已同步的历史作业。

---

## 🛠️ 如何获取与生成 APK 安装包

### 方式一：GitHub 自动化在线打包（强烈推荐，0 环境要求）

不需要在电脑上安装任何 Android Studio 或 Java 环境，借助 GitHub Actions 免费云端编译器即可一键输出 APK：

1. **新建 GitHub 仓库**：
   * 登录你的 [GitHub](https://github.com/) 账号，点击右上角 `+` -> **New repository**；
   * 仓库名随意填（例如 `homework-app`），选择 **Public** 或 **Private** 均可。
2. **上传项目代码**：
   * 在新建的仓库页面，点击 **uploading an existing file**；
   * 将当前 `homework_aggregator_android` 文件夹内的所有文件（包括隐藏的 `.github` 目录）拖拽上传并提交保存。
3. **下载 APK 安装包**：
   * 上传完成后，点击仓库顶部的 **Actions** 标签页；
   * 会看到一个正在运行的任务 **Build Android APK**；
   * 大约 1~2 分钟后显示绿色对勾（构建成功），点击该任务，在最下方的 **Artifacts** 处直接下载 **`HomeworkAssistant-Debug-APK`**；
   * 解压后得到 `app-debug.apk`，发送到手机安装即可！

---

### 方式二：使用电脑上的 Android Studio 本地打包

如果你电脑上安装了 Android 开发环境：

1. 打开 **Android Studio**，选择 **Open** 打开 `homework_aggregator_android` 文件夹；
2. 等待 Gradle 依赖同步完成；
3. 点击顶部菜单栏：**Build** -> **Build Bundle(s) / APK(s)** -> **Build APK(s)**；
4. 构建完成后，右下角弹窗点击 **locate**，即可看到生成的 `app-debug.apk` 安装包。

---

## ⚙️ 手机端使用说明

1. 手机安装 `app-debug.apk` 并打开；
2. 首次启动会弹出配置框，粘贴你的超星学习通 Cookie 或云班课 Cookie（在电脑浏览器登录后 F12 复制一次即可永久生效）；
3. 点击 **保存并同步**，手机就会直接向学习通与云班课请求最新的作业数据，并按截止时间倒序排列；
4. 平时想要刷新作业，只需在手机界面上**往下一拉（下拉刷新）**即可实时同步！
