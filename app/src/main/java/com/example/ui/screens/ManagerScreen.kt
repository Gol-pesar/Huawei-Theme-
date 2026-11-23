package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ThemeItem
import com.example.data.ThemeSourceType
import com.example.ui.components.ThemeArtworkImage
import com.example.ui.components.formatFileSizeFa
import com.example.ui.theme.EmeraldInstalled
import com.example.viewmodel.ThemeManagerUiState

@Composable
fun ManagerScreen(
    uiState: ThemeManagerUiState,
    onConnectFolder: () -> Unit,
    onScanFolder: () -> Unit,
    onImportHwt: () -> Unit,
    onOpenHuaweiThemes: () -> Unit,
    onSelectTheme: (ThemeItem) -> Unit,
    onInstallTheme: (ThemeItem) -> Unit,
    onInspectTheme: (ThemeItem) -> Unit,
    onShareTheme: (ThemeItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = !uiState.preferences.huaweiFolderUri.isNullOrBlank()
    val installedCount = uiState.themes.count { it.isInstalledToHuawei }
    val userPackageCount = uiState.themes.count { it.parsedSourceType != ThemeSourceType.BUILTIN }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "manager_header") {
            Column {
                Text(
                    text = "مدیریت نصب و پوشه هواوی",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "اتصال مستقیم به پوشه Huawei/Themes، اسکن بسته‌های HWT و اجرای برنامه تم هواوی",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Huawei Folder SAF Connection Hub
        item(key = "folder_hub") {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                if (isConnected) EmeraldInstalled.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                MaterialTheme.colorScheme.outlineVariant
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isConnected) EmeraldInstalled.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.primaryContainer
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isConnected) Icons.Rounded.FolderOpen else Icons.Rounded.CreateNewFolder,
                                contentDescription = null,
                                tint = if (isConnected) EmeraldInstalled else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isConnected) "پوشه تم‌های هواوی متصل است" else "پوشه Huawei/Themes متصل نیست",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isConnected) {
                                    "مسیر: ${uiState.preferences.huaweiFolderDisplayName ?: "Huawei/Themes"}"
                                } else {
                                    "با یک بار انتخاب پوشه Huawei/Themes، تمام فایل‌های HWT به‌صورت مستقیم نصب می‌شوند"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onConnectFolder,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("manager_connect_folder_button")
                        ) {
                            Text(
                                text = if (isConnected) stringResource(R.string.action_change_folder)
                                else stringResource(R.string.action_connect_folder),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        if (isConnected) {
                            FilledTonalButton(
                                onClick = onScanFolder,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("manager_scan_folder_button")
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("اسکن", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        // Quick Actions Row: Launch Huawei Themes + Import Local HWT
        item(key = "quick_actions") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onOpenHuaweiThemes,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("manager_launch_huawei_themes_button")
                ) {
                    Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("برنامه Themes هواوی", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onImportHwt,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("manager_import_hwt_button")
                ) {
                    Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("وارد کردن HWT", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Metrics Strip
        item(key = "metrics") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ManagerMetricBox("کل تم‌ها", uiState.themes.size.toString(), Modifier.weight(1f))
                ManagerMetricBox("نصب در هواوی", installedCount.toString(), Modifier.weight(1f))
                ManagerMetricBox("ساخته / واردشده", userPackageCount.toString(), Modifier.weight(1f))
            }
        }

        item(key = "list_heading") {
            Text(
                text = "بسته‌های آماده نصب و مدیریت",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(uiState.themes, key = { it.id }) { theme ->
            ManagedThemeRowCard(
                theme = theme,
                onClick = { onSelectTheme(theme) },
                onInstall = { onInstallTheme(theme) },
                onInspect = { onInspectTheme(theme) },
                onShare = { onShareTheme(theme) }
            )
        }
    }
}

@Composable
private fun ManagerMetricBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ManagedThemeRowCard(
    theme: ThemeItem,
    onClick: () -> Unit,
    onInstall: () -> Unit,
    onInspect: () -> Unit,
    onShare: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("manager_theme_row_${theme.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ThemeArtworkImage(
                theme = theme,
                contentDescription = theme.title,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = theme.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (theme.isInstalledToHuawei) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "نصب‌شده",
                            tint = EmeraldInstalled,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = theme.fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${theme.emuiTarget} • ${formatFileSizeFa(theme.fileSizeBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onInstall,
                    modifier = Modifier.testTag("manager_install_${theme.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.InstallMobile,
                        contentDescription = stringResource(R.string.action_install_to_huawei),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onInspect,
                    modifier = Modifier.testTag("manager_inspect_${theme.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderZip,
                        contentDescription = stringResource(R.string.action_inspect_package),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.testTag("manager_share_${theme.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = stringResource(R.string.action_share_hwt),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
