package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.BuildCircle
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.IconShapeStyle
import com.example.data.LockClockStyle
import com.example.ui.components.EmuiPhonePreview
import com.example.ui.components.drawableResForName
import com.example.ui.components.parseHexColor
import com.example.ui.components.shapeForIconStyle
import com.example.viewmodel.StudioDraftState

private val StudioAccentSwatches = listOf(
    "#E63946" to "قرمز پیورا",
    "#24C6DC" to "فیروزه‌ای",
    "#E5B87A" to "طلایی شامپاین",
    "#26C281" to "زمردی",
    "#4F7CFF" to "آبی هارمونی",
    "#A855F7" to "ارغوانی میت"
)

private val StudioPresetArtworks = listOf(
    "img_theme_persian_dawn" to "طلوع پرشیا",
    "img_theme_obsidian_pulse" to "شب اوبسیدین",
    "img_theme_harmony_turquoise" to "فیروزه هارمونی",
    "img_theme_mate_nebula" to "کهکشان میت"
)

private val EmuiTargetOptions = listOf(
    "EMUI 14 / HarmonyOS",
    "EMUI 13",
    "EMUI 12",
    "EMUI 10 / 11"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioScreen(
    draft: StudioDraftState,
    onTitleChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
    onEmuiTargetChange: (String) -> Unit,
    onAccentChange: (String) -> Unit,
    onIconShapeChange: (IconShapeStyle) -> Unit,
    onClockStyleChange: (LockClockStyle) -> Unit,
    onPickWallpaperFromGallery: () -> Unit,
    onSelectPresetDrawable: (String) -> Unit,
    onBuildThemeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "studio_header") {
            Column {
                Text(
                    text = "استودیو تم‌ساز هواوی",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "تم اختصاصی خود را طراحی کنید و خروجی واقعی با فرمت استاندارد HWT بگیرید",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Interactive Live Preview Container
        item(key = "studio_preview") {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(26.dp))
            ) {
                EmuiPhonePreview(
                    customWallpaperUri = draft.pickedWallpaperUri,
                    presetDrawableName = draft.presetDrawableName,
                    accentHex = draft.accentHex,
                    iconShape = draft.iconShape,
                    clockStyle = draft.clockStyle,
                    themeTitle = draft.title.ifBlank { "تم اختصاصی" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp)
                )
            }
        }

        // Wallpaper Source Card (Zero-permission Photo Picker + Built-in Studio Presets)
        item(key = "studio_wallpaper") {
            StudioSectionCard(title = "تصویر زمینه اصلی و قفل صفحه") {
                FilledTonalButton(
                    onClick = onPickWallpaperFromGallery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("studio_pick_wallpaper_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (draft.pickedWallpaperUri != null) {
                            "تصویر از گالری انتخاب شد (تغییر تصویر)"
                        } else {
                            stringResource(R.string.action_pick_wallpaper)
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "یا انتخاب از پس‌زمینه‌های استودیویی:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(StudioPresetArtworks) { (drawableName, label) ->
                        val selected = draft.pickedWallpaperUri == null && draft.presetDrawableName == drawableName
                        val resId = drawableResForName(drawableName) ?: R.drawable.img_theme_persian_dawn
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(78.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelectPresetDrawable(drawableName) }
                                .testTag("studio_preset_$drawableName")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = label,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Metadata inputs
        item(key = "studio_meta") {
            StudioSectionCard(title = "مشخصات بسته HWT (description.xml)") {
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = onTitleChange,
                    label = { Text("نام تم") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("studio_title_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = draft.author,
                    onValueChange = onAuthorChange,
                    label = { Text("نام طراح") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("studio_author_input")
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "نسخه هدف EMUI / HarmonyOS:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EmuiTargetOptions.forEach { option ->
                        FilterChip(
                            selected = draft.emuiTarget == option,
                            onClick = { onEmuiTargetChange(option) },
                            label = { Text(option, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("studio_emui_${option.hashCode()}")
                        )
                    }
                }
            }
        }

        // Accent color, Icon shape, Clock style
        item(key = "studio_style") {
            StudioSectionCard(title = "پالت رنگی، فرم آیکون و ساعت قفل") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StudioAccentSwatches.forEach { (hex, label) ->
                        val color = parseHexColor(hex, MaterialTheme.colorScheme.primary)
                        val selected = draft.accentHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable { onAccentChange(hex) }
                                .testTag("studio_color_${hex.removePrefix("#")}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.25f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = label,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "فرم آیکون‌های صفحه اصلی:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                val accentColor = parseHexColor(draft.accentHex, MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconShapeStyle.entries.forEach { style ->
                        val selected = draft.iconShape == style
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainer
                                )
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onIconShapeChange(style) }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                                .testTag("studio_icon_shape_${style.name.lowercase()}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(shapeForIconStyle(style))
                                    .background(accentColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Palette,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = style.labelFa.substringBefore(" "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "چیدمان ساعت صفحه قفل:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LockClockStyle.entries.forEach { style ->
                        FilterChip(
                            selected = draft.clockStyle == style,
                            onClick = { onClockStyleChange(style) },
                            label = { Text(style.labelFa, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("studio_clock_${style.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // Build CTA
        item(key = "studio_build_cta") {
            Button(
                onClick = onBuildThemeClick,
                enabled = !draft.isBuilding,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("studio_build_hwt_button")
            ) {
                if (draft.isBuilding) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("در حال ساخت آرشیو HWT...")
                } else {
                    Icon(Icons.Rounded.BuildCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_create_theme),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
