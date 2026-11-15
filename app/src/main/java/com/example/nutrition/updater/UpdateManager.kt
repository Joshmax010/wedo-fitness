package com.example.nutrition.updater

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.example.nutrition.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class VersionInfo(
    @SerialName("versionCode") val versionCode: Int = 0,
    @SerialName("versionName") val versionName: String = "",
    @SerialName("changelog") val changelog: String = "",
    @SerialName("downloadUrl") val downloadUrl: String = "",
    @SerialName("releaseUrl") val releaseUrl: String = ""
)

@Serializable
private data class GitHubRelease(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("name") val name: String? = null,
    @SerialName("body") val body: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("assets") val assets: List<GitHubAsset> = emptyList()
)

@Serializable
private data class GitHubAsset(
    @SerialName("name") val name: String = "",
    @SerialName("browser_download_url") val browserDownloadUrl: String = ""
)

sealed interface UpdateResult {
    data object Disabled : UpdateResult
    data object UpToDate : UpdateResult
    data class NewVersionAvailable(val info: VersionInfo) : UpdateResult
    data class Error(val message: String) : UpdateResult
}

class UpdateManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("app_update", Context.MODE_PRIVATE)
    private val checkMutex = Mutex()

    var autoCheckEnabled: Boolean by mutableStateOf(prefs.getBoolean(KEY_AUTO_CHECK, true))
        private set

    var updateAvailable: Boolean by mutableStateOf(false)
        private set

    var latestInfo: VersionInfo? by mutableStateOf(null)
        private set

    var isChecking: Boolean by mutableStateOf(false)
        private set

    var lastCheckResult: UpdateResult? by mutableStateOf(null)
        private set

    fun setAutoCheck(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_CHECK, enabled) }
        autoCheckEnabled = enabled
        if (!enabled) {
            updateAvailable = false
            latestInfo = null
            lastCheckResult = null
        }
    }

    suspend fun checkUpdate(force: Boolean = false): UpdateResult = checkMutex.withLock {
        if (!force && !autoCheckEnabled) {
            return UpdateResult.Disabled
        }

        isChecking = true
        return withContext(Dispatchers.IO) {
            try {
                // Node 1: GitHub Pages version.json
                val infoFromPages = probePrimary()
                val candidate = infoFromPages ?: probeGitHubApi()

                val result = if (candidate == null) {
                    UpdateResult.Error("无法获取更新信息")
                } else if (isNewer(candidate)) {
                    latestInfo = candidate
                    updateAvailable = true
                    UpdateResult.NewVersionAvailable(candidate)
                } else {
                    updateAvailable = false
                    UpdateResult.UpToDate
                }

                lastCheckResult = result
                result
            } catch (c: CancellationException) {
                throw c
            } catch (_: Exception) {
                val errorResult = UpdateResult.Error("检测更新失败，请稍后重试")
                lastCheckResult = errorResult
                errorResult
            } finally {
                isChecking = false
            }
        }
    }

    private fun probePrimary(): VersionInfo? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(PRIMARY_URL)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                jsonParser.decodeFromString<VersionInfo>(jsonStr)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun probeGitHubApi(): VersionInfo? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(FALLBACK_URL)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/vnd.github.v3+json")
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val release = jsonParser.decodeFromString<GitHubRelease>(jsonStr)
                val rawTag = release.tagName.removePrefix("v").removePrefix("V").trim()
                val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                val downloadUrl = apkAsset?.browserDownloadUrl ?: release.htmlUrl.orEmpty()
                VersionInfo(
                    versionCode = 0,
                    versionName = rawTag,
                    changelog = release.body.orEmpty(),
                    downloadUrl = downloadUrl,
                    releaseUrl = release.htmlUrl ?: "https://github.com/Joshmax010/wedo-fitness/releases/latest"
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    fun isNewer(remote: VersionInfo): Boolean {
        return UpdateManagerComparisonHelper.isNewer(
            remoteCode = remote.versionCode,
            remoteName = remote.versionName,
            currentCode = BuildConfig.VERSION_CODE,
            currentName = BuildConfig.VERSION_NAME
        )
    }

    companion object {
        private const val KEY_AUTO_CHECK = "auto_check_update"
        private const val PRIMARY_URL = "https://joshmax010.github.io/wedo-fitness/version.json"
        private const val FALLBACK_URL = "https://api.github.com/repos/Joshmax010/wedo-fitness/releases/latest"
        private const val TIMEOUT_MS = 2500
        private const val USER_AGENT = "WeDo-Fitness-Android"

        private val jsonParser = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        @Volatile
        private var instance: UpdateManager? = null

        fun getInstance(context: Context): UpdateManager {
            return instance ?: synchronized(this) {
                instance ?: UpdateManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

object UpdateManagerComparisonHelper {
    fun isNewer(
        remoteCode: Int,
        remoteName: String,
        currentCode: Int,
        currentName: String
    ): Boolean {
        if (remoteCode > 0 && currentCode > 0) {
            return remoteCode > currentCode
        }
        val remoteVer = remoteName.removePrefix("v").removePrefix("V").trim()
        val currentVer = currentName.removePrefix("v").removePrefix("V").trim()
        return compareVersions(remoteVer, currentVer) > 0
    }

    fun compareVersions(v1: String, v2: String): Int {
        val parts1 = parseVersionParts(v1)
        val parts2 = parseVersionParts(v2)
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1.compareTo(p2)
        }
        return 0
    }

    private fun parseVersionParts(version: String): List<Int> {
        val core = version.substringBefore('-').substringBefore('+').trim()
        return core.split('.').mapNotNull { part ->
            part.takeWhile { it.isDigit() }.toIntOrNull()
        }
    }
}
