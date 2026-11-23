package com.example.data

import android.net.Uri
import com.example.engine.HuaweiSystemBridge
import com.example.engine.HwtPackageEngine
import com.example.engine.LaunchThemeAppResult
import com.example.engine.OnlineHwtAsset
import com.example.engine.OnlineThemeRepositoryItem
import com.example.engine.OnlineThemeService
import com.example.engine.WallpaperTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File

class ThemeRepository(
    private val themeDao: ThemeDao,
    private val preferencesRepository: PreferencesRepository,
    val hwtEngine: HwtPackageEngine,
    val systemBridge: HuaweiSystemBridge,
    val onlineService: OnlineThemeService
) {
    val allThemes: Flow<List<ThemeItem>> = themeDao.observeAllThemes()
    val preferences: Flow<AppPreferences> = preferencesRepository.preferencesFlow

    suspend fun seedBuiltInThemesIfNeeded() {
        if (themeDao.countThemes() > 0) return

        val now = System.currentTimeMillis()
        val builtIns = listOf(
            ThemeItem(
                title = "طلوع پرشیا",
                englishName = "Persian_Dawn_EMUI14",
                author = "استودیو تم‌یار",
                version = "14.1.0",
                emuiTarget = "EMUI 14 / HarmonyOS",
                fileName = "Persian_Dawn_EMUI14.hwt",
                drawableResName = "img_theme_persian_dawn",
                accentColorHex = "#E63946",
                iconShape = IconShapeStyle.SQUIRCLE.name,
                clockStyle = LockClockStyle.HARMONY_CENTER.name,
                fileSizeBytes = 2_450_000L,
                sourceType = ThemeSourceType.BUILTIN.name,
                isInstalledToHuawei = false,
                isFavorite = true,
                isApplied = true,
                modulesCsv = "صفحه قفل,تصویر زمینه,آیکون‌ها,کنترل سنتر",
                createdAt = now
            ),
            ThemeItem(
                title = "شب اوبسیدین",
                englishName = "Obsidian_Pulse_EMUI13",
                author = "آتلیه پیورا",
                version = "13.2.4",
                emuiTarget = "EMUI 13 / HarmonyOS",
                fileName = "Obsidian_Pulse_EMUI13.hwt",
                drawableResName = "img_theme_obsidian_pulse",
                accentColorHex = "#FF4D6D",
                iconShape = IconShapeStyle.ROUNDED_SQUARE.name,
                clockStyle = LockClockStyle.MAGAZINE_BOLD.name,
                fileSizeBytes = 2_180_000L,
                sourceType = ThemeSourceType.BUILTIN.name,
                isInstalledToHuawei = false,
                isFavorite = false,
                isApplied = false,
                modulesCsv = "صفحه قفل,تصویر زمینه,آیکون‌های تیره,کنترل سنتر",
                createdAt = now - 10_000L
            ),
            ThemeItem(
                title = "فیروزه هارمونی",
                englishName = "Harmony_Turquoise_OS4",
                author = "هارمونی دیزاین",
                version = "4.2.0",
                emuiTarget = "HarmonyOS 4 / EMUI 14",
                fileName = "Harmony_Turquoise_OS4.hwt",
                drawableResName = "img_theme_harmony_turquoise",
                accentColorHex = "#24C6DC",
                iconShape = IconShapeStyle.TEARDROP.name,
                clockStyle = LockClockStyle.HARMONY_CENTER.name,
                fileSizeBytes = 2_310_000L,
                sourceType = ThemeSourceType.BUILTIN.name,
                isInstalledToHuawei = false,
                isFavorite = true,
                isApplied = false,
                modulesCsv = "صفحه قفل,تصویر زمینه,آیکون‌های شیشه‌ای,کنترل سنتر",
                createdAt = now - 20_000L
            ),
            ThemeItem(
                title = "کهکشان میت",
                englishName = "Mate_Nebula_Gold",
                author = "استودیو میت",
                version = "12.5.1",
                emuiTarget = "EMUI 12 - 14",
                fileName = "Mate_Nebula_Gold.hwt",
                drawableResName = "img_theme_mate_nebula",
                accentColorHex = "#E5B87A",
                iconShape = IconShapeStyle.CIRCLE.name,
                clockStyle = LockClockStyle.EMUI_LEFT.name,
                fileSizeBytes = 2_620_000L,
                sourceType = ThemeSourceType.BUILTIN.name,
                isInstalledToHuawei = false,
                isFavorite = false,
                isApplied = false,
                modulesCsv = "صفحه قفل,تصویر زمینه,آیکون‌ها,شماره‌گیر",
                createdAt = now - 30_000L
            )
        )
        themeDao.insertAll(builtIns)
    }

    suspend fun importThemeFromUri(uri: Uri): ThemeItem {
        val imported = hwtEngine.importHwtFromUri(uri, ThemeSourceType.IMPORTED)
        val existing = themeDao.getThemeByFileName(imported.fileName)
        return if (existing != null) {
            val updated = imported.copy(
                id = existing.id,
                isFavorite = existing.isFavorite,
                isApplied = existing.isApplied
            )
            themeDao.updateTheme(updated)
            updated
        } else {
            val id = themeDao.insertTheme(imported)
            imported.copy(id = id)
        }
    }

    suspend fun createCustomTheme(
        title: String,
        author: String,
        emuiTarget: String,
        accentHex: String,
        iconShape: IconShapeStyle,
        clockStyle: LockClockStyle,
        wallpaperUri: Uri?,
        fallbackDrawableName: String
    ): ThemeItem {
        val created = hwtEngine.createCustomThemePackage(
            title = title,
            author = author,
            emuiTarget = emuiTarget,
            accentHex = accentHex,
            iconShape = iconShape,
            clockStyle = clockStyle,
            wallpaperUri = wallpaperUri,
            fallbackDrawableName = fallbackDrawableName
        )
        val newId = themeDao.insertTheme(created)
        return created.copy(id = newId)
    }

    suspend fun installThemeToHuaweiFolder(theme: ThemeItem): Result<ThemeItem> = runCatching {
        val prefs = preferencesRepository.preferencesFlow.first()
        val folderUriStr = prefs.huaweiFolderUri
            ?: throw IllegalStateException("FOLDER_NOT_CONNECTED")
        val treeUri = Uri.parse(folderUriStr)
        val localFile = hwtEngine.ensureHwtFileExists(theme)
        hwtEngine.copyHwtToHuaweiTree(treeUri, localFile, theme.fileName)
        themeDao.updateInstallStatus(
            id = theme.id,
            installed = true,
            localFilePath = localFile.absolutePath,
            size = localFile.length()
        )
        theme.copy(
            isInstalledToHuawei = true,
            localFilePath = localFile.absolutePath,
            fileSizeBytes = localFile.length()
        )
    }

    suspend fun scanConnectedHuaweiFolder(): Result<Int> = runCatching {
        val prefs = preferencesRepository.preferencesFlow.first()
        val folderUriStr = prefs.huaweiFolderUri
            ?: throw IllegalStateException("FOLDER_NOT_CONNECTED")
        val treeUri = Uri.parse(folderUriStr)
        val foundFiles = hwtEngine.scanHuaweiTreeForThemeUris(treeUri)
        var importedCount = 0
        for ((name, docUri) in foundFiles) {
            val existing = themeDao.getThemeByFileName(name)
            if (existing == null) {
                val scanned = hwtEngine.importHwtFromUri(docUri, ThemeSourceType.SCANNED)
                themeDao.insertTheme(scanned)
                importedCount++
            } else if (!existing.isInstalledToHuawei) {
                themeDao.updateInstallStatus(existing.id, true, existing.localFilePath, existing.fileSizeBytes)
            }
        }
        importedCount
    }

    suspend fun exportThemeToUri(theme: ThemeItem, destUri: Uri): Result<Unit> = runCatching {
        val localFile = hwtEngine.ensureHwtFileExists(theme)
        hwtEngine.exportFileToUri(localFile, destUri)
        themeDao.updateInstallStatus(theme.id, theme.isInstalledToHuawei, localFile.absolutePath, localFile.length())
    }

    suspend fun inspectTheme(theme: ThemeItem): Result<HwtInspectionResult> = runCatching {
        val localFile = hwtEngine.ensureHwtFileExists(theme)
        themeDao.updateInstallStatus(theme.id, theme.isInstalledToHuawei, localFile.absolutePath, localFile.length())
        hwtEngine.inspectHwtFile(localFile, theme.fileName)
    }

    suspend fun shareTheme(theme: ThemeItem): Result<Unit> = runCatching {
        val localFile = hwtEngine.ensureHwtFileExists(theme)
        systemBridge.shareHwtPackage(localFile, theme.title)
    }

    suspend fun applyThemeWallpaper(theme: ThemeItem, target: WallpaperTarget): Result<Unit> {
        val bmp = hwtEngine.loadThemeBitmap(theme)
            ?: return Result.failure(IllegalStateException("تصویر زمینه برای این تم یافت نشد"))
        val res = systemBridge.applyWallpaper(bmp, target)
        if (res.isSuccess) {
            markThemeAsApplied(theme.id)
        }
        return res
    }

    suspend fun markThemeAsApplied(themeId: Long) {
        themeDao.clearAllApplied()
        themeDao.markThemeApplied(themeId)
    }

    suspend fun toggleFavorite(theme: ThemeItem) {
        themeDao.updateFavorite(theme.id, !theme.isFavorite)
    }

    suspend fun deleteTheme(theme: ThemeItem) {
        theme.localFilePath?.let { path ->
            runCatching { File(path).delete() }
        }
        theme.previewImagePath?.let { path ->
            runCatching { File(path).delete() }
        }
        themeDao.deleteThemeById(theme.id)
    }

    suspend fun saveHuaweiFolderUri(treeUri: Uri) {
        val displayName = hwtEngine.getTreeDisplayName(treeUri)
        preferencesRepository.setHuaweiFolder(treeUri.toString(), displayName)
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        preferencesRepository.setDarkTheme(isDark)
    }

    fun launchHuaweiThemesApp(): LaunchThemeAppResult = systemBridge.openHuaweiThemesApp()

    suspend fun searchOnlineRepositories(
        query: String,
        categoryQuery: String
    ): Result<List<OnlineThemeRepositoryItem>> =
        onlineService.searchGithubThemeRepositories(query, categoryQuery)

    suspend fun fetchGithubRepoAssets(
        repo: OnlineThemeRepositoryItem
    ): Result<List<OnlineHwtAsset>> =
        onlineService.fetchGithubRepoHwtAssets(repo)

    suspend fun fetchGithubRepoInfo(ownerRepo: String): Result<OnlineThemeRepositoryItem> =
        onlineService.fetchGithubRepositoryInfo(ownerRepo)

    suspend fun fetchCustomJsonCatalog(url: String): Result<List<OnlineHwtAsset>> =
        onlineService.fetchCustomJsonCatalog(url)

    suspend fun downloadAndImportOnlineTheme(
        url: String,
        suggestedFileName: String?,
        fallbackAuthor: String? = null,
        fallbackEmuiTarget: String? = null,
        onProgress: (Float) -> Unit
    ): Result<ThemeItem> = runCatching {
        val (tempFile, resolvedName) = onlineService.downloadHwtPackage(
            rawUrl = url,
            suggestedFileName = suggestedFileName,
            onProgress = onProgress
        ).getOrThrow()

        val imported = hwtEngine.importHwtFromDownloadedFile(
            tempFile = tempFile,
            suggestedFileName = resolvedName,
            fallbackAuthor = fallbackAuthor,
            fallbackEmuiTarget = fallbackEmuiTarget
        )

        val existing = themeDao.getThemeByFileName(imported.fileName)
        if (existing != null) {
            val updated = imported.copy(
                id = existing.id,
                isFavorite = existing.isFavorite,
                isApplied = existing.isApplied,
                isInstalledToHuawei = existing.isInstalledToHuawei
            )
            themeDao.updateTheme(updated)
            updated
        } else {
            val newId = themeDao.insertTheme(imported)
            imported.copy(id = newId)
        }
    }

    suspend fun addCustomOnlineSource(source: String) {
        preferencesRepository.addCustomOnlineSource(source)
    }

    suspend fun removeCustomOnlineSource(source: String) {
        preferencesRepository.removeCustomOnlineSource(source)
    }
}
