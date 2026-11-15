package com.example.nutrition.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.painterResource
import com.example.nutrition.BuildConfig
import com.example.nutrition.R
import kotlinx.coroutines.launch
import com.example.nutrition.ui.components.ObserveUiEvents
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.nutrition.ui.components.NutritionField
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import com.example.nutrition.ui.theme.nutritionFieldColors
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.nutrition.ui.components.DataLoadError
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.nutrition.NutritionApp
import com.example.nutrition.domain.model.ActivityLevel
import com.example.nutrition.domain.model.Gender
import com.example.nutrition.domain.usecase.BackupManager
import com.example.nutrition.ui.components.NutrientPicker
import com.example.nutrition.ui.theme.BgCard
import com.example.nutrition.ui.theme.BgMain
import com.example.nutrition.ui.theme.Error
import com.example.nutrition.ui.theme.Primary
import com.example.nutrition.ui.theme.TextPrimary
import com.example.nutrition.ui.theme.TextSecondary
import com.example.nutrition.ui.theme.TextPlaceholder
import com.example.nutrition.ui.theme.Warning
import com.example.nutrition.ui.theme.LocalAppearance
import com.example.nutrition.ui.theme.ThemeMode
import com.example.nutrition.viewmodel.SettingsViewModel

/**
 * 设置页 —— 对应小程序 pages/settings/settings.wxml
 *
 * 每日营养目标表单 + 数据管理 + 关于 + 导入弹窗
 */
enum class SettingsSection(val title: String) {
    MAIN("设置"),
    TARGETS("档案与目标"),
    DATA("数据管理"),
    ABOUT("关于")
}

@Composable
fun SettingsScreen(
    onNavigateToTemplates: () -> Unit = {},
    onNavigateToBodyStats: () -> Unit = {},
    onNavigateToTargets: () -> Unit = {},
    onNavigateToData: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    section: SettingsSection = SettingsSection.MAIN,
    viewModel: SettingsViewModel = viewModel(
        factory = NutritionApp.appViewModelFactory { repo, backup ->
            SettingsViewModel(repo, backup)
        }
    )
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    com.example.nutrition.ui.navigation.TrackRootTitleScroll { scrollState.value.toFloat() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAppearance by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    // 一次性事件（Toast）
    ObserveUiEvents(viewModel.events, blocked = uiState.showCalcDialog || uiState.showImportModal || uiState.showClearConfirm || uiState.showNutrientPicker || showAppearance)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(com.example.nutrition.ui.navigation.LocalPageContentPadding.current)
                .padding(horizontal = 16.dp)
        ) {
            com.example.nutrition.ui.navigation.PageTitle(section.title)
            uiState.targetsError?.let { DataLoadError(it, viewModel::initialize) }
            Spacer(modifier = Modifier.height(8.dp))

            when (section) {
                SettingsSection.MAIN -> {
                    SettingsMenuCard("个人与目标") {
                        DataRow("身体档案与营养目标", "档案、代谢估算与每日目标", onClick = onNavigateToTargets)
                    }
                    FeaturesCard(onTemplates = onNavigateToTemplates, onBodyStats = onNavigateToBodyStats)
                    val appearance = LocalAppearance.current
                    val updateManager = remember(context) { com.example.nutrition.updater.UpdateManager.getInstance(context) }
                    SettingsMenuCard("偏好与显示") {
                        DataRow("外观", appearance.mode.label) { showAppearance = true }
                        SwitchRow(
                            title = "显示微量营养素",
                            subtitle = "在首页、记录页及周报中展开微量营养素明细",
                            checked = appearance.showMicronutrients,
                            onCheckedChange = { appearance.updateShowMicronutrients(it) }
                        )
                    }
                    SettingsMenuCard("应用与数据") {
                        DataRow("数据管理", "备份、导入与清空饮食记录", onClick = onNavigateToData)
                        DataRow(
                            title = "关于",
                            desc = if (updateManager.updateAvailable) "发现新版本可用 · 点击查看" else "版本、品牌、更新与存储占用",
                            badgeDot = updateManager.updateAvailable,
                            onClick = onNavigateToAbout
                        )
                    }
                }
                SettingsSection.TARGETS -> {
                    BodyProfileCard(viewModel, uiState)
                    TargetsCard(viewModel, uiState)
                }
                SettingsSection.DATA -> DataManagementCard(viewModel)
                SettingsSection.ABOUT -> AboutCard(viewModel, uiState)
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        if (showAppearance) {
            AlertDialog(
                onDismissRequest = { showAppearance = false },
                title = { Text("外观", color = TextPrimary) },
                text = { AppearanceChoices() },
                confirmButton = { TextButton(onClick = { showAppearance = false }) { Text("完成") } }
            )
        }

        // ========== 推荐值预览弹窗 ==========
        if (uiState.showCalcDialog) {
            CalculationDialog(viewModel = viewModel, uiState = uiState)
        }

        // ========== 营养素选择器 ==========
        NutrientPicker(
            visible = uiState.showNutrientPicker,
            existingKeys = uiState.micronutrients.map { it.key },
            onPick = { item -> viewModel.addMicronutrient(item) },
            onDismiss = { viewModel.dismissNutrientPicker() }
        )

        // ========== 导入弹窗 ==========
        if (uiState.showImportModal) {
            ImportModal(viewModel = viewModel, uiState = uiState)
        }

        // ========== 清空确认弹窗 ==========
        if (uiState.showClearConfirm) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissClear() },
                title = { Text("确认清空", fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = { Text("将清空全部饮食记录（目标设置保留），此操作不可恢复。", color = TextSecondary) },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmClear() }) {
                        Text("清空", color = Error, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissClear() }) {
                        Text("取消", color = TextSecondary)
                    }
                }
            )
        }
    }
}

// ==================== 营养目标卡片 ====================

@Composable
private fun SettingsMenuCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) { SectionTitle(title); content() }
    }
}

@Composable
private fun AppearanceChoices() {
    val appearance = LocalAppearance.current
    Column(Modifier.selectableGroup()) {
        ThemeMode.entries.forEach { mode ->
            Row(
                modifier = Modifier.fillMaxWidth().selectable(
                    selected = appearance.mode == mode, role = Role.RadioButton,
                    onClick = { appearance.select(mode) }
                ).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = appearance.mode == mode, onClick = null)
                Text(mode.label, color = TextPrimary)
            }
        }
    }
}

@Composable
private fun TargetsCard(viewModel: SettingsViewModel, uiState: SettingsViewModel.UiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle("每日营养目标")

            // 热量
            NutritionField(label = "热量", unit = "kcal", value = uiState.calories, placeholder = "如 2000", onValueChange = viewModel::onCaloriesInput)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

            // 宏量
            NutritionField(label = "蛋白质", unit = "g", value = uiState.protein, placeholder = "如 120", onValueChange = viewModel::onProteinInput)
            NutritionField(label = "脂肪", unit = "g", value = uiState.fat, placeholder = "如 65", onValueChange = viewModel::onFatInput)
            NutritionField(label = "碳水", unit = "g", value = uiState.carbs, placeholder = "如 250", onValueChange = viewModel::onCarbsInput)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

            // 微量营养素标题
            Text(
                text = "微量营养素目标",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 微量营养素行
            uiState.micronutrients.forEachIndexed { index, micro ->
                MicroTargetRow(
                    micro = micro,
                    onValueChange = { viewModel.onMicroTargetInput(index, it) },
                    onDelete = { viewModel.deleteMicronutrient(index) }
                )
            }

            // 添加营养素
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { viewModel.openNutrientPicker() }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("+ 添加营养素", fontSize = 14.sp, color = Primary, fontWeight = FontWeight.Medium)
            }

            // 保存按钮
            Button(
                onClick = { viewModel.saveTargets() },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("保存档案与目标", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ==================== 功能卡片 ====================

@Composable
private fun FeaturesCard(
    onTemplates: () -> Unit,
    onBodyStats: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle("记录工具")

            DataRow(
                title = "食物模板",
                desc = "管理常用食物营养参数，录入时自动补全",
                onClick = onTemplates
            )
            DataRow(
                title = "身体记录",
                desc = "记录体重、体脂等趋势",
                onClick = onBodyStats
            )
        }
    }
}

// ==================== 数据管理卡片 ====================

@Composable
private fun DataManagementCard(viewModel: SettingsViewModel) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle("数据管理")

            DataRow(
                title = "导出数据",
                desc = "复制全部数据到剪贴板",
                onClick = { viewModel.exportData(context) }
            )
            DataRow(
                title = "导入数据",
                desc = "粘贴备份数据恢复记录",
                onClick = { viewModel.showImport() }
            )
            DataRow(
                title = "清空记录",
                desc = "清空全部饮食记录（保留目标）",
                isDanger = true,
                onClick = { viewModel.requestClear() }
            )
        }
    }
}

// ==================== 关于卡片 ====================

@Composable
private fun AboutCard(viewModel: SettingsViewModel, uiState: SettingsViewModel.UiState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateManager = remember { com.example.nutrition.updater.UpdateManager.getInstance(context) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 完整 Brand Logo
            Image(
                painter = painterResource(R.drawable.ic_brand_logo_full),
                contentDescription = "WeDo Full Logo",
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(20.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "WeDo",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "本地优先 · 极简纯净 · 营养与身体管理",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "版本 v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                fontSize = 12.sp,
                color = com.example.nutrition.ui.theme.TextPlaceholder
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(thickness = 0.5.dp, color = com.example.nutrition.ui.theme.Border)
            Spacer(modifier = Modifier.height(12.dp))

            // 版本更新状态区
            if (updateManager.updateAvailable && updateManager.latestInfo != null) {
                val latest = updateManager.latestInfo!!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Primary.copy(alpha = 0.1f))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                            )
                            Text(
                                text = "发现新版本 v${latest.versionName}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                        if (latest.changelog.isNotBlank()) {
                            Text(
                                text = latest.changelog,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 4.dp),
                                maxLines = 4
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val url = latest.downloadUrl.ifBlank {
                                    latest.releaseUrl.ifBlank { "https://github.com/Joshmax010/wedo-fitness/releases" }
                                }
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                } catch (_: ActivityNotFoundException) {
                                    Toast.makeText(context, "未找到可用浏览器", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("立即前往下载更新", fontSize = 13.sp, color = com.example.nutrition.ui.theme.TextInverse)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                val statusDesc = when {
                    updateManager.isChecking -> "正在检测更新..."
                    updateManager.lastCheckResult is com.example.nutrition.updater.UpdateResult.UpToDate -> "已是最新版本"
                    updateManager.lastCheckResult is com.example.nutrition.updater.UpdateResult.Error -> "未能获取到更新"
                    else -> "点击按钮检查新版本"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("版本状态", fontSize = 14.sp, color = TextSecondary)
                        Text(
                            text = statusDesc,
                            fontSize = 12.sp,
                            color = com.example.nutrition.ui.theme.TextPlaceholder
                        )
                    }
                    if (updateManager.isChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Primary)
                    } else {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    when (val res = updateManager.checkUpdate(force = true)) {
                                        is com.example.nutrition.updater.UpdateResult.NewVersionAvailable -> {
                                            Toast.makeText(context, "发现新版本 v${res.info.versionName}", Toast.LENGTH_SHORT).show()
                                        }
                                        is com.example.nutrition.updater.UpdateResult.UpToDate -> {
                                            Toast.makeText(context, "当前已是最新版本", Toast.LENGTH_SHORT).show()
                                        }
                                        is com.example.nutrition.updater.UpdateResult.Error -> {
                                            Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                        }
                                        is com.example.nutrition.updater.UpdateResult.Disabled -> {}
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("检查更新", fontSize = 12.sp, color = Primary)
                        }
                    }
                }
            }

            // 自动检测版本更新开关
            SwitchRow(
                title = "自动检测版本更新 (联网)",
                subtitle = "启动时静默检查 GitHub 发行版更新，离线时自动跳过",
                checked = updateManager.autoCheckEnabled,
                onCheckedChange = { updateManager.setAutoCheck(it) }
            )

            HorizontalDivider(thickness = 0.5.dp, color = com.example.nutrition.ui.theme.Border, modifier = Modifier.padding(vertical = 6.dp))

            // 存储占用
            AboutRow(
                label = "本地存储占用",
                value = "${uiState.storageInfo?.currentKB ?: "0"} KB (本地 SQLite 数据库)"
            )

            // 隐私保证
            AboutRow(
                label = "数据存储与隐私",
                value = "100% 本地保存，无第三方 SDK"
            )

            // 开源社区
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Joshmax010/wedo-fitness")))
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(context, "未找到可用浏览器", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("开源主页 (MIT License)", fontSize = 14.sp, color = TextSecondary)
                Text("GitHub ›", fontSize = 14.sp, color = Primary, fontWeight = FontWeight.SemiBold)
            }

            // 免责声明
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Text(
                    text = "免责声明",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "本工具仅提供数据记录与汇总功能，用于对比目标进度，不构成任何专业医疗或健身指导建议。",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 备份提示
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(com.example.nutrition.ui.theme.Warning.copy(alpha = 0.08f))
                    .padding(12.dp)
            ) {
                Text(
                    text = "建议定期使用「数据管理」功能导出备份数据，卸载应用或清除应用数据可能导致本地记录丢失。",
                    fontSize = 12.sp,
                    color = com.example.nutrition.ui.theme.Warning
                )
            }
        }
    }
}

// ==================== 导入弹窗 ====================

@Composable
private fun ImportModal(viewModel: SettingsViewModel, uiState: SettingsViewModel.UiState) {
    AlertDialog(
        onDismissRequest = { viewModel.hideImport() },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("导入数据", fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    text = "×",
                    fontSize = 22.sp,
                    color = TextSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { viewModel.hideImport() }
                        .padding(4.dp)
                )
            }
        },
        text = {
            Column {
                if (!uiState.showPreview) {
                    // 文本输入
                    OutlinedTextField(
                        value = uiState.importText,
                        onValueChange = { viewModel.onImportTextInput(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        placeholder = {
                            Text("在此粘贴导出的 JSON 数据", color = TextPlaceholder)
                        },
                        textStyle = TextStyle(fontSize = 13.sp),
                        colors = nutritionFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // 错误提示
                    if (uiState.importError.isNotEmpty()) {
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("⚠", fontSize = 14.sp, color = Error)
                            Text(
                                uiState.importError,
                                fontSize = 13.sp,
                                color = Error
                            )
                        }
                    }
                } else {
                    // 预览
                    uiState.importPreview?.let { preview ->
                        ImportPreviewContent(preview = preview)
                    }
                }
            }
        },
        confirmButton = {
            if (!uiState.showPreview) {
                Button(
                    onClick = { viewModel.doPreview() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("预览", fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = { viewModel.confirmImport() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("确认导入", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            if (!uiState.showPreview) {
                OutlinedButton(
                    onClick = { viewModel.hideImport() },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("取消", color = TextSecondary)
                }
            } else {
                OutlinedButton(
                    onClick = { viewModel.backToEdit() },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("返回", color = TextSecondary)
                }
            }
        }
    )
}

// ==================== 导入预览内容 ====================

@Composable
private fun ImportPreviewContent(preview: BackupManager.BackupPreview) {
    Column {
        Text(
            text = "数据概览",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 天数 + 记录数
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PreviewStat(preview.dayCount.toString(), "天数")
            PreviewStat(preview.recordCount.toString(), "记录数")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (preview.earliestDate.isNotEmpty()) {
            PreviewDetail("日期范围", "${preview.earliestDate} ~ ${preview.latestDate}")
        }
        if (preview.hasTargets) {
            preview.targetCalories?.let { PreviewDetail("热量目标", "$it kcal") }
            preview.targetProtein?.let { PreviewDetail("蛋白质目标", "$it g") }
        }
        if (preview.templateCount > 0) {
            PreviewDetail("自定义模板", "${preview.templateCount} 个")
        }
        if (preview.bodyRecordCount > 0) {
            PreviewDetail("身体记录", "${preview.bodyRecordCount} 条")
        }
        PreviewDetail("数据版本", "v${preview.schemaVersion}")
        if (preview.exportedAt.isNotEmpty()) {
            PreviewDetail("导出时间", preview.exportedAt)
        }

        // 警告
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Error.copy(alpha = 0.08f))
                .padding(10.dp)
                .padding(top = 12.dp)
        ) {
            Text(
                text = "确认导入将覆盖当前数据，此操作不可撤销",
                fontSize = 12.sp,
                color = Error
            )
        }
    }
}

// ==================== 通用组件 ====================

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}


@Composable
private fun MicroTargetRow(
    micro: SettingsViewModel.MicroTargetRow,
    onValueChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(0.35f), verticalAlignment = Alignment.CenterVertically) {
            Text(micro.name, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(" ${micro.unit}", fontSize = 12.sp, color = TextPlaceholder)
        }
        Row(
            modifier = Modifier.weight(0.65f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                value = micro.target,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("目标值", fontSize = 13.sp, color = TextPlaceholder) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = TextStyle(fontSize = 14.sp),
                colors = nutritionFieldColors(),
                shape = RoundedCornerShape(8.dp)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onDelete)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("×", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Error)
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            subtitle?.let {
                Text(text = it, fontSize = 12.sp, color = TextPlaceholder, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Primary
            )
        )
    }
}

@Composable
private fun DataRow(
    title: String,
    desc: String,
    isDanger: Boolean = false,
    badgeDot: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDanger) Error else TextPrimary
                )
                if (badgeDot) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                }
            }
            Text(text = desc, fontSize = 12.sp, color = TextPlaceholder)
        }
        Text(
            text = "›",
            fontSize = 20.sp,
            color = TextPlaceholder
        )
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = TextSecondary)
        Text(value, fontSize = 14.sp, color = TextPrimary)
    }
}

@Composable
private fun PreviewStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Primary)
        Text(label, fontSize = 12.sp, color = TextPlaceholder)
    }
}

@Composable
private fun PreviewDetail(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Text(value, fontSize = 12.sp, color = TextPrimary)
    }
}

// ==================== 身体档案卡片 ====================

@Composable
private fun BodyProfileCard(viewModel: SettingsViewModel, uiState: SettingsViewModel.UiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle("身体档案")

            // 性别
            Text("性别", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
            Row(modifier = Modifier.fillMaxWidth()) {
                Gender.entries.forEach { g ->
                    Row(
                        modifier = Modifier
                            .clickable { viewModel.onGenderInput(g) }
                            .padding(end = 16.dp)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.gender == g,
                            onClick = { viewModel.onGenderInput(g) }
                        )
                        Text(
                            text = g.displayName,
                            fontSize = 14.sp,
                            color = TextPrimary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            // 年龄 / 身高 / 体重
            NutritionField(
                label = "年龄",
                unit = "岁",
                value = uiState.age,
                placeholder = "25",
                onValueChange = viewModel::onAgeInput
            )
            NutritionField(
                label = "身高",
                unit = "cm",
                value = uiState.height,
                placeholder = "170",
                onValueChange = viewModel::onHeightInput
            )
            NutritionField(
                label = "体重",
                unit = "kg",
                value = uiState.weight,
                placeholder = "65",
                onValueChange = viewModel::onWeightInput
            )

            // 活动系数
            Text(
                text = "活动量",
                fontSize = 14.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp)
            )
            ActivityLevel.entries.forEach { level ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onActivityLevelInput(level) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = uiState.activityLevel == level,
                        onClick = { viewModel.onActivityLevelInput(level) }
                    )
                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text(
                            text = level.displayName,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = level.description,
                            fontSize = 12.sp,
                            color = TextPlaceholder
                        )
                    }
                }
            }

            // 计算按钮
            Button(
                onClick = { viewModel.calculateRecommendations() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("自动计算推荐值", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ==================== 推荐值预览弹窗 ====================

@Composable
private fun CalculationDialog(viewModel: SettingsViewModel, uiState: SettingsViewModel.UiState) {
    val result = uiState.calcResult ?: return
    val bmr = result.bmr
    val tdee = result.tdee
    val targets = result.targets

    AlertDialog(
        onDismissRequest = { viewModel.dismissCalcDialog() },
        title = { Text("推荐目标", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CalcStatItem("BMR", "${bmr.toInt()} kcal")
                    CalcStatItem("TDEE", "${tdee.toInt()} kcal")
                }
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))
                PreviewDetail("热量", "${targets.calories.toInt()} kcal")
                PreviewDetail("蛋白质", "${targets.protein} g")
                PreviewDetail("脂肪", "${targets.fat} g")
                PreviewDetail("碳水", "${targets.carbs} g")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "注：推荐值基于 Mifflin-St Jeor 公式估算，仅供参考。",
                    fontSize = 12.sp,
                    color = TextPlaceholder
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.applyCalculatedTargets() },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("应用到表单", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { viewModel.dismissCalcDialog() },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun CalcStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Primary)
        Text(label, fontSize = 12.sp, color = TextPlaceholder)
    }
}
