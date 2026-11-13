package com.example.nutrition.ui.theme

import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF3D7A45),          // 更清透的森林绿（原 #507755 偏灰偏暗）
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4EDDA), // 更鲜活的浅绿容器
    onPrimaryContainer = Color(0xFF1B4721),
    secondary = Color(0xFF5C6BC0),        // 靛蓝（原 #6575AB 偏灰）
    tertiary = Color(0xFFE6A817),         // 警告琥珀提亮（原 #946D2F 太暗）
    error = Color(0xFFD14D41),            // 更标准的红（原 #B36155 偏土）
    onError = Color.White,
    background = Color(0xFFF5F6F8),       // ← 关键：背景改为浅灰
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,                // ← 关键：卡片改为纯白（浮起）
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFEDEEF0),   // 输入框填充
    onSurfaceVariant = Color(0xFF5F6368),
    outline = Color(0xFFB0B3B8),
    outlineVariant = Color(0xFFE0E2E6)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FD69A),          // 更鲜活的绿（原 #9FBEA0 太灰太淡）
    onPrimary = Color(0xFF0F2818),
    primaryContainer = Color(0xFF294D30),
    onPrimaryContainer = Color(0xFFCAEED1),
    secondary = Color(0xFFB0BEF0),
    tertiary = Color(0xFFF0C05A),         // 警告金（原 #D0AB73 偏灰）
    error = Color(0xFFEF8A80),            // 柔和错误红（原 #DE9789 偏桃）
    onError = Color(0xFF1A0A08),
    background = Color(0xFF111113),       // 底色微偏冷灰
    onBackground = Color(0xFFE5E6EA),
    surface = Color(0xFF1E1F23),          // ← 卡片提亮到可辨识程度
    onSurface = Color(0xFFE5E6EA),
    surfaceVariant = Color(0xFF2A2B30),   // 输入框填充
    onSurfaceVariant = Color(0xFFB5B7BD),
    outline = Color(0xFF6E7078),
    outlineVariant = Color(0xFF35363B)
)

@Composable
fun NutritionTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val appearance = remember(context.applicationContext) { AppearancePreferences(context) }
    val dark = when (appearance.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkColors else LightColors
    DisposableEffect(context, dark) {
        (context as? ComponentActivity)?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(
                AndroidColor.parseColor("#F5F6F8"),
                AndroidColor.parseColor("#111113")
            ) { dark }
        )
        onDispose { }
    }
    CompositionLocalProvider(LocalAppearance provides appearance) {
        MaterialTheme(
            colorScheme = colors,
            typography = AppTypography,
            shapes = Shapes(medium = RoundedCornerShape(14.dp), large = RoundedCornerShape(20.dp))
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.onBackground, content = content)
        }
    }
}

/** Filled, quiet fields; a focused field keeps a clear accent outline. */
@Composable
fun nutritionFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
    focusedContainerColor = BgTag, unfocusedContainerColor = BgTag,
    focusedBorderColor = Primary, unfocusedBorderColor = Color.Transparent,
    focusedLabelColor = Primary, unfocusedLabelColor = TextSecondary,
    focusedPlaceholderColor = TextPlaceholder, unfocusedPlaceholderColor = TextPlaceholder,
    cursorColor = Primary
)
