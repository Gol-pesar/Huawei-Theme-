package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.R
import com.example.data.ThemeItem
import java.io.File
import java.util.Locale

fun parseHexColor(hex: String, fallback: Color): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrDefault(fallback)
}

fun formatFileSizeFa(bytes: Long): String {
    if (bytes <= 0L) return "بسته آماده"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
        else -> "$bytes B"
    }
}

fun drawableResForName(name: String?): Int? {
    return when (name) {
        "img_theme_persian_dawn" -> R.drawable.img_theme_persian_dawn
        "img_theme_obsidian_pulse" -> R.drawable.img_theme_obsidian_pulse
        "img_theme_harmony_turquoise" -> R.drawable.img_theme_harmony_turquoise
        "img_theme_mate_nebula" -> R.drawable.img_theme_mate_nebula
        else -> null
    }
}

@Composable
fun ThemeArtworkImage(
    theme: ThemeItem,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val file = remember(theme.previewImagePath) {
        theme.previewImagePath?.let { File(it) }?.takeIf { it.exists() }
    }
    val resId = remember(theme.drawableResName) {
        drawableResForName(theme.drawableResName)
    }

    when {
        file != null -> {
            AsyncImage(
                model = file,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        }
        resId != null -> {
            Image(
                painter = painterResource(id = resId),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        }
        else -> {
            val accent = parseHexColor(theme.accentColorHex, MaterialTheme.colorScheme.primary)
            Box(
                modifier = modifier.background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                )
            )
        }
    }
}

@Composable
fun StudioArtworkImage(
    pickedUri: Uri?,
    presetDrawableName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val resId = remember(presetDrawableName) {
        drawableResForName(presetDrawableName) ?: R.drawable.img_theme_persian_dawn
    }
    if (pickedUri != null) {
        AsyncImage(
            model = pickedUri,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier.fillMaxSize()
        )
    } else {
        Image(
            painter = painterResource(id = resId),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier.fillMaxSize()
        )
    }
}
