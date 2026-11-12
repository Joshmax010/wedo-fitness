<p align="center">
  <strong>WeDo</strong><br>
  <em>Android 营养摄入与身体数据管理工具</em>
</p>

<p align="center">
  <a href="https://github.com/Joshmax010/wedo-fitness/releases/latest"><img src="https://img.shields.io/github/v/release/Joshmax010/wedo-fitness?style=for-the-badge&color=3DDC84&label=Release" alt="Release"></a>
  <a href="https://developer.android.com"><img src="https://img.shields.io/badge/Android-8.0+-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android"></a>
  <a href="https://github.com/Joshmax010/wedo-fitness/actions/workflows/cloud-tests.yml"><img src="https://img.shields.io/github/actions/workflow/status/Joshmax010/wedo-fitness/cloud-tests.yml?style=for-the-badge&label=CI" alt="CI"></a>
  <a href="./LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License"></a>
</p>

---

**WeDo** 是一款面向 Android 平台的本地优先营养记录与身体数据管理工具。它允许用户按餐次记录每日饮食摄入，自动换算热量与营养素，对照个性化目标查看达成情况，并通过周报趋势和身体指标回顾长期变化。

所有数据存储于设备本地，无需注册账户，核心功能完全离线可用。

<p align="center">
  <a href="https://github.com/Joshmax010/wedo-fitness/releases/download/v0.2.1/wedo-fitness-v0.2.1.apk"><strong>📦 下载最新版 v0.2.1</strong></a>
  &nbsp;·&nbsp;
  <a href="https://github.com/Joshmax010/wedo-fitness/releases/tag/v0.2.1">版本说明</a>
  &nbsp;·&nbsp;
  <a href="./MAINTENANCE.md">维护指南</a>
  &nbsp;·&nbsp;
  <a href="#english">English</a>
</p>

---

## 目录

- [功能概览](#功能概览)
- [安装与升级](#安装与升级)
- [版本更新](#版本更新)
- [数据与隐私](#数据与隐私)
- [开发与构建](#开发与构建)
- [技术架构](#技术架构)
- [项目文档](#项目文档)
- [开源协议](#开源协议)
- [English](#english)

---

## 功能概览

| 模块 | 功能描述 |
| :--- | :--- |
| **总览** | 查看指定日期的热量、宏量与微量营养素摄入，四餐独立汇总，直观对照每日目标完成度 |
| **饮食录入** | 支持早餐、午餐、晚餐、加餐四餐独立记录；kcal / kJ 双单位联动换算；按克重精确计算摄入量；支持编辑和删除历史记录 |
| **食物模板** | 内置常用食物数据库，支持自定义食物模板、分类标签管理与录入时自动补全 |
| **周报统计** | 7 日营养趋势图表、各营养素达标率统计、热量缺口分析汇总，支持一键复制报告文本 |
| **身体档案** | 记录体重、体脂率与肌肉量变化；维护个人身体档案；自动计算 BMR / TDEE 并推荐每日营养目标 |
| **外观设置** | 支持跟随系统、浅色、深色三种主题模式，偏好设置持久化保存 |
| **数据管理** | JSON 格式导出备份、导入前内容预览与确认，兼容旧版本备份及同名微信小程序备份格式 |

---

## 安装与升级

### 当前版本

| 项目 | 详情 |
| :--- | :--- |
| 版本号 | **0.2.1**（versionCode 9） |
| 系统要求 | Android 8.0（API 26）及以上 |
| 安装包 | [`wedo-fitness-v0.2.1.apk`](https://github.com/Joshmax010/wedo-fitness/releases/download/v0.2.1/wedo-fitness-v0.2.1.apk)（约 2.2 MB） |
| 应用包名 | `com.example.nutrition` |
| SHA-256 校验 | [`wedo-fitness-v0.2.1.apk.sha256`](https://github.com/Joshmax010/wedo-fitness/releases/download/v0.2.1/wedo-fitness-v0.2.1.apk.sha256) |

### 安装说明

1. 从 [GitHub Releases](https://github.com/Joshmax010/wedo-fitness/releases/latest) 下载 APK 安装包。
2. 在 Android 设备上打开安装包，首次安装需授权对应来源的安装权限。
3. 升级前请在「设置 → 数据管理」中导出 JSON 备份，并将备份文件保存至应用外部存储。

### 升级路径

| 升级场景 | 操作方式 |
| :--- | :--- |
| **正式版覆盖升级** | 使用相同签名密钥构建，支持直接覆盖升级并保留全部本地数据 |
| **Debug 测试包用户** | 测试包与正式包签名不同，无法覆盖安装。请先导出 JSON 备份 → 卸载测试包 → 安装正式包 → 导入备份。**卸载将清除本地数据，请务必确认备份已保存。** |

---

## 版本更新

### v0.2.1 · 2026-11-12

- **交互体验精简**：移除所有主界面与二级页面标题下方的冗余小字副标题，视觉更简洁清爽。
- **食物模板交互对齐**：食物模板标题支持与主页一致的滚动折叠动效（32sp → 20sp）；新增回到顶部 FAB 悬浮按钮（列表滚动后自动浮现）。
- **版本号规范调整**：调整版本号为 `0.2.1`（Alpha/预览体验版阶段），明确当前仍处于持续演进与打磨阶段。

---

## 数据与隐私

WeDo 将用户隐私视为核心设计原则：

- **本地存储**：所有数据保存在设备本地 Room 数据库中，外观偏好保存于本机 SharedPreferences。
- **无网络权限**：应用不申请 `INTERNET` 权限，不包含任何账号体系、云端同步或数据上传接口。
- **透明备份**：备份采用人类可读的 JSON 格式，导出至剪贴板，导入前支持完整内容预览。
- **数据清除提醒**：卸载应用或清除应用数据将永久删除本地记录。应用已关闭 Android 自动备份功能，请自行妥善保存导出的 JSON 文件。

> [!NOTE]
> 当前备份 Schema 版本为 v3，数据库版本为 v4。主界面 WeDo 链接通过系统浏览器打开 GitHub，该操作由外部浏览器处理网络连接。

### 备份兼容规则

| 场景 | 行为 |
| :--- | :--- |
| 备份中包含的日期 | 覆盖对应饮食记录 |
| 备份中未包含的日期 | 保留现有记录不变 |
| 旧备份缺少字段（如自定义模板、身体记录） | 保留设备上的现有数据 |
| 备份明确提供空列表 | 清空对应数据，内置食物模板保留 |

---

## 开发与构建

### 环境要求

| 依赖项 | 版本 |
| :--- | :--- |
| JDK | 21 |
| Android SDK Platform | 34 |
| Gradle | 9.4.1（通过 Wrapper 自动管理） |

### 快速开始

```bash
# 克隆仓库
git clone https://github.com/Joshmax010/wedo-fitness.git
cd wedo-fitness

# 构建 Debug APK 并运行单元测试与 Lint 检查
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Windows PowerShell：

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Debug APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

### Room Instrumentation 测试

```bash
# 需要连接 Android 设备或模拟器
./gradlew :app:connectedDebugAndroidTest
```

### 正式发布构建

正式构建需使用本地、未入库的 `keystore.properties` 配置发布签名密钥，随后执行 `:app:assembleRelease`。未配置签名时将生成 unsigned APK，不可直接作为安装包发布。完整的签名管理、版本更新与发布流程见 [MAINTENANCE.md](./MAINTENANCE.md)。

---

## 技术架构

| 层级 | 技术选型 |
| :--- | :--- |
| **语言与 UI** | Kotlin 2.2.10 · Jetpack Compose · Material 3 |
| **状态管理** | ViewModel / StateFlow · 一次性事件通道 · UseCase · Repository |
| **导航** | Navigation Compose 2.8.2，类型安全路由 |
| **持久化** | Room 2.8.4 · kotlinx.serialization 1.6.3 |
| **构建工具** | Gradle 9.4.1 · AGP 9.2.1 · KSP 2.3.2 |
| **Android SDK** | minSdk 26 · targetSdk / compileSdk 34 |
| **质量保障** | JUnit 4 / coroutines-test · Room Instrumentation · Android Lint · 手工设备验收 |

### 源码结构

```
app/src/main/java/com/example/nutrition/
├── data/          # 数据层：Room 数据库、DAO、实体与本地存储实现
├── domain/        # 领域层：业务模型、用例、校验器与常量定义
├── viewmodel/     # 表示层：页面状态管理与 UI 事件处理
└── ui/            # 界面层：Compose 页面、导航、组件、图表与主题
```

---

## 项目文档

| 文档 | 说明 |
| :--- | :--- |
| [维护指南](./MAINTENANCE.md) | 构建环境配置、版本与签名管理、测试边界、发布流程、分支策略与回滚方案 |
| [项目文档](./PROJECT_DOCUMENTATION.md) | 系统架构、数据模型、数据库迁移、备份兼容性与完整变更历史 |
| [回归测试清单](./LOCAL_REGRESSION_CHECKLIST.md) | 业务逻辑、备份导入导出、数据持久化与设备兼容性检查 |
| [页头复测清单](./PAGE_HEADER_DEVICE_CHECKLIST.md) | 滚动标题折叠、WeDo 链接、编辑定位与二级导航验证 |
| [变更日志](./CHANGELOG.md) | 各版本详细变更记录 |

> 反馈问题时请提供：应用版本、Android 版本、复现步骤及截图。涉及数据问题时请使用脱敏示例，**请勿提交个人备份、签名密钥或本机配置文件。**

---

## 开源协议

本项目基于 [MIT License](./LICENSE) 开源。

Copyright © 2026 [Joshmax010](https://github.com/Joshmax010)

---

## English

<details>
<summary><strong>Click to expand English documentation</strong></summary>

### Overview

**WeDo** is a local-first nutrition tracking and body metrics management application for Android 8.0+. It enables users to log meals across four daily categories, automatically calculate caloric and nutrient intake, compare progress against personalized goals, and review long-term trends through weekly reports and body composition tracking.

All data is stored locally on the device. No account registration is required, and core features are fully functional offline.

**[Download v0.2.1](https://github.com/Joshmax010/wedo-fitness/releases/download/v0.2.1/wedo-fitness-v0.2.1.apk)** · [Release Notes](https://github.com/Joshmax010/wedo-fitness/releases/tag/v0.2.1)

### What's New in v0.2.1

- **Refined Typography**: Removed redundant subtitle text under page headlines across all screens for a cleaner, modern look.
- **Enhanced Food Templates**: Added smooth collapsing title animation on scroll and a floating "scroll-to-top" FAB that appears after scrolling.
- **Version Calibration**: Re-anchored version to `0.2.1` to reflect early preview status.

### Privacy

The app requests no `INTERNET` permission and contains no account system, cloud sync, or data upload functionality. All records reside in a local Room database. Backups use human-readable JSON exported to the clipboard, with full preview before import. Uninstalling the app permanently deletes all local data.

### Build

Requires JDK 21 and Android SDK Platform 34. Use the Gradle Wrapper commands documented above. Release builds require a local `keystore.properties` file (not committed to version control). See [MAINTENANCE.md](./MAINTENANCE.md) and [PROJECT_DOCUMENTATION.md](./PROJECT_DOCUMENTATION.md) for release procedures and implementation details.

### License

[MIT License](./LICENSE) · Copyright © 2026 Joshmax010

</details>
