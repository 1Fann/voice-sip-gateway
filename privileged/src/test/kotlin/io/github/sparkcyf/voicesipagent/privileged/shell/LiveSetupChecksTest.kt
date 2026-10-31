package io.github.sparkcyf.voicesipagent.privileged.shell

import android.app.AppOpsManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.PowerManager
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

class LiveSetupChecksTest {
    private val pkg = "com.example.gateway"
    private val uid = 12345
    private val packages = mock(PackageManager::class.java)
    private val appOps = mock(AppOpsManager::class.java)
    private val power = mock(PowerManager::class.java)
    private val checks = LiveSetupChecks(packages, appOps, power)

    private fun target() {
        val info = mock(ApplicationInfo::class.java).also { it.uid = uid }
        `when`(packages.getApplicationInfo(pkg, 0)).thenReturn(info)
    }

    @Test fun queriesTargetUidAndCurrentRawModeEveryTime() {
        target()
        `when`(appOps.unsafeCheckOpRawNoThrow("android:manage_ongoing_calls", uid, pkg))
            .thenReturn(AppOpsManager.MODE_ALLOWED, AppOpsManager.MODE_IGNORED)
        assertTrue(checks.ongoingCallsAllowed(pkg) { error("unexpected fallback") })
        assertFalse(checks.ongoingCallsAllowed(pkg) { error("unexpected fallback") })
        verify(packages, times(2)).getApplicationInfo(pkg, 0)
        verify(appOps, times(2)).unsafeCheckOpRawNoThrow("android:manage_ongoing_calls", uid, pkg)
    }

    @Test fun foregroundAndDefaultAreNotPermanentGrants() {
        target()
        `when`(appOps.unsafeCheckOpRawNoThrow("android:manage_ongoing_calls", uid, pkg))
            .thenReturn(AppOpsManager.MODE_FOREGROUND, AppOpsManager.MODE_DEFAULT)
        repeat(2) { assertFalse(checks.ongoingCallsAllowed(pkg) { error("unexpected fallback") }) }
    }

    @Test fun rejectedOrUnavailableFrameworkQueryFallsBackWithoutInventingSuccess() {
        target()
        `when`(appOps.unsafeCheckOpRawNoThrow("android:manage_ongoing_calls", uid, pkg))
            .thenReturn(AppOpsManager.MODE_ERRORED).thenThrow(SecurityException())
        var calls = 0
        assertTrue(checks.ongoingCallsAllowed(pkg) { calls++; true })
        assertFalse(checks.ongoingCallsAllowed(pkg) { calls++; false })
        assertEquals(2, calls)
        assertFalse(LiveSetupChecks(packages, null, null).ongoingCallsAllowed(pkg) { false })
    }

    @Test fun packageLookupFailureDoesNotQueryAnotherUid() {
        `when`(packages.getApplicationInfo(pkg, 0)).thenThrow(PackageManager.NameNotFoundException())
        assertFalse(checks.ongoingCallsAllowed(pkg) { false })
        verifyNoInteractions(appOps)
    }

    @Test fun batteryAllowlistRevocationIsObservedWithoutCachedReadiness() {
        `when`(power.isIgnoringBatteryOptimizations(pkg)).thenReturn(true, false)
        assertTrue(checks.dozeAllowlisted(pkg) { error("unexpected fallback") })
        assertFalse(checks.dozeAllowlisted(pkg) { error("unexpected fallback") })
        verify(power, times(2)).isIgnoringBatteryOptimizations(pkg)
    }

    @Test fun unavailablePowerServiceUsesExistingFallback() {
        `when`(power.isIgnoringBatteryOptimizations(pkg)).thenThrow(SecurityException())
        assertTrue(checks.dozeAllowlisted(pkg) { true })
        assertFalse(LiveSetupChecks(packages, appOps, null).dozeAllowlisted(pkg) { false })
    }
}
