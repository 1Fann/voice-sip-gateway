package io.github.sparkcyf.voicesipagent.privileged.shell

import android.content.pm.PackageManager
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

class RuntimeGrantsTest {
    @Test fun onlySkipsGrantWhenTheTargetPackageCurrentlyHasPermission() {
        val packages = mock(PackageManager::class.java)
        val grants = RuntimeGrants(packages)
        `when`(packages.checkPermission("phone", "gateway")).thenReturn(PackageManager.PERMISSION_GRANTED)
        `when`(packages.checkPermission("phone", "other")).thenReturn(PackageManager.PERMISSION_DENIED)
        var attempts = 0
        assertFalse(grants.ensure("gateway", "phone") { attempts++ })
        assertTrue(grants.ensure("other", "phone") { attempts++ })
        assertEquals(1, attempts)
        verify(packages).checkPermission("phone", "gateway")
        verify(packages).checkPermission("phone", "other")
    }

    @Test fun revocationAfterAnEarlierSetupIsObservedWithoutCachedReadiness() {
        val packages = mock(PackageManager::class.java)
        `when`(packages.checkPermission("phone", "gateway"))
            .thenReturn(PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_DENIED)
        val grants = RuntimeGrants(packages)
        assertFalse(grants.ensure("gateway", "phone") { fail("already granted") })
        var repaired = false
        assertTrue(grants.ensure("gateway", "phone") { repaired = true })
        assertTrue(repaired)
    }

    @Test fun failedObservationUsesExistingRepairRatherThanAssumingPermission() {
        val packages = mock(PackageManager::class.java)
        `when`(packages.checkPermission("phone", "gateway")).thenThrow(IllegalStateException("unavailable"))
        var repaired = false
        assertTrue(RuntimeGrants(packages).ensure("gateway", "phone") { repaired = true })
        assertTrue(repaired)
    }
}
