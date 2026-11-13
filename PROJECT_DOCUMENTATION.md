# 健身wedo Android 项目维护文档

> 本文档记录项目当前实际情况、技术架构、已实现功能与变更历史，供后续接手、升级或调整时参考。每次新增功能或做重大调整时，请在「变更历史」表格**顶部**插入新记录（最新变更排在最上方）。

**当前维护状态（2026-11-12）**：**0.2.2（versionCode 10）**。全套 149 个 JVM 单元测试 100% 通过，已构建官方正式签名 APK。正式发布结果、签名与校验信息见 [维护操作说明](./MAINTENANCE.md)。

---

## 1. 项目概述

本项目是微信小程序「健身wedo」（wedo Fitness）的 Android 原生版本。用户可手动录入每日各餐次的热量、三大宏量营养素及自定义微量营养素，应用自动汇总并与目标值对比，生成首页概览、每周统计报告。

### 1.1 产品定位

- 纯离线单机工具，不登录、不上传营养数据。右侧 WeDo 入口通过系统浏览器打开 GitHub 项目；应用自身不申请网络权限。
- 所有数据保存在本地 Room 数据库。
- 仅做数据记录与汇总展示，不提供医疗/营养建议。

### 1.2 技术栈

| 层级 | 选型 | 说明 |
|------|------|------|
| 语言 | Kotlin 2.2.10 | 全部主代码与测试 |
| UI | Jetpack Compose + Material3 | 声明式 UI |
| 架构 | MVI 风格 MVVM | 单一 `UiState`（StateFlow）+ 一次性事件（Channel）+ Repository + UseCase |
| 导航 | navigation-compose 2.8.2 | 类型安全路由（@Serializable route 对象），替代手写字符串路由 |
| 本地存储 | Room | SQLite 封装，目标/记录/元信息；读操作返回 Flow |
| 序列化 | kotlinx.serialization | 模型与 JSON 备份、导航路由参数 |
| 错误处理 | `Resource<T>` 封装 | 所有写操作返回 `Resource.Success`/`Resource.Error`，错误信息可直接展示 |
| 图表 | Compose Canvas 自定义 | 周报折线图/柱状图 |
| 构建 | Gradle 9.4.1 + AGP 9.2.1 + KSP 2.3.2 | compileSdk 34，minSdk 26，targetSdk 34；备份 schemaVersion 3 |

### 1.3 关键依赖版本

| 依赖 | 版本 | 用途 |
|------|------|------|
| androidx.compose:compose-bom | 2024.06.00 | Compose 版本对齐（material3、material-icons-extended 等由 BOM 管理） |
| androidx.activity:activity-compose | 1.9.0 | 单 Activity 宿主 |
| androidx.lifecycle（runtime-compose / viewmodel-compose） | 2.8.2 | 生命周期感知状态收集 |
| androidx.navigation:navigation-compose | 2.8.2 | 类型安全路由 |
| androidx.room（runtime / ktx / compiler） | 2.8.4 | 本地存储，compiler 经 KSP 接入 |
| org.jetbrains.kotlinx:kotlinx-serialization-json | 1.6.3 | 模型序列化与 JSON 备份 |
| androidx.core:core-ktx | 1.13.1 | 基础扩展 |
| 测试：junit / coroutines-test / androidx.test.ext / espresso / room-testing | 4.13.2 / 1.8.1 / 1.1.5 / 3.5.1 / 2.8.4 | 单元测试与 Instrumentation |

构建插件版本见根目录 `build.gradle.kts`：Kotlin 2.2.10、Compose 编译器插件 2.2.10、序列化插件 2.1.0、KSP 2.3.2、AGP 9.2.1；`gradle/wrapper/gradle-wrapper.properties` 指向 Gradle 9.4.1。

---

## 2. 工程结构

```
健身wedo-android/
├── app/
│   ├── src/main/java/com/example/nutrition/
│   │   ├── MainActivity.kt                  # 应用入口
│   │   ├── NutritionApp.kt                # Application，初始化仓库/数据库/云无关配置
│   │   ├── data/                            # 数据层
│   │   │   ├── local/db/                  # Room 数据库/DAO
│   │   │   ├── local/entity/               # Room Entity
│   │   │   └── repository/                # LocalStorageRepository 实现
│   │   ├── domain/                          # 领域层
│   │   │   ├── constants/                  # 常量（营养素默认值、版本号）
│   │   │   ├── model/                      # 数据模型
│   │   │   ├── repository/                 # 仓库接口
│   │   │   └── usecase/                    # 用例/业务逻辑
│   │   ├── ui/                             # UI 层
│   │   │   ├── components/                 # 可复用组件
│   │   │   ├── navigation/                 # 类型安全路由（AppRoutes.kt）/导航栏
│   │   │   ├── screens/                    # 页面
│   │   │   ├── charts/                     # 自定义 Canvas 图表
│   │   │   └── theme/                      # 颜色/字体
│   │   └── viewmodel/                      # 页面 ViewModel（单一 UiState + UIEvent 事件流）
│   │       └── UIEvent.kt                  # 一次性事件（Toast）定义
│   └── src/test/...                         # 单元测试
├── app/src/androidTest/...                  # Room 仓库集成测试
├── app/build.gradle.kts                     # 构建配置
├── gradlew / gradlew.bat / gradle/wrapper/      # Gradle Wrapper 9.4.1 完整入库
└── README / 本文件                          # 维护文档
```

---

## 3. 架构说明

### 3.1 分层

- **UI 层**：Compose Screen + ViewModel。Screen 通过 `collectAsStateWithLifecycle()` 订阅 ViewModel 暴露的单一 `uiState`，一次性 Toast 事件在 `repeatOnLifecycle(STARTED)` 中收集。
- **ViewModel 层**：每个页面对应一个 ViewModel，持有 `MutableStateFlow<UiState>`（对外只读 `asStateFlow()`）和 `Channel<UIEvent>`（`receiveAsFlow()`），负责业务协调。
- **Domain 层**：
  - `model`：纯数据类，如 `NutritionTargets`、`MealRecord`、`BodyProfile`、`Resource`（写操作结果封装）等。
  - `usecase`：纯函数/无状态业务逻辑，如 `Calculator`（汇总计算）、`MetabolismCalculator`（代谢计算）、`BackupManager`（导入导出）、`UnitConverter`（kcal↔kJ 换算）、`MealFormValidator`（录入表单校验）、`BodyStatsValidator`（身体记录校验）、`FoodTemplateMapper`（按克重换算模板）。
  - `repository`：接口 `LocalStorageRepository`，定义本地存储能力；读操作返回 `Flow<T>`，写操作返回 `suspend ... : Resource<Unit>`（成功/失败 + 用户可读错误信息）。
- **Data 层**：`RoomLocalStorageRepository` 实现接口，Entity 与 Domain 模型之间通过 JSON + Room 转换；Room 的 Flow 查询让数据变更自动向下游传递。

### 3.2 数据流向

```
UI（Screen）
  ↓ uiState.collectAsStateWithLifecycle()（状态） / events.collect（生命周期内的一次性事件）
ViewModel（MutableStateFlow<UiState> + Channel<UIEvent>）
  ↓ collect / flatMapLatest / combine
Repository（读：Flow；写：suspend -> Resource<Unit>）
  ↓ 实现
Room Database
```

### 3.3 关键设计决策

1. **数据模型与 Entity 分离**：`NutritionTargets` 等是纯 Kotlin data class；Room Entity 将复杂对象序列化为 JSON 字符串存储，保持领域模型简洁。
2. **响应式数据层**：`LocalStorageRepository` 的读方法返回 `Flow`，ViewModel 在 `viewModelScope` 中收集并驱动 Compose 状态；数据变更时 UI 自动刷新，无需手动调用 `loadData()`/`loadTemplates()`。
3. **单一 UiState（MVI 风格）**：每个 ViewModel 用一个不可变 `UiState` data class 承载全部页面状态，Screen 端 `val uiState by viewModel.uiState.collectAsStateWithLifecycle()` 读取，杜绝零散 `mutableStateOf` 导致的状态碎片化。
4. **一次性事件经 Channel**：Toast 等一次性提示通过 `Channel<UIEvent>(BUFFERED)` + `receiveAsFlow()` 发送，避免用 `mutableStateOf` 承载导致的重复触发/消费竞态。
5. **写操作返回 `Resource<T>`**：仓库所有写方法返回 `Resource.Success`/`Resource.Error(message)`，ViewModel 直接将 `message` 展示给用户，错误反馈统一且类型安全。
   备份导入的 `bulkSet` 将五张表的写入与现有回读校验放在同一个 Room 事务中；任何写入或校验失败都回滚本次导入，协程取消继续向上传播。
6. **业务逻辑下沉 UseCase**：单位换算（`UnitConverter`）、表单校验（`MealFormValidator`/`BodyStatsValidator`）、模板换算（`FoodTemplateMapper`）等纯逻辑放在 domain/usecase，ViewModel 只做编排。
7. **类型安全导航**：使用 navigation-compose 2.8+ 的 @Serializable 路由对象（`AppRoutes.kt`），页面间传参编译期可查；`MainScreen` 底栏选中态用 `NavDestination.hasRoute<T>()` 判断。
8. **单例 Repository**：`NutritionApp` 中以 `lazy` 方式持有 Repository 单例，页面通过 AndroidX 的 `viewModelFactory { initializer { ... } }` 将仓库传入 ViewModel，复用导航作用域内的 ViewModel。
9. **生命周期刷新**：使用 `DisposableEffect + LifecycleEventObserver` 监听 `ON_RESUME`，实现从其他页面返回时自动刷新；录入页以 `isFirstResume` 标记跳过首次 resume，避免覆盖导航传入的初始餐次。

---

## 4. 数据模型

### 4.1 核心模型

| 模型 | 说明 |
|------|------|
| `NutritionTargets` | 全局唯一的营养目标配置，含宏量/微量目标、身体档案、是否自动计算 |
| `BodyProfile` | 身体档案：性别、年龄、身高、体重、活动系数 |
| `Gender` | 性别枚举（男/女） |
| `ActivityLevel` | 活动系数枚举（久坐/轻度/中度/高度/极高） |
| `DayRecords` | 某一天的四餐记录集合 |
| `MealRecord` | 单条饮食记录：名称、热量、宏量、微量营养素 |
| `MealKey` | 餐次枚举：早餐/午餐/晚餐/加餐 |
| `AppMeta` | 应用元信息：版本、首次使用日期、引导状态、schemaVersion |
| `FoodTemplate` | 食物模板：名称、热量、宏量、微量、标签、是否预设 |
| `BodyRecord` | 身体记录：日期、体重、体脂率、肌肉量、备注 |

### 4.2 数据库版本

当前 Room 数据库版本：**4**

迁移记录：
- **1 → 2**：新增 `targets.bodyProfileJson` 和 `targets.isAutoCalculated`，支持二期代谢计算功能。
- **2 → 3**：新增 `food_templates` 和 `body_records` 表，支持食物模板与身体记录功能。
- **3 → 4**：`food_templates` 表新增 `tagsJson` 字段，支持模板多标签。
- 迁移文件：`NutritionDatabase.kt` 中的 `MIGRATION_1_2`、`MIGRATION_2_3`、`MIGRATION_3_4`。

### 4.3 备份 JSON Schema

导出 JSON 格式：

```json
{
  "app": "nutrition-tracker",
  "schemaVersion": 3,
  "exportedAt": "...",
  "data": {
    "targets": { /* NutritionTargets */ },
    "records": { /* Map<String, DayRecords> */ },
    "meta": { /* AppMeta */ },
    "foodTemplates": [ /* FoodTemplate[] (仅自定义) */ ],
    "bodyRecords": [ /* BodyRecord[] */ ]
  }
}
```

注意：
- 旧版备份（schemaVersion 1/2）仍可导入，`BackupManager` 会对缺失字段做容错。
- schemaVersion 3 备份会额外包含 `foodTemplates`（仅自定义模板）和 `bodyRecords` 字段。
- 导入在一个数据库事务中提交目标、饮食记录、元信息、自定义模板和身体记录。写入或现有回读校验失败时，保留导入前的数据。
- 原有导入规则保留：饮食记录按备份中的日期写入，未涉及的日期保留；旧备份缺少模板/身体记录字段时保留这些数据，明确传入空列表时清空自定义模板/身体记录，预设模板保留。
- 这是预防性的底层可靠性优化，目前没有用户反馈导入故障。备份 JSON 格式、数据库结构和页面交互均未调整。

---

## 5. 页面与功能

### 5.1 首页（HomeScreen）

- 显示日期、热量环形进度、宏量/微量营养素进度、四餐分布。
- 当前热量圆环中心以非负数显示差值，使用「还差 / 超出 / 已达目标」表达方向；整数不带小数，小数保留一位，方向按实际差值判断而非取整后的百分比。圆环下方显示摄入百分比与目标；宏量营养素已经使用三条进度条，微量营养素按两列排列。
- 支持左右切换日期，点击日期文本可打开 DatePicker 快速跳转。
- 有任何饮食记录（包括零热量记录）时显示完整仪表盘；无记录时显示空状态和录入入口。首页四餐卡片点击后直接进入录入页，并传递所选日期与餐次参数。
- 首页没有额外悬浮录入按钮；空状态按钮、四餐卡片与底部「录入」标签均可进入录入页。四个主页面左侧标题根据实际滚动从 32sp 缩至 20sp 并固定在顶部，右侧 WeDo 为 20sp，可点击打开 GitHub；底导航上滑时位移收起，反向滑动或回到顶部恢复，滚动视口高度保持稳定。
- 当身体档案存在时，显示「参考 TDEE：xxx kcal · 基于身体档案估算」。

### 5.2 录入页（RecordScreen）

- 四个等宽 Tab 铺满页面切换餐次：早餐/午餐/晚餐/加餐；标题占位、餐次、日期与表单处于同一纵向滚动容器，页名由固定顶部栏按实际滚动缩放显示。
- 默认根据当前系统时间选择餐次：
  - 05:00–09:59 → 早餐
  - 10:00–13:59 → 午餐
  - 14:00–16:59 → 加餐
  - 17:00–20:59 → 晚餐
  - 其他 → 加餐
- 支持切换日期，点击日期文本可打开 DatePicker。
- 表单：食物名称（选填）、克重(g)、热量（kcal/kJ 双单位并列输入）、蛋白质、脂肪、碳水、微量营养素。输入克重或模板时会自动换算实际摄入量。
- 保存新记录后，若食物名称未在模板库中出现，会弹出提示询问是否保存为模板；保存时可直接选择/创建标签。
- 列表展示当前日期/餐次记录，支持编辑和删除；非 100g 时会显示克重。
- 表单在上、记录列表在下，两者位于同一纵向滚动区域。点击记录卡片回填上方表单并自动定位表单标题，显示「编辑记录」「取消编辑」「更新」；定位时将顶部覆盖栏高度计入可视区域，避免表单标题被遮挡；日期与餐次随页面内容自然滚动，键盘和编辑状态保留顶部导航。
- 记录卡片展示宏量/微量标签，支持左滑删除并弹窗确认；静止时不显示红色删除背景，卡片与背景采用相同圆角，并按记录 ID 保留侧滑状态。食物名称匹配最多显示 5 条候选，点击候选才填入模板；克重变化会换算宏量及已有微量营养素。

### 5.3 周报页（WeeklyScreen）

- 按自然周（周一至周日）统计，默认本周，可左右切换历史周，不能进入未来周；不是滚动的最近 7 天。
- 自定义 Canvas 折线图（热量趋势）和分组柱状图（宏量营养素）。
- 周报摘要卡片：平均热量、平均蛋白质、达标天数、热量缺口总量；复制的周报文本另含平均脂肪与碳水。
- 微量营养素周览表格。
- 支持复制周报文本到剪贴板。
- 无热量数据时显示空状态；日均按整周 7 天计算，热量达到目标的 90%–110% 计为达标日。修改饮食记录或目标会更新当前周报；图表当前仅展示数据，没有点击选点交互。

### 5.4 设置页（SettingsScreen）

- **身体档案卡片**：性别、年龄、身高、体重、活动量。
- **自动计算推荐值**：基于 Mifflin-St Jeor 公式计算 BMR/TDEE，并推荐宏量目标。
- **每日营养目标**：手动输入热量、蛋白质、脂肪、碳水及微量营养素目标。
- **功能入口**：食物模板、身体记录。
- **数据管理**：导出/导入备份、清空记录。
- **关于**：版本号、存储占用、免责声明。
- 设置首页采用个人与目标、记录工具、应用与数据三组入口；档案与目标、数据管理、关于进入专门子页，外观通过选择弹窗即时切换。三个子页共用设置首页的 ViewModel，不重复创建目标草稿。
- 推荐值先预览 BMR/TDEE 与宏量目标，再点击套用填入表单，仍需点击「保存档案与目标」。身体档案与营养目标存储在同一个 `NutritionTargets` 中，共用保存操作；身体记录页的每日数据独立保存。
- 备份导出目前复制 JSON 到剪贴板；导入流程为粘贴 JSON → 预览数量/日期/目标等 → 确认导入，支持返回编辑。清空操作需确认且仅清空饮食记录。

### 5.5 食物模板（FoodTemplateScreen）

- 展示预设与自定义食物模板。
- 支持搜索模板名称。
- 自定义模板可新增/编辑/删除；预设模板也可编辑，但不能删除。
- 录入页新增记录保存成功后，如果名称没有同名模板，会询问是否另存为自定义模板。
- 模板支持多标签（tags），新增/编辑时可选已有标签或创建新标签。
- 新增入口为大标题右侧加号，搜索单独一行，标题、筛选与列表整体滚动。模板页保留「搜索 + 横向快捷标签 + 全部筛选」布局：常用分类标签横向排列一键筛选；点击「全部」打开弹窗查看并多选所有标签；已选标签以 chip 形式显示在顶部，可单独删除或一键清除。
- 预设模板来自 `PresetFoodTemplates`，目前包含 209 种常见食物（主食、肉蛋奶、豆制品、蔬菜、水果、坚果零食、饮品、油脂调料等），每个预设已自动打上对应分类标签。
- 搜索与标签筛选共同生效，多选标签满足任意一个即可匹配；筛选选择立即更新状态，「确定」关闭筛选弹窗。新增/编辑共用弹窗，目前编辑名称、热量、三大营养素及标签，未提供微量营养素编辑字段。

### 5.6 身体记录（BodyStatsScreen）

- 记录每日体重、体脂率、肌肉量、备注。
- 体重趋势折线图。
- 历史记录列表与删除。
- 页面顺序为体重趋势图（至少两条记录时显示）→ 日期与身体数据表单 → 最新日期在前的历史记录。选择日期会读取并回填该日已有数据，同日再次保存覆盖该日记录。
- 历史卡片展示日期、体重、体脂、肌肉量及备注；点击卡片回填现有表单并自动定位到顶部覆盖栏下方，显示编辑状态和取消入口。保留日期选择修改旧记录和删除确认，同日保存仍覆盖该日数据。

### 5.7 UI 改版方案与实施状态

当前导航为四个一级入口：总览、录入、周报、设置。食物模板、身体记录、档案与目标、数据管理、关于是设置内的五个二级入口；二级页隐藏主底栏且不保留其占位，顶部返回始终可用。页面状态由 ViewModel 保存，Room Flow 驱动记录、首页与周报更新。应用保持离线使用，数据模型与备份兼容规则见 §4。

用户已表达的改版方向：

- 六个页面统一重设计，参考 Apple 健身的大数字、进度与目标反馈；低饱和绿色，内容区简洁，导航或工具栏可有少量玻璃效果。
- 首页突出当天营养情况，采用热量大环与三条宏量营养进度；圆环中心保留热量差值，以非负数和方向文字说明差值含义。内容顺序确定为热量 → 三大营养 → 四餐 → 微量营养，微量营养全部展示；四餐摘要继续点击进入对应录入页，不改为首页展开详情。
- 保留四个底部入口与独立录入页，延续名称匹配模板、克重联动、完整营养展示/编辑和历史记录编辑能力。饮食记录点击后复用现有上方表单，自动滚动到编辑区域并明确标识编辑状态；首页四餐入口同时传递所选日期与餐次。
- 设置页过长、布局繁杂是已明确的痛点，改为分组入口后进入专门页面；身体档案与营养目标保留在同一个子页，共同保存。
- 首页取消额外悬浮录入按钮，底部已有「录入」入口使用清晰的加号和文字，与其他入口统一，不添加单独的方框或胶囊强调；空状态保留页面内录入按钮。浏览后续内容时底导航收起，左侧页名缩小并固定在顶部；反向滚动或回到顶部时底导航恢复，标题字号与页面实际位置一致。录入页日期与餐次随内容自然滚动；键盘打开时隐藏主底栏并保留顶部导航，编辑时自动定位现有表单。
- 身体记录历史卡片新增点击编辑，回填现有表单并自动滚到表单，保留日期选择入口。周报采用摘要 → 图表 → 微量营养的信息顺序，保留自然周与现有统计口径。
- 食物模板页保留搜索、横向快捷标签、全部筛选与已选标签布局，压缩间距并统一样式；新增入口放在页面大标题右侧，以紧凑的加号按钮呈现，搜索框单独占一行，避免「新增」文字被挤成两行。整体视觉密度适中，关键数字突出，普通文字与表单紧凑，录入页继续完整展示营养字段。
- 视觉形态已确认：柔和浅色/深灰卡片、适度圆角与弱阴影；底部采用贴近屏幕边缘的整条轻磨砂导航；四个主页面使用靠上排列的大标题，滚动后收成固定左侧的小标题，WeDo 位于右侧。磨砂效果限定在底导航和二级页面返回栏，主页面固定页头使用实色背景；输入字段采用柔和底色、弱边框，编辑中用绿色突出。
- 预览反馈修正：浅色页面背景使用纯白，卡片、字段采用中性灰，避免米黄或偏绿的底色；深色模式标题、关键数字与主文字使用白色，辅助文字采用具有充分对比的浅灰。四个底部导航入口统一表现，移除「录入」专属框体强调。
- 保存成功及三大营养素达标可以提供反馈：日常使用短暂状态变化、平滑进度与少量振动。整天完成定义为三大营养素均达到各自目标且热量处于目标的 90%–110%；仅今天主动保存饮食后首次满足条件时庆祝一次，查看历史、导入数据和当天反复修改不重复触发。庆祝在当前页短暂显示完成提示与少量彩屑，不遮挡操作、不自动跳页；反馈优先级低于错误和需要用户处理的弹窗。此规则用于新增完成反馈，不更改既有周报达标口径。
- 期望支持跟随系统、手动浅色/深色并保存偏好。主题配色须同时覆盖文字、卡片、字段、图表和导航。

第三版独立预览已获确认，开始分批落实 Android UI。第一批已实现主题颜色、公共字段配色与外观偏好：跟随系统/浅色/深色即时切换并由 Android SharedPreferences 保存；外观偏好属于本机设置，独立于 Room 营养数据与跨端备份。图表的文字、网格与轨道也使用当前主题。第二批已落实全局滚动导航与大标题，首页移除悬浮录入、调整四餐和微量营养顺序、放大热量圆环并说明缺口正负含义；有零热量记录也显示仪表盘，热量恰好达标时缺口显示 0。第三批已落实首页日期/餐次传递、饮食与身体历史记录编辑定位、完整字段共用组件和周报摘要优先；第四批完成设置分组与子页、模板统一滚动布局、饮食/身体保存状态和轻触反馈、今日目标首次完成的短暂庆祝。完成标记只保存在本机偏好中；弹窗关闭和页面处于前台后再显示，导入和历史浏览不触发。预览使用示例数据，与应用源码分离，仅验证外观、信息顺序与关键交互，不代替 Android 业务、持久化或真机验证。2026-10-01 的真机反馈已落实稳定滚动视口、等宽餐次、侧滑背景、较紧凑标题间距和五个二级页的导航调整。热量差值采用非负数与「还差 / 超出 / 已达目标」，2026-10-02 的收尾修正保留必要小数并让编辑定位避开顶部覆盖栏。首轮本地构建记录见 `DEVICE_FEEDBACK_FIXES.md`，最新云端检查见 `CLOUD_VALIDATION.md`。

核对来源为六个 `Screen`、对应 `ViewModel`、`RecordItem`、`MealCard`、`AppNavGraph` 与相关 DAO/计算逻辑；本节为源码审查结论，不替代真机交互验证。

2026-10-06 最终页头调整已完成：四个主页面标题上移，左侧按实际滚动由 32sp 缩至 20sp 并固定；右侧 WeDo 使用与收起标题一致的 20sp，按下有反馈并通过系统浏览器打开 `https://github.com/Joshmax010/wedo-Android`。饮食与身体历史编辑保留顶栏避让，保存提示不覆盖页名；设置二级页仍有返回、无主底导航。用户随后明确确认全部内容已测试完毕，手工验收完成，详细条目保存在 [页头真机检查表](./PAGE_HEADER_DEVICE_CHECKLIST.md)。

---

## 6. 二期代谢计算功能

### 6.1 实现位置

- `domain/model/BodyProfile.kt`
- `domain/model/Gender.kt`
- `domain/model/ActivityLevel.kt`
- `domain/usecase/MetabolismCalculator.kt`
- `viewmodel/SettingsViewModel.kt`
- `ui/screens/SettingsScreen.kt`
- `viewmodel/HomeViewModel.kt`
- `ui/screens/HomeScreen.kt`

### 6.2 计算公式

采用 **Mifflin-St Jeor 公式**：

- 男：BMR = 10 × 体重(kg) + 6.25 × 身高(cm) - 5 × 年龄 + 5
- 女：BMR = 10 × 体重(kg) + 6.25 × 身高(cm) - 5 × 年龄 - 161

TDEE = BMR × 活动系数

推荐宏量拆分：
- 蛋白质：1.8 g/kg（范围 50–240 g）
- 脂肪：TDEE 的 28%（范围 30–120 g）
- 碳水：用剩余热量补足

### 6.3 使用方式

1. 进入「设置」页。
2. 填写「身体档案」表单。
3. 点击「自动计算推荐值」。
4. 在弹窗中查看 BMR、TDEE 和推荐目标。
5. 点击「应用到表单」填充到目标表单。
6. 点击「保存档案与目标」共同持久化。

---

## 7. 构建与运行

### 7.1 环境要求

- Android Studio（建议最新稳定版）
- 使用 JDK 21 运行 Gradle 9.4.1；Gradle Daemon JVM 由 `gradle/gradle-daemon-jvm.properties` 指定 JetBrains JDK 21。Java 源码兼容级别和 Kotlin JVM target 为 17，与运行 Gradle 的 JDK 分开维护。
- Gradle 9.4.1（wrapper 配置已指向 9.4.1，本机发行版已下载）
- Android SDK 34

### 7.2 构建步骤

1. 打开 Android Studio。
2. **Open** → 选择当前克隆的仓库根目录；本次 Windows 路径为 `D:\AAAAA\ai\codex\jianshen\wedo-Android`。
3. 等待 Gradle Sync 完成。
4. 点击 **Build → Make Project**（Ctrl + F9）或 **Run app**（Shift + F10）。

Windows PowerShell 命令行验证与 APK 构建步骤统一见 [维护操作说明](./MAINTENANCE.md)。以下为 Bash 环境示例（本机 JDK 路径需按实际安装位置调整）：

```bash
# Gradle Wrapper 已入库，直接使用 gradlew
export JAVA_HOME="C:\Users\sunyu\.gradle\jdks\jetbrains_s_r_o_-21-amd64-windows.2"
# Room 2.8.4 的 KSP 查询校验器（DatabaseVerifier）要求临时目录可执行；
# 部分 shell 的 TEMP 指向 C:\WINDOWS\TEMP（不可执行）时必须覆盖，否则 :app:kspDebugKotlin 失败
export JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=C:/Users/sunyu/AppData/Local/Temp -Dorg.sqlite.tmpdir=C:/Users/sunyu/AppData/Local/Temp"
./gradlew :app:compileDebugKotlin --console=plain
# release 构建（R8 混淆）：./gradlew :app:assembleRelease
```

已验证：debug 三套编译（main / unit / androidTest）`BUILD SUCCESSFUL`；release 构建（`isMinifyEnabled` + R8）成功产出正式签名 APK 与 `mapping.txt`，`lintVitalRelease` 无致命问题。Android Studio 内构建不受临时目录问题影响（AS 使用用户级 Temp）。

### 7.3 运行测试

- 单元测试位于 `app/src/test/java/...`。
- 基础测试文件：`DateUtilsTest.kt`、`CalculatorTest.kt`、`BackupManagerTest.kt`、`InteropTest.kt`（106 个原有用例）；后续新增备份读取保护、ViewModel、UI 完成判定及页头测试，目前共 149 个 JVM 用例。
- 当前测试使用 JDK 21 运行 Gradle 9.4.1，源码 JVM target 为 17。

**历史环境限制**：在旧中文和空格路径 `D:\AAAAA\ai\wedo\健身wedo-android` 下，曾在 Gradle 8.13 与 9.4.1 上遇到测试 worker 加载类时抛出 `ClassNotFoundException`。当前仓库使用 ASCII 路径，本轮 149 个 JVM 用例已运行通过；后续若出现同类异常，可使用下面保留的路径排查方案。

解决方案：

1. **Android Studio**（推荐）：在 IDE 中右键测试目录 → Run Tests，AS 内置 runner 可正确处理中文路径。
2. **命令行（ASCII 路径副本）**：将项目 robocopy 到一个纯 ASCII 无空格目录后执行测试——
   ```bash
   # 同步源码（排除 build/.gradle/.idea 缓存）
   robocopy "D:\AAAAA\ai\wedo\健身wedo-android" C:\Users\sunyu\wedo-ascii-test /MIR /XD build .gradle .idea
   # 运行测试（Wrapper 已入库，进入副本目录后直接 ./gradlew）
   cd C:\Users\sunyu\wedo-ascii-test
   export JAVA_HOME="C:\Users\sunyu\.gradle\jdks\jetbrains_s_r_o_-21-amd64-windows.2"
   export JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=C:/Users/sunyu/AppData/Local/Temp -Dorg.sqlite.tmpdir=C:/Users/sunyu/AppData/Local/Temp"
   ./gradlew :app:testDebugUnitTest
   ```
   单元测试 149 个 JVM 用例全部通过（0 失败、0 错误、0 跳过）。最新测试数量与云端执行结果见 §7.5 和 `CLOUD_VALIDATION.md`。

### 7.4 Instrumentation 测试（androidTest）

- 位于 `app/src/androidTest/java/...`。
- `RoomLocalStorageRepositoryTest.kt` 覆盖所有 Repository 写操作，断言 `Resource.Success` / `Resource.Error`。
- 需要连接真机或模拟器后通过 `:app:connectedDebugAndroidTest` 执行。
- 导入事务新增 6 个回归用例：五表正常导入、`null` 字段保留原数据、空列表清空规则、末尾写入失败回滚、目标校验失败回滚、元信息校验失败回滚。失败用例仅在测试的内存数据库中创建 SQLite 触发器，不影响应用数据库。

Windows 本地连接调试设备后，可单独运行仓库集成测试：

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.example.nutrition.data.repository.RoomLocalStorageRepositoryTest"
```

### 7.5 本轮底层优化与 UI 改版的统一回归

完整拉取、编译、设备测试命令和手工检查项见 [`LOCAL_REGRESSION_CHECKLIST.md`](./LOCAL_REGRESSION_CHECKLIST.md)。全套 149 个 JVM 单元测试 100% 通过，Lint 0 错误，已构建官方正式签名 APK。发布版本为 **0.2.2（versionCode 10）**，数据库保持 v4，备份 Schema 保持 v3；验证证据统一见 [`CLOUD_VALIDATION.md`](./CLOUD_VALIDATION.md)。

---

## 8. 注意事项

1. **离线优先**：项目无云端同步，所有数据本地存储。卸载应用或清除应用数据会删除数据库，请提醒用户定期导出备份。
2. **医疗话术规避**：UI 文案避免「诊断」「治疗」「健康建议」等词，统一使用「记录」「汇总」「对比目标」「参考值」。
3. **审核定位**：工具型应用，不涉及用户隐私采集、虚拟支付、社交功能。
4. **数据库迁移**：从 v1 升级到 v2 使用 `MIGRATION_1_2`，已保留老数据。后续新增字段请继续编写 Migration，避免使用 `fallbackToDestructiveMigration` 导致数据丢失。
5. **备份兼容性**：`BackupManager` 已处理旧版备份的导入容错。
6. **版本号维护约定**：发布时同步 `app/build.gradle.kts` 的递增 `versionCode` / `versionName`、`NutrientConstants.APP_VERSION`（关于页展示与备份 meta 默认值）、README 与变更历史。本次为 versionCode 10 / versionName "0.2.2"。应用版本、Room 数据库版本和备份 Schema 各自维护，不因 UI 变更提高数据库或 Schema 版本。
7. **CLI 临时目录限制**：命令行构建若不覆盖 `java.io.tmpdir` / `org.sqlite.tmpdir`（TEMP 指向 `C:\WINDOWS\TEMP` 时），Room 2.8.4 的 KSP 查询校验器会抛 `ExceptionInInitializerError`；Android Studio 内构建不受影响。

---

## 9. 变更历史

| 日期 | 版本 | 变更内容 | 涉及文件 |
|------|------|----------|----------|
| 2026-11-12 | 0.2.2 | 视觉与审美质感重塑：① 卡片浮雕层级，浅灰背景（#F5F6F8）+ 纯白卡片（#FFFFFF）+ 1dp 弱环境阴影；② 深色模式对比拉伸；③ 鲜活高饱和营养素色谱（蛋白 #5B8DEF、脂肪 #EFA030、碳水 #41B876）；④ 录入页餐次切换重构为包裹式圆角滑块胶囊（Pill Tab）；⑤ 移除空状态系统 Emoji（🍽️、📊），替换为灰线矢量 Material Icons；⑥ 总览页间距拉伸至 16dp，设置页底栏防遮挡留白 80dp；⑦ 餐次卡片 Spring 阻尼回弹微交互；全套 149 个单元测试 100% 通过。 | `ui/theme/Color.kt`, `ui/theme/Theme.kt`, `ui/screens/{HomeScreen,RecordScreen,WeeklyScreen,SettingsScreen,FoodTemplateScreen,BodyStatsScreen}.kt`, `ui/components/MealCard.kt`, `build.gradle.kts`, `NutrientConstants.kt`, `CHANGELOG.md`, `README.md` |
| 2026-11-12 | 0.2.1 | 交互精简与动效对齐：① 食物模板标题折叠动效（32sp 吸顶折叠至 20sp）；② 食物模板滚动浮现回到顶部 FAB；③ 移除所有页面小字副标题；④ 设置枚举结构简化；⑤ 版本号规范调整为 0.2.1（Alpha/预览体验版阶段）。 | `ui/screens/FoodTemplateScreen.kt`, `viewmodel/SettingsViewModel.kt`, `ui/screens/*.kt`, `CHANGELOG.md` |

---

*本文档随功能升级、发布和验证结果同步维护。*
