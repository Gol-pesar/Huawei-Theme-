package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ThemeSourceType {
    BUILTIN,
    IMPORTED,
    CREATED,
    SCANNED,
    ONLINE
}

enum class IconShapeStyle(val labelFa: String) {
    SQUIRCLE("مربعی نرم EMUI"),
    CIRCLE("دایره‌ای کلاسیک"),
    TEARDROP("قطره‌ای هارمونی"),
    ROUNDED_SQUARE("شیشه‌ای مدرن")
}

enum class LockClockStyle(val labelFa: String) {
    HARMONY_CENTER("ساعت میانی Harmony"),
    EMUI_LEFT("ساعت کلاسیک EMUI"),
    MAGAZINE_BOLD("ساعت مجله‌ای درشت")
}

@Entity(tableName = "themes")
data class ThemeItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val englishName: String,
    val author: String,
    val version: String,
    val emuiTarget: String,
    val fileName: String,
    val localFilePath: String? = null,
    val previewImagePath: String? = null,
    val drawableResName: String? = null,
    val accentColorHex: String = "#E63946",
    val iconShape: String = IconShapeStyle.SQUIRCLE.name,
    val clockStyle: String = LockClockStyle.HARMONY_CENTER.name,
    val fileSizeBytes: Long = 0L,
    val sourceType: String = ThemeSourceType.BUILTIN.name,
    val isInstalledToHuawei: Boolean = false,
    val isFavorite: Boolean = false,
    val isApplied: Boolean = false,
    val modulesCsv: String = "صفحه قفل,تصویر زمینه,آیکون‌ها,کنترل سنتر",
    val createdAt: Long = System.currentTimeMillis()
) {
    val modulesList: List<String>
        get() = modulesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    val parsedSourceType: ThemeSourceType
        get() = runCatching { ThemeSourceType.valueOf(sourceType) }.getOrDefault(ThemeSourceType.IMPORTED)

    val parsedIconShape: IconShapeStyle
        get() = runCatching { IconShapeStyle.valueOf(iconShape) }.getOrDefault(IconShapeStyle.SQUIRCLE)

    val parsedClockStyle: LockClockStyle
        get() = runCatching { LockClockStyle.valueOf(clockStyle) }.getOrDefault(LockClockStyle.HARMONY_CENTER)
}

data class HwtInspectionResult(
    val fileName: String,
    val totalSizeBytes: Long,
    val entryCount: Int,
    val entries: List<String>,
    val descriptionXmlRaw: String?,
    val parsedTitle: String?,
    val parsedAuthor: String?,
    val parsedVersion: String?,
    val parsedScreen: String?,
    val parsedFont: String?,
    val detectedModules: List<String>,
    val isValidHwtStructure: Boolean
)
