package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IconShapeStyle
import com.example.data.LockClockStyle
import com.example.data.ThemeItem

enum class PreviewMode(val labelFa: String) {
    LOCK("صفحه قفل"),
    HOME("صفحه اصلی"),
    CONTROL("کنترل سنتر")
}

@Composable
fun EmuiPhonePreview(
    theme: ThemeItem? = null,
    customWallpaperUri: Uri? = null,
    presetDrawableName: String = "img_theme_persian_dawn",
    accentHex: String = theme?.accentColorHex ?: "#E63946",
    iconShape: IconShapeStyle = theme?.parsedIconShape ?: IconShapeStyle.SQUIRCLE,
    clockStyle: LockClockStyle = theme?.parsedClockStyle ?: LockClockStyle.HARMONY_CENTER,
    themeTitle: String = theme?.title ?: "EMUI Studio",
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(PreviewMode.LOCK) }
    val accent = parseHexColor(accentHex, MaterialTheme.colorScheme.primary)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mode switcher pills
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PreviewMode.entries.forEach { item ->
                val selected = mode == item
                Surface(
                    color = if (selected) accent else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { mode = item }
                        .testTag("preview_mode_${item.name.lowercase()}")
                ) {
                    Text(
                        text = item.labelFa,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Flagship Huawei Phone Frame
        Box(
            modifier = Modifier
                .width(216.dp)
                .aspectRatio(9f / 17.5f)
                .clip(RoundedCornerShape(34.dp))
                .border(
                    width = 3.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.35f),
                            accent.copy(alpha = 0.55f),
                            Color.White.copy(alpha = 0.12f)
                        )
                    ),
                    shape = RoundedCornerShape(34.dp)
                )
                .background(Color.Black)
                .semantics { contentDescription = "پیش‌نمایش زنده رابط کاربری هواوی برای تم $themeTitle" }
        ) {
            // Wallpaper background
            if (theme != null) {
                ThemeArtworkImage(
                    theme = theme,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                StudioArtworkImage(
                    pickedUri = customWallpaperUri,
                    presetDrawableName = presetDrawableName,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Subtle readability scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.12f),
                                Color.Black.copy(alpha = 0.55f)
                            )
                        )
                    )
            )

            // Status bar + Huawei pill cutout (LTR for authentic phone status icons)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "09:41",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .width(38.dp)
                            .height(11.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.85f))
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SignalCellularAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Icon(
                            imageVector = Icons.Rounded.Wifi,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            // Inner interactive mode content
            AnimatedContent(
                targetState = mode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "emui_preview_mode",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 30.dp, bottom = 14.dp, start = 14.dp, end = 14.dp)
            ) { currentMode ->
                when (currentMode) {
                    PreviewMode.LOCK -> LockScreenOverlay(
                        accent = accent,
                        clockStyle = clockStyle,
                        themeTitle = themeTitle
                    )
                    PreviewMode.HOME -> HomeScreenOverlay(
                        accent = accent,
                        iconShape = iconShape,
                        themeTitle = themeTitle
                    )
                    PreviewMode.CONTROL -> ControlCenterOverlay(
                        accent = accent
                    )
                }
            }

            // Bottom gesture pill
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .width(68.dp)
                    .height(3.5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.78f))
            )
        }
    }
}

@Composable
private fun LockScreenOverlay(
    accent: Color,
    clockStyle: LockClockStyle,
    themeTitle: String
) {
    val alignment = if (clockStyle == LockClockStyle.EMUI_LEFT) Alignment.End else Alignment.CenterHorizontally
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = alignment
    ) {
        Spacer(modifier = Modifier.height(18.dp))
        Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (clockStyle == LockClockStyle.MAGAZINE_BOLD) "۰۹\n۴۱" else "۰۹:۴۱",
            color = Color.White,
            fontSize = if (clockStyle == LockClockStyle.MAGAZINE_BOLD) 44.sp else 38.sp,
            lineHeight = 42.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "دوشنبه ۲ آذر • $themeTitle",
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        // Huawei Magazine / Fingerprint indicator
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.28f))
                .border(1.5.dp, accent.copy(alpha = 0.9f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Fingerprint,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun HomeScreenOverlay(
    accent: Color,
    iconShape: IconShapeStyle,
    themeTitle: String
) {
    val shape = shapeForIconStyle(iconShape)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // HarmonyOS Smart Service Card Widget
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Black.copy(alpha = 0.42f))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(18.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تهران ۱۸°",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = themeTitle,
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 10.sp,
                maxLines = 1
            )
        }

        // Icon Grid
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MiniEmuiIcon(Icons.Rounded.CameraAlt, accent, shape)
                MiniEmuiIcon(Icons.Rounded.Image, Color(0xFF3E92CC), shape)
                MiniEmuiIcon(Icons.Rounded.MusicNote, Color(0xFFE5B87A), shape)
                MiniEmuiIcon(Icons.Rounded.Settings, Color(0xFF6B7280), shape)
            }
            // Dock bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MiniEmuiIcon(Icons.Rounded.Call, Color(0xFF26C281), shape, compact = true)
                MiniEmuiIcon(Icons.Rounded.Message, Color(0xFF3B82F6), shape, compact = true)
                MiniEmuiIcon(Icons.Rounded.NotificationsActive, accent, shape, compact = true)
            }
        }
    }
}

@Composable
private fun ControlCenterOverlay(accent: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Black.copy(alpha = 0.56f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Control Panel",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        // Top 2 large HarmonyOS pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ControlPill(
                icon = Icons.Rounded.Wifi,
                label = "Wi-Fi",
                activeColor = accent,
                modifier = Modifier.weight(1f)
            )
            ControlPill(
                icon = Icons.Rounded.Bluetooth,
                label = "BT",
                activeColor = accent,
                modifier = Modifier.weight(1f)
            )
        }
        // Brightness slider bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.16f)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.88f))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.Rounded.BrightnessHigh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        // Quick toggle row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ControlCircle(Icons.Rounded.FlashlightOn, accent, true)
            ControlCircle(Icons.Rounded.NotificationsActive, accent, true)
            ControlCircle(Icons.Rounded.Settings, Color.White.copy(alpha = 0.2f), false)
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Super Device • آماده اتصال",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 9.5.sp
            )
        }
    }
}

@Composable
private fun ControlPill(
    icon: ImageVector,
    label: String,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(activeColor)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ControlCircle(icon: ImageVector, color: Color, active: Boolean) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (active) color else color),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun MiniEmuiIcon(
    icon: ImageVector,
    tintBg: Color,
    shape: Shape,
    compact: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(if (compact) 32.dp else 36.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(tintBg, tintBg.copy(alpha = 0.72f))
                )
            )
            .border(0.75.dp, Color.White.copy(alpha = 0.25f), shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(if (compact) 16.dp else 18.dp)
        )
    }
}

fun shapeForIconStyle(style: IconShapeStyle): Shape {
    return when (style) {
        IconShapeStyle.SQUIRCLE -> RoundedCornerShape(11.dp)
        IconShapeStyle.CIRCLE -> CircleShape
        IconShapeStyle.TEARDROP -> RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
        IconShapeStyle.ROUNDED_SQUARE -> RoundedCornerShape(7.dp)
    }
}
