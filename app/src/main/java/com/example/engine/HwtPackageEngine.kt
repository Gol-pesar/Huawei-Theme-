package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Xml
import com.example.R
import com.example.data.HwtInspectionResult
import com.example.data.IconShapeStyle
import com.example.data.LockClockStyle
import com.example.data.ThemeItem
import com.example.data.ThemeSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.StringReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ParsedHwtMetadata(
    val title: String,
    val englishName: String,
    val author: String,
    val version: String,
    val emuiTarget: String,
    val previewImagePath: String?,
    val modules: List<String>,
    val fileSizeBytes: Long
)

class HwtPackageEngine(private val context: Context) {

    private val themesDir: File
        get() = File(context.filesDir, "themes").apply { if (!exists()) mkdirs() }

    private val previewsDir: File
        get() = File(context.cacheDir, "previews").apply { if (!exists()) mkdirs() }

    fun resolveDrawableResId(drawableResName: String?): Int? {
        return when (drawableResName) {
            "img_theme_persian_dawn" -> R.drawable.img_theme_persian_dawn
            "img_theme_obsidian_pulse" -> R.drawable.img_theme_obsidian_pulse
            "img_theme_harmony_turquoise" -> R.drawable.img_theme_harmony_turquoise
            "img_theme_mate_nebula" -> R.drawable.img_theme_mate_nebula
            else -> null
        }
    }

    suspend fun ensureHwtFileExists(theme: ThemeItem): File = withContext(Dispatchers.IO) {
        val existingPath = theme.localFilePath
        if (!existingPath.isNullOrBlank()) {
            val existing = File(existingPath)
            if (existing.exists() && existing.length() > 0L) {
                return@withContext existing
            }
        }
        val bitmap = loadThemeBitmap(theme)
        createHwtArchiveFile(
            title = theme.title,
            englishName = theme.englishName,
            author = theme.author,
            version = theme.version,
            emuiTarget = theme.emuiTarget,
            accentHex = theme.accentColorHex,
            iconShape = theme.parsedIconShape,
            clockStyle = theme.parsedClockStyle,
            fileName = sanitizeHwtFileName(theme.fileName),
            wallpaperBitmap = bitmap
        )
    }

    suspend fun loadThemeBitmap(theme: ThemeItem): Bitmap? = withContext(Dispatchers.IO) {
        val previewPath = theme.previewImagePath
        if (!previewPath.isNullOrBlank()) {
            val file = File(previewPath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)?.let { return@withContext it }
            }
        }
        val resId = resolveDrawableResId(theme.drawableResName)
        if (resId != null) {
            return@withContext BitmapFactory.decodeResource(context.resources, resId)
        }
        null
    }

    suspend fun decodeBitmapFromUri(uri: Uri, maxDimension: Int = 1440): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            var sampleSize = 1
            val maxSide = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
            while (maxSide / sampleSize > maxDimension) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            }
        }.getOrNull()
    }

    suspend fun createCustomThemePackage(
        title: String,
        author: String,
        emuiTarget: String,
        accentHex: String,
        iconShape: IconShapeStyle,
        clockStyle: LockClockStyle,
        wallpaperUri: Uri?,
        fallbackDrawableName: String
    ): ThemeItem = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim().ifEmpty { "تم اختصاصی هواوی" }
        val cleanAuthor = author.trim().ifEmpty { "طراح تم‌یار" }
        val timestamp = System.currentTimeMillis()
        val slug = "custom_emui_${timestamp % 100000}"
        val fileName = "$slug.hwt"

        val bitmap = if (wallpaperUri != null) {
            decodeBitmapFromUri(wallpaperUri)
        } else {
            resolveDrawableResId(fallbackDrawableName)?.let {
                BitmapFactory.decodeResource(context.resources, it)
            }
        }

        val previewFile = if (bitmap != null) {
            val outPreview = File(previewsDir, "preview_$slug.jpg")
            FileOutputStream(outPreview).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, fos)
            }
            outPreview
        } else null

        val hwtFile = createHwtArchiveFile(
            title = cleanTitle,
            englishName = slug,
            author = cleanAuthor,
            version = "1.0.0",
            emuiTarget = emuiTarget,
            accentHex = accentHex,
            iconShape = iconShape,
            clockStyle = clockStyle,
            fileName = fileName,
            wallpaperBitmap = bitmap
        )

        ThemeItem(
            title = cleanTitle,
            englishName = slug,
            author = cleanAuthor,
            version = "1.0.0",
            emuiTarget = emuiTarget,
            fileName = fileName,
            localFilePath = hwtFile.absolutePath,
            previewImagePath = previewFile?.absolutePath,
            drawableResName = if (previewFile == null) fallbackDrawableName else null,
            accentColorHex = accentHex,
            iconShape = iconShape.name,
            clockStyle = clockStyle.name,
            fileSizeBytes = hwtFile.length(),
            sourceType = ThemeSourceType.CREATED.name,
            isInstalledToHuawei = false,
            isFavorite = false,
            isApplied = false,
            modulesCsv = "صفحه قفل,تصویر زمینه,آیکون‌ها,پالت رنگی,ساختار HWT",
            createdAt = timestamp
        )
    }

    private fun createHwtArchiveFile(
        title: String,
        englishName: String,
        author: String,
        version: String,
        emuiTarget: String,
        accentHex: String,
        iconShape: IconShapeStyle,
        clockStyle: LockClockStyle,
        fileName: String,
        wallpaperBitmap: Bitmap?
    ): File {
        val targetFile = File(themesDir, sanitizeHwtFileName(fileName))
        val descriptionXml = buildDescriptionXml(
            title = title,
            englishName = englishName,
            author = author,
            version = version,
            emuiTarget = emuiTarget
        )
        val themeMetaJson = """
            {
              "generator": "HuaweiThemeManager",
              "title": "${escapeJson(title)}",
              "emuiTarget": "${escapeJson(emuiTarget)}",
              "accentColor": "${escapeJson(accentHex)}",
              "iconShape": "${iconShape.name}",
              "clockStyle": "${clockStyle.name}"
            }
        """.trimIndent()

        val wallpaperBytes = wallpaperBitmap?.let { bmp ->
            ByteArrayOutputStream().use { bos ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, bos)
                bos.toByteArray()
            }
        }

        val previewBytes = wallpaperBitmap?.let { bmp ->
            val scaled = scaleBitmapForPreview(bmp, 640)
            ByteArrayOutputStream().use { bos ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 82, bos)
                bos.toByteArray()
            }
        }

        ZipOutputStream(FileOutputStream(targetFile)).use { zos ->
            writeZipEntry(zos, "description.xml", descriptionXml.toByteArray(Charsets.UTF_8))
            writeZipEntry(zos, "theme_config.json", themeMetaJson.toByteArray(Charsets.UTF_8))

            if (wallpaperBytes != null) {
                writeZipEntry(zos, "wallpaper/home_wallpaper_0.jpg", wallpaperBytes)
                writeZipEntry(zos, "wallpaper/unlock_wallpaper_0.jpg", wallpaperBytes)
            }
            if (previewBytes != null) {
                writeZipEntry(zos, "preview/preview_cover_0.jpg", previewBytes)
                writeZipEntry(zos, "preview/preview_unlock_0.jpg", previewBytes)
                writeZipEntry(zos, "preview/preview_icons_0.jpg", previewBytes)
            }
            val unlockLayoutXml = """
                <?xml version="1.0" encoding="utf-8"?>
                <HwLockscreen version="1" style="${clockStyle.name}" accent="${escapeXml(accentHex)}">
                    <Clock position="${if (clockStyle == LockClockStyle.EMUI_LEFT) "left" else "center"}" />
                </HwLockscreen>
            """.trimIndent()
            writeZipEntry(zos, "unlock/theme.xml", unlockLayoutXml.toByteArray(Charsets.UTF_8))
        }
        return targetFile
    }

    suspend fun importHwtFromUri(uri: Uri, sourceType: ThemeSourceType = ThemeSourceType.IMPORTED): ThemeItem =
        withContext(Dispatchers.IO) {
            val rawName = queryDisplayName(uri) ?: "imported_theme_${System.currentTimeMillis() % 10000}.hwt"
            val safeName = sanitizeHwtFileName(rawName)
            val destFile = File(themesDir, safeName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("خطا در خواندن فایل انتخاب‌شده")

            val parsed = parseHwtFile(destFile, safeName)
            ThemeItem(
                title = parsed.title,
                englishName = parsed.englishName,
                author = parsed.author,
                version = parsed.version,
                emuiTarget = parsed.emuiTarget,
                fileName = safeName,
                localFilePath = destFile.absolutePath,
                previewImagePath = parsed.previewImagePath,
                drawableResName = if (parsed.previewImagePath == null) "img_theme_obsidian_pulse" else null,
                accentColorHex = "#E63946",
                iconShape = IconShapeStyle.SQUIRCLE.name,
                clockStyle = LockClockStyle.HARMONY_CENTER.name,
                fileSizeBytes = parsed.fileSizeBytes,
                sourceType = sourceType.name,
                isInstalledToHuawei = sourceType == ThemeSourceType.SCANNED,
                isFavorite = false,
                isApplied = false,
                modulesCsv = parsed.modules.joinToString(","),
                createdAt = System.currentTimeMillis()
            )
        }

    suspend fun importHwtFromDownloadedFile(
        tempFile: File,
        suggestedFileName: String,
        fallbackAuthor: String? = null,
        fallbackEmuiTarget: String? = null
    ): ThemeItem = withContext(Dispatchers.IO) {
        if (!tempFile.exists() || tempFile.length() < 4L) {
            tempFile.delete()
            throw IllegalStateException("فایل دانلودشده خالی یا ناقص است")
        }
        // Verify ZIP header magic bytes (PK\x03\x04) since HWT/HNT are ZIP archives
        val header = ByteArray(4)
        FileInputStream(tempFile).use { it.read(header) }
        val isZipHeader = header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
            (header[2] == 0x03.toByte() || header[2] == 0x05.toByte())
        if (!isZipHeader) {
            tempFile.delete()
            throw IllegalStateException("فایل دانلودشده ساختار استاندارد بسته HWT/ZIP ندارد")
        }

        val safeName = sanitizeHwtFileName(suggestedFileName)
        val destFile = File(themesDir, safeName)
        val baseCanonical = themesDir.canonicalPath + File.separator
        if (!destFile.canonicalPath.startsWith(baseCanonical)) {
            tempFile.delete()
            throw SecurityException("مسیر فایل مقصد معتبر نیست")
        }

        FileInputStream(tempFile).use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        tempFile.delete()

        val parsed = parseHwtFile(destFile, safeName)
        ThemeItem(
            title = parsed.title,
            englishName = parsed.englishName,
            author = if (parsed.author == "طراح ناشناس" && !fallbackAuthor.isNullOrBlank()) fallbackAuthor else parsed.author,
            version = parsed.version,
            emuiTarget = fallbackEmuiTarget?.takeIf { it.isNotBlank() } ?: parsed.emuiTarget,
            fileName = safeName,
            localFilePath = destFile.absolutePath,
            previewImagePath = parsed.previewImagePath,
            drawableResName = if (parsed.previewImagePath == null) "img_theme_mate_nebula" else null,
            accentColorHex = "#24C6DC",
            iconShape = IconShapeStyle.SQUIRCLE.name,
            clockStyle = LockClockStyle.HARMONY_CENTER.name,
            fileSizeBytes = parsed.fileSizeBytes,
            sourceType = ThemeSourceType.ONLINE.name,
            isInstalledToHuawei = false,
            isFavorite = false,
            isApplied = false,
            modulesCsv = parsed.modules.joinToString(","),
            createdAt = System.currentTimeMillis()
        )
    }

    suspend fun inspectHwtFile(file: File, fileName: String): HwtInspectionResult = withContext(Dispatchers.IO) {
        val entries = mutableListOf<String>()
        var descriptionXmlRaw: String? = null
        val modules = linkedSetOf<String>()

        runCatching {
            ZipInputStream(FileInputStream(file)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (!entry.isDirectory && !name.contains("..")) {
                        entries.add(name)
                        detectModuleFromEntry(name, modules)
                        if (name.equals("description.xml", ignoreCase = true)) {
                            descriptionXmlRaw = readBoundedBytes(zis, 256 * 1024).toString(Charsets.UTF_8)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        val xmlMap = descriptionXmlRaw?.let { parseDescriptionXml(it) } ?: emptyMap()
        if (modules.isEmpty()) {
            modules.add("ساختار بسته HWT")
        }

        HwtInspectionResult(
            fileName = fileName,
            totalSizeBytes = file.length(),
            entryCount = entries.size,
            entries = entries.sorted().take(40),
            descriptionXmlRaw = descriptionXmlRaw,
            parsedTitle = xmlMap["title"] ?: xmlMap["title-cn"],
            parsedAuthor = xmlMap["author"] ?: xmlMap["designer"],
            parsedVersion = xmlMap["version"],
            parsedScreen = xmlMap["screen"],
            parsedFont = xmlMap["font"],
            detectedModules = modules.toList(),
            isValidHwtStructure = descriptionXmlRaw != null || entries.any {
                it.contains("wallpaper", ignoreCase = true) || it.contains("preview", ignoreCase = true)
            }
        )
    }

    private fun parseHwtFile(file: File, fileName: String): ParsedHwtMetadata {
        var descriptionXml: String? = null
        var previewPath: String? = null
        var previewPriority = Int.MAX_VALUE
        val modules = linkedSetOf<String>()

        runCatching {
            ZipInputStream(FileInputStream(file)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (!entry.isDirectory && !name.contains("..")) {
                        detectModuleFromEntry(name, modules)
                        if (name.equals("description.xml", ignoreCase = true)) {
                            descriptionXml = readBoundedBytes(zis, 256 * 1024).toString(Charsets.UTF_8)
                        } else if (isImageEntry(name)) {
                            val priority = imagePreviewPriority(name)
                            if (priority < previewPriority) {
                                val bytes = readBoundedBytes(zis, 12 * 1024 * 1024)
                                if (bytes.size in 512..(12 * 1024 * 1024)) {
                                    val outFile = File(previewsDir, "imp_${fileName.hashCode()}_${System.currentTimeMillis() % 10000}.jpg")
                                    val basePreview = previewsDir.canonicalPath + File.separator
                                    if (outFile.canonicalPath.startsWith(basePreview)) {
                                        FileOutputStream(outFile).use { it.write(bytes) }
                                        previewPath = outFile.absolutePath
                                        previewPriority = priority
                                    }
                                }
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        val xmlFields = descriptionXml?.let { parseDescriptionXml(it) } ?: emptyMap()
        val baseName = fileName.substringBeforeLast(".")
        val title = xmlFields["title"]?.takeIf { it.isNotBlank() }
            ?: xmlFields["title-cn"]?.takeIf { it.isNotBlank() }
            ?: baseName
        val author = xmlFields["author"]?.takeIf { it.isNotBlank() }
            ?: xmlFields["designer"]?.takeIf { it.isNotBlank() }
            ?: "طراح ناشناس"
        val version = xmlFields["version"]?.takeIf { it.isNotBlank() } ?: "10.0"
        val osVersion = xmlFields["osversion"]?.takeIf { it.isNotBlank() }
            ?: inferEmuiTargetFromVersion(version)

        if (modules.isEmpty()) {
            modules.add("بسته تم هواوی")
        }

        return ParsedHwtMetadata(
            title = title,
            englishName = baseName,
            author = author,
            version = version,
            emuiTarget = osVersion,
            previewImagePath = previewPath,
            modules = modules.toList(),
            fileSizeBytes = file.length()
        )
    }

    suspend fun copyHwtToHuaweiTree(treeUri: Uri, sourceFile: File, targetFileName: String): Uri =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId)

            // Check if a file with the same name exists and overwrite it cleanly
            var existingDocUri: Uri? = null
            resolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val docId = if (idIdx >= 0) cursor.getString(idIdx) else null
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    if (docId != null && name.equals(targetFileName, ignoreCase = true)) {
                        existingDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        break
                    }
                }
            }

            val destinationUri = existingDocUri ?: DocumentsContract.createDocument(
                resolver,
                parentDocUri,
                "application/octet-stream",
                targetFileName
            ) ?: throw IllegalStateException("امکان ایجاد فایل در پوشه انتخاب‌شده وجود ندارد")

            resolver.openOutputStream(destinationUri, "wt")?.use { out ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(out)
                }
            } ?: throw IllegalStateException("خطا در نوشتن فایل داخل پوشه تم‌های هواوی")

            destinationUri
        }

    suspend fun scanHuaweiTreeForThemeUris(treeUri: Uri): List<Pair<String, Uri>> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val results = mutableListOf<Pair<String, Uri>>()
        val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId)

        resolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val docId = if (idIdx >= 0) cursor.getString(idIdx) else continue
                val name = if (nameIdx >= 0) cursor.getString(nameIdx) else continue
                if (name.endsWith(".hwt", ignoreCase = true) || name.endsWith(".hnt", ignoreCase = true)) {
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    results.add(name to docUri)
                }
            }
        }
        results
    }

    suspend fun exportFileToUri(sourceFile: File, destUri: Uri) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(destUri, "wt")?.use { out ->
            FileInputStream(sourceFile).use { input ->
                input.copyTo(out)
            }
        } ?: throw IllegalStateException("خطا در ذخیره فایل HWT")
    }

    fun getTreeDisplayName(treeUri: Uri): String {
        val docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
        if (!docId.isNullOrBlank()) {
            return docId.substringAfter(":", docId)
        }
        return "Huawei/Themes"
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
    }

    private fun parseDescriptionXml(xmlContent: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        runCatching {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xmlContent.trimStart('\uFEFF')))
            var eventType = parser.eventType
            var currentTag: String? = null
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> currentTag = parser.name?.lowercase()
                    XmlPullParser.TEXT -> {
                        val tag = currentTag
                        val text = parser.text?.trim()
                        if (!tag.isNullOrBlank() && !text.isNullOrBlank() && !result.containsKey(tag)) {
                            result[tag] = text
                        }
                    }
                    XmlPullParser.END_TAG -> currentTag = null
                }
                eventType = parser.next()
            }
        }
        return result
    }

    private fun detectModuleFromEntry(entryName: String, modules: MutableSet<String>) {
        val lower = entryName.lowercase()
        when {
            lower.contains("unlock") || lower.contains("lockscreen") -> modules.add("صفحه قفل")
            lower.contains("wallpaper") -> modules.add("تصویر زمینه")
            lower.contains("icons") -> modules.add("آیکون‌ها")
            lower.contains("systemui") || lower.contains("framework") -> modules.add("کنترل سنتر و رابط کاربری")
            lower.contains("contacts") || lower.contains("telecom") || lower.contains("dialer") -> modules.add("شماره‌گیر و مخاطبین")
            lower.contains("mms") || lower.contains("message") -> modules.add("پیام‌رسان")
            lower.contains("fonts") || lower.endsWith(".ttf") -> modules.add("فونت اختصاصی")
        }
    }

    private fun isImageEntry(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")
    }

    private fun imagePreviewPriority(name: String): Int {
        val lower = name.lowercase()
        return when {
            lower.contains("preview_cover") -> 0
            lower.contains("preview_unlock") -> 1
            lower.contains("home_wallpaper") -> 2
            lower.contains("unlock_wallpaper") -> 3
            lower.contains("preview") -> 4
            lower.contains("wallpaper") -> 5
            else -> 10
        }
    }

    private fun inferEmuiTargetFromVersion(version: String): String {
        return when {
            version.startsWith("14") || version.startsWith("4.") -> "EMUI 14 / HarmonyOS"
            version.startsWith("13") || version.startsWith("3.") -> "EMUI 13 / HarmonyOS"
            version.startsWith("12") -> "EMUI 12"
            version.startsWith("11") -> "EMUI 11"
            else -> "EMUI 10 - 14"
        }
    }

    private fun buildDescriptionXml(
        title: String,
        englishName: String,
        author: String,
        version: String,
        emuiTarget: String
    ): String {
        return """
            <?xml version="1.0" encoding="utf-8"?>
            <HwTheme>
                <title>${escapeXml(title)}</title>
                <title-cn>${escapeXml(englishName)}</title-cn>
                <author>${escapeXml(author)}</author>
                <designer>${escapeXml(author)}</designer>
                <screen>FHD</screen>
                <version>${escapeXml(version)}</version>
                <osversion>${escapeXml(emuiTarget)}</osversion>
                <font>Default</font>
                <font-cn>默认</font-cn>
            </HwTheme>
        """.trimIndent()
    }

    private fun writeZipEntry(zos: ZipOutputStream, name: String, data: ByteArray) {
        val entry = ZipEntry(name)
        zos.putNextEntry(entry)
        zos.write(data)
        zos.closeEntry()
    }

    private fun scaleBitmapForPreview(source: Bitmap, maxSide: Int): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= maxSide && h <= maxSide) return source
        val ratio = maxSide.toFloat() / maxOf(w, h).toFloat()
        return Bitmap.createScaledBitmap(source, (w * ratio).toInt().coerceAtLeast(1), (h * ratio).toInt().coerceAtLeast(1), true)
    }

    private fun sanitizeHwtFileName(name: String): String {
        val clean = name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return when {
            clean.endsWith(".hwt", ignoreCase = true) || clean.endsWith(".hnt", ignoreCase = true) -> clean
            else -> "$clean.hwt"
        }
    }

    private fun escapeXml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun escapeJson(input: String): String {
        return input.replace("\\", "\\\\").replace("\"", "\\\"")
    }

    private fun readBoundedBytes(input: ZipInputStream, maxBytes: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0
        while (total < maxBytes) {
            val toRead = minOf(buf.size, maxBytes - total)
            val read = input.read(buf, 0, toRead)
            if (read == -1) break
            out.write(buf, 0, read)
            total += read
        }
        return out.toByteArray()
    }
}
