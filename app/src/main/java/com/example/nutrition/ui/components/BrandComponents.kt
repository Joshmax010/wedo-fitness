package com.example.nutrition.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrition.BuildConfig
import com.example.nutrition.R
import com.example.nutrition.ui.theme.BgCard
import com.example.nutrition.ui.theme.BgMain
import com.example.nutrition.ui.theme.Border
import com.example.nutrition.ui.theme.Primary
import com.example.nutrition.ui.theme.TextInverse
import com.example.nutrition.ui.theme.TextPlaceholder
import com.example.nutrition.ui.theme.TextPrimary
import com.example.nutrition.ui.theme.TextSecondary
import com.example.nutrition.updater.UpdateManager
import com.example.nutrition.updater.UpdateResult
import kotlinx.coroutines.launch

/**
 * WeDo 品牌标识徽章 —— 顶栏专属
 *
 * 包含圆形沙拉 Emblem 徽标 + WeDo 文字，以及可弹跳缩放的弹簧反馈动画。
 * 若有新版本可用，右上角静默展示绿色圆点徽标。
 */
@Composable
fun WeDoBrandBadge(
    updateAvailable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bounceScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .scale(bounceScale.value)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = {
                    scope.launch {
                        bounceScale.animateTo(0.86f, animationSpec = tween(60))
                        bounceScale.animateTo(
                            1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                    onClick()
                }
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .semantics { contentDescription = "打开 WeDo 品牌与版本详情" },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Image(
                    painter = painterResource(R.drawable.ic_brand_emblem),
                    contentDescription = null,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                )
                if (updateAvailable) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                            .border(1.dp, BgMain, CircleShape)
                    )
                }
            }
            Text(
                text = "WeDo",
                style = MaterialTheme.typography.headlineSmall,
                color = Primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
        }
    }
}

/**
 * WeDo 品牌弹窗 —— 点击 WeDoBrandBadge 展开
 *
 * 展示完整新版 Logo、品牌口号、当前版本、自动更新状态/检查入口、开源项目链接
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandBottomSheet(
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateManager = remember { UpdateManager.getInstance(context) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = BgMain,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 完整 Brand Logo
            Image(
                painter = painterResource(R.drawable.ic_brand_logo_full),
                contentDescription = "WeDo Brand Logo",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(24.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "WeDo",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "本地优先 · 极简纯净 · 营养与身体管理",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                fontSize = 12.sp,
                color = TextPlaceholder
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 更新状态卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (updateManager.updateAvailable && updateManager.latestInfo != null) {
                        val latest = updateManager.latestInfo!!
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
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }

                        if (latest.changelog.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = latest.changelog,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                maxLines = 5
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val targetUrl = latest.downloadUrl.ifBlank {
                                    latest.releaseUrl.ifBlank { "https://github.com/Joshmax010/wedo-fitness/releases" }
                                }
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)))
                                } catch (_: ActivityNotFoundException) {
                                    Toast.makeText(context, "未找到可用浏览器", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("立即前往下载更新", color = TextInverse, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        val statusText = when {
                            updateManager.isChecking -> "正在检测更新..."
                            updateManager.lastCheckResult is UpdateResult.UpToDate -> "已是最新版本"
                            updateManager.lastCheckResult is UpdateResult.Error -> "未能获取到更新"
                            else -> "版本检查"
                        }
                        val hintText = when {
                            updateManager.isChecking -> "正在连接 GitHub 服务节点..."
                            updateManager.lastCheckResult is UpdateResult.UpToDate -> "本地代码已同步至最新稳定发布"
                            updateManager.lastCheckResult is UpdateResult.Error -> "点击右侧按钮重试检测"
                            updateManager.autoCheckEnabled -> "已启用联网自动检查"
                            else -> "自动检查已关闭"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = statusText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = hintText,
                                    fontSize = 11.sp,
                                    color = TextPlaceholder
                                )
                            }

                            if (updateManager.isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Primary
                                )
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            when (val res = updateManager.checkUpdate(force = true)) {
                                                is UpdateResult.NewVersionAvailable -> {
                                                    Toast.makeText(context, "发现新版本 v${res.info.versionName}", Toast.LENGTH_SHORT).show()
                                                }
                                                is UpdateResult.UpToDate -> {
                                                    Toast.makeText(context, "当前已是最新版本", Toast.LENGTH_SHORT).show()
                                                }
                                                is UpdateResult.Error -> {
                                                    Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                                }
                                                is UpdateResult.Disabled -> {}
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
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // GitHub 社区卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Joshmax010/wedo-fitness")))
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(context, "未找到可用浏览器", Toast.LENGTH_SHORT).show()
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "开源主页与社区",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = "github.com/Joshmax010/wedo-fitness",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = "访问 →",
                        fontSize = 13.sp,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("关闭", color = TextSecondary, fontSize = 14.sp)
            }
        }
    }
}
