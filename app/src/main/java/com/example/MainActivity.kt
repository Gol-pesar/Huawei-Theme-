package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.HwtInspectionDialog
import com.example.ui.components.ThemeDetailSheet
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.GuideScreen
import com.example.ui.screens.ManagerScreen
import com.example.ui.screens.OnlineRepoAssetsSheet
import com.example.ui.screens.OnlineStoreScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.HuaweiThemeManagerTheme
import com.example.viewmodel.AppDestination
import com.example.viewmodel.ThemeManagerViewModel
import com.example.viewmodel.UiEvent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ThemeManagerViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isDark = uiState.preferences.isDarkTheme

            LaunchedEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(Color.Transparent.toArgb())
                    } else {
                        SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb())
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(Color.Transparent.toArgb())
                    } else {
                        SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb())
                    }
                )
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HuaweiThemeManagerTheme(darkTheme = isDark) {
                    HuaweiThemeManagerApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun HuaweiThemeManagerApp(viewModel: ThemeManagerViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var currentDestination by rememberSaveable { mutableStateOf(AppDestination.GALLERY) }

    // 1. SAF Import .hwt / .hnt file launcher
    val importHwtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importHwtUri(uri)
        }
    }

    // 2. SAF Connect Huawei/Themes folder launcher
    val huaweiFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
            }
            viewModel.onHuaweiFolderConnected(treeUri)
        }
    }

    // 3. SAF Export .hwt file launcher
    val exportHwtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { destUri ->
        if (destUri != null) {
            viewModel.onExportDestinationPicked(destUri)
        }
    }

    // 4. Zero-permission Android Photo Picker for Theme Studio wallpaper
    val studioPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.updateStudioPickedWallpaper(uri)
        }
    }

    // Collect one-shot UI events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                UiEvent.RequestHuaweiFolderPicker -> {
                    huaweiFolderLauncher.launch(null)
                }
                is UiEvent.RequestExportDocument -> {
                    exportHwtLauncher.launch(event.suggestedFileName)
                }
            }
        }
    }

    // Back handling for non-home tabs
    BackHandler(enabled = currentDestination != AppDestination.GALLERY) {
        currentDestination = AppDestination.GALLERY
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp
        val galleryColumns = when {
            maxWidth >= 900.dp -> 4
            maxWidth >= 600.dp -> 3
            else -> 2
        }

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
                if (currentDestination == AppDestination.GALLERY) {
                    ExtendedFloatingActionButton(
                        onClick = { importHwtLauncher.launch(arrayOf("*/*")) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.action_import_hwt),
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        modifier = Modifier.testTag("fab_import_hwt")
                    )
                }
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        AppDestination.entries.forEach { dest ->
                            val selected = currentDestination == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentDestination = dest },
                                icon = {
                                    Icon(
                                        imageVector = destinationIcon(dest, selected),
                                        contentDescription = dest.labelFa
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.labelFa,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        AppDestination.entries.forEach { dest ->
                            val selected = currentDestination == dest
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentDestination = dest },
                                icon = {
                                    Icon(
                                        imageVector = destinationIcon(dest, selected),
                                        contentDescription = dest.labelFa
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.labelFa,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 920.dp)
                    ) {
                        when (currentDestination) {
                            AppDestination.GALLERY -> GalleryScreen(
                                uiState = uiState,
                                columns = galleryColumns,
                                onSearchChange = viewModel::onSearchQueryChange,
                                onFilterChange = viewModel::onFilterChange,
                                onSelectTheme = viewModel::selectTheme,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onConnectFolderClick = {
                                    if (uiState.preferences.huaweiFolderUri.isNullOrBlank()) {
                                        huaweiFolderLauncher.launch(null)
                                    } else {
                                        currentDestination = AppDestination.MANAGER
                                    }
                                },
                                onImportHwtClick = { importHwtLauncher.launch(arrayOf("*/*")) }
                            )

                            AppDestination.ONLINE -> OnlineStoreScreen(
                                onlineState = uiState.onlineState,
                                customSources = uiState.preferences.customOnlineSources,
                                onEnsureLoaded = viewModel::ensureOnlineRepositoriesLoaded,
                                onSearchQueryChange = viewModel::onOnlineSearchQueryChange,
                                onSearchSubmit = viewModel::searchOnlineRepositories,
                                onCategoryChange = viewModel::onOnlineCategoryChange,
                                onOpenRepository = viewModel::openOnlineRepository,
                                onOpenCustomSource = viewModel::openCustomOnlineSource,
                                onAddCustomSource = viewModel::addCustomOnlineSource,
                                onRemoveCustomSource = viewModel::removeCustomOnlineSource,
                                onDirectDownloadUrl = viewModel::downloadDirectHwtUrl
                            )

                            AppDestination.STUDIO -> StudioScreen(
                                draft = uiState.studioDraft,
                                onTitleChange = viewModel::updateStudioTitle,
                                onAuthorChange = viewModel::updateStudioAuthor,
                                onEmuiTargetChange = viewModel::updateStudioEmuiTarget,
                                onAccentChange = viewModel::updateStudioAccent,
                                onIconShapeChange = viewModel::updateStudioIconShape,
                                onClockStyleChange = viewModel::updateStudioClockStyle,
                                onPickWallpaperFromGallery = {
                                    studioPhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onSelectPresetDrawable = viewModel::updateStudioPresetDrawable,
                                onBuildThemeClick = viewModel::buildCustomHwtTheme
                            )

                            AppDestination.MANAGER -> ManagerScreen(
                                uiState = uiState,
                                onConnectFolder = { huaweiFolderLauncher.launch(null) },
                                onScanFolder = viewModel::scanConnectedFolder,
                                onImportHwt = { importHwtLauncher.launch(arrayOf("*/*")) },
                                onOpenHuaweiThemes = { viewModel.openHuaweiThemesApp() },
                                onSelectTheme = viewModel::selectTheme,
                                onInstallTheme = viewModel::installThemeToHuawei,
                                onInspectTheme = viewModel::inspectTheme,
                                onShareTheme = viewModel::shareTheme
                            )

                            AppDestination.GUIDE -> GuideScreen(
                                diagnostics = uiState.diagnostics,
                                isDarkTheme = uiState.preferences.isDarkTheme,
                                onToggleDarkTheme = viewModel::setDarkTheme,
                                onOpenHuaweiThemes = { viewModel.openHuaweiThemesApp() }
                            )
                        }

                        if (uiState.isBusy) {
                            LinearProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    // Online Repository HWT Files Bottom Sheet
    if (uiState.onlineState.selectedRepoTitle != null) {
        OnlineRepoAssetsSheet(
            onlineState = uiState.onlineState,
            onDismiss = viewModel::dismissOnlineRepoSheet,
            onDownloadAsset = viewModel::downloadOnlineAsset
        )
    }

    // Theme Detail Bottom Sheet
    uiState.selectedTheme?.let { selectedTheme ->
        ThemeDetailSheet(
            theme = selectedTheme,
            isFolderConnected = !uiState.preferences.huaweiFolderUri.isNullOrBlank(),
            onDismiss = { viewModel.selectTheme(null) },
            onInstallToHuawei = { viewModel.installThemeToHuawei(selectedTheme) },
            onOpenHuaweiThemes = { viewModel.openHuaweiThemesApp(selectedTheme) },
            onApplyWallpaper = { target -> viewModel.applyThemeWallpaper(selectedTheme, target) },
            onInspect = { viewModel.inspectTheme(selectedTheme) },
            onExport = { viewModel.requestExportTheme(selectedTheme) },
            onShare = { viewModel.shareTheme(selectedTheme) },
            onToggleFavorite = { viewModel.toggleFavorite(selectedTheme) },
            onDelete = { viewModel.deleteTheme(selectedTheme) }
        )
    }

    // HWT Archive Inspector Dialog
    uiState.inspectionResult?.let { inspection ->
        HwtInspectionDialog(
            result = inspection,
            onDismiss = viewModel::dismissInspection
        )
    }
}

private fun destinationIcon(dest: AppDestination, selected: Boolean): ImageVector {
    return when (dest) {
        AppDestination.GALLERY -> if (selected) Icons.Rounded.AutoAwesome else Icons.Outlined.AutoAwesome
        AppDestination.ONLINE -> if (selected) Icons.Rounded.CloudDownload else Icons.Outlined.CloudDownload
        AppDestination.STUDIO -> if (selected) Icons.Rounded.Brush else Icons.Outlined.Brush
        AppDestination.MANAGER -> if (selected) Icons.Rounded.FolderSpecial else Icons.Outlined.FolderSpecial
        AppDestination.GUIDE -> if (selected) Icons.Rounded.Help else Icons.Outlined.HelpOutline
    }
}
