package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppPreferences
import com.example.data.HwtInspectionResult
import com.example.data.IconShapeStyle
import com.example.data.LockClockStyle
import com.example.data.PreferencesRepository
import com.example.data.ThemeDatabase
import com.example.data.ThemeItem
import com.example.data.ThemeRepository
import com.example.data.ThemeSourceType
import com.example.engine.DeviceDiagnostics
import com.example.engine.HuaweiSystemBridge
import com.example.engine.HwtPackageEngine
import com.example.engine.LaunchThemeAppResult
import com.example.engine.OnlineHwtAsset
import com.example.engine.OnlineThemeRepositoryItem
import com.example.engine.OnlineThemeService
import com.example.engine.WallpaperTarget
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppDestination(val route: String, val labelFa: String) {
    GALLERY("gallery", "گالری"),
    ONLINE("online", "منبع آنلاین"),
    STUDIO("studio", "تم‌ساز"),
    MANAGER("manager", "مدیریت"),
    GUIDE("guide", "راهنما")
}

enum class ThemeFilter(val labelFa: String) {
    ALL("همه تم‌ها"),
    INSTALLED("نصب‌شده"),
    FAVORITES("علاقه‌مندی‌ها"),
    CREATED("ساخته‌شده"),
    IMPORTED("فایل‌های HWT")
}

enum class OnlineCategory(val labelFa: String, val queryTerm: String) {
    ALL_EMUI("همه EMUI", "huawei emui theme hwt"),
    HARMONY("HarmonyOS", "harmonyos huawei theme hwt"),
    DARK("تاریک / AMOLED", "huawei emui dark theme hwt"),
    IOS_ONEUI("سبک مدرن و iOS", "huawei emui theme ios oneui"),
    MINIMAL("مینیمال و آیکون", "huawei emui icon theme hwt")
}

data class OnlineStoreState(
    val searchQuery: String = "",
    val selectedCategory: OnlineCategory = OnlineCategory.ALL_EMUI,
    val repositories: List<OnlineThemeRepositoryItem> = emptyList(),
    val isLoadingRepos: Boolean = false,
    val reposError: String? = null,
    val hasLoadedOnce: Boolean = false,
    val selectedRepoTitle: String? = null,
    val selectedRepoSubtitle: String? = null,
    val repoAssets: List<OnlineHwtAsset> = emptyList(),
    val isLoadingAssets: Boolean = false,
    val assetsError: String? = null,
    val downloadingAssetId: String? = null,
    val downloadProgress: Float = 0f,
    val isDirectDownloading: Boolean = false
)

data class StudioDraftState(
    val title: String = "تم شخصی EMUI",
    val author: String = "طراح تم‌یار",
    val emuiTarget: String = "EMUI 14 / HarmonyOS",
    val accentHex: String = "#E63946",
    val iconShape: IconShapeStyle = IconShapeStyle.SQUIRCLE,
    val clockStyle: LockClockStyle = LockClockStyle.HARMONY_CENTER,
    val pickedWallpaperUri: Uri? = null,
    val presetDrawableName: String = "img_theme_persian_dawn",
    val isBuilding: Boolean = false
)

data class ThemeManagerUiState(
    val themes: List<ThemeItem> = emptyList(),
    val filteredThemes: List<ThemeItem> = emptyList(),
    val activeTheme: ThemeItem? = null,
    val selectedTheme: ThemeItem? = null,
    val inspectionResult: HwtInspectionResult? = null,
    val searchQuery: String = "",
    val selectedFilter: ThemeFilter = ThemeFilter.ALL,
    val preferences: AppPreferences = AppPreferences(),
    val diagnostics: DeviceDiagnostics? = null,
    val studioDraft: StudioDraftState = StudioDraftState(),
    val onlineState: OnlineStoreState = OnlineStoreState(),
    val isBusy: Boolean = false,
    val pendingInstallAfterFolderConnect: ThemeItem? = null
)

sealed interface UiEvent {
    data class ShowMessage(val message: String) : UiEvent
    data object RequestHuaweiFolderPicker : UiEvent
    data class RequestExportDocument(val suggestedFileName: String) : UiEvent
}

class ThemeManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ThemeDatabase.getInstance(application)
    private val preferencesRepo = PreferencesRepository(application)
    private val hwtEngine = HwtPackageEngine(application)
    private val systemBridge = HuaweiSystemBridge(application)
    private val onlineService = OnlineThemeService(application)
    private val repository = ThemeRepository(
        themeDao = database.themeDao(),
        preferencesRepository = preferencesRepo,
        hwtEngine = hwtEngine,
        systemBridge = systemBridge,
        onlineService = onlineService
    )

    private val diagnostics = systemBridge.getDeviceDiagnostics()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow(ThemeFilter.ALL)
    private val _selectedThemeId = MutableStateFlow<Long?>(null)
    private val _inspectionResult = MutableStateFlow<HwtInspectionResult?>(null)
    private val _studioDraft = MutableStateFlow(StudioDraftState())
    private val _onlineState = MutableStateFlow(OnlineStoreState())
    private val _isBusy = MutableStateFlow(false)
    private val _pendingInstallTheme = MutableStateFlow<ThemeItem?>(null)
    private var pendingExportTheme: ThemeItem? = null

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private val filterControlsFlow = combine(
        _searchQuery,
        _selectedFilter,
        _selectedThemeId,
        _inspectionResult,
        _isBusy
    ) { query, filter, selectedId, inspection, busy ->
        FilterBundle(query, filter, selectedId, inspection, busy)
    }

    private val secondaryFlow = combine(
        _studioDraft,
        _onlineState,
        _pendingInstallTheme
    ) { draft, online, pendingInstall ->
        Triple(draft, online, pendingInstall)
    }

    val uiState: StateFlow<ThemeManagerUiState> = combine(
        repository.allThemes,
        repository.preferences,
        filterControlsFlow,
        secondaryFlow
    ) { themes, prefs, controls, secondary ->
        val (draft, online, pendingInstall) = secondary
        val filtered = themes.filter { theme ->
            val matchesFilter = when (controls.filter) {
                ThemeFilter.ALL -> true
                ThemeFilter.INSTALLED -> theme.isInstalledToHuawei || theme.isApplied
                ThemeFilter.FAVORITES -> theme.isFavorite
                ThemeFilter.CREATED -> theme.parsedSourceType == ThemeSourceType.CREATED
                ThemeFilter.IMPORTED -> theme.parsedSourceType == ThemeSourceType.IMPORTED ||
                    theme.parsedSourceType == ThemeSourceType.SCANNED ||
                    theme.parsedSourceType == ThemeSourceType.ONLINE
            }
            val q = controls.query.trim()
            val matchesQuery = q.isEmpty() ||
                theme.title.contains(q, ignoreCase = true) ||
                theme.author.contains(q, ignoreCase = true) ||
                theme.fileName.contains(q, ignoreCase = true) ||
                theme.emuiTarget.contains(q, ignoreCase = true)
            matchesFilter && matchesQuery
        }

        val active = themes.firstOrNull { it.isApplied } ?: themes.firstOrNull()
        val selected = controls.selectedId?.let { id -> themes.firstOrNull { it.id == id } }

        ThemeManagerUiState(
            themes = themes,
            filteredThemes = filtered,
            activeTheme = active,
            selectedTheme = selected,
            inspectionResult = controls.inspection,
            searchQuery = controls.query,
            selectedFilter = controls.filter,
            preferences = prefs,
            diagnostics = diagnostics,
            studioDraft = draft,
            onlineState = online,
            isBusy = controls.busy,
            pendingInstallAfterFolderConnect = pendingInstall
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeManagerUiState(diagnostics = diagnostics)
    )

    init {
        viewModelScope.launch {
            repository.seedBuiltInThemesIfNeeded()
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChange(filter: ThemeFilter) {
        _selectedFilter.value = filter
    }

    fun selectTheme(theme: ThemeItem?) {
        _selectedThemeId.value = theme?.id
        if (theme == null) {
            _inspectionResult.value = null
        }
    }

    fun dismissInspection() {
        _inspectionResult.value = null
    }

    fun toggleFavorite(theme: ThemeItem) {
        viewModelScope.launch {
            repository.toggleFavorite(theme)
        }
    }

    fun deleteTheme(theme: ThemeItem) {
        viewModelScope.launch {
            if (_selectedThemeId.value == theme.id) {
                _selectedThemeId.value = null
            }
            repository.deleteTheme(theme)
            _events.emit(UiEvent.ShowMessage("تم «${theme.title}» حذف شد"))
        }
    }

    fun importHwtUri(uri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            runCatching {
                val imported = repository.importThemeFromUri(uri)
                _selectedThemeId.value = imported.id
                _events.emit(UiEvent.ShowMessage("فایل «${imported.fileName}» با موفقیت تحلیل و اضافه شد"))
            }.onFailure { err ->
                _events.emit(UiEvent.ShowMessage("خطا در وارد کردن فایل HWT: ${err.localizedMessage ?: "فرمت نامعتبر"}"))
            }
            _isBusy.value = false
        }
    }

    fun onHuaweiFolderConnected(treeUri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.saveHuaweiFolderUri(treeUri)
            val pending = _pendingInstallTheme.value
            if (pending != null) {
                _pendingInstallTheme.value = null
                installThemeInternal(pending)
            } else {
                val scanResult = repository.scanConnectedHuaweiFolder()
                val added = scanResult.getOrDefault(0)
                val msg = if (added > 0) {
                    "پوشه هواوی متصل شد و $added تم موجود شناسایی گردید"
                } else {
                    "پوشه Huawei/Themes با موفقیت متصل شد و آماده نصب تم‌هاست"
                }
                _events.emit(UiEvent.ShowMessage(msg))
            }
            _isBusy.value = false
        }
    }

    fun scanConnectedFolder() {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.scanConnectedHuaweiFolder()
            result.onSuccess { count ->
                val msg = if (count > 0) {
                    "$count فایل تم جدید از پوشه هواوی اضافه شد"
                } else {
                    "اسکن انجام شد؛ تمام تم‌های پوشه به‌روز هستند"
                }
                _events.emit(UiEvent.ShowMessage(msg))
            }.onFailure { err ->
                if (err.message == "FOLDER_NOT_CONNECTED") {
                    _events.emit(UiEvent.RequestHuaweiFolderPicker)
                } else {
                    _events.emit(UiEvent.ShowMessage("خطا در اسکن پوشه: ${err.localizedMessage ?: "دسترسی مجدد لازم است"}"))
                }
            }
            _isBusy.value = false
        }
    }

    fun installThemeToHuawei(theme: ThemeItem) {
        viewModelScope.launch {
            val folderUri = uiState.value.preferences.huaweiFolderUri
            if (folderUri.isNullOrBlank()) {
                _pendingInstallTheme.value = theme
                _events.emit(UiEvent.ShowMessage("ابتدا پوشه Huawei/Themes را انتخاب کنید تا فایل HWT مستقیم نصب شود"))
                _events.emit(UiEvent.RequestHuaweiFolderPicker)
                return@launch
            }
            _isBusy.value = true
            installThemeInternal(theme)
            _isBusy.value = false
        }
    }

    private suspend fun installThemeInternal(theme: ThemeItem) {
        val result = repository.installThemeToHuaweiFolder(theme)
        result.onSuccess {
            repository.markThemeAsApplied(theme.id)
            _events.emit(
                UiEvent.ShowMessage(
                    "بسته ${theme.fileName} در پوشه تم‌های هواوی نصب شد! اکنون برنامه Themes هواوی را باز و اعمال کنید"
                )
            )
        }.onFailure { err ->
            if (err.message == "FOLDER_NOT_CONNECTED") {
                _pendingInstallTheme.value = theme
                _events.emit(UiEvent.RequestHuaweiFolderPicker)
            } else {
                _events.emit(UiEvent.ShowMessage("خطا در کپی به پوشه هواوی: ${err.localizedMessage ?: "مجدداً پوشه را متصل کنید"}"))
            }
        }
    }

    fun openHuaweiThemesApp(theme: ThemeItem? = null) {
        viewModelScope.launch {
            if (theme != null) {
                repository.markThemeAsApplied(theme.id)
            }
            when (val res = repository.launchHuaweiThemesApp()) {
                is LaunchThemeAppResult.Launched -> {
                    _events.emit(UiEvent.ShowMessage("برنامه ${res.appName} باز شد؛ از بخش «من > تم‌ها» تم را اعمال کنید"))
                }
                LaunchThemeAppResult.NotInstalled -> {
                    _events.emit(
                        UiEvent.ShowMessage(
                            "برنامه رسمی Huawei Themes روی این دستگاه یافت نشد. می‌توانید فایل HWT را ذخیره کنید یا تصویر زمینه را مستقیم اعمال نمایید."
                        )
                    )
                }
            }
        }
    }

    fun requestExportTheme(theme: ThemeItem) {
        pendingExportTheme = theme
        viewModelScope.launch {
            _events.emit(UiEvent.RequestExportDocument(theme.fileName))
        }
    }

    fun onExportDestinationPicked(destUri: Uri) {
        val theme = pendingExportTheme ?: return
        pendingExportTheme = null
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.exportThemeToUri(theme, destUri)
            res.onSuccess {
                _events.emit(UiEvent.ShowMessage("فایل ${theme.fileName} با موفقیت ذخیره شد"))
            }.onFailure { err ->
                _events.emit(UiEvent.ShowMessage("خطا در ذخیره فایل: ${err.localizedMessage ?: "نامشخص"}"))
            }
            _isBusy.value = false
        }
    }

    fun shareTheme(theme: ThemeItem) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.shareTheme(theme).onFailure { err ->
                _events.emit(UiEvent.ShowMessage("خطا در اشتراک‌گذاری: ${err.localizedMessage ?: "نامشخص"}"))
            }
            _isBusy.value = false
        }
    }

    fun inspectTheme(theme: ThemeItem) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.inspectTheme(theme)
            result.onSuccess { inspection ->
                _inspectionResult.value = inspection
            }.onFailure { err ->
                _events.emit(UiEvent.ShowMessage("خطا در تحلیل فایل HWT: ${err.localizedMessage ?: "نامشخص"}"))
            }
            _isBusy.value = false
        }
    }

    fun applyThemeWallpaper(theme: ThemeItem, target: WallpaperTarget) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.applyThemeWallpaper(theme, target)
            res.onSuccess {
                _events.emit(UiEvent.ShowMessage("تصویر زمینه «${theme.title}» روی ${target.labelFa} اعمال شد"))
            }.onFailure { err ->
                _events.emit(UiEvent.ShowMessage("خطا در اعمال تصویر زمینه: ${err.localizedMessage ?: "نامشخص"}"))
            }
            _isBusy.value = false
        }
    }

    fun setDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            repository.setDarkTheme(isDark)
        }
    }

    // --- Online Theme Source Controls ---

    fun ensureOnlineRepositoriesLoaded() {
        val current = _onlineState.value
        if (!current.hasLoadedOnce && !current.isLoadingRepos) {
            searchOnlineRepositories()
        }
    }

    fun onOnlineSearchQueryChange(query: String) {
        _onlineState.update { it.copy(searchQuery = query) }
    }

    fun onOnlineCategoryChange(category: OnlineCategory) {
        _onlineState.update { it.copy(selectedCategory = category) }
        searchOnlineRepositories()
    }

    fun searchOnlineRepositories() {
        val state = _onlineState.value
        viewModelScope.launch {
            _onlineState.update { it.copy(isLoadingRepos = true, reposError = null) }
            val result = repository.searchOnlineRepositories(
                query = state.searchQuery,
                categoryQuery = state.selectedCategory.queryTerm
            )
            result.onSuccess { repos ->
                _onlineState.update {
                    it.copy(
                        repositories = repos,
                        isLoadingRepos = false,
                        reposError = null,
                        hasLoadedOnce = true
                    )
                }
            }.onFailure { err ->
                _onlineState.update {
                    it.copy(
                        isLoadingRepos = false,
                        reposError = err.localizedMessage ?: "خطا در برقراری ارتباط با منبع آنلاین",
                        hasLoadedOnce = true
                    )
                }
            }
        }
    }

    fun openOnlineRepository(repo: OnlineThemeRepositoryItem) {
        viewModelScope.launch {
            _onlineState.update {
                it.copy(
                    selectedRepoTitle = repo.name,
                    selectedRepoSubtitle = "${repo.fullName} • ${repo.stars} ستاره",
                    repoAssets = emptyList(),
                    isLoadingAssets = true,
                    assetsError = null
                )
            }
            val result = repository.fetchGithubRepoAssets(repo)
            result.onSuccess { assets ->
                _onlineState.update {
                    it.copy(
                        repoAssets = assets,
                        isLoadingAssets = false,
                        assetsError = if (assets.isEmpty()) {
                            "فایل آماده .hwt یا .hnt در این مخزن یافت نشد (ممکن است فقط فایل‌های خام منبع تم باشد)."
                        } else null
                    )
                }
            }.onFailure { err ->
                _onlineState.update {
                    it.copy(
                        isLoadingAssets = false,
                        assetsError = err.localizedMessage ?: "خطا در دریافت لیست فایل‌های مخزن"
                    )
                }
            }
        }
    }

    fun openCustomOnlineSource(source: String) {
        viewModelScope.launch {
            val trimmed = source.trim()
            val githubSlug = onlineService.normalizeGithubRepoSlug(trimmed)
            if (githubSlug != null && !trimmed.endsWith(".json", ignoreCase = true)) {
                _onlineState.update {
                    it.copy(
                        selectedRepoTitle = githubSlug.substringAfter("/"),
                        selectedRepoSubtitle = "مخزن سفارشی GitHub • $githubSlug",
                        repoAssets = emptyList(),
                        isLoadingAssets = true,
                        assetsError = null
                    )
                }
                val infoRes = repository.fetchGithubRepoInfo(githubSlug)
                infoRes.onSuccess { repo ->
                    val assetsRes = repository.fetchGithubRepoAssets(repo)
                    assetsRes.onSuccess { assets ->
                        _onlineState.update {
                            it.copy(
                                repoAssets = assets,
                                isLoadingAssets = false,
                                assetsError = if (assets.isEmpty()) {
                                    "فایل .hwt یا .hnt آماده در این مخزن یافت نشد."
                                } else null
                            )
                        }
                    }.onFailure { err ->
                        _onlineState.update {
                            it.copy(
                                isLoadingAssets = false,
                                assetsError = err.localizedMessage ?: "خطا در خواندن فایل‌های مخزن"
                            )
                        }
                    }
                }.onFailure { err ->
                    _onlineState.update {
                        it.copy(
                            isLoadingAssets = false,
                            assetsError = err.localizedMessage ?: "مخزن گیت‌هاب یافت نشد"
                        )
                    }
                }
            } else {
                _onlineState.update {
                    it.copy(
                        selectedRepoTitle = "کاتالوگ آنلاین سفارشی",
                        selectedRepoSubtitle = trimmed,
                        repoAssets = emptyList(),
                        isLoadingAssets = true,
                        assetsError = null
                    )
                }
                val catalogRes = repository.fetchCustomJsonCatalog(trimmed)
                catalogRes.onSuccess { assets ->
                    _onlineState.update {
                        it.copy(
                            repoAssets = assets,
                            isLoadingAssets = false,
                            assetsError = if (assets.isEmpty()) "هیچ بسته HWT معتبری در این کاتالوگ یافت نشد." else null
                        )
                    }
                }.onFailure { err ->
                    _onlineState.update {
                        it.copy(
                            isLoadingAssets = false,
                            assetsError = err.localizedMessage ?: "خطا در خواندن کاتالوگ JSON آنلاین"
                        )
                    }
                }
            }
        }
    }

    fun dismissOnlineRepoSheet() {
        _onlineState.update {
            it.copy(
                selectedRepoTitle = null,
                selectedRepoSubtitle = null,
                repoAssets = emptyList(),
                isLoadingAssets = false,
                assetsError = null
            )
        }
    }

    fun downloadOnlineAsset(asset: OnlineHwtAsset) {
        if (_onlineState.value.downloadingAssetId != null) return
        viewModelScope.launch {
            _onlineState.update { it.copy(downloadingAssetId = asset.id, downloadProgress = 0f) }
            val result = repository.downloadAndImportOnlineTheme(
                url = asset.downloadUrl,
                suggestedFileName = asset.fileName,
                fallbackAuthor = asset.author,
                fallbackEmuiTarget = asset.emuiTarget,
                onProgress = { p -> _onlineState.update { it.copy(downloadProgress = p) } }
            )
            _onlineState.update { it.copy(downloadingAssetId = null, downloadProgress = 0f) }
            result.onSuccess { imported ->
                dismissOnlineRepoSheet()
                _selectedThemeId.value = imported.id
                _events.emit(UiEvent.ShowMessage("تم «${imported.title}» دانلود شد و آماده نصب در پوشه هواوی است"))
            }.onFailure { err ->
                _events.emit(UiEvent.ShowMessage("خطا در دانلود بسته: ${err.localizedMessage ?: "نامشخص"}"))
            }
        }
    }

    fun downloadDirectHwtUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) {
            viewModelScope.launch {
                _events.emit(UiEvent.ShowMessage("لطفاً لینک HTTPS فایل .hwt یا .hnt را وارد کنید"))
            }
            return
        }
        viewModelScope.launch {
            _onlineState.update { it.copy(isDirectDownloading = true, downloadProgress = 0f) }
            val result = repository.downloadAndImportOnlineTheme(
                url = trimmed,
                suggestedFileName = null,
                onProgress = { p -> _onlineState.update { it.copy(downloadProgress = p) } }
            )
            _onlineState.update { it.copy(isDirectDownloading = false, downloadProgress = 0f) }
            result.onSuccess { imported ->
                _selectedThemeId.value = imported.id
                _events.emit(UiEvent.ShowMessage("بسته «${imported.fileName}» با موفقیت دانلود و تحلیل شد"))
            }.onFailure { err ->
                _events.emit(UiEvent.ShowMessage("خطا در دانلود مستقیم: ${err.localizedMessage ?: "لینک معتبر نیست"}"))
            }
        }
    }

    fun addCustomOnlineSource(input: String) {
        val trimmed = input.trim()
        viewModelScope.launch {
            val isGithubSlug = onlineService.normalizeGithubRepoSlug(trimmed) != null
            val isHttps = trimmed.startsWith("https://", ignoreCase = true)
            if (!isGithubSlug && !isHttps) {
                _events.emit(
                    UiEvent.ShowMessage("آدرس باید یک مخزن گیت‌هاب (مثل user/repo) یا لینک امن HTTPS به فایل JSON باشد")
                )
                return@launch
            }
            repository.addCustomOnlineSource(trimmed)
            _events.emit(UiEvent.ShowMessage("منبع آنلاین جدید با موفقیت ذخیره شد"))
            openCustomOnlineSource(trimmed)
        }
    }

    fun removeCustomOnlineSource(source: String) {
        viewModelScope.launch {
            repository.removeCustomOnlineSource(source)
            _events.emit(UiEvent.ShowMessage("منبع آنلاین حذف شد"))
        }
    }

    // --- Studio Draft Controls ---
    fun updateStudioTitle(title: String) = _studioDraft.update { it.copy(title = title) }
    fun updateStudioAuthor(author: String) = _studioDraft.update { it.copy(author = author) }
    fun updateStudioEmuiTarget(target: String) = _studioDraft.update { it.copy(emuiTarget = target) }
    fun updateStudioAccent(hex: String) = _studioDraft.update { it.copy(accentHex = hex) }
    fun updateStudioIconShape(shape: IconShapeStyle) = _studioDraft.update { it.copy(iconShape = shape) }
    fun updateStudioClockStyle(style: LockClockStyle) = _studioDraft.update { it.copy(clockStyle = style) }
    fun updateStudioPickedWallpaper(uri: Uri?) = _studioDraft.update { it.copy(pickedWallpaperUri = uri) }
    fun updateStudioPresetDrawable(name: String) = _studioDraft.update {
        it.copy(presetDrawableName = name, pickedWallpaperUri = null)
    }

    fun buildCustomHwtTheme() {
        val draft = _studioDraft.value
        if (draft.isBuilding) return
        viewModelScope.launch {
            _studioDraft.update { it.copy(isBuilding = true) }
            runCatching {
                val created = repository.createCustomTheme(
                    title = draft.title,
                    author = draft.author,
                    emuiTarget = draft.emuiTarget,
                    accentHex = draft.accentHex,
                    iconShape = draft.iconShape,
                    clockStyle = draft.clockStyle,
                    wallpaperUri = draft.pickedWallpaperUri,
                    fallbackDrawableName = draft.presetDrawableName
                )
                _selectedThemeId.value = created.id
                _events.emit(UiEvent.ShowMessage("بسته واقعی ${created.fileName} ساخته شد و آماده نصب در هواوی است!"))
            }.onFailure { err ->
                _events.emit(UiEvent.ShowMessage("خطا در ساخت بسته HWT: ${err.localizedMessage ?: "نامشخص"}"))
            }
            _studioDraft.update { it.copy(isBuilding = false) }
        }
    }

    private data class FilterBundle(
        val query: String,
        val filter: ThemeFilter,
        val selectedId: Long?,
        val inspection: HwtInspectionResult?,
        val busy: Boolean
    )
}
