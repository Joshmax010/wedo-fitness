package com.example.nutrition.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrition.ui.theme.BgCard
import com.example.nutrition.ui.theme.Primary
import com.example.nutrition.ui.theme.TextPrimary

/**
 * 通用日期导航栏 —— 左右箭头 + 中间日期文字（可选"今天"标签）
 *
 * 在首页、录入页复用
 */
@Composable
fun DateNavigationBar(
    dateStr: String,
    isToday: Boolean = false,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDateClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 前一天
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(BgCard)
                .clickable(onClick = onPrev),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "‹", fontSize = 22.sp, color = TextPrimary)
        }

        // 日期 + 今天标签
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onDateClick)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = dateStr,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            if (isToday) {
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Primary.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = "今天", fontSize = 10.sp, color = Primary)
                }
            }
        }

        // 后一天
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(BgCard)
                .clickable(onClick = onNext),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "›", fontSize = 22.sp, color = TextPrimary)
        }
    }
}
