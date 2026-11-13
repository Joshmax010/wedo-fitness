# 构建、发布与后续维护

## 1. 当前版本与验证边界

- 发布版本：**0.2.2 / versionCode 10**；Room 数据库 v4，备份 Schema v3。
- 全套 149 个 JVM 单元测试 100% 通过（0 失败、0 错误、0 跳过）；Lint 0 错误。
- 正式发布构建已通过：`assembleRelease`（R8 混淆）、`testDebugUnitTest`。
- 正式 APK 已完成本地版本、包名、非 debuggable 与签名核验；证书与官方正式密钥一致。

| 正式 APK 项目 | 本地核验结果 |
|---|---|
| 文件名 | `wedo-fitness-v0.2.2.apk` |
| 文件大小 | 2,210,709 字节 |
| versionName / versionCode | 0.2.2 / 10 |
| applicationId | `com.example.nutrition` |
| SHA-256 | `216ba933f707ec6896556bf5d74964bbf59b83dbb59b622d0e1945fe1f748d9a` |
| 签名证书 SHA-256 | `041d81d18eb0d91506d4ec5c88d1c0f3a49aacf59bbaeb8f6d3fd697f8e4dd23` |
| apksigner | 验证成功，v2=true、v3=false |

[GitHub Release v0.2.2](https://github.com/Joshmax010/wedo-fitness/releases/tag/v0.2.2) 标记为 Latest。安装包下载与校验见 [README.md](./README.md)。

## 2. 获取代码与构建环境

主线为 `main`，正式版本由 `v0.2.2` 标签定位。首次克隆后进入仓库根目录；命令不依赖固定本地路径。

```powershell
git fetch origin --tags
git switch main
git pull --ff-only
git status --short
git rev-parse HEAD
java -version
.\gradlew.bat --version
```

使用 **JDK 21 运行 Gradle 9.4.1**。`gradle/gradle-daemon-jvm.properties` 指定 JetBrains JDK 21；`app/build.gradle.kts` 的 Java 源码兼容级别和 Kotlin JVM target 为 **17**，它们不是 Gradle 运行时 JDK。需要 Android SDK 34；通过本机 `local.properties` 配置 `sdk.dir`，不要提交个人 SDK 路径。Wrapper、插件和依赖版本以仓库源码为准。

Windows CLI 曾因 SQLite/KSP 临时目录不可执行而失败。必要时在当前 PowerShell 进程设置仓库内临时目录，再运行检查：

```powershell
$buildTemp = Join-Path (Get-Location) '.gradle\sqlite-tmp'
New-Item -ItemType Directory -Force -Path $buildTemp | Out-Null
$tempForward = $buildTemp -replace '\\', '/'
$env:JAVA_TOOL_OPTIONS = "-Djava.io.tmpdir=`"$tempForward`" -Dorg.sqlite.tmpdir=`"$tempForward`""
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
```

JVM 报告为 `app/build/reports/tests/testDebugUnitTest/index.html`，Lint 报告在 `app/build/reports/`，Debug APK 在 `app/build/outputs/apk/debug/`。ASCII 路径已完成本轮验证；若中文或空格路径出现历史测试 worker 加载类异常，先在 ASCII 路径复现，排查记录见 [项目文档 §7](./PROJECT_DOCUMENTATION.md#7-构建与运行)。

## 3. 正式签名与安装迁移

正式签名从仓库根目录的本地 `keystore.properties` 读取：`storeFile`、`storePassword`、`keyAlias`、`keyPassword`。该文件及密钥保持在 `.gitignore` 排除范围；不提交、不打印密码，也不把密钥放进 Release 资产。缺少签名配置时可能生成 unsigned APK，它只能用于构建检查，不能作为正式安装包发布。

官方包的正式签名证书 SHA-256 为：

```text
041d81d18eb0d91506d4ec5c88d1c0f3a49aacf59bbaeb8f6d3fd697f8e4dd23
```

本轮临时 Debug 测试包证书 SHA-256 为：

```text
9c8eb7fb92b5134ead5d7af01e0a1d18cf42563d98bc486c3b382d65be7481f7
```

正式包核验与原官方证书一致后，官方包安装可使用递增 versionCode 的正式包覆盖升级。临时 Debug 包与正式包签名不同，不能直接覆盖；先在应用「数据管理」导出 JSON，保存并检查备份，再卸载测试包、安装正式包、导入备份。**卸载会删除本地数据**，不要在备份保存前卸载。

```powershell
.\gradlew.bat :app:assembleRelease :app:testDebugUnitTest :app:lintRelease :app:lintDebug --console=plain
# 在 Android SDK 的 build-tools 目录选择已安装版本的 apksigner.bat：
# & '<SDK>\build-tools\<版本>\apksigner.bat' verify --verbose --print-certs '<正式 APK 路径>'
Get-FileHash -Algorithm SHA256 -LiteralPath 'app\build\outputs\apk\release\app-release.apk'
```

核对最终 APK 的 `versionName`、`versionCode`、包名和证书摘要；检查签名验证实际成功。保留 R8 `mapping.txt` 与构建/测试结果；不要凭文件名判断版本或签名。Release 上传后下载同一资产，再对照 SHA-256 和证书，确认发布的就是核验过的 APK。

## 4. 发布与分支维护

1. 新改动从最新 main 创建 `codex/` 分支，保留可审查的提交；不要直接改写已发布标签。
2. 应用版本同步维护 `app/build.gradle.kts` 的 `versionName` 与递增 `versionCode`、`NutrientConstants.APP_VERSION`、README 和文档变更历史。UI 调整不自动提高 Room 或备份 Schema 版本；需要迁移时另写 Migration 与兼容测试。
3. 执行相关构建、JVM 和 Lint；界面变化按 [统一回归表](./LOCAL_REGRESSION_CHECKLIST.md)及[页头专项表](./PAGE_HEADER_DEVICE_CHECKLIST.md)确认。需要 instrumentation 时连接设备执行 `:app:connectedDebugAndroidTest`，记录实际结果。
4. 完成正式签名、版本和 APK 核验，写清新增行为、升级方式、已执行检查与未执行项目。得到用户当前发布授权后合并 main，给对应提交建立版本标签、创建 GitHub Release 并上传正式 APK。
5. 回读 GitHub 上 main、标签、Release 和资产；下载回环确认哈希与签名。发布完成再清理已完成的临时分支；存在独有提交或证据时，先完成合并或建立归档标签。

本轮用户已于 2026-10-06 明确授权合并、发布与清理其他分支。历史报告树通过 `archive/cloud-test-results-2026-10-06` 标签保留，指向 `5eba86f069c51f865e35a15da8150925aef39428`；删除报告分支不删除该标签。历史验证链接已固定到提交，见 [验证记录](./CLOUD_VALIDATION.md)。

## 5. 故障定位与版本回退

1. 先保留用户数据：能进入应用时导出 JSON 并保存到应用之外，确认备份内容可读；记录安装版本、设备信息、复现步骤和截图。先不要卸载或清除应用数据。
2. 用 `git switch --detach v0.2.2` 等已发布标签在隔离工作目录或独立测试设备复现，再从 main 创建修复分支。构建、JVM、Lint 和影响范围内的手工步骤分别记录；数据相关问题补迁移、备份或 Room 测试。
3. 已安装正式版的 versionCode 不能用普通安装流程直接降级。需要恢复旧行为时，将修复提交或旧实现移植到当前 main，使用原正式证书并发布 **更高 versionCode** 的修复版本；保留数据库与备份兼容，避免要求普通用户卸载降级而丢失本地数据。
4. 修复后核验签名、版本与 APK 哈希，再发布递增版本；旧发布标签和历史报告保留用于定位，不强制重写标签。确需在专用测试设备安装旧包时，先备份并单独评估旧版本是否能读取较新的数据库和备份，不能将该步骤作为普通用户恢复流程。

## 6. 文档与 CI 维护

| 文档 | 维护内容 |
|---|---|
| README.md | 面向用户的功能、下载入口、版本与安装说明 |
| PROJECT_DOCUMENTATION.md | 架构、数据兼容、当前行为及按时间追加的变更历史 |
| CLOUD_VALIDATION.md | 每次提交、执行环境、运行链接和实际检查结果；保留历史证据 |
| DEVICE_FEEDBACK_FIXES.md | 设备反馈、修复范围与验收日期 |
| LOCAL_REGRESSION_CHECKLIST.md | 后续版本可复用的基础和 UI 回归步骤 |
| PAGE_HEADER_DEVICE_CHECKLIST.md | 页头、滚动、项目入口和相关布局复测步骤 |
| MAINTENANCE.md | 构建、签名、升级、发布及分支清理流程 |

`.github/workflows/cloud-tests.yml` 在 main 与面向 main 的 PR 上执行编译、JVM 和 Lint，也支持手动触发；仓库权限只读。Actions artifacts 保存报告 90 天，工作流不再生成报告分支。需要永久保留的发布证据应在到期前另行归档，并记录稳定链接。

后续每次更新分别记录自动化、手工验收和正式发布结果。项目持续按需求和反馈演进打磨。
