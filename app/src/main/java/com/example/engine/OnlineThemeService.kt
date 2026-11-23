package com.example.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class OnlineThemeRepositoryItem(
    val fullName: String,
    val name: String,
    val ownerLogin: String,
    val ownerAvatarUrl: String?,
    val description: String,
    val stars: Int,
    val defaultBranch: String,
    val updatedAt: String,
    val htmlUrl: String,
    val topics: List<String> = emptyList(),
    val isCustomSource: Boolean = false
)

data class OnlineHwtAsset(
    val id: String,
    val title: String,
    val fileName: String,
    val downloadUrl: String,
    val previewImageUrl: String? = null,
    val author: String = "",
    val emuiTarget: String = "EMUI / HarmonyOS",
    val sizeBytes: Long = 0L,
    val sourceLabel: String = "GitHub"
)

class OnlineThemeService(private val context: Context) {

    companion object {
        private const val USER_AGENT = "HuaweiThemeManager-Android/1.0"
        private const val MAX_JSON_BYTES = 2L * 1024L * 1024L // 2 MB
        private const val MAX_HWT_BYTES = 80L * 1024L * 1024L // 80 MB
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(false)
        .addNetworkInterceptor { chain ->
            validateSafeExternalHttpsUrl(chain.request().url.toString())
            chain.proceed(chain.request())
        }
        .build()

    private val downloadsDir: File
        get() = File(context.cacheDir, "online_downloads").apply { if (!exists()) mkdirs() }

    /**
     * Live search of public GitHub repositories hosting Huawei EMUI / HarmonyOS .hwt / .hnt themes.
     */
    suspend fun searchGithubThemeRepositories(
        query: String,
        categoryQuery: String
    ): Result<List<OnlineThemeRepositoryItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val combined = buildString {
                if (query.isNotBlank()) {
                    append(query.trim())
                    append(" ")
                }
                append(categoryQuery.ifBlank { "huawei emui theme hwt" })
            }.trim()

            val encoded = URLEncoder.encode(combined, "UTF-8")
            val apiUrl = "https://api.github.com/search/repositories?q=$encoded&sort=stars&order=desc&per_page=20"
            val body = executeJsonGet(apiUrl)
            val root = JSONObject(body)
            val items = root.optJSONArray("items") ?: JSONArray()

            buildList {
                for (i in 0 until items.length()) {
                    val obj = items.optJSONObject(i) ?: continue
                    val fullName = obj.optString("full_name").takeIf { it.isNotBlank() } ?: continue
                    val ownerObj = obj.optJSONObject("owner")
                    val topicsArray = obj.optJSONArray("topics")
                    val topics = buildList {
                        if (topicsArray != null) {
                            for (t in 0 until minOf(topicsArray.length(), 4)) {
                                topicsArray.optString(t).takeIf { it.isNotBlank() }?.let { add(it) }
                            }
                        }
                    }
                    add(
                        OnlineThemeRepositoryItem(
                            fullName = fullName,
                            name = obj.optString("name", fullName.substringAfter("/")),
                            ownerLogin = ownerObj?.optString("login") ?: fullName.substringBefore("/"),
                            ownerAvatarUrl = ownerObj?.optString("avatar_url")?.takeIf { it.startsWith("https://") },
                            description = obj.optString("description")
                                .takeIf { it.isNotBlank() && it != "null" }
                                ?: "مخزن عمومی تم‌های هواوی (EMUI / HarmonyOS)",
                            stars = obj.optInt("stargazers_count", 0),
                            defaultBranch = obj.optString("default_branch", "main").ifBlank { "main" },
                            updatedAt = obj.optString("updated_at", "").take(10),
                            htmlUrl = obj.optString("html_url", "https://github.com/$fullName"),
                            topics = topics
                        )
                    )
                }
            }
        }
    }

    /**
     * Fetches repository metadata for a user-saved custom GitHub repo (`owner/repo`).
     */
    suspend fun fetchGithubRepositoryInfo(ownerRepo: String): Result<OnlineThemeRepositoryItem> =
        withContext(Dispatchers.IO) {
            runCatching {
                val clean = normalizeGithubRepoSlug(ownerRepo)
                    ?: throw IllegalArgumentException("فرمت مخزن گیت‌هاب معتبر نیست (مثال: user/repo)")
                val body = executeJsonGet("https://api.github.com/repos/$clean")
                val obj = JSONObject(body)
                val ownerObj = obj.optJSONObject("owner")
                OnlineThemeRepositoryItem(
                    fullName = obj.optString("full_name", clean),
                    name = obj.optString("name", clean.substringAfter("/")),
                    ownerLogin = ownerObj?.optString("login") ?: clean.substringBefore("/"),
                    ownerAvatarUrl = ownerObj?.optString("avatar_url")?.takeIf { it.startsWith("https://") },
                    description = obj.optString("description")
                        .takeIf { it.isNotBlank() && it != "null" }
                        ?: "مخزن آنلاین سفارشی کاربر",
                    stars = obj.optInt("stargazers_count", 0),
                    defaultBranch = obj.optString("default_branch", "main").ifBlank { "main" },
                    updatedAt = obj.optString("updated_at", "").take(10),
                    htmlUrl = obj.optString("html_url", "https://github.com/$clean"),
                    isCustomSource = true
                )
            }
        }

    /**
     * Inspects a GitHub repository's Releases AND Git Tree to find downloadable .hwt / .hnt theme packages
     * along with any preview screenshot image found in the same repository.
     */
    suspend fun fetchGithubRepoHwtAssets(repo: OnlineThemeRepositoryItem): Result<List<OnlineHwtAsset>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val found = linkedMapOf<String, OnlineHwtAsset>()
                var repoPreviewUrl: String? = null

                // 1. Check Releases first (often holds packaged .hwt / .hnt binaries)
                runCatching {
                    val relBody = executeJsonGet("https://api.github.com/repos/${repo.fullName}/releases?per_page=10")
                    val releases = JSONArray(relBody)
                    for (r in 0 until releases.length()) {
                        val rel = releases.optJSONObject(r) ?: continue
                        val assets = rel.optJSONArray("assets") ?: continue
                        for (a in 0 until assets.length()) {
                            val asset = assets.optJSONObject(a) ?: continue
                            val name = asset.optString("name")
                            val downloadUrl = asset.optString("browser_download_url")
                            if (isHwtFileName(name) && isSafeHttpsUrl(downloadUrl)) {
                                found.putIfAbsent(
                                    name.lowercase(),
                                    OnlineHwtAsset(
                                        id = "rel_${repo.fullName}_$name",
                                        title = name.substringBeforeLast(".").replace('_', ' ').replace('-', ' '),
                                        fileName = name,
                                        downloadUrl = downloadUrl,
                                        previewImageUrl = repoPreviewUrl,
                                        author = repo.ownerLogin,
                                        emuiTarget = inferTargetFromName(name + " " + repo.name),
                                        sizeBytes = asset.optLong("size", 0L),
                                        sourceLabel = "GitHub Release"
                                    )
                                )
                            }
                        }
                    }
                }

                // 2. Scan repository tree for committed .hwt / .hnt files and preview screenshots
                runCatching {
                    val branch = URLEncoder.encode(repo.defaultBranch, "UTF-8")
                    val treeBody = executeJsonGet(
                        "https://api.github.com/repos/${repo.fullName}/git/trees/$branch?recursive=1"
                    )
                    val tree = JSONObject(treeBody).optJSONArray("tree") ?: JSONArray()

                    // First pass: find a clean preview image in the repo if any exists
                    for (i in 0 until tree.length()) {
                        val node = tree.optJSONObject(i) ?: continue
                        if (node.optString("type") != "blob") continue
                        val path = node.optString("path")
                        val lower = path.lowercase()
                        if ((lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")) &&
                            (lower.contains("preview") || lower.contains("screen") || lower.contains("cover") || lower.contains("banner"))
                        ) {
                            repoPreviewUrl = buildRawGithubUrl(repo.fullName, repo.defaultBranch, path)
                            break
                        }
                    }

                    // Second pass: collect all .hwt and .hnt blobs
                    for (i in 0 until tree.length()) {
                        val node = tree.optJSONObject(i) ?: continue
                        if (node.optString("type") != "blob") continue
                        val path = node.optString("path")
                        val fileName = path.substringAfterLast("/")
                        if (isHwtFileName(fileName)) {
                            val rawUrl = buildRawGithubUrl(repo.fullName, repo.defaultBranch, path)
                            found.putIfAbsent(
                                fileName.lowercase(),
                                OnlineHwtAsset(
                                    id = "tree_${repo.fullName}_$path",
                                    title = fileName.substringBeforeLast(".").replace('_', ' ').replace('-', ' '),
                                    fileName = fileName,
                                    downloadUrl = rawUrl,
                                    previewImageUrl = repoPreviewUrl,
                                    author = repo.ownerLogin,
                                    emuiTarget = inferTargetFromName("$fileName ${repo.name}"),
                                    sizeBytes = node.optLong("size", 0L),
                                    sourceLabel = "GitHub Repo"
                                )
                            )
                        }
                    }
                }

                found.values.map { asset ->
                    if (asset.previewImageUrl == null && repoPreviewUrl != null) {
                        asset.copy(previewImageUrl = repoPreviewUrl)
                    } else asset
                }
            }
        }

    /**
     * Fetches a custom HTTPS JSON theme catalog (either a top-level array or `{ "themes": [...] }`).
     */
    suspend fun fetchCustomJsonCatalog(catalogUrl: String): Result<List<OnlineHwtAsset>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val normalized = normalizeDirectDownloadUrl(catalogUrl)
                validateSafeExternalHttpsUrl(normalized)
                val body = executeJsonGet(normalized)
                val trimmed = body.trim()
                val array = if (trimmed.startsWith("[")) {
                    JSONArray(trimmed)
                } else {
                    val obj = JSONObject(trimmed)
                    obj.optJSONArray("themes")
                        ?: obj.optJSONArray("items")
                        ?: throw IllegalArgumentException("ساختار JSON کاتالوگ باید شامل لیست themes باشد")
                }

                buildList {
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        val rawUrl = item.optString("downloadUrl")
                            .ifBlank { item.optString("url") }
                            .ifBlank { item.optString("hwtUrl") }
                        val downloadUrl = normalizeDirectDownloadUrl(rawUrl)
                        if (!isSafeHttpsUrl(downloadUrl)) continue

                        val fileName = item.optString("fileName")
                            .ifBlank { downloadUrl.substringAfterLast("/").substringBefore("?") }
                            .ifBlank { "online_theme_$i.hwt" }
                        val preview = item.optString("previewUrl")
                            .ifBlank { item.optString("imageUrl") }
                            .takeIf { isSafeHttpsUrl(it) }

                        add(
                            OnlineHwtAsset(
                                id = "catalog_${catalogUrl.hashCode()}_$i",
                                title = item.optString("title", fileName.substringBeforeLast(".")),
                                fileName = if (isHwtFileName(fileName)) fileName else "$fileName.hwt",
                                downloadUrl = downloadUrl,
                                previewImageUrl = preview,
                                author = item.optString("author", "منبع آنلاین"),
                                emuiTarget = item.optString("emuiTarget", "EMUI / HarmonyOS"),
                                sizeBytes = item.optLong("sizeBytes", 0L),
                                sourceLabel = "کاتالوگ سفارشی"
                            )
                        )
                    }
                }
            }
        }

    /**
     * Streams a real .hwt / .hnt theme package from an HTTPS URL into a confined temp file with progress.
     */
    suspend fun downloadHwtPackage(
        rawUrl: String,
        suggestedFileName: String?,
        onProgress: (Float) -> Unit
    ): Result<Pair<File, String>> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedUrl = normalizeDirectDownloadUrl(rawUrl)
            val parsedUrl = validateSafeExternalHttpsUrl(normalizedUrl)

            val request = Request.Builder()
                .url(parsedUrl)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/octet-stream, application/zip, */*")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("خطای سرور هنگام دانلود (کد ${response.code})")
                }
                val body = response.body ?: throw IOException("پاسخ سرور خالی بود")
                val contentLength = body.contentLength()
                if (contentLength > MAX_HWT_BYTES) {
                    throw IOException("حجم فایل بیش از حد مجاز (۸۰ مگابایت) است")
                }

                val urlName = parsedUrl.pathSegments.lastOrNull()?.takeIf { it.isNotBlank() }
                val finalName = sanitizeDownloadName(suggestedFileName ?: urlName ?: "huawei_theme.hwt")
                val tempFile = File(downloadsDir, "dl_${System.currentTimeMillis()}_$finalName")

                // Path confinement safeguard
                val baseCanonical = downloadsDir.canonicalPath + File.separator
                if (!tempFile.canonicalPath.startsWith(baseCanonical)) {
                    throw SecurityException("مسیر فایل دانلود نامعتبر است")
                }

                var totalRead = 0L
                val buffer = ByteArray(16 * 1024)
                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            totalRead += read
                            if (totalRead > MAX_HWT_BYTES) {
                                tempFile.delete()
                                throw IOException("حجم فایل دانلودشده از سقف ۸۰ مگابایت فراتر رفت")
                            }
                            output.write(buffer, 0, read)
                            if (contentLength > 0L) {
                                onProgress((totalRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f))
                            }
                        }
                    }
                }
                onProgress(1f)
                tempFile to finalName
            }
        }
    }

    fun normalizeGithubRepoSlug(input: String): String? {
        val trimmed = input.trim().removeSuffix("/").removeSuffix(".git")
        val cleaned = when {
            trimmed.startsWith("https://github.com/", ignoreCase = true) ->
                trimmed.removePrefix("https://github.com/").removePrefix("HTTPS://GITHUB.COM/")
            trimmed.startsWith("github.com/", ignoreCase = true) ->
                trimmed.substringAfter("github.com/")
            else -> trimmed
        }
        val parts = cleaned.split("/").filter { it.isNotBlank() }
        if (parts.size < 2) return null
        val owner = parts[0]
        val repo = parts[1]
        val validRegex = Regex("^[A-Za-z0-9._-]+$")
        return if (validRegex.matches(owner) && validRegex.matches(repo)) "$owner/$repo" else null
    }

    fun normalizeDirectDownloadUrl(input: String): String {
        val trimmed = input.trim()
        // Convert GitHub web blob URL to raw file URL so users can paste regular GitHub file links
        val githubBlobRegex = Regex("^https://github\\.com/([^/]+)/([^/]+)/blob/(.+)$")
        val match = githubBlobRegex.matchEntire(trimmed)
        if (match != null) {
            val (owner, repo, rest) = match.destructured
            return "https://raw.githubusercontent.com/$owner/$repo/$rest"
        }
        return trimmed
    }

    private fun executeJsonGet(url: String): String {
        val parsed = validateSafeExternalHttpsUrl(url)
        val request = Request.Builder()
            .url(parsed)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/vnd.github+json, application/json")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 403 || response.code == 429) {
                throw IOException("محدودیت موقت درخواست‌های API گیت‌هاب. چند دقیقه دیگر دوباره تلاش کنید یا لینک مستقیم HWT وارد نمایید.")
            }
            if (!response.isSuccessful) {
                throw IOException("خطا در ارتباط با سرور آنلاین (کد ${response.code})")
            }
            val body = response.body ?: throw IOException("پاسخ سرور خالی بود")
            val contentLength = body.contentLength()
            if (contentLength > MAX_JSON_BYTES) {
                throw IOException("حجم پاسخ سرور بیش از حد مجاز است")
            }
            val bytes = body.byteStream().use { stream ->
                val out = java.io.ByteArrayOutputStream()
                val buf = ByteArray(8192)
                var total = 0L
                while (true) {
                    val r = stream.read(buf)
                    if (r == -1) break
                    total += r
                    if (total > MAX_JSON_BYTES) throw IOException("حجم داده JSON بیش از حد مجاز است")
                    out.write(buf, 0, r)
                }
                out.toByteArray()
            }
            return bytes.toString(Charsets.UTF_8)
        }
    }

    private fun validateSafeExternalHttpsUrl(url: String): HttpUrl {
        val parsed = url.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("آدرس اینترنتی معتبر نیست")
        if (!parsed.isHttps) {
            throw IllegalArgumentException("فقط لینک‌های امن HTTPS پشتیبانی می‌شوند")
        }
        val host = parsed.host.lowercase()
        if (host == "localhost" || host.endsWith(".local") || host.endsWith(".internal")) {
            throw SecurityException("دسترسی به آدرس‌های محلی مجاز نیست")
        }
        val addresses = try {
            InetAddress.getAllByName(host)
        } catch (_: Exception) {
            emptyArray()
        }
        for (addr in addresses) {
            if (addr.isLoopbackAddress || addr.isAnyLocalAddress ||
                addr.isLinkLocalAddress || addr.isSiteLocalAddress || addr.isMulticastAddress
            ) {
                throw SecurityException("دسترسی به شبکه‌های داخلی مجاز نیست")
            }
        }
        return parsed
    }

    private fun isSafeHttpsUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val parsed = url.toHttpUrlOrNull() ?: return false
        return parsed.isHttps && parsed.host.lowercase() != "localhost"
    }

    private fun buildRawGithubUrl(fullName: String, branch: String, filePath: String): String {
        val encodedPath = filePath.split("/").joinToString("/") { segment ->
            URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
        }
        return "https://raw.githubusercontent.com/$fullName/$branch/$encodedPath"
    }

    private fun isHwtFileName(name: String): Boolean {
        return name.endsWith(".hwt", ignoreCase = true) || name.endsWith(".hnt", ignoreCase = true)
    }

    private fun sanitizeDownloadName(name: String): String {
        val clean = name.substringAfterLast("/").replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return when {
            clean.endsWith(".hwt", ignoreCase = true) || clean.endsWith(".hnt", ignoreCase = true) -> clean
            else -> "$clean.hwt"
        }
    }

    private fun inferTargetFromName(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("harmony") -> "HarmonyOS / EMUI 14"
            lower.contains("emui14") || lower.contains("emui 14") -> "EMUI 14"
            lower.contains("emui13") || lower.contains("emui 13") -> "EMUI 13"
            lower.contains("emui12") || lower.contains("emui 12") -> "EMUI 12"
            lower.contains("emui11") || lower.contains("emui 11") -> "EMUI 11"
            lower.contains("emui10") || lower.contains("emui 10") -> "EMUI 10"
            lower.contains("magic") -> "MagicOS / EMUI"
            else -> "EMUI 10 - 14 / HarmonyOS"
        }
    }
}
