package com.example.engine

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class WallpaperTarget(val labelFa: String) {
    HOME("صفحه اصلی (Home)"),
    LOCK("صفحه قفل (Lock)"),
    BOTH("هر دو صفحه (اصلی و قفل)")
}

data class DeviceDiagnostics(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
    val isHuaweiOrHonor: Boolean,
    val huaweiThemeAppInstalled: Boolean,
    val honorThemeAppInstalled: Boolean,
    val recommendedPath: String
)

sealed interface LaunchThemeAppResult {
    data class Launched(val appName: String) : LaunchThemeAppResult
    data object NotInstalled : LaunchThemeAppResult
}

class HuaweiSystemBridge(private val context: Context) {

    companion object {
        const val HUAWEI_THEME_PKG = "com.huawei.android.thememanager"
        const val HONOR_THEME_PKG = "com.hihonor.android.thememanager"
    }

    fun getDeviceDiagnostics(): DeviceDiagnostics {
        val manufacturer = Build.MANUFACTURER ?: "Unknown"
        val model = Build.MODEL ?: "Android Device"
        val isHuawei = manufacturer.equals("HUAWEI", ignoreCase = true)
        val isHonor = manufacturer.equals("HONOR", ignoreCase = true)
        val huaweiInstalled = isPackageInstalled(HUAWEI_THEME_PKG)
        val honorInstalled = isPackageInstalled(HONOR_THEME_PKG)

        val recommendedPath = when {
            isHonor || honorInstalled -> "Honor/Themes (یا Huawei/Themes)"
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> "Huawei/Themes (و در EMUI 12+: پوشه Themes)"
            else -> "Huawei/Themes"
        }

        return DeviceDiagnostics(
            manufacturer = manufacturer,
            model = model,
            androidVersion = Build.VERSION.RELEASE ?: "-",
            sdkInt = Build.VERSION.SDK_INT,
            isHuaweiOrHonor = isHuawei || isHonor,
            huaweiThemeAppInstalled = huaweiInstalled,
            honorThemeAppInstalled = honorInstalled,
            recommendedPath = recommendedPath
        )
    }

    fun openHuaweiThemesApp(): LaunchThemeAppResult {
        val pm = context.packageManager
        val huaweiIntent = pm.getLaunchIntentForPackage(HUAWEI_THEME_PKG)
        if (huaweiIntent != null) {
            huaweiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(huaweiIntent)
            return LaunchThemeAppResult.Launched("Huawei Themes")
        }

        val honorIntent = pm.getLaunchIntentForPackage(HONOR_THEME_PKG)
        if (honorIntent != null) {
            honorIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(honorIntent)
            return LaunchThemeAppResult.Launched("Honor Themes")
        }

        return LaunchThemeAppResult.NotInstalled
    }

    fun shareHwtPackage(file: File, themeTitle: String) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "بسته تم هواوی: $themeTitle")
            putExtra(Intent.EXTRA_TEXT, "بسته تم هواوی ($themeTitle) ساخته و آماده‌شده برای کپی در پوشه Huawei/Themes")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "اشتراک‌گذاری بسته HWT").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    suspend fun applyWallpaper(bitmap: Bitmap, target: WallpaperTarget): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val manager = WallpaperManager.getInstance(context)
                val flags = when (target) {
                    WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                    WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
                    WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                }
                manager.setBitmap(bitmap, null, true, flags)
                Unit
            }
        }

    private fun isPackageInstalled(packageName: String): Boolean {
        return runCatching {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        }.getOrDefault(false)
    }
}
