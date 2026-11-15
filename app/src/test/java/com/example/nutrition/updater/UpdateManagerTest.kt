package com.example.nutrition.updater

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateManagerTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun parseVersionInfo_success() {
        val payload = """
            {
                "versionCode": 11,
                "versionName": "0.2.3",
                "changelog": "优化启动速度与全新品牌Logo",
                "downloadUrl": "https://github.com/Joshmax010/wedo-fitness/releases/download/v0.2.3/wedo-fitness-v0.2.3.apk",
                "releaseUrl": "https://github.com/Joshmax010/wedo-fitness/releases/tag/v0.2.3"
            }
        """.trimIndent()

        val info = json.decodeFromString<VersionInfo>(payload)
        assertEquals(11, info.versionCode)
        assertEquals("0.2.3", info.versionName)
        assertEquals("优化启动速度与全新品牌Logo", info.changelog)
        assertEquals("https://github.com/Joshmax010/wedo-fitness/releases/download/v0.2.3/wedo-fitness-v0.2.3.apk", info.downloadUrl)
    }

    @Test
    fun isNewer_versionCodeComparison() {
        val helper = UpdateManagerComparisonHelper
        assertTrue(helper.isNewer(remoteCode = 11, remoteName = "0.2.3", currentCode = 10, currentName = "0.2.2"))
        assertFalse(helper.isNewer(remoteCode = 10, remoteName = "0.2.2", currentCode = 10, currentName = "0.2.2"))
        assertFalse(helper.isNewer(remoteCode = 9, remoteName = "0.2.1", currentCode = 10, currentName = "0.2.2"))
    }

    @Test
    fun isNewer_semanticVersionFallback() {
        val helper = UpdateManagerComparisonHelper
        assertTrue(helper.isNewer(remoteCode = 0, remoteName = "0.2.3", currentCode = 10, currentName = "0.2.2"))
        assertTrue(helper.isNewer(remoteCode = 0, remoteName = "v0.3.0", currentCode = 10, currentName = "0.2.2"))
        assertTrue(helper.isNewer(remoteCode = 0, remoteName = "1.0.0", currentCode = 10, currentName = "0.2.2"))
        assertFalse(helper.isNewer(remoteCode = 0, remoteName = "0.2.2", currentCode = 10, currentName = "0.2.2"))
        assertFalse(helper.isNewer(remoteCode = 0, remoteName = "0.2.1", currentCode = 10, currentName = "0.2.2"))
        assertFalse(helper.isNewer(remoteCode = 0, remoteName = "v0.1.9", currentCode = 10, currentName = "0.2.2"))
    }

    @Test
    fun isNewer_handlesPrereleaseAndBuildMetadataTags() {
        val helper = UpdateManagerComparisonHelper
        // Suffixes such as -rc1, -beta, +build should not break numeric version comparison
        assertTrue(helper.isNewer(remoteCode = 0, remoteName = "v0.2.3-rc1", currentCode = 10, currentName = "0.2.2"))
        assertTrue(helper.isNewer(remoteCode = 0, remoteName = "0.2.3-beta.1", currentCode = 10, currentName = "0.2.2"))
        assertTrue(helper.isNewer(remoteCode = 0, remoteName = "0.2.3+11", currentCode = 10, currentName = "0.2.2"))
        assertTrue(helper.isNewer(remoteCode = 0, remoteName = "0.2.2.1", currentCode = 10, currentName = "0.2.2"))
        assertFalse(helper.isNewer(remoteCode = 0, remoteName = "0.2.1-rc2", currentCode = 10, currentName = "0.2.2"))
    }
}
