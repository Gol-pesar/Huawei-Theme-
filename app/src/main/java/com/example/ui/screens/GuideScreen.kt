package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.engine.DeviceDiagnostics
import com.example.ui.theme.EmeraldInstalled

private data class GuideStep(
    val number: String,
    val title: String,
    val body: String
)

private val HuaweiApplySteps = listOf(
    GuideStep(
        number = "۱",
        title = "اتصال پوشه تم‌های هواوی",
        body = "در تب «مدیریت نصب»، گزینه «اتصال پوشه Huawei/Themes» را بزنید و پوشه Huawei سپس Themes (در گوشی‌های آنر: Honor/Themes) را انتخاب کنید."
    ),
    GuideStep(
        number = "۲",
        title = "انتخاب، وارد کردن یا ساخت فایل HWT",
        body = "از گالری تم‌های آماده، بخش تم‌ساز یا دکمه «وارد کردن فایل HWT» بسته مورد نظر خود را انتخاب و روی «نصب مستقیم در پوشه Huawei/Themes» بزنید."
    ),
    GuideStep(
        number = "۳",
        title = "اعمال نهایی در برنامه رسمی Themes هواوی",
        body = "دکمه «باز کردن برنامه تم هواوی» را بزنید، وارد تب «من (Me)» شوید، بخش «تم‌ها (Themes)» را باز کنید، تم جدید را انتخاب کرده و گزینه Apply (اعمال) را لمس کنید."
    ),
    GuideStep(
        number = "۴",
        title = "رفع مشکل عدم نمایش تم در EMUI 12 / 13 / 14",
        body = "اگر تم جدید بلافاصله دیده نشد، برنامه رسمی Huawei Themes را از لیست برنامه‌های اخیر ببندید و دوباره باز کنید تا پوشه Themes بازخوانی شود."
    )
)

@Composable
fun GuideScreen(
    diagnostics: DeviceDiagnostics?,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onOpenHuaweiThemes: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "guide_header") {
            Column {
                Text(
                    text = "راهنمای اعمال و تنظیمات",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "تشخیص وضعیت دستگاه و آموزش گام‌به‌گام نصب تم در EMUI و HarmonyOS",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Device Diagnostics Card
        item(key = "diagnostics_card") {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhoneAndroid,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column {
                            Text(
                                text = "وضعیت دستگاه شما",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (diagnostics != null) {
                                    "${diagnostics.manufacturer} ${diagnostics.model} • Android ${diagnostics.androidVersion}"
                                } else {
                                    "در حال بررسی دستگاه..."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    DiagnosticRow(
                        label = "برنامه Huawei Themes",
                        value = if (diagnostics?.huaweiThemeAppInstalled == true) "نصب و آماده اعمال" else "یافت نشد",
                        positive = diagnostics?.huaweiThemeAppInstalled == true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DiagnosticRow(
                        label = "برنامه Honor Themes",
                        value = if (diagnostics?.honorThemeAppInstalled == true) "نصب و آماده اعمال" else "یافت نشد",
                        positive = diagnostics?.honorThemeAppInstalled == true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DiagnosticRow(
                        label = "مسیر پیشنهادی نصب",
                        value = diagnostics?.recommendedPath ?: "Huawei/Themes",
                        positive = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    FilledTonalButton(
                        onClick = onOpenHuaweiThemes,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("guide_open_huawei_themes_button")
                    ) {
                        Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اجرای مستقیم برنامه تم هواوی / آنر")
                    }
                }
            }
        }

        // Theme Mode Toggle Card
        item(key = "appearance_card") {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Column {
                            Text(
                                text = "حالت تاریک اوبسیدین",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isDarkTheme) "نمای تیره پرچمدار فعال است" else "نمای روشن پرسلن فعال است",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = onToggleDarkTheme,
                        modifier = Modifier.testTag("settings_dark_mode_switch")
                    )
                }
            }
        }

        // Step-by-step EMUI / HarmonyOS Installation Guide
        item(key = "guide_steps_title") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.TipsAndUpdates,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "مراحل نصب و اعمال تم در گوشی‌های هواوی",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        items(HuaweiApplySteps.size) { index ->
            val step = HuaweiApplySteps[index]
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = step.number,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = step.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String,
    positive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = if (positive) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                contentDescription = null,
                tint = if (positive) EmeraldInstalled else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
