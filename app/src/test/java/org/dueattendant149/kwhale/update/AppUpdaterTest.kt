package org.dueattendant149.kwhale.update

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.dueattendant149.kwhale.network.model.AppRelease
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdaterTest {

    private fun updater(
        scope: TestScope,
        latest: suspend () -> AppRelease?,
        current: Int = 20,
    ) = AppUpdater(
        fetchLatest = latest,
        downloadApk = { target, _ -> target },
        scope = scope,
        currentVersionCode = { current },
    )

    @Test
    fun manualCheckReportsUpdateWhenServerIsNewer() = runTest {
        val u = updater(this, { AppRelease(versionCode = 25, versionName = "0.1.15") })
        u.checkManual()
        advanceUntilIdle()
        val s = u.state.value
        assertTrue(s is AppUpdater.State.Available)
        assertEquals(25, (s as AppUpdater.State.Available).release.versionCode)
    }

    @Test
    fun manualCheckReportsUpToDateWhenCurrent() = runTest {
        val u = updater(this, { AppRelease(versionCode = 20, versionName = "0.1.14") }, current = 20)
        u.checkManual()
        advanceUntilIdle()
        assertTrue(u.state.value is AppUpdater.State.UpToDate)
    }

    @Test
    fun manualCheckReportsFailure() = runTest {
        val u = updater(this, { throw IOException("нет сети") })
        u.checkManual()
        advanceUntilIdle()
        assertTrue(u.state.value is AppUpdater.State.Failed)
    }

    @Test
    fun startupCheckIsSilentWhenUpToDate() = runTest {
        val u = updater(this, { AppRelease(versionCode = 20) }, current = 20)
        u.checkOnStartup()
        advanceUntilIdle()
        assertEquals(AppUpdater.State.Idle, u.state.value)
    }

    @Test
    fun startupCheckSurfacesNewerVersion() = runTest {
        val u = updater(this, { AppRelease(versionCode = 25) }, current = 20)
        u.checkOnStartup()
        advanceUntilIdle()
        assertTrue(u.state.value is AppUpdater.State.Available)
    }

    @Test
    fun startupCheckDebounces() = runTest {
        var calls = 0
        val u = updater(this, { calls++; AppRelease(versionCode = 25) }, current = 20)
        u.checkOnStartup()
        u.checkOnStartup()
        advanceUntilIdle()
        assertEquals(1, calls)
    }

    @Test
    fun dismissClearsTerminalStates() = runTest {
        val u = updater(this, { throw IOException("нет сети") })
        u.checkManual()
        advanceUntilIdle()
        u.dismiss()
        assertEquals(AppUpdater.State.Idle, u.state.value)
    }
}
