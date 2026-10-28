package com.spotify.clone

import com.spotify.clone.core.update.AppUpdateManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateUnitTest {

    @Test
    fun versionComparison_newerPatch() {
        assertTrue(AppUpdateManager.isNewerVersion("1.0.1", "1.0.0"))
    }

    @Test
    fun versionComparison_newerMinor() {
        assertTrue(AppUpdateManager.isNewerVersion("1.1.0", "1.0.9"))
    }

    @Test
    fun versionComparison_newerMajor() {
        assertTrue(AppUpdateManager.isNewerVersion("2.0.0", "1.9.9"))
    }

    @Test
    fun versionComparison_equalVersion() {
        assertFalse(AppUpdateManager.isNewerVersion("1.0.0", "1.0.0"))
    }

    @Test
    fun versionComparison_olderVersion() {
        assertFalse(AppUpdateManager.isNewerVersion("0.9.5", "1.0.0"))
    }

    @Test
    fun versionComparison_withExtraDigits() {
        assertTrue(AppUpdateManager.isNewerVersion("1.0.0.1", "1.0.0"))
        assertFalse(AppUpdateManager.isNewerVersion("1.0.0", "1.0.0.1"))
    }
}
